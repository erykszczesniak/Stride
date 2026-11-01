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
- **Deployment:** Maven, Docker Compose, Kubernetes, Terraform and GitHub Actions
- **QA:** JUnit 5, AssertJ, Mockito, JaCoCo, ArchUnit, Testcontainers,
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
├── monitoring/                     # Prometheus, alerts, rule tests, Grafana
├── docs/                           # ADRs, SLOs, runbooks, screenshots
├── k8s/                            # Kubernetes manifests
├── terraform/aws/                  # AWS free-tier example
├── scripts/smoke.sh                # post-deployment smoke test
├── .github/workflows/              # CI, CodeQL, Trivy, PR policy, release
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

The React frontend is shared with `main`, so both branches look the same.
Captured from the local Docker Compose stack with the sample accounts. Images
live in [`docs/screenshots/`](docs/screenshots/) and are listed in
[`docs/SCREENSHOTS_CHECKLIST.md`](docs/SCREENSHOTS_CHECKLIST.md).

### Authentication

| Login | Register |
|---|---|
| ![Login](docs/screenshots/login.png) | ![Register](docs/screenshots/register.png) |
| Email and password sign-in, or Strava OAuth. | Password strength is shown while you type. |

### Dashboard

| Light mode | Dark mode |
|---|---|
| ![Dashboard](docs/screenshots/dashboard.png) | ![Dashboard in dark mode](docs/screenshots/dark-mode.png) |
| Monthly stats, recent activities, weekly goals and upcoming events. | The same view with the dark theme from the sidebar toggle. |

### Activities

| Feed | Record |
|---|---|
| ![Activities](docs/screenshots/activities.png) | ![Record activity](docs/screenshots/record-activity.png) |
| Feed with scope and sport filters, search and challenge progress. | Manual entry or GPX, TCX and FIT upload. |

| Details | Details in dark mode |
|---|---|
| ![Activity details](docs/screenshots/activity-details.png) | ![Activity details in dark mode](docs/screenshots/activity-details-dark.png) |
| Route map, pace, heart rate, elevation, splits, likes and comments. | Leaflet map and charts follow the theme. |

### Profile

| Overview | Personal records |
|---|---|
| ![Profile](docs/screenshots/profile.png) | ![Personal records](docs/screenshots/profile-personal-records.png) |
| All-time stats, followers and achievements. | Best times per distance with improvement deltas. |

### Goals, challenges and training

| Goals | Challenges |
|---|---|
| ![Goals](docs/screenshots/goals.png) | ![Challenges](docs/screenshots/challenges.png) |
| Weekly and monthly targets with progress bars. | Joined challenges, explore tab and leaderboard. |

| Training plans | Reports |
|---|---|
| ![Training plans](docs/screenshots/training-plans.png) | ![Reports](docs/screenshots/reports.png) |
| Ready-made plans and an AI generator. | Report builder with CSV and PDF export. |

### Social

| Clubs | Events |
|---|---|
| ![Clubs](docs/screenshots/clubs.png) | ![Events](docs/screenshots/events.png) |
| Club discovery with categories and membership. | Upcoming events with capacity and registration. |

| Create event | Mobile |
|---|---|
| ![Create event](docs/screenshots/create-event.png) | <img src="docs/screenshots/mobile-view.png" alt="Mobile dashboard" width="260"> |
| Event form with type, date, location and capacity. | Collapsed sidebar at phone width. |

### Settings

| Profile | Preferences |
|---|---|
| ![Settings profile](docs/screenshots/settings-profile.png) | ![Settings preferences](docs/screenshots/settings-preferences.png) |
| Personal data, bio, weight and height. | Units, default sport, language and theme. |

| Gear | |
|---|---|
| ![Settings gear](docs/screenshots/settings-gear.png) | |
| Shoes and bikes with mileage tracking. | |

### Admin

| Overview | Users |
|---|---|
| ![Admin overview](docs/screenshots/admin-overview.png) | ![Admin users](docs/screenshots/admin-users.png) |
| Platform totals and the activity table. | User list with roles, status and actions. |

| Export | |
|---|---|
| ![Admin export](docs/screenshots/admin-export.png) | |
| Export activities or users as TXT, XLSX or PDF. | |

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
| CI | reactor and frontend tests, JaCoCo, ArchUnit, PostgreSQL migrations and a SonarCloud quality gate |
| Security | CodeQL, Trivy, Dependabot, actions pinned to commit SHAs, `SECURITY.md` and gateway identity checks |
| Alerts | Prometheus rules, promtool tests, owners, runbooks and Alertmanager |
| SLOs | availability, latency, burn-rate and error-budget rules |
| Observability | service metrics, distributed traces and Grafana dashboards |
| AI safety | rate limits, prompt checks, PII masking, failover and evals |
| Cost | service and AI usage attribution with dashboards and alerts |
| Verification | compose smoke tests and nightly Playwright E2E tests |
| Releases | release-please tags, `release/ms-vX.Y` maintenance branches, hotfix cherry-picks and a PR policy check |
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
| [`docs/release.md`](docs/release.md) | release lifecycle, hotfixes and PR policy |
| [`docs/sonar.md`](docs/sonar.md) | SonarCloud setup and what the gate checks |
| [`docs/API_DOCUMENTATION.md`](docs/API_DOCUMENTATION.md) | REST contract |
| [`docs/GRAPHQL_DOCUMENTATION.md`](docs/GRAPHQL_DOCUMENTATION.md) | GraphQL contract |
| [`docs/parity-plan.md`](docs/parity-plan.md) | monolith to microservices migration |
| [`AGENTS.md`](AGENTS.md) | repository rules and commands |

## License

Copyright (c) 2026 Eryk Szczesniak. All rights reserved.

This repository is source-available for viewing only. You may read the code,
and clone and run it locally, solely to review or evaluate the author's work.
Copying, modifying, reusing, distributing or deploying it, in whole or in
part, is not permitted without written permission from the copyright holder.
See [LICENSE](LICENSE).
