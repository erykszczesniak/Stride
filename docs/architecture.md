# Architecture overview

This branch (`microservices`) is a complete rewrite of the Stride monolith
(on `main`) as a Spring Cloud microservices stack. Same product, different
architecture — the two branches sit side-by-side as portfolio pieces showing
how the same problem space looks under a monolith vs. a service-oriented
decomposition.

## Service inventory

| Service                  | Port  | Language | Owns                                              | Phase |
|--------------------------|-------|----------|---------------------------------------------------|-------|
| config-server            | 8888  | Java     | Spring Cloud Config (classpath-served YAML)       | 1     |
| discovery-server         | 8761  | Java     | Eureka registry                                   | 1     |
| api-gateway              | 8080  | Java     | Routing, JWT validation, rate-limit, CORS         | 1     |
| user-service             | 8081  | Java     | Identity, auth (JWT/Google/Strava), profiles, follow graph, ES search, admin users | 2, 12 |
| activity-service         | 8082  | Java     | Activities, stats, photos, PRs, gear, file import, Strava integration | 3, 13–15 |
| notification-service     | 8083  | **Kotlin** | Notifications, SSE push, email + weekly summary  | 4, 17 |
| social-service           | 8084  | Java     | Likes, comments + emoji reactions, per-user CQRS feed fan-out | 5, 16 |
| club-event-service       | 8085  | Java     | Clubs, members, events, participants              | 6, 18 |
| challenge-goal-service   | 8086  | Java     | Challenges, goals, training plans (Resilience4j + Redis cache) | 7, 18 |
| ai-service               | 8087  | **Kotlin** | Persisted AI activity chat (Anthropic) + goal/plan generation (Groq) | 8, 19 |
| web-bff                  | 8088  | Java     | Web BFF: dashboard composition, report GraphQL, CSV export, admin stats | 20 |
| pipeline-service         | 8089  | Java     | iOS BFF: /pipeline/v1 facade + tri_ personal tokens (own auth chain) | 21 |

Two services in Kotlin (notification-service, ai-service) deliberately,
to demonstrate incremental JVM-language migration without rewriting the
whole stack at once. The shared modules (`shared/events`, `shared/security`)
stay in Java and are consumed by both Java and Kotlin callers via the
JVM's bytecode interop.

## High-level diagram

```
                            Internet
                                │
                                ▼
                       ┌────────────────┐
                       │ Ingress (nginx)│
                       └────────┬───────┘
                                │
                       ┌────────▼───────┐   Redis (rate limit)
                       │  api-gateway   │←──────────────┐
                       │  · JWT verify  │               │
                       │  · CORS        │               │
                       │  · Rate limit  │               │
                       └────────┬───────┘               │
                                │  (lb:// via Eureka)   │
        ┌───────────┬───────────┼───────────┬───────────┼───────────┐
        ▼           ▼           ▼           ▼           ▼           ▼
   ┌────────┐  ┌─────────┐  ┌─────────┐  ┌────────┐  ┌─────────┐  ┌────┐
   │  user  │  │activity │  │ social  │  │ club/  │  │challenge│  │ ai │
   │        │  │         │  │         │  │ event  │  │ /goal   │  │    │
   └───┬────┘  └────┬────┘  └────┬────┘  └────┬───┘  └────┬────┘  └─┬──┘
       │            │            │            │           │         │
       │            └──┬─────────┴──┬─────────┘           │         │
       │               │            │                     │      external
       ▼               ▼            ▼                     ▼      (Groq API)
  ┌─────────────────────────────────────────────────────────┐
  │                      Kafka topics                       │
  │  user.registered · activity.created · activity.updated  │
  │  pr.achieved · comment.added · reaction.added           │
  └──────────────────────────┬──────────────────────────────┘
                             ▼
                  ┌────────────────────┐
                  │  notification-svc  │ ← Kotlin, SSE push
                  └────────────────────┘

  Per-service Postgres × 7  ·  Distributed tracing → Jaeger
  Metrics → Prometheus → Grafana  ·  Logs structured JSON
```

## Pillars

### 1. Database-per-service

Each domain service owns its own Postgres schema. Cross-service references
are **logical** (raw `BIGINT user_id` columns, no FK), never foreign-key
constrained across schemas. This is the rule that prevents a microservices
stack from collapsing into a distributed monolith over time — if you can't
write a join, you can't accidentally couple read paths.

When a service needs another service's data on the read path, it has three
options in this codebase:

1. **Async denormalize via Kafka events** — `social-service` keeps a
   `user_cache` table populated from `UserRegisteredEvent`. The feed renders
   user names locally without a network call.
2. **Sync REST call wrapped in Resilience4j + cache** — `challenge-goal-
   service` fetches activity stats from `activity-service` through Spring
   Cloud LoadBalancer, with circuit breaker + retry + Redis cache.
3. **Skip it entirely** — most reads stay inside their owning service.

### 2. Event-driven communication via Kafka

| Topic                       | Producer                | Consumer(s)                     |
|-----------------------------|-------------------------|---------------------------------|
| `stride.user.registered`    | user-service            | social + activity (user_cache projections) |
| `stride.user.updated`       | user-service            | social, activity (cache refresh) |
| `stride.user.deleted`       | user-service            | social (purge), activity (cache) |
| `stride.user.followed`      | user-service            | social (follow graph + feed backfill), notification (FOLLOW) |
| `stride.user.unfollowed`    | user-service            | social (edge removal + feed purge) |
| `stride.activity.created`   | activity-service        | social (snapshot + feed fan-out), challenge-goal (cache invalidation) |
| `stride.activity.updated`   | activity-service        | social (snapshot refresh)       |
| `stride.activity.deleted`   | activity-service        | social (feed retraction + local cleanup) |
| `stride.activity.visibility-changed` | activity-service | social (feed retraction / re-fan-out) |
| `stride.activity.liked`     | social-service          | notification (LIKE)             |
| `stride.pr.achieved`        | activity-service        | notification (ACHIEVEMENT)      |
| `stride.comment.added`      | social-service          | notification (COMMENT)          |

Producers and consumers share event DTOs through the `shared/events`
module (Java records) — typed contracts on top of JSON-serialized Kafka
messages. Trusted package list on the consumer side is restricted to
`com.strava.shared.events.*` so a poisoned message can't deserialize into
arbitrary classes.

### 3. CQRS-light (social-service feed)

The activity feed is a denormalized read model built from events, not
joined from canonical tables. `social-service` has two consumer groups:

- `social-service` — projection group: `UserRegisteredEvent` → upsert
  `user_cache`; `ActivityCreatedEvent` → insert into `feed_items` with
  denormalized activity metadata.
- `social-service-counts` — counter group: `CommentAddedEvent` and
  `ReactionAddedEvent` → increment the matching feed_item's count.

Reads of `GET /api/feed` hit a single table with one local join against
`user_cache`. No call to user-service. No call to activity-service. The
feed is fast and resilient to those services being slow or down, at the
cost of eventual consistency on counts.

### 4. Sync calls protected by Resilience4j

When sync is the right shape (activity aggregation for progress bars,
external Groq calls), the outbound call is wrapped:

```
@TimeLimiter(name)        ← cap per-call latency
  @CircuitBreaker(name)   ← open after sustained failures, fall back
    @Retry(name)          ← absorb transient blips
      actual call         ← lb://service or HTTPS to external
```

Two deployments of this pattern:
- `challenge-goal-service` → `activity-service` (peer microservice via
  Eureka).
- `ai-service` → Groq Chat Completions (external HTTP, much higher
  variance, tighter timeouts, longer wait-in-open-state).

Tuning lives in `application.yml` under
`resilience4j.{circuitbreaker,retry,timelimiter}.instances.<name>`, with
prod-tighter overlays served by Config Server.

### 5. JWT-only-at-gateway trust model

The gateway is the single ingress point. It does the full JWT validation,
strips any spoofed `X-User-*` headers from inbound traffic, then propagates
the authenticated identity via headers to downstream services:

```
External request: Authorization: Bearer <token>
                  ↓ gateway validates HS256 signature + expiry + claims
                  ↓ extracts userId, email, roles
                  ↓ rewrites request:
External → Gateway: X-User-Id: 42
                    X-User-Email: alice@example.com
                    X-User-Roles: USER,ADMIN
```

Each downstream service uses the shared `HeaderAuthenticationFilter`
(`shared/security`) to translate those headers into a Spring Security
`Authentication`, so controllers keep using idiomatic
`@AuthenticationPrincipal Long userId` without knowing about the gateway
boundary. The Kubernetes `NetworkPolicy` enforces that only the gateway
(and Prometheus) can reach domain services — defense-in-depth on top of
the application-layer trust.

## Deployment options

| Mode                  | What runs where                                                       | Use case                            |
|-----------------------|------------------------------------------------------------------------|-------------------------------------|
| `docker compose up`   | Everything locally                                                    | Development, full-stack smoke test  |
| `kind` + Helm charts  | All services + managed Kafka/Redis/Postgres charts on a local k8s     | Production-shaped local demo        |
| AWS slice             | Gateway + 1-2 services on EC2 t3.micro, RDS Postgres, S3, ECR         | Free-tier-eligible "live" demo      |
| EKS                   | Full stack on managed Kubernetes                                      | Not free — would be ~$73/mc baseline |

Standalone profile (`-Dspring.profiles.active=standalone`) on any service
boots it independently with an in-memory H2 database and Kafka listeners
disabled — useful for fast unit-of-deployment smoke tests without spinning
up the whole stack.
