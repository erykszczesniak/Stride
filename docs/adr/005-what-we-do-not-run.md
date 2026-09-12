# ADR-005 — No gateway, no discovery, no config server, no Kafka, no breakers, no Redis

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-08, recorded 2026-09-12 |
| **Deciders** | platform |

## Context

The `microservices` branch runs an API gateway, Eureka, Config Server,
Kafka, gRPC, Resilience4j, Redis and eight databases to deliver the same
features as `main`. Each of those exists to solve a problem that a second
deployable creates. With one deployable (ADR-001) the question for each is
not "is it good practice" but "what problem would it solve here".

## Decision

| Not on `main` | What does the job instead |
|---|---|
| **API gateway** | The frontend's nginx proxies `/api/` and `/graphql` to `backend:8080`; in k8s the ingress routes `/api/(.*)` to the backend. CORS is `CorsConfiguration` with `app.cors.allowed-origins`; auth is the app's filter (ADR-004). |
| **Service discovery (Eureka)** | Compose DNS (`backend`, `postgres`, `elasticsearch`, `jaeger`) and k8s Services. Prometheus has one static target with a static `app` label. |
| **Config Server** | `application.yml` + `application-{dev,prod}.yml` + environment variables; secrets from `.env` (compose), `backend-secret` (k8s), SSM parameters (Terraform). |
| **Kafka** | Spring `ApplicationEventPublisher`: `UserFeedProjector` (`AFTER_COMMIT`, `REQUIRES_NEW`), `NotificationEventListener` (`@Async`, SSE push), `UserSearchIndexSyncListener` (`@Async`, ADR-003). |
| **gRPC** | Method calls. |
| **Resilience4j (breakers, retries)** | Failures surface as 5xx and are alerted on ratios; the AI chat has a code-level failover (ADR-010); the Strava import has its own budgeted client-side limiter (`StravaRateLimiter`). |
| **Redis** | In-memory per replica: `RateLimitingFilter` buckets, SSE emitters, AI per-user limits. No `@Cacheable` anywhere. |
| **Service mesh, EKS** | Terraform provisions VPC, EC2, RDS, ALB (+ ACM), ECR, IAM and SSM; Kubernetes is `k8s/` on any cluster (README uses minikube). |

The rule: a component is added when the monolith cannot do the job
in-process, not before.

## Alternatives considered

Each row above was built for the `microservices` branch, so the cost is
known from experience rather than estimated: 24 containers, per-JVM memory
limits to keep a laptop usable, a gateway configuration bug that made every
login 401, Kafka trace propagation that is still incomplete. None of those
costs buys anything for one process.

## Consequences

- The stack boots with `docker compose up -d`: Postgres, Elasticsearch,
  backend, frontend, Jaeger, Prometheus, Alertmanager, alert-sink, Grafana.
- No retry storms and no breaker to mis-tune, but also no automatic
  short-circuit: a slow provider is waited for until the client gives up
  (30 s axios, 60 s nginx) while a DB connection is held
  ([db-pool-exhausted.md](../runbooks/db-pool-exhausted.md)).
- Event listeners run in the same JVM: a bug in the feed projector runs in
  the request that created the activity (`AFTER_COMMIT`, so the write is
  already durable).
- Per-replica state is the known limit of this decision (ADR-001).

## Known gaps

- With more than one replica, rate limits are N× and SSE is replica-local;
  a shared store (Redis) is the first thing to add if that matters.
- No async job queue: the Strava bulk import runs on an `@Async` thread
  (`StravaImportRunner`) and dies with the JVM.
