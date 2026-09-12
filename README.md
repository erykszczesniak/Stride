# Stride — Microservices Architecture

> This is the **`microservices` branch**. The same application also exists as a Spring Boot monolith on [`main`](https://github.com/erykszczesniak/Stride/tree/main). Both branches implement the same product (a Strava-like fitness tracking app); the split makes the codebase serve as a portfolio piece showing two architectural approaches to the same domain side-by-side.

```
┌──────────────────────────────────────────────────────────────────────┐
│  12 Spring Boot/Kotlin services · Kafka events · gRPC inside         │
│  Spring Cloud Config · Redis cache · Elasticsearch · Resilience4j    │
│  CQRS feed fan-out · 2 BFFs (web + iOS) · OpenTelemetry → Jaeger     │
│  Micrometer → Prometheus → Grafana · K8s · Terraform · DB-per-service│
└──────────────────────────────────────────────────────────────────────┘
```

**Full product parity with `main`:** the React frontend and the iOS
`/pipeline/v1` client run unchanged against this stack — same `/api/v1`
contract, same DTOs, same behavior, served by 9 domain services behind the
gateway instead of one monolith. The parity work (Phases 10–22) is tracked
in [docs/parity-plan.md](docs/parity-plan.md); every service module ships
with tests (about 950 across the reactor plus 141 in the frontend), and
the reactor build is gated on coverage, architecture rules and a real-
PostgreSQL migration test — see [Operations & quality](#operations--quality).

## Contents

- [Phases](#phases)
- [Architecture at a glance](#architecture-at-a-glance)
- [Service inventory](#service-inventory)
- [Patterns demonstrated](#patterns-demonstrated)
- [Operations & quality](#operations--quality)
- [Run it locally](#run-it-locally)
- [Run it on AWS (free tier)](#run-it-on-aws-free-tier)
- [Repository layout](#repository-layout)
- [Tech stack](#tech-stack)
- [What's intentionally not here](#whats-intentionally-not-here)
- [Documentation](#documentation)
- [License](#license)

## Phases

| #  | Scope                                                                    | Commit       |
|----|--------------------------------------------------------------------------|--------------|
| 0  | Scaffold (parent pom, shared libs, docker-compose, monolith removal)     | `7ed0f5a`    |
| 1  | API Gateway + Eureka + Config Server                                     | `899b71b`    |
| 2  | user-service (auth, profiles, follow graph)                              | `1d3b9d1`    |
| 3  | activity-service (activities, personal records, Kafka events)            | `731ce72`    |
| 4  | notification-service in Kotlin (Kafka consumer + SSE push)               | `93a9a90`    |
| 5  | social-service (CQRS-light read model from events)                       | `ae26987`    |
| 6  | club-event-service (combined: clubs + group events)                      | `86f2ce5`    |
| 7  | challenge-goal-service (Resilience4j + Redis cache + Kafka invalidation) | `ecba2b0`    |
| 8  | ai-service in Kotlin (Groq + Resilience4j on external API)               | `7239306`    |
| 9  | K8s manifests · Terraform AWS slice · architecture docs · saga doc       | `1a035af`    |
| 10 | Frontend synced 1:1 with `main`                                          | `a91f0a9`    |
| 11 | Shared events v2 · gateway `/api/v1` contract · first tests              | `4ef85b5`    |
| 12 | user-service full parity (OAuth logins, reset, search/ES, admin)         | `646a386`    |
| 13 | activity-service core parity (stats, photos, streams, visibility, PRs)   | `306b3e1`    |
| 14–15 | gear tracking · file import (GPX/TCX/FIT) · Strava integration        | PR #141      |
| 16 | social-service parity (likes, comments+reactions, CQRS feed fan-out)     | PR #144      |
| 17 | notification-service parity (contract, email, weekly summary)            | `4d7a526`    |
| 18 | challenge-goal + club-event parity · training plan templates             | `db2a66c`    |
| 19 | ai-service parity (persisted AI chat, goal/plan generation)              | PR #140      |
| 20 | web-bff (dashboard composition, report GraphQL, CSV export)              | PR #142      |
| 21 | pipeline-service (iOS BFF, `tri_` personal tokens)                       | PR #143      |
| 22 | Infra wiring (ES, new DBs, compose, k8s, CI) · docs · E2E verification   | this commit  |

## Architecture at a glance

```
                            Internet
                                │
                                ▼
                       ┌────────────────┐
                       │ Ingress (nginx)│
                       └────────┬───────┘
                                │
                       ┌────────▼───────┐   Redis
                       │  api-gateway   │←──(rate limit + session)
                       │  · JWT verify  │   serves the monolith's exact
                       │  · CORS        │   /api/v1 · /graphql · /pipeline/v1
                       └────────┬───────┘
                                │  (lb:// via Eureka)
        ┌───────────┬───────────┼───────────┬───────────┬─────────┬───────────┐
        ▼           ▼           ▼           ▼           ▼         ▼           ▼
   ┌────────┐  ┌─────────┐  ┌─────────┐  ┌────────┐  ┌─────────┐  ┌────┐  ┌─────────┐
   │  user  │  │activity │  │ social  │  │ club/  │  │challenge│  │ ai │  │ BFFs:   │
   │ :8081  │  │ :8082   │  │ :8084   │  │ event  │  │ /goal   │  │ Kt │  │ web 8088│
   └───┬────┘  └────┬────┘  └────┬────┘  │ :8085  │  │ :8086   │  │8087│  │ iOS 8089│
       │            │            │       └────────┘  └────┬────┘  └─┬──┘  └─────────┘
       │            └────────┬───┴──────────┬─────────────┘         │
       │                     │              │              Groq + Anthropic APIs
       ▼                     ▼              ▼
  Elasticsearch  ┌──────────────────────────────────────────────────────────┐
  (user search)  │                       Kafka topics                       │
                 │  user.{registered,updated,deleted,followed,unfollowed}   │
                 │  activity.{created,updated,deleted,visibility,liked}     │
                 │  pr.achieved · comment.added                             │
                 └──────────────────────┬───────────────────────────────────┘
                                        ▼
                             ┌────────────────────┐
                             │ notification (Kt)  │ ← SSE push + email
                             │ :8083              │
                             └────────────────────┘

  8× Postgres (database-per-service)   Tracing → Jaeger   Metrics → Prometheus
```

Full details in [docs/architecture.md](docs/architecture.md) ·
event chain walkthrough in [docs/saga.md](docs/saga.md) ·
observability setup in [docs/observability.md](docs/observability.md).

## Service inventory

| Service                  | Port  | Language   | Owns                                              |
|--------------------------|-------|------------|---------------------------------------------------|
| config-server            | 8888  | Java       | Spring Cloud Config (classpath-served YAML)       |
| discovery-server         | 8761  | Java       | Netflix Eureka registry                           |
| api-gateway              | 8080  | Java       | Routing, JWT validation, rate-limit, CORS         |
| user-service             | 8081  | Java       | Identity, auth (JWT/Google/Strava), profiles, follow graph, ES search, admin users |
| activity-service         | 8082  | Java       | Activities, stats, photos, personal records, gear, file import, Strava integration |
| notification-service     | 8083  | **Kotlin** | Notifications (Kafka consumers, SSE push, email + weekly summary) |
| social-service           | 8084  | Java       | Likes, comments + emoji reactions, per-user CQRS feed fan-out |
| club-event-service       | 8085  | Java       | Clubs, members, events, participants              |
| challenge-goal-service   | 8086  | Java       | Challenges, goals, training plans (Resilience4j + Redis cache) |
| ai-service               | 8087  | **Kotlin** | Persisted AI activity chat (Anthropic) + goal/plan generation (Groq) |
| web-bff                  | 8088  | Java       | Web BFF: dashboard composition, report GraphQL, CSV export, admin stats |
| pipeline-service         | 8089  | Java       | iOS BFF: `/pipeline/v1` facade, `tri_` personal tokens |

Two services are deliberately in Kotlin (notification-service, ai-service)
to demonstrate incremental JVM-language migration without rewriting the
whole stack.

## Patterns demonstrated

| Pattern                       | Where it lives                                                                              |
|-------------------------------|---------------------------------------------------------------------------------------------|
| **Database-per-service**      | Every domain service owns its own Postgres schema; cross-service refs are logical (`BIGINT user_id`, no FK across schemas) |
| **API Gateway + JWT trust**   | `infrastructure/api-gateway` validates JWT, strips spoofed `X-User-*`, propagates verified identity downstream |
| **Service discovery**         | Eureka — every service registers, gateway resolves `lb://service-name`, Prometheus scrapes via `eureka_sd_configs` |
| **Centralized config**        | Spring Cloud Config Server serves YAML from classpath; per-service files in `infrastructure/config-server/.../config/` |
| **Event-driven choreography** | Kafka topics in `shared/events/Topics.java`; saga walkthrough in `docs/saga.md`             |
| **CQRS read model**           | `social-service` builds the feed from `ActivityCreatedEvent`/`CommentAddedEvent`; no joins across services |
| **Local denormalized cache**  | `social-service.user_cache` populated by `UserRegisteredEvent` consumer — feed renders names locally |
| **Circuit breaker + retry**   | Resilience4j on `challenge-goal-service → activity-service` (peer) and `ai-service → Groq` (external) |
| **Cache with event eviction** | Spring `@Cacheable` over Redis in `challenge-goal-service`; `@KafkaListener` on `ActivityCreatedEvent` evicts |
| **Kotlin + Java interop**     | Kotlin services consume Java records from `shared/events`, plug into the Java `HeaderAuthenticationFilter` from `shared/security` |
| **Defense-in-depth security** | NetworkPolicy in `k8s/network-policies.yaml` blocks domain services from external traffic — only the gateway can reach them |
| **Per-service Resilience4j tuning** | dev defaults in each service's `application.yml`, prod-tighter overlay in Config Server |
| **Backend-for-frontend (×2)** | `web-bff` (dashboard composition, report GraphQL, CSV export) and `pipeline-service` (iOS facade with its own `tri_` token auth) — one BFF per client type |
| **Per-user feed fan-out (CQRS)** | `social-service.user_feed_entries` materialized from activity/follow events: author-always, public-only follower fan-out, visibility retraction, follow backfill, unfollow purge |
| **Search index with graceful fallback** | `user-service` mirrors users into Elasticsearch (async, swallow-on-failure); search falls back to SQL `ILIKE` when the index is down |
| **API composition with identity forwarding** | BFFs call domain services with the caller's verified `X-User-*` headers over the public REST contract |
| **gRPC inside, REST at the edge** | Cluster-internal calls use typed proto contracts (`shared/grpc-contracts`) over Eureka-discovered gRPC channels with per-call deadlines; the browser/iOS contract stays REST at the gateway |

## Operations & quality

Everything an on-call engineer, a reviewer or a finance question needs is
code in this repository and checked in CI. Added in the September 2026
series (#156–#165); details in [`CHANGELOG.md`](CHANGELOG.md).

| Area | What is there | Where |
|---|---|---|
| **CI gates** | JaCoCo floor per module (60 % lines / 40 % branches; lowest module today 68 %), ArchUnit rules (found and fixed a controller bypassing the service layer), Testcontainers test on real PostgreSQL with the production migration set, frontend Jest in CI, CodeQL, Trivy filesystem + image | `pom.xml`, `*/ArchitectureTest`, `.github/workflows/{ci,codeql,security-scan}.yml` |
| **Alerts as code** | 41 Prometheus rules in 7 groups, 50+ promtool unit-test cases, a label/runbook/test contract enforced by CI, Alertmanager routing with inhibition, an echo receiver so deliveries are visible locally, Eureka label normalised at scrape | [`monitoring/alerts/`](monitoring/alerts), [`monitoring/alertmanager/`](monitoring/alertmanager), `.github/workflows/monitoring.yml` |
| **SLOs** | 4 objectives (edge 99.5 % / 95 % < 500 ms, per-service 99.5 %, AI 98 %), recording rules, multi-window burn-rate alerts (14.4× pages, 6× tickets), error-budget policy, `stride-slo` dashboard | [`docs/slo.md`](docs/slo.md), [`monitoring/alerts/slo.rules.yml`](monitoring/alerts/slo.rules.yml) |
| **Runbooks & post-mortems** | 13 runbooks in one format (front matter for agents, investigation steps with the stack's real commands), 2 post-mortems reconstructed from merged PRs | [`docs/runbooks/`](docs/runbooks), [`docs/postmortems/`](docs/postmortems) |
| **AI guardrails** | per-user rate limit, prompt guard with a 42-case golden eval that gates the build, PII masking before the provider *and* before persistence, token/latency/cost metrics + JSON audit line per call, Anthropic → Groq failover, live rubric evals (weekly, keys required) | [`docs/ai-guardrails.md`](docs/ai-guardrails.md), `services/ai-service/.../{guard,telemetry}`, `.github/workflows/ai-eval.yml` |
| **Cost attribution** | `team` / `cost_center` on every metric (set per service in Config Server), modelled infra USD/hour from CPU consumed and memory reserved, real AI spend from tokens, `stride-cost` dashboard, spend-spike alerts | [`docs/cost-attribution.md`](docs/cost-attribution.md), [`monitoring/alerts/cost.rules.yml`](monitoring/alerts/cost.rules.yml) |
| **E2E & smoke** | `scripts/smoke.sh` (gateway, one request per service, guard, Eureka, Prometheus targets and rules, Watchdog, no critical alert) usable against compose, kind or prod; nightly Playwright run on the composed stack | [`scripts/smoke.sh`](scripts/smoke.sh), `.github/workflows/e2e.yml` |
| **Decisions** | 10 ADRs with the alternatives that were on the table and the known gaps | [`docs/adr/`](docs/adr) |
| **Repo standards** | CODEOWNERS, PR template with a mandatory verification section, Dependabot, release-please from Conventional Commit titles, `AGENTS.md` + path-scoped Copilot instructions | [`AGENTS.md`](AGENTS.md), `.github/` |

Grafana dashboards (`:3001`): **`stride-jvm`** (per-service JVM & HTTP), **`stride-slo`** (SLIs, burn rate, error budget), **`stride-cost`** (spend by team/service/model). Alert deliveries locally: `docker compose logs -f alert-sink`.

## Run it locally

### Option A — docker-compose (full stack)

```bash
cp .env.example .env       # adjust JWT_SECRET, GROQ_API_KEY / ANTHROPIC_API_KEY for real AI
./mvnw clean package -DskipTests
docker compose up -d                                   # lean core (12 services + Kafka/Redis/Postgres)
docker compose --profile observability --profile search up -d   # + Prometheus, Alertmanager, Grafana, Jaeger, Kafka UI, Elasticsearch
scripts/smoke.sh                                       # gateway, every service, guard, Eureka, monitoring pipeline
```

Endpoints:

| URL                    | What                          |
|------------------------|-------------------------------|
| http://localhost:8080  | API Gateway                   |
| http://localhost:8761  | Eureka dashboard              |
| http://localhost:8888  | Config Server                 |
| http://localhost:8090  | Kafka UI                      |
| http://localhost:16686 | Jaeger UI                     |
| http://localhost:9090  | Prometheus (rules, targets)   |
| http://localhost:9093  | Alertmanager                  |
| http://localhost:3001  | Grafana (admin/admin): `stride-jvm`, `stride-slo`, `stride-cost` |

### Option B — kind (Kubernetes locally)

See [`k8s/README.md`](k8s/README.md) for the full kind + Helm setup.
About 5 minutes from `kind create cluster` to working stack.

### Option C — standalone profile (single service, no infra)

Each service can boot independently with H2 + Kafka consumers disabled:

```bash
SPRING_PROFILES_ACTIVE=standalone \
  java -jar services/user-service/target/user-service.jar
```

Used during development for fast iteration. Each `Phase N: smoke test`
commit message documents the standalone test path for that service.

## Run it on AWS (free tier)

Terraform skeleton in [`terraform/aws/`](terraform/aws/) — RDS Postgres,
S3, ECR × 10 repos, EC2 t3.micro, Parameter Store. **Free for 12 months**
within the AWS free tier. Not auto-deployed.

```bash
cd terraform/aws
terraform init
TF_VAR_db_password=... TF_VAR_jwt_secret=... terraform apply
```

EKS is deliberately not in the Terraform — the control plane is ~$73/mo,
which defeats the "free tier demo" purpose. Use kind locally; the AWS
slice is for showing real-cloud-deployment chops, not for running the
full stack.

## Repository layout

```
.
├── pom.xml                         # multi-module Maven aggregator
├── docker-compose.yml              # full local stack
├── shared/
│   ├── events/                     # Kafka event DTOs (Java records)
│   └── security/                   # JwtParser, UserContext, HeaderAuthenticationFilter
├── infrastructure/
│   ├── config-server/              # Spring Cloud Config (classpath-served)
│   ├── discovery-server/           # Netflix Eureka standalone
│   └── api-gateway/                # Spring Cloud Gateway + JWT filter
├── services/
│   ├── user-service/               # Phase 2
│   ├── activity-service/           # Phase 3
│   ├── notification-service/       # Phase 4 (Kotlin)
│   ├── social-service/             # Phase 5 (CQRS)
│   ├── club-event-service/         # Phase 6
│   ├── challenge-goal-service/     # Phase 7 (Resilience4j + Redis)
│   ├── ai-service/                 # Phase 8 (Kotlin, Groq + Anthropic)
│   ├── web-bff/                    # Phase 20 (web BFF: dashboard/report/export)
│   └── pipeline-service/           # Phase 21 (iOS BFF: /pipeline/v1 + tri_ tokens)
├── k8s/                            # Kubernetes manifests (Phase 9)
├── terraform/aws/                  # AWS free-tier deployment skeleton
├── monitoring/
│   ├── alerts/                     # Prometheus rules (7 groups) + promtool unit tests
│   ├── alertmanager/               # routing, inhibition, receivers
│   ├── grafana/dashboards/         # stride-jvm, stride-slo, stride-cost (linted in CI)
│   └── scripts/                    # alert contract check
├── scripts/smoke.sh                # post-deploy smoke test (compose / kind / prod)
├── docs/
│   ├── adr/                        # architecture decision records
│   ├── runbooks/ · postmortems/    # one runbook per alert family, blameless post-mortems
│   ├── slo.md · ai-guardrails.md · cost-attribution.md
│   └── architecture.md · saga.md · observability.md
├── AGENTS.md · CHANGELOG.md        # working conventions, release notes (release-please)
└── frontend/                       # React SPA (unchanged from monolith) + Playwright e2e
```

## Tech stack

- **Spring Boot 3.4** · **Spring Cloud 2024.0.0** · **Java 21** · **Kotlin 1.9**
- **Postgres 16** · **Kafka 3.7 (KRaft)** · **Redis 7**
- **Resilience4j 2.2** · **Flyway 10** · **JJWT 0.12**
- **Jaeger 1.71** · **Prometheus 2.55** · **Alertmanager 0.27** · **Grafana 11**
- **JaCoCo** · **ArchUnit** · **Testcontainers** · **Playwright** · **promtool** · **grafana/dashboard-linter**
- **CodeQL** · **Trivy** · **Dependabot** · **release-please**
- **Maven 3.9** · **Docker** · **Kubernetes 1.30+**
- **Terraform 1.6+** (AWS provider 5.x)

## What's intentionally not here

Documented decisions, not gaps:

- **gRPC** — REST + JSON is fine at this scale, gRPC would obscure the demo
- **Istio / service mesh** — Spring Cloud LoadBalancer + Eureka cover discovery + LB; mesh adds setup cost without buying anything new for the portfolio narrative
- **Full event sourcing** — only the social feed is event-sourced; everything else is conventional CRUD
- **Saga orchestrator** — choreographed sagas (Kafka fan-out) are the right shape for this domain; documented in `docs/saga.md`
- **EKS in Terraform** — control plane is ~$73/mo, breaks the "free-tier demo" frame

## Documentation

| Document | What it answers |
|---|---|
| [`docs/architecture.md`](docs/architecture.md) | how the services fit together, the five pillars |
| [`docs/saga.md`](docs/saga.md) | the event chain from activity created to notification, why choreography |
| [`docs/observability.md`](docs/observability.md) | tracing, metrics, logs, health |
| [`docs/slo.md`](docs/slo.md) | the four objectives, burn-rate alerting, error-budget policy |
| [`docs/runbooks/`](docs/runbooks) · [`docs/postmortems/`](docs/postmortems) | what to do when it pages; what happened last time |
| [`docs/ai-guardrails.md`](docs/ai-guardrails.md) | rate limits, prompt guard, PII masking, failover, cost telemetry, evals |
| [`docs/cost-attribution.md`](docs/cost-attribution.md) | who pays for what, and how the model is built |
| [`docs/adr/`](docs/adr) | the decisions, their alternatives and their known gaps |
| [`docs/API_DOCUMENTATION.md`](docs/API_DOCUMENTATION.md) · [`docs/GRAPHQL_DOCUMENTATION.md`](docs/GRAPHQL_DOCUMENTATION.md) | the public contract |
| [`docs/parity-plan.md`](docs/parity-plan.md) | the monolith → microservices migration, phase by phase |
| [`AGENTS.md`](AGENTS.md) | commands, invariants and definition of done for humans and coding agents |

## License

Same as monolith — see `LICENSE` on `main`.
