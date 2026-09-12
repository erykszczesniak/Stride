# AGENTS.md — working in this repository

Instructions for coding agents (Claude Code, Copilot, Codex) and humans alike.
Keep it factual; if something here stops being true, fix it in the same PR.

## What this repo is

Stride, `microservices` branch: a Strava-like fitness app split into 12 Spring
Boot services (Java 21 + Kotlin 1.9) behind an API gateway, with Kafka events,
gRPC inside the cluster, one Postgres per service, and an observability stack
(Prometheus, Alertmanager, Grafana, Jaeger). `main` holds the same product as a
monolith. Product behavior must stay identical between the two branches.

## Map

| Path | What lives there |
|---|---|
| `infrastructure/{api-gateway,config-server,discovery-server}` | Edge + platform. Gateway validates JWT and forwards `X-User-*`; Config Server serves `src/main/resources/config/*.yml` |
| `services/<name>` | Domain services. Each owns its schema (`src/main/resources/db/migration/common` + `h2` flavor for tests) |
| `shared/{events,security,grpc-contracts}` | Kafka event records, identity-header filter, proto contracts |
| `monitoring/` | Prometheus config, **alert rules + rule unit tests**, Alertmanager, Grafana dashboards (provisioned from JSON) |
| `docs/` | architecture, saga walkthrough, observability, SLOs, ADRs, runbooks, post-mortems |
| `k8s/`, `terraform/aws/` | Manifests for kind/prod-shaped deploys; free-tier AWS slice |
| `frontend/` | React SPA (CRA) + Playwright e2e in `frontend/e2e` |

## Commands

```bash
./mvnw -q verify                                   # full reactor, ~900 tests, JaCoCo gates
./mvnw -q -pl services/ai-service -am verify       # one module + what it needs
./mvnw -q -pl services/activity-service test -Dtest='*ArchitectureTest'
cd frontend && npm ci && CI=true npm test -- --watchAll=false
docker compose up -d                               # lean core (no observability/search)
docker compose --profile observability --profile search up -d
docker run --rm -v "$PWD/monitoring:/m" --entrypoint promtool prom/prometheus:v2.55.0 check rules /m/alerts/*.yml
docker run --rm -v "$PWD/monitoring:/m" --entrypoint promtool prom/prometheus:v2.55.0 test rules /m/alerts/tests/*.yml
cd frontend && npm run e2e                         # needs the composed stack on :8080 and the SPA on :3000
```

Standalone profile boots any service alone with H2 and mocks:
`SPRING_PROFILES_ACTIVE=standalone java -jar services/<svc>/target/<svc>.jar`.

## Invariants — do not break these

1. **No cross-service database access.** Cross-service references are logical ids; joins happen in BFFs or read models, never in SQL across schemas.
2. **REST only at the edge, gRPC inside.** Public contract is `/api/v1`, `/graphql`, `/pipeline/v1` through the gateway. Service-to-service calls use `shared/grpc-contracts` with per-call deadlines and Resilience4j.
3. **Identity comes from the gateway.** Services trust `X-User-Id/Email/Roles` only because the gateway strips inbound copies. Never read JWTs in a domain service.
4. **Events are at-least-once.** Consumers must be idempotent (see `docs/saga.md`).
5. **Every alert has an owner and a runbook.** A rule in `monitoring/alerts/` needs `severity`, `team`, `service` labels and a `runbook_url` annotation, and a case in `monitoring/alerts/tests/`. CI rejects rules without them.
6. **Public contract parity with `main`.** If you change a DTO or path, the same change lands on `main`.

## Conventions

- Branch names: `feat/ms-*`, `fix/ms-*`, `perf/ms-*`, `docs/ms-*`, `ci/ms-*`, `chore/ms-*`.
- PR title = Conventional Commit (`feat(ai): ...`). Squash merge with the title as the commit subject; release-please reads it.
- PR body follows `.github/PULL_REQUEST_TEMPLATE.md`; the **Verification** section must state what was actually run.
- Tests: JUnit 5 + AssertJ; Mockito (`mockito-kotlin` in Kotlin modules). Web layer tests use `@WebMvcTest` with `HeaderAuthenticationFilter`; repository tests run on H2 in PostgreSQL mode with the real Flyway migrations. Postgres-only behavior gets a Testcontainers test (tag `integration`).
- Kotlin modules (notification-service, ai-service): constructor injection, `internal` for test seams, no Lombok.
- Config: dev defaults live in each service's `application.yml`; prod-shaped overrides in `infrastructure/config-server/.../config/<service>.yml`. Secrets only via env vars.
- Flyway: never edit an applied migration; add `V<n>__<snake_case>.sql` under `db/migration/common` (and `db/migration/h2` when the SQL is vendor-specific).

## Definition of done

Code + tests + docs in one PR. For anything user-visible: `CHANGELOG.md` entry
under `Unreleased`. For anything that can page someone: rule, unit test for the
rule, runbook. For a design decision with alternatives: an ADR in `docs/adr/`.
