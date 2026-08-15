# Parity plan — aligning `microservices` with `main`

Goal: the `microservices` branch serves **the exact same product** as `main`
(same frontend, same `/api/v1` + `/graphql` + `/pipeline/v1` contract, same
behavior), implemented with microservices best practices. Phases continue the
branch's phase-commit convention.

## Contract rules

- Public contract = the monolith's, byte-for-byte: paths, verbs, DTO shapes,
  list envelopes with `page-total-count` / `page-total-elements` headers.
- The gateway does **no rewriting**; services mount public paths directly.
- Errors: RFC-7807 `ProblemDetail` + an extra `message` property (the frontend
  reads `err.response.data.message`).
- Services authenticate via `X-User-*` headers injected by the gateway
  (`HeaderAuthenticationFilter`); admin endpoints additionally require
  `ROLE_ADMIN`.
- SSE: `/api/v1/notifications/stream` accepts `?token=` (EventSource cannot set
  headers); the gateway validates it and injects the identity headers.
- `/pipeline/v1/**` bypasses gateway JWT validation — pipeline-service
  authenticates `tri_` personal tokens (or a Stride JWT) itself.

## Ownership map

| Public surface | Owner |
|---|---|
| `/api/v1/auth/**` (incl. Google + Strava login, password reset), `/api/v1/users/**`, `/api/v1/search/**`, `/api/v1/admin/users/**` | user-service |
| `/api/v1/activities/**` (CRUD, stats, photos, `/uploads/**`), `/api/v1/personal-records/**`, `/api/v1/gear/**`, `/api/v1/import/**`, `/api/v1/integrations/strava/**`, `/api/v1/admin/activities/**` | activity-service |
| `/api/v1/activities/feed`, `/public`, `/{id}/like`, `/{id}/comments/**` | social-service (CQRS read model + social graph projections) |
| `/api/v1/notifications/**` (+ SSE, email, weekly summary) | notification-service |
| `/api/v1/clubs/**`, `/api/v1/events/**` | club-event-service |
| `/api/v1/challenges/**`, `/api/v1/goals/**`, `/api/v1/training-plans/**` | challenge-goal-service |
| `/api/v1/ai/**` (incl. persisted activity chat) | ai-service |
| `/api/v1/dashboard`, `/graphql` (GenerateReport), `/api/v1/export/**`, `/api/v1/admin/stats` | web-bff (new) |
| `/pipeline/v1/**`, `/api/v1/pipeline/tokens` | pipeline-service (new) |

Deliberate deviation: the monolith's REST-shadowed GraphQL surface (8
controllers) is not duplicated — no client uses it except the report wizard's
`GenerateReport` query, which web-bff implements. Everything a client actually
calls is ported 1:1.

## Cross-service data flows

- Kafka (choreography): user registered/updated/deleted, follow/unfollow,
  activity created/updated/deleted/visibility-changed/liked, comment added,
  reaction added, PR achieved → social projections (user cache, follow graph,
  per-user feed fan-out) and notifications (LIKE / COMMENT / FOLLOW / PR).
- Sync REST + Resilience4j (composition): social feed hydration ← activity
  batch API; activity social counts ← social batch API; BFF dashboards/reports
  ← domain services; ai chat context ← activity-service.

## Phases

| Phase | Scope | Status |
|---|---|---|
| 10 | Frontend synced 1:1 with `main` | done |
| 11 | Shared events v2, gateway `/api/v1` route table, SSE `?token=`, CORS page headers, first tests | done |
| 12 | user-service full parity | pending |
| 13 | activity-service core parity | pending |
| 14 | gear tracking | pending |
| 15 | file import + Strava integration | pending |
| 16 | social-service parity (likes, comments+reactions, CQRS feed fan-out) | pending |
| 17 | notification-service parity (contract, email, weekly summary) | pending |
| 18 | challenge-goal + club-event parity + training plans | pending |
| 19 | ai-service parity (persisted chat, generate-goal/plan) | pending |
| 20 | web-bff (dashboard, GenerateReport, CSV export, admin stats) | pending |
| 21 | pipeline-service (iOS facade + `tri_` tokens) | pending |
| 22 | infra wiring (compose, config, k8s, monitoring, CI), docs, full verification | pending |

## Testing convention (established in Phase 11)

- Pure unit tests (JUnit 5 + Mockito/AssertJ) for services, projectors,
  calculators, filters.
- `@WebMvcTest` slices for controllers (with `HeaderAuthenticationFilter`).
- `@DataJpaTest` on H2 (`MODE=PostgreSQL`) where repository queries are
  non-trivial.
- Every phase lands with tests; `./mvnw verify -B` must stay green.
