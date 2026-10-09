# Security policy

How security issues in this repository are reported, triaged and fixed.

## Supported versions

| Branch / tag | Supported |
|---|---|
| `microservices` | yes, always |
| latest `vX.Y.Z` release tag | yes, until the next release |
| older tags | no, upgrade to the latest release |

## Reporting a vulnerability

- Do not open a public issue for anything that could be exploited.
- Use GitHub's private vulnerability reporting: **Security → Report a
  vulnerability** on this repository. The report is visible only to the
  maintainer until a fix is published.
- Include the affected area (service, endpoint, dependency or image), steps
  to reproduce, the impact you expect and, if you have one, a suggested fix.

## Triage and response times

| Severity | Acknowledge | Fix target |
|---|---|---|
| Critical (remote code execution, auth bypass, data exposure) | 1 business day | start immediately, release as soon as the fix is verified |
| High | 2 business days | next release |
| Medium / Low | triaged into the regular backlog | scheduled with normal work |

Severity follows CVSS where a score exists; otherwise the impact on user
data and the gateway's authentication decides.

## Dependencies and images

- Dependabot opens grouped weekly PRs for Maven, npm, Docker base images and
  GitHub Actions (`.github/dependabot.yml`); security updates are opened as
  soon as an advisory is published.
- Every PR runs CodeQL and a Trivy filesystem scan; the gateway image is
  built and scanned as the representative JVM image (`.github/workflows/security-scan.yml`).
  Fixable CRITICAL findings fail the job.
- GitHub Actions are pinned to full commit SHAs with the version in a
  comment, so a moved tag cannot change what runs in CI.
- Prefer the nearest patched version over a major upgrade when fixing a
  single advisory; run `./mvnw verify` before merging.

## Secure development baseline

- Secrets only through environment variables; nothing sensitive in the
  repository, in sample files or in test fixtures. CI fails on committed
  secrets found by the scanners above.
- Identity is decided once, at the API gateway (JWT validation and the
  `X-User-*` headers); domain services trust the gateway and never read
  tokens themselves.
- Login and token endpoints are rate limited; keep the limits when changing
  the auth flow.
- A fix for a reported vulnerability ships with a regression test and a
  `CHANGELOG.md` entry under **Security**.
