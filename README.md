# Stride - Microservices

Stride is a Strava-like platform for tracking activities, goals, records, clubs,
events and training progress. This branch presents the product as 12 Spring Boot
services. The [`main`](https://github.com/erykszczesniak/Stride/tree/main) branch
provides the same features as a monolith.

**Built with:** Java 21, Kotlin, Spring Boot, React, PostgreSQL, Kafka, gRPC,
Redis, Elasticsearch, Docker and Kubernetes.

The React frontend and iOS `/pipeline/v1` client use the same public contract on
both branches: `/api/v1`, `/graphql` and `/pipeline/v1`.

## Table of contents

- [Technologies](#technologies)
- [Project structure](#project-structure)
- [How to run](#how-to-run)
- [Screenshots](#screenshots)
- [Architecture at a glance](#architecture-at-a-glance)
- [Service inventory](#service-inventory)
- [Patterns demonstrated](#patterns-demonstrated)
- [Operations and quality](#operations-and-quality)
- [Phases](#phases)
- [What's intentionally not here](#whats-intentionally-not-here)
- [Documentation](#documentation)
- [License](#license)

## Technologies

- **Backend:** Java 21, Kotlin 1.9, Spring Boot 3.4, Spring Cloud 2024
- **Frontend:** React, Axios, Playwright
- **Data:** PostgreSQL 16, Redis 7, Elasticsearch
- **Communication:** REST at the edge, gRPC between services, Kafka events
- **Resilience:** Resilience4j, retries, circuit breakers and timeouts
- **Observability:** OpenTelemetry, Jaeger, Prometheus, Alertmanager and Grafana
- **Delivery:** Maven, Docker Compose, Kubernetes, Terraform and GitHub Actions
- **Quality:** JUnit 5, AssertJ, Mockito, JaCoCo, ArchUnit, Testcontainers,
  CodeQL and Trivy

## Project structure

### Repository layout

```text
.
├── frontend/                       # React SPA and Playwright tests
├── infrastructure/
│   ├── api-gateway/                # routing, JWT, CORS, rate limiting
│   ├── config-server/              # central service configuration
│   └── discovery-server/           # Eureka registry
├── services/
│   ├── user-service/               # auth, profiles, follows, search
│   ├── activity-service/           # activities, records, gear, imports
│   ├── notification-service/       # notifications, SSE, email
│   ├── social-service/             # feed, likes, comments, reactions
│   ├── club-event-service/         # clubs and events
│   ├── challenge-goal-service/     # challenges, goals, training plans
│   ├── ai-service/                 # AI coach and plan generation
│   ├── web-bff/                    # dashboard, reports and exports
│   └── pipeline-service/           # iOS API and personal tokens
├── shared/
│   ├── events/                     # Kafka event contracts
│   ├── grpc-contracts/             # internal protobuf contracts
│   └── security/                   # shared identity filters
├── monitoring/                     # dashboards, alerts and tests
├── docs/                           # ADRs, SLOs, runbooks and guides
├── k8s/                            # Kubernetes manifests
├── terraform/aws/                  # AWS free-tier example
├── scripts/smoke.sh                # post-deployment smoke test
├── docker-compose.yml              # local stack
└── pom.xml                         # Maven reactor
```

## How to run

### Run it locally

#### Docker Compose

Requirements: Java 21, Docker and Docker Compose.

```bash
cp .env.example .env
./mvnw clean package -DskipTests
docker compose up -d
```

The default command starts the lean application stack. Add optional tooling when
needed:

```bash
docker compose --profile observability --profile search up -d
scripts/smoke.sh
```

| URL | Purpose |
|---|---|
| [localhost:8080](http://localhost:8080) | API Gateway |
| [localhost:8761](http://localhost:8761) | Eureka |
| [localhost:8888](http://localhost:8888) | Config Server |
| [localhost:8090](http://localhost:8090) | Kafka UI |
| [localhost:16686](http://localhost:16686) | Jaeger |
| [localhost:9090](http://localhost:9090) | Prometheus |
| [localhost:9093](http://localhost:9093) | Alertmanager |
| [localhost:3001](http://localhost:3001) | Grafana, login `admin/admin` |

#### Kubernetes with kind

Follow [`k8s/README.md`](k8s/README.md) for the kind and Helm setup.

#### One service in standalone mode

Standalone mode uses H2 and local mocks, without Kafka or Eureka:

```bash
SPRING_PROFILES_ACTIVE=standalone \
  java -jar services/user-service/target/user-service.jar
```

#### Stop the stack

```bash
docker compose down
```

### Run it on AWS (free tier)

[`terraform/aws/`](terraform/aws/) contains an example with RDS, S3, ECR, EC2
and Parameter Store. It is not deployed automatically.

```bash
cd terraform/aws
terraform init
TF_VAR_db_password=... TF_VAR_jwt_secret=... terraform apply
```

EKS is excluded because its control plane is not free. Use kind for the full
Kubernetes demo.

## Screenshots

Screenshots use the same React frontend on both branches. The planned gallery
covers:

| View | Features shown |
|---|---|
| Dashboard | recent activity, weekly progress, goals and events |
| Activities | list, details, route map, stats and personal records |
| AI Coach | activity analysis and saved conversations |
| Social | personalized feed, likes, comments and emoji reactions |
| Clubs and events | discovery, membership and event registration |
| Goals and challenges | progress, leaderboards and training plans |
| Profile and settings | user data, privacy and notification preferences |
| Admin | platform statistics, users, activities and exports |

See [`docs/SCREENSHOTS_CHECKLIST.md`](docs/SCREENSHOTS_CHECKLIST.md) for file
names, routes and capture guidelines. Add final images to `docs/screenshots/`.

## Architecture at a glance

```text
Browser / iOS
      |
      v
API Gateway :8080  ->  JWT, CORS, routing, rate limits
      |
      +---------------- REST public API ----------------+
      |                                                  |
      v                                                  v
Domain services                                     Web and iOS BFFs
      |                                                  |
      +------------- gRPC internal calls ---------------+
      |
      +------------- Kafka domain events ---------------+
      |
      +-> PostgreSQL per service, Redis and Elasticsearch

Metrics -> Prometheus -> Grafana
Traces  -> OpenTelemetry -> Jaeger
```

Eureka provides service discovery. Config Server provides shared runtime
configuration. Internal gRPC ports are not exposed through the gateway.

Details: [architecture](docs/architecture.md), [event saga](docs/saga.md) and
[observability](docs/observability.md).

## Service inventory

| Service | HTTP | Language | Responsibility |
|---|---:|---|---|
| config-server | 8888 | Java | central YAML configuration |
| discovery-server | 8761 | Java | Eureka service registry |
| api-gateway | 8080 | Java | routing, JWT, CORS and rate limiting |
| user-service | 8081 | Java | auth, profiles, follows, search and user admin |
| activity-service | 8082 | Java | activities, records, gear, files and Strava import |
| notification-service | 8083 | Kotlin | notifications, SSE, email and weekly summaries |
| social-service | 8084 | Java | feed, likes, comments and reactions |
| club-event-service | 8085 | Java | clubs, members, events and registrations |
| challenge-goal-service | 8086 | Java | challenges, goals and training plans |
| ai-service | 8087 | Kotlin | AI activity chat and goal or plan generation |
| web-bff | 8088 | Java | dashboard, GraphQL reports, exports and admin stats |
| pipeline-service | 8089 | Java | iOS facade and `tri_` personal tokens |

Notification and AI services show Kotlin adoption inside a Java platform.

## Patterns demonstrated

| Pattern | Implementation |
|---|---|
| Database per service | each service owns its schema; remote references are IDs |
| REST at the edge | clients use `/api/v1`, `/graphql` and `/pipeline/v1` |
| gRPC inside | protobuf calls use Eureka, deadlines and Resilience4j |
| Event choreography | Kafka distributes typed events from `shared/events` |
| CQRS feed | social-service builds a per-user feed from activity and follow events |
| Gateway trust | the gateway validates JWTs and replaces `X-User-*` headers |
| BFF per client | web-bff serves the SPA; pipeline-service serves iOS |
| Local projections | services keep only remote data needed for their reads |
| Graceful degradation | caches, fallbacks and breakers isolate failures |
| Search fallback | user search uses Elasticsearch, then SQL `ILIKE` |
| Central configuration | Config Server provides production overrides |
| Defense in depth | NetworkPolicy limits access to domain and gRPC ports |

## Operations and quality

The repository keeps operational checks close to the code.

| Area | Included |
|---|---|
| CI | reactor and frontend tests, JaCoCo, ArchUnit and PostgreSQL migrations |
| Security | CodeQL, Trivy, Dependabot, actions pinned to commit SHAs, `SECURITY.md` and gateway identity checks |
| Alerts | Prometheus rules, promtool tests, owners, runbooks and Alertmanager |
| SLOs | availability, latency, burn-rate and error-budget rules |
| Observability | service metrics, distributed traces and Grafana dashboards |
| AI safety | rate limits, prompt checks, PII masking, failover and evals |
| Cost | service and AI usage attribution with dashboards and alerts |
| Verification | compose smoke tests and nightly Playwright E2E tests |
| Decisions | ADRs, runbooks and post-mortems in `docs/` |

Main references: [`CHANGELOG.md`](CHANGELOG.md), [`docs/slo.md`](docs/slo.md),
[`docs/runbooks/`](docs/runbooks), [`docs/ai-guardrails.md`](docs/ai-guardrails.md)
and [`docs/cost-attribution.md`](docs/cost-attribution.md).

Grafana provides `stride-jvm`, `stride-slo` and `stride-cost`. View local alert
deliveries with `docker compose logs -f alert-sink`.

## Phases

| Phase | Scope |
|---:|---|
| 0-1 | repository scaffold, gateway, Eureka and Config Server |
| 2-9 | initial domain services, Kafka, deployment and architecture docs |
| 10-11 | frontend parity, `/api/v1`, shared events and first tests |
| 12-19 | complete feature parity across domain services |
| 20-21 | web BFF and iOS pipeline BFF |
| 22 | infrastructure, CI, documentation and E2E wiring |
| 23 | protobuf contracts, gRPC servers and internal client migration |

The detailed migration history is in
[`docs/parity-plan.md`](docs/parity-plan.md) and the Git history.

## What's intentionally not here

These are deliberate scope choices:

- **Service mesh:** Eureka, Spring Cloud and NetworkPolicy cover current needs.
- **Full event sourcing:** only the social feed uses an event-built read model.
- **Saga orchestrator:** Kafka choreography fits the current workflows.
- **EKS provisioning:** its control plane conflicts with the free-tier example.
- **Shared domain database:** database ownership stays a hard service boundary.

## Documentation

| Document | Topic |
|---|---|
| [`docs/architecture.md`](docs/architecture.md) | service boundaries and communication |
| [`docs/saga.md`](docs/saga.md) | Kafka event flow and choreography |
| [`docs/observability.md`](docs/observability.md) | metrics, traces, logs and health |
| [`docs/slo.md`](docs/slo.md) | objectives, burn rates and error budgets |
| [`docs/runbooks/`](docs/runbooks) | incident response |
| [`docs/postmortems/`](docs/postmortems) | incident analysis |
| [`docs/ai-guardrails.md`](docs/ai-guardrails.md) | AI privacy, telemetry and evals |
| [`docs/cost-attribution.md`](docs/cost-attribution.md) | infrastructure and AI cost model |
| [`docs/adr/`](docs/adr) | architecture decisions and alternatives |
| [`docs/API_DOCUMENTATION.md`](docs/API_DOCUMENTATION.md) | REST contract |
| [`docs/GRAPHQL_DOCUMENTATION.md`](docs/GRAPHQL_DOCUMENTATION.md) | GraphQL contract |
| [`docs/parity-plan.md`](docs/parity-plan.md) | monolith to microservices migration |
| [`AGENTS.md`](AGENTS.md) | repository rules and commands |

## License

Same license as the monolith. See `LICENSE` on `main`.
