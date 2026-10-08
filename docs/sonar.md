# SonarCloud

Static analysis and a quality gate on every pull request, next to the
JaCoCo floor that `./mvnw verify` already enforces. The scan runs in
`.github/workflows/sonar.yml`; the project settings are properties in
`pom.xml`.

## What the gate checks

SonarCloud's default "Sonar way" gate looks only at **new code** (the diff of
the PR, or everything since the previous analysis on `main`):

| Condition | Threshold |
|---|---|
| Coverage on new code | ≥ 80 % |
| Duplicated lines on new code | ≤ 3 % |
| New bugs, vulnerabilities, security hotspots | 0 unreviewed |
| Maintainability rating on new code | A |

The workflow runs `sonar:sonar` with `sonar.qualitygate.wait=true`, so a
failed gate fails the **Sonar / analysis and quality gate** check on the PR.
Coverage comes from `target/site/jacoco/jacoco.xml`, produced by the same
`verify` that runs the tests, ArchUnit rules and the JaCoCo floor; the two
gates agree on what counts (the exclusions in `pom.xml` are the same).

## One-time setup (repository owner)

1. Sign in at <https://sonarcloud.io> with GitHub and create the organization
   `erykszczesniak` (free for public repositories).
2. **Analyze new project** → pick `erykszczesniak/Stride`. The project key
   must be `erykszczesniak_Stride` (it is what `pom.xml` declares).
3. In the project: **Administration → Analysis Method**, turn **Automatic
   Analysis off** (the CI scan uploads coverage; automatic analysis cannot,
   and the two must not run together).
4. **My Account → Security → Generate token**, then add it to the repository
   as the `SONAR_TOKEN` secret (**Settings → Secrets and variables →
   Actions**).
5. Push any commit, or re-run the Sonar workflow on an open PR. The badges in
   `README.md` turn live once the first analysis is in.

Without the secret the workflow's first job reports "SONAR_TOKEN not
available" and the scan job is skipped, so pull requests from forks and
Dependabot stay green.

## Branches

The `main` and `microservices` branches are different products with different
module layouts, so each has its own SonarCloud project:

| Branch | Project key |
|---|---|
| `main` | `erykszczesniak_Stride` |
| `microservices` | `erykszczesniak_Stride_microservices` |

SonarCloud's own branch analysis would put both under one project and
compare the microservices reactor against the monolith as "new code", which
is not a meaningful diff.

## Local run

Not needed for the gate; the workflow is the source of truth. To reproduce a
finding locally:

```bash
export SONAR_TOKEN=...   # your own token from sonarcloud.io
./mvnw verify sonar:sonar
```

The scanner prints the dashboard URL at the end.
