# ADR-001 — One deployable: a layered Spring Boot monolith

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-04 (first version), recorded 2026-09-12 |
| **Deciders** | platform |

## Context

Stride is one product — activities, social feed, clubs, challenges, goals,
notifications, imports, AI coach, an iOS read-only facade — built by one
team and run on one host (compose), one small cluster (`k8s/`) or one EC2
instance behind an ALB (`terraform/`). Every feature reads the same users
and activities tables. The question was whether to split by domain from
the start or to keep one process and split later if a reason appeared.

## Decision

One Spring Boot 3.4 / Java 21 application, `strava-backend`, organised by
layer: `controller` → `service` (+ `service/impl`) → `repository` →
`domain`, with `security`, `configuration`, `event`, `search`,
`service/projection` and `graphql` beside them. Cross-feature effects run
in-process through Spring application events (feed fan-out, notifications,
search index). The layer boundaries are executable (`ArchitectureTest`,
ADR-008) rather than network boundaries.

The `microservices` branch is the same product split into twelve services
with a gateway, Eureka, Config Server, Kafka and gRPC. It exists to compare
the two shapes on identical behaviour; public-contract parity between the
branches is an invariant (`AGENTS.md`).

## Alternatives considered

- **Split from the start** — done on the `microservices` branch: twelve
  deployables, eight databases, a message broker and a gateway to run the
  same features. Right when teams need independent deploys; not needed for
  one team and one host.
- **Spring Modulith-style modules inside the monolith** — package-by-feature
  with enforced module boundaries. The codebase is package-by-layer; the
  ArchUnit rules pin the layers instead. Revisit if features start to share
  state through anything other than the service layer.

## Consequences

- One image, one log stream, one Prometheus target, one Flyway history.
  Deploys are `docker compose up -d --build` or one `kubectl rollout`.
- One process: a leak or a hot loop in any feature degrades every feature
  (`JvmHeapHigh`, `EdgeHighLatencyP95` are whole-app alerts by construction).
- Scaling is by replica (k8s HPA 2–5, `docker-compose.prod.yml` 2 replicas),
  and per-replica in-memory state does not follow the user: SSE emitters
  (`SseEmitterServiceImpl`), the per-IP login limiter (`RateLimitingFilter`)
  and the AI per-user limits (ADR-009) live in each JVM.
- Adding a second deployable means revisiting ADR-005; the `microservices`
  branch shows the cost.

## Known gaps

- With more than one replica, a notification created on replica A is
  pushed over SSE only to users connected to replica A.
- The `microservices` branch and `main` drift unless parity is checked by
  hand; there is no automated contract diff between them.
