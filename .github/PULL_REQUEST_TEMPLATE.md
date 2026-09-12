<!-- Title: Conventional Commits, e.g. `feat(activity): ...`, `fix(gateway): ...`, `ci: ...`, `docs: ...`.
     The squash-merge commit reuses the title, and release-please builds CHANGELOG + tags from it. -->

## What / why

<!-- One paragraph. What was broken or missing, and what this PR changes. -->

## Changes

- 

## Verification

<!-- Mandatory. Say what you ran and what you saw, not what should happen.
     e.g. `./mvnw -pl services/ai-service verify` → 71 tests green;
          `docker compose --profile observability up -d` → alert fires in Alertmanager UI within 2 min. -->

- 

## Operational impact

<!-- Delete rows that don't apply. -->

| Surface | Touched? | Notes |
|---|---|---|
| Public API contract (`/api/v1`, `/graphql`, `/pipeline/v1`) | no | |
| Kafka events / gRPC contracts | no | |
| DB migration (Flyway) | no | |
| Alert rules / SLOs / dashboards | no | |
| Runbooks | no | |
| Config Server values / env vars | no | |

## Checklist

- [ ] Tests added or updated for the behavior in this PR
- [ ] `CHANGELOG.md` → `Unreleased` updated (skip for pure refactors / CI noise)
- [ ] Docs touched where the behavior is documented (`docs/`, `README.md`, runbook)
- [ ] No secrets, tokens, or real user data in the diff
