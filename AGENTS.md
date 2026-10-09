# AGENTS.md — working in this repository

Instructions for coding agents (Claude Code, Copilot, Codex) and humans alike.
Keep it factual; if something here stops being true, fix it in the same PR.

## What this repo is

Stride, `main` branch: a Strava-like fitness app as one Spring Boot 3.4
monolith (Java 21 with a little Kotlin), a React SPA, an iOS client talking to
`/pipeline/v1`, Postgres + Flyway, Elasticsearch for user search, Anthropic
(activity chat) and Groq (goal/plan generation), and an observability stack
(Prometheus, Alertmanager, Grafana, Jaeger). The `microservices` branch is the
same product split into 12 services; product behaviour must stay identical
between the two branches.

## Map

| Path | What lives there |
|---|---|
| `src/main/java/com/strava/backend/controller` | REST controllers (`BaseController.getAuthenticatedUserId()`), paths in `constants/PathConstants` |
| `.../service` + `.../service/impl` | interfaces and implementations; AI: `AiChatService`, `ClaudeService`, `GroqService` |
| `.../domain` · `.../repository` | JPA entities (only here) and Spring Data repositories |
| `.../security` · `.../configuration` | JWT filter, `SecurityConfiguration`, properties classes |
| `.../event` · `.../search` · `.../service/projection` | Spring application events, Elasticsearch sync, feed projection |
| `src/main/resources/db/migration` | Flyway, append-only |
| `src/test/java/.../integration` | Testcontainers PostgreSQL (`AbstractIntegrationTest`, profile `testcontainers`) |
| `monitoring/` | Prometheus config, **alert rules + rule unit tests**, Alertmanager, Grafana dashboards |
| `docs/` | API/GraphQL docs, SLOs, ADRs, runbooks, post-mortems, AI guardrails, cost attribution |
| `frontend/` | React SPA (CRA) + Playwright e2e in `frontend/e2e` |
| `k8s/`, `terraform/` | manifests and the AWS slice |

## Commands

```bash
./mvnw -q verify                                   # unit + integration tests, ArchUnit, JaCoCo gate
./mvnw -q test -Dtest='ArchitectureTest'           # architecture rules only
cd frontend && npm ci && CI=true npm test -- --watchAll=false
make up                                            # docker compose: postgres, backend, frontend, jaeger, prometheus, grafana
docker compose --profile observability up -d       # + alertmanager, alert-sink
docker run --rm -v "$PWD/monitoring:/m" --entrypoint promtool prom/prometheus:v2.55.0 check rules /m/alerts/*.yml
docker run --rm -v "$PWD/monitoring:/m" --entrypoint promtool prom/prometheus:v2.55.0 test rules /m/alerts/tests/*.yml
scripts/smoke.sh                                   # post-deploy check against the running stack
cd frontend && npm run e2e                         # needs the stack on :8081 and the SPA on :3000
```

## Invariants — do not break these

1. **Entities live in `domain`, persistence goes through `repository`.** Controllers call services; the three controllers that still touch repositories directly are frozen in `src/test/resources/archunit_store` — do not add a fourth.
2. **Identity comes from the security filter.** Controllers read the user via `BaseController.getAuthenticatedUserId()`; services take the id as a parameter, never the request.
3. **Flyway is append-only.** New `V<n>__*.sql`, never an edited one; H2 (tests) and PostgreSQL (Testcontainers, prod) must both apply it.
4. **Every alert has an owner and a runbook.** A rule in `monitoring/alerts/` needs `severity`, `team`, `service` labels, a `runbook_url` and a case in `monitoring/alerts/tests/`; CI rejects the rest.
5. **Public contract parity with `microservices`.** A DTO or path change lands on both branches.

## Conventions

- Branch names: `feat/*`, `fix/*`, `perf/*`, `docs/*`, `ci/*`, `chore/*`; `hotfix/*` only for backports into `release/vX.Y` (see `docs/release.md`). The PR policy workflow checks the title, the branch and the Verification section.
- PR title = Conventional Commit; squash merge with the title as subject; release-please reads it.
- PR body follows `.github/PULL_REQUEST_TEMPLATE.md`; the **Verification** section says what was actually run.
- Tests: JUnit 5, AssertJ, Mockito; `@WebMvcTest` slices for controllers, `@DataJpaTest` on H2 for repositories, `AbstractIntegrationTest` (Testcontainers) for anything PostgreSQL-specific or end-to-end.
- Lombok in Java; no Lombok in the Kotlin files.

## Definition of done

Code + tests + docs in one PR. For anything user-visible: `CHANGELOG.md`
entry under `Unreleased`. For anything that can page someone: rule, unit test
for the rule, runbook. For a design decision with alternatives: an ADR in
`docs/adr/`.
