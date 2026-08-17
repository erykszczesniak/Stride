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
| 10 | Frontend synced 1:1 with `main` | done (`a91f0a9`) |
| 11 | Shared events v2, gateway `/api/v1` route table, SSE `?token=`, CORS page headers, first tests | done (`4ef85b5`) |
| 12 | user-service full parity | done (`646a386`, 143 tests) |
| 13 | activity-service core parity | done (`306b3e1`, 95 tests) |
| 14 | gear tracking | done (PR #141) |
| 15 | file import + Strava integration | done (PR #141, module at 218 tests) |
| 16 | social-service parity (likes, comments+reactions, CQRS feed fan-out) | done (PR #144, 85 tests) |
| 17 | notification-service parity (contract, email, weekly summary) | done (`4d7a526`, 57 tests) |
| 18 | challenge-goal + club-event parity + training plans | done (`db2a66c`, 179 tests) |
| 19 | ai-service parity (persisted chat, generate-goal/plan) | done (PR #140, 45 tests) |
| 20 | web-bff (dashboard, GenerateReport, CSV export, admin stats) | done (PR #142, 35 tests) |
| 21 | pipeline-service (iOS facade + `tri_` tokens) | done (PR #143, 63 tests) |
| 22 | infra wiring (compose, config, k8s, monitoring, CI), docs, full verification | done (PR #145) |
| 23a | shared gRPC contracts (protos + generated stubs) | done (PR #147) |
| 23b | gRPC servers for the internal APIs (user/activity/social) | done (PR #148) |
| 23c | every internal client cut over to gRPC stubs | done (PR #149) |
| 23d | REST /internal removal, k8s gRPC ports, docs, E2E | done (this PR) |

Full-reactor `./mvnw verify` on the merged tree: **BUILD SUCCESS, 833 tests, 0
failures** (including the 8 gateway filter tests).

Known deliberate deviations (documented at their code sites): ProblemDetail +
`message` errors instead of the monolith's ad-hoc maps; the REST-shadowed dead
GraphQL surface is not duplicated; social-served feed rows carry `gear: null`;
BFF admin club/challenge/event counts are approximations; refresh tokens must
carry `type=refresh`; per-section BFF degradation on downstream outages.
Follow-up open: user-service profile activity counts still come from a stub
port until wired to activity-service internal stats.

## Testing convention (established in Phase 11)

- Pure unit tests (JUnit 5 + Mockito/AssertJ) for services, projectors,
  calculators, filters.
- `@WebMvcTest` slices for controllers (with `HeaderAuthenticationFilter`).
- `@DataJpaTest` on H2 (`MODE=PostgreSQL`) where repository queries are
  non-trivial.
- Every phase lands with tests; `./mvnw verify -B` must stay green.
