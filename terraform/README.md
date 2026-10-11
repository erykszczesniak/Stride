# AWS slice

The free-tier shaped deployment of the monolith: an application load
balancer, one EC2 host running the two containers with Docker Compose, RDS
PostgreSQL, two ECR repositories, secrets in SSM Parameter Store. The root
[`README.md`](../README.md#aws-deployment-with-terraform) has the apply
walkthrough; this file is about how the configuration is kept correct.

| File | What it declares |
|---|---|
| `main.tf`, `variables.tf`, `locals.tf`, `outputs.tf` | provider pin, inputs (`environment` becomes a tag), the common tags, outputs |
| `vpc.tf`, `security-groups.tf` | VPC with two public subnets; ALB ← internet (80/443), EC2 ← ALB only, RDS ← EC2 only |
| `alb.tf` | ALB, target group, HTTP listener (redirects to HTTPS when `domain_name` is set, forwards otherwise), optional ACM certificate + HTTPS listener |
| `ec2.tf`, `scripts/user-data.sh` | the host (IMDSv2, encrypted root volume), boots Compose from ECR with secrets read from SSM at start |
| `rds.tf` | PostgreSQL 16, private, encrypted, 7-day backups, final snapshot |
| `ecr.tf`, `iam.tf`, `ssm.tf` | immutable, scan-on-push repositories; least-privilege instance role; `SecureString` parameters |
| `tests/*.tftest.hcl` | the guard rails below, run against a mocked provider |
| `.tflint.hcl`, `.trivyignore.yaml` | linter config (AWS ruleset pinned) and the accepted scanner findings, each with its reason |

## Quality gates

`.github/workflows/terraform.yml` runs on every pull request that touches
`terraform/` and fails the PR when any gate fails. Nothing here needs AWS
credentials.

| Gate | What it catches | Tool |
|---|---|---|
| `terraform fmt -check -recursive` | formatting drift | Terraform 1.9.8 |
| `terraform init -backend=false` + `validate` | syntax, types, unknown attributes, missing variables | Terraform 1.9.8 |
| `tflint` | provider-specific mistakes: invalid instance types, deprecated arguments, unused declarations (`terraform` preset `recommended` + `aws` ruleset 0.33.0) | tflint v0.53.0 |
| `trivy config` | misconfigurations at HIGH/CRITICAL: unencrypted storage, open security groups, IMDSv1, plain HTTP … | Trivy (`aquasecurity/trivy-action`) |
| `terraform test` | the behaviour we promise, see below | Terraform 1.9.8, `mock_provider "aws"` |

What `tests/` asserts (8 runs, 3 files):

- **`security.tftest.hcl`** — no security group opens port 22; port 5432 is
  never open to `0.0.0.0/0`; RDS accepts connections only from the EC2
  security group and has no egress; EC2 accepts traffic only from the ALB
  group; the ALB exposes only 80 and 443. RDS is not publicly accessible,
  encrypted, keeps 7 days of backups and takes a final snapshot. The host
  requires IMDSv2 and has an encrypted root volume. Both ECR repositories
  use immutable tags and scan on push.
- **`tags.tftest.hcl`** — every taggable resource (25 of them, listed
  explicitly) carries `Project`, `Environment` and `Name`; `environment`
  only accepts `dev`, `staging`, `prod`.
- **`alb.tftest.hcl`** — with a domain, HTTP redirects to HTTPS with a 301
  and there is one TLS 1.3 listener with a DNS-validated certificate; without
  a domain, HTTP forwards and there is no HTTPS listener or certificate. The
  ALB drops invalid header fields.

Mocked provider means: Terraform plans or applies against an in-memory
provider that returns made-up values for computed attributes. Everything
declared in the `.tf` files is real; nothing is created anywhere. One run
uses `command = apply` because a security group's egress set is only known
after apply.

### Run the gates locally

Same versions as CI, through containers (nothing to install):

```bash
# from the repository root
tf() { docker run --rm -v "$PWD/terraform:/t" -w /t hashicorp/terraform:1.9.8 "$@"; }
tf fmt -check -recursive -diff
tf init -backend=false -input=false
tf validate
tf test

docker run --rm -v "$PWD/terraform:/data" -w /data -e TFLINT_PLUGIN_DIR=/data/.tflint.d \
  ghcr.io/terraform-linters/tflint:v0.53.0 --init
docker run --rm -v "$PWD/terraform:/data" -w /data -e TFLINT_PLUGIN_DIR=/data/.tflint.d \
  ghcr.io/terraform-linters/tflint:v0.53.0 --format compact

trivy config --severity HIGH,CRITICAL --exit-code 1 --ignorefile terraform/.trivyignore.yaml terraform/
# or: docker run --rm -v "$PWD/terraform:/t" aquasec/trivy:0.65.0 config --severity HIGH,CRITICAL --exit-code 1 --ignorefile /t/.trivyignore.yaml /t
```

`terraform fmt -recursive` (without `-check`) fixes formatting in place.
`.terraform/`, `.tflint.d/` and `*.tfvars` are git-ignored;
`.terraform.lock.hcl` is committed with checksums for linux and macOS on
amd64 and arm64 (`terraform providers lock -platform=…`).

### Accepted scanner findings

`.trivyignore.yaml` lists four findings, each scoped to a file with a reason:
the ALB is public on purpose; the HTTP listener exists (it redirects when a
domain is configured, and HTTP-only is the explicit default without a
certificate); the EC2 host has unrestricted egress because the free tier
has no NAT gateway or VPC endpoints; the subnets assign public IPs for the
same reason. Anything else at HIGH or CRITICAL fails the build. Add to the
file only with a reason, and remove the entry when the design changes.

## No manual changes

Every change to this infrastructure goes through a pull request: the gates
above run, a reviewer reads the plan reasoning, and the merge is the record.
Nothing is edited in the console or applied from a laptop with uncommitted
changes. Drift detection exists to catch the exceptions, not to legitimise
them.

### Drift detection

The `drift` job in the same workflow runs nightly (03:15 UTC) and on
`workflow_dispatch`. It is a no-op until the repository has:

| Setting | Kind | Purpose |
|---|---|---|
| `AWS_ROLE_TO_ASSUME` **or** `AWS_ACCESS_KEY_ID` + `AWS_SECRET_ACCESS_KEY` | secret | credentials; the OIDC role is preferred (`id-token: write` is already granted to the job) |
| `TF_STATE_BUCKET` (+ optional `TF_STATE_KEY`, `AWS_REGION`) | variable | the S3 bucket holding the state. The checked-in configuration keeps local state so `terraform init` works on a laptop; the job adds a `backend "s3"` override file on the runner only |
| `TF_VAR_DB_PASSWORD`, `TF_VAR_JWT_SECRET` (+ optional `TF_VAR_GROQ_API_KEY`, `TF_VAR_GOOGLE_CLIENT_ID`, `TF_VAR_GOOGLE_CLIENT_SECRET`) | secret | the sensitive variables; with wrong values the SSM parameters show as drift |
| `TF_VAR_DOMAIN_NAME`, `TF_VAR_ENVIRONMENT` | variable | must match what was applied (`environment` defaults to `prod` in the job) |

Without credentials or a bucket the job writes `skipped: no credentials
configured` (or `no state bucket configured`) to the step summary and
passes. With them it runs `terraform plan -detailed-exitcode -lock=false`:
exit 0 is reported as "no drift", exit 2 opens an issue labelled `drift`
(or updates the open one and comments) with the `Plan:` line and the
resource list, and fails the run. Any other exit code fails the run with
the plan output in the log.

Next step, not done: posting the plan as a comment on pull requests. It
needs the same credentials and state access as the drift job, so it is
worth adding at the moment those are configured.
