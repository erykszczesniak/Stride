# AWS deployment slice (Terraform)

Skeleton for a free-tier-eligible deployment of a subset of the Stride
stack to AWS. Not auto-deployed — intended as a portfolio piece showing
infrastructure-as-code chops, and as a recipe for someone who wants to
actually spin it up for a live demo.

## What it provisions

| Resource                                          | Free tier        | Used for                            |
|---------------------------------------------------|------------------|-------------------------------------|
| RDS PostgreSQL db.t3.micro × 1                    | 750h/mo, 12 mo   | user-service database                |
| S3 bucket                                         | 5 GB always free | activity uploads (GPX/photos)        |
| ECR private repo × 10                             | 500 MB always free | one per service image              |
| EC2 t3.micro × 1                                  | 750h/mo, 12 mo   | "demo slice" running 2-3 JVMs        |
| SSM Parameter Store × 3                           | always free      | JWT/DB/Groq secrets                  |

Estimated cost during free tier: **~$0/mo** if you stay within limits.
After 12 months: ~$13/mo (RDS instance) + bandwidth.

## What's deliberately missing

| Wanted                | Why not                                                    | Workaround                  |
|-----------------------|------------------------------------------------------------|-----------------------------|
| EKS (managed K8s)     | Control plane is $0.10/h = $73/mo, not free                | Use the EC2 + docker-compose slice, or run K8s locally on kind |
| Application Load Balancer | $0.0252/h + traffic, not free                          | nginx on the EC2 instance routes to api-gateway |
| Multi-AZ RDS          | Doubles cost                                               | Single AZ in dev/demo, multi-AZ in real prod |
| Custom VPC + NAT GW   | NAT gateway is $0.045/h = $32/mo                           | Default VPC, public-subnet EC2 |
| Secrets Manager       | $0.40/secret/month                                         | Parameter Store SecureString (free) |
| CloudWatch dashboards | First 3 free, then $3 each                                 | Self-hosted Grafana on EC2  |

## Local kind setup vs. AWS slice

These two modes are **complementary, not alternatives**:

- **kind** runs the FULL stack (all 7 services + Kafka + Redis + per-
  service Postgres) on a single laptop. Great for development, full
  smoke tests, recording demo videos. Free.
- **AWS slice** runs a SUBSET (gateway + user-service + activity-service,
  3 JVMs in 1 GiB RAM with `-Xmx256m`) on real cloud infrastructure with
  a real public hostname. Great for the resume link. Free for 12 months.

The full architecture lives in code regardless of where it runs. The
AWS slice is "evidence I can deploy to AWS"; it's not the production
deployment target for the imaginary Stride startup.

## Run it

```bash
cd terraform/aws

# 1. Initialize the provider plugins.
terraform init

# 2. Generate a plan (will prompt for db_password and jwt_secret unless
#    you set them via TF_VAR_db_password=... TF_VAR_jwt_secret=...).
terraform plan -out=stride.tfplan

# 3. Apply — creates ~15 resources, takes ~5 min (RDS is the slow one).
terraform apply stride.tfplan

# 4. Outputs include the RDS endpoint and EC2 DNS. SSH into EC2 and
#    docker-compose up the subset of services you want online.

# 5. Tear it down before the free tier window closes.
terraform destroy
```

## Image push workflow

```bash
# Authenticate once per session.
aws ecr get-login-password --region eu-central-1 \
  | docker login --username AWS --password-stdin \
      <account>.dkr.ecr.eu-central-1.amazonaws.com

# Per service:
docker tag stride/user-service:dev \
  <account>.dkr.ecr.eu-central-1.amazonaws.com/stride/user-service:latest
docker push \
  <account>.dkr.ecr.eu-central-1.amazonaws.com/stride/user-service:latest
```

A `Makefile` target for "push all images" would be a nice follow-up.
