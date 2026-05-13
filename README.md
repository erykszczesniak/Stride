# Stride — Microservices Architecture

> This is the **`microservices` branch**. The same application also exists as a Spring Boot monolith on [`main`](https://github.com/erykszczesniak/Stride/tree/main). Both branches implement the same product (a Strava-like fitness tracking app); the split makes the codebase serve as a portfolio piece showing two architectural approaches to the same domain side-by-side.

```
┌──────────────────────────────────────────────────────────────────────┐
│  10 Spring Boot/Kotlin services · Kafka events · Eureka discovery    │
│  Spring Cloud Config · Redis cache · Resilience4j · CQRS-light       │
│  OpenTelemetry → Jaeger · Micrometer → Prometheus → Grafana          │
│  K8s manifests · Terraform AWS slice · Database-per-service          │
└──────────────────────────────────────────────────────────────────────┘
```

## Phases (all 9 shipped)

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
| 9  | K8s manifests · Terraform AWS slice · architecture docs · saga doc       | this commit  |

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
                       │  · JWT verify  │
                       │  · CORS        │
                       └────────┬───────┘
                                │  (lb:// via Eureka)
        ┌───────────┬───────────┼───────────┬───────────┬─────────┐
        ▼           ▼           ▼           ▼           ▼         ▼
   ┌────────┐  ┌─────────┐  ┌─────────┐  ┌────────┐  ┌─────────┐  ┌────┐
   │  user  │  │activity │  │ social  │  │ club/  │  │challenge│  │ ai │
   │ :8081  │  │ :8082   │  │ :8084   │  │ event  │  │ /goal   │  │ Kt │
   └───┬────┘  └────┬────┘  └────┬────┘  │ :8085  │  │ :8086   │  │8087│
       │            │            │       └────────┘  └────┬────┘  └─┬──┘
       │            └────────┬───┴──────────┬─────────────┘         │
       │                     │              │                       ▼
       │                     ▼              ▼                  Groq API
  ┌────────────────────────────────────────────────────────────────────┐
  │                          Kafka topics                              │
  │  user.registered · activity.created · activity.updated             │
  │  pr.achieved · comment.added · reaction.added                      │
  └─────────────────────────────────┬──────────────────────────────────┘
                                    ▼
                         ┌────────────────────┐
                         │ notification (Kt)  │ ← SSE push to browsers
                         │ :8083              │
                         └────────────────────┘

  7× Postgres (database-per-service)   Tracing → Jaeger   Metrics → Prometheus
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
| user-service             | 8081  | Java       | Identity, auth, profiles, follow graph            |
| activity-service         | 8082  | Java       | Activities, personal records                      |
| notification-service     | 8083  | **Kotlin** | Notifications (Kafka consumer + SSE push)         |
| social-service           | 8084  | Java       | Comments, reactions, CQRS feed                    |
| club-event-service       | 8085  | Java       | Clubs, members, events, participants              |
| challenge-goal-service   | 8086  | Java       | Challenges, goals (Resilience4j + Redis cache)    |
| ai-service               | 8087  | **Kotlin** | Groq Chat Completions proxy                       |

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

## Run it locally

### Option A — docker-compose (full stack)

```bash
cp .env.example .env       # adjust JWT_SECRET, GROQ_API_KEY if you want real Groq
./mvnw clean package -DskipTests
docker compose up -d
```

Endpoints:

| URL                    | What                          |
|------------------------|-------------------------------|
| http://localhost:8080  | API Gateway                   |
| http://localhost:8761  | Eureka dashboard              |
| http://localhost:8888  | Config Server                 |
| http://localhost:8090  | Kafka UI                      |
| http://localhost:16686 | Jaeger UI                     |
| http://localhost:9090  | Prometheus                    |
| http://localhost:3001  | Grafana (admin/admin)         |

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
│   └── ai-service/                 # Phase 8 (Kotlin, Groq)
├── k8s/                            # Kubernetes manifests (Phase 9)
├── terraform/aws/                  # AWS free-tier deployment skeleton
├── monitoring/                     # Prometheus + Grafana provisioning
├── docs/                           # architecture, saga, observability
└── frontend/                       # React SPA (unchanged from monolith)
```

## Tech stack

- **Spring Boot 3.4** · **Spring Cloud 2024.0.0** · **Java 21** · **Kotlin 1.9**
- **Postgres 16** · **Kafka 3.7 (KRaft)** · **Redis 7**
- **Resilience4j 2.2** · **Flyway 10** · **JJWT 0.12**
- **Jaeger 1.62** · **Prometheus 2.55** · **Grafana 11**
- **Maven 3.9** · **Docker** · **Kubernetes 1.30+**
- **Terraform 1.6+** (AWS provider 5.x)

## What's intentionally not here

Documented decisions, not gaps:

- **gRPC** — REST + JSON is fine at this scale, gRPC would obscure the demo
- **Istio / service mesh** — Spring Cloud LoadBalancer + Eureka cover discovery + LB; mesh adds setup cost without buying anything new for the portfolio narrative
- **Full event sourcing** — only the social feed is event-sourced; everything else is conventional CRUD
- **Saga orchestrator** — choreographed sagas (Kafka fan-out) are the right shape for this domain; documented in `docs/saga.md`
- **EKS in Terraform** — control plane is ~$73/mo, breaks the "free-tier demo" frame

## License

Same as monolith — see `LICENSE` on `main`.
