# ADR-011 — SonarCloud quality gate on every pull request

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-10-08 |
| **Deciders** | platform |

## Context

The build already has a coverage floor (JaCoCo in `verify`), architecture
rules (ArchUnit), CodeQL and Trivy. What it did not have is a gate on the
*quality of new code*: duplicated blocks, code smells that grow into bugs,
security hotspots that CodeQL's query set does not cover, and a coverage
condition that looks at the lines a PR adds rather than at the whole module.
A reviewer had to spot those by hand.

## Decision

1. **SonarCloud, scanned from CI, not automatic analysis.** The Sonar
   workflow runs `./mvnw verify sonar:sonar` so the analysis sees the
   JaCoCo XML report; SonarCloud's automatic analysis cannot ingest coverage.
2. **The gate blocks the PR.** `sonar.qualitygate.wait=true` turns a failed
   gate into a failed check. The default "Sonar way" gate is used as is:
   conditions on new code only, so legacy code does not block work and the
   bar is the same for every PR.
3. **Skip, never fail, without a token.** A first job checks whether
   `SONAR_TOKEN` is reachable; forks, Dependabot and a fresh clone of the
   repository skip the scan. A red check must always mean something about
   the code.
4. **One project per branch.** `main` and `microservices` get separate
   project keys; their modules and layouts are too different for "new code"
   to mean anything across them.
5. **Settings in `pom.xml`, secret in GitHub.** Project key, organization and
   report paths are versioned next to the JaCoCo configuration they depend
   on; only the token is a secret.

## Alternatives considered

- **JaCoCo floor only.** Already there and kept; it says nothing about new
  code, duplication or smells, and it cannot show a reviewer *which* lines a
  PR left uncovered.
- **Self-hosted SonarQube.** The same engine, but a server to run, back up
  and upgrade for a repository with one maintainer. SonarCloud is free for
  public repositories and needs nothing to operate.
- **SonarCloud automatic analysis.** Zero CI changes, but no coverage, no
  control over when it runs, and it conflicts with CI-based analysis.

## Consequences

- One more required check per PR, roughly the length of the reactor `verify` build.
- The repository owner has to create the SonarCloud project and the
  `SONAR_TOKEN` secret once (`docs/sonar.md`); until then the check is a
  skip, not a pass, which the PR list makes visible.
- Exclusions must stay in sync between JaCoCo and Sonar, which is why both
  live in the same `<properties>` block.

## Known gaps

- The React frontend is not analysed; the Maven scanner only sees the
  reactor. A second project with the JavaScript scanner is the obvious next
  step once the frontend has coverage output in CI.
