# Stride — Microservices Architecture

> **This is the `microservices` branch.** For the monolithic implementation of the same application, see the [`main`](https://github.com/erykszczesniak/strava/tree/main) branch.
>
> Both branches implement the same product (a Strava-like fitness tracking app). The split lets the codebase serve as a portfolio piece showing two architectural approaches side-by-side.

## What's here

A Spring Cloud microservices rewrite of the Stride monolith. Eight domain services behind an API gateway, communicating via Kafka events and synchronous REST where appropriate, each with its own PostgreSQL schema (database-per-service).

```
                    ┌─────────────────┐
                    │  API Gateway    │  Spring Cloud Gateway
                    │  (JWT validate) │  · Redis rate limit · CORS
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        ▼                    ▼                    ▼
  ┌──────────┐         ┌──────────┐         ┌──────────┐
  │  User /  │         │ Activity │         │  Social  │
  │   Auth   │         │          │         │  / Feed  │
  └──────────┘         └──────────┘         └──────────┘
        │                    │                    │
        │              ┌─────┴──────┐             │
        │              ▼            ▼             │
        │        ┌──────────┐ ┌──────────┐        │
        │        │  Clubs   │ │Challenges│        │
        │        │ / Events │ │ / Goals  │        │
        │        └──────────┘ └──────────┘        │
        │                    │                    │
        └────────┬───────────┴──────────┬─────────┘
                 ▼                      ▼
         ┌──────────────┐      ┌──────────────┐
         │Notifications │      │      AI      │
         │  (Kotlin)    │      │  (Kotlin)    │
         └──────────────┘      └──────────────┘

Cross-cutting infra:
  · Eureka (discovery) · Spring Cloud Config (config) · Kafka (events)
  · Redis (cache + rate limit + SSE) · Resilience4j (CB + retry)
  · OpenTelemetry → Jaeger (tracing) · Micrometer → Prometheus → Grafana
```

## Status

| Phase | Scope                                                    | State    |
|-------|----------------------------------------------------------|----------|
| 0     | Scaffold (parent pom, shared libs, infra docker-compose) | done     |
| 1     | API Gateway + Eureka + Config Server                     | done     |
| 2     | User / Auth service                                      | next     |
| 3     | Activity service                                         | planned  |
| 4     | Notification service (Kotlin)                            | planned  |
| 5     | Social / Feed service (CQRS read-model)                  | planned  |
| 6     | Clubs + Events service                                   | planned  |
| 7     | Challenges + Goals service                               | planned  |
| 8     | AI service (Kotlin, Groq, Resilience4j)                  | planned  |
| 9     | Saga, full observability, AWS demo slice                 | planned  |

## Run locally

Prereqs: Docker, Java 21, Maven 3.9+.

```bash
cp .env.example .env
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

## Layout

```
.
├── pom.xml                          # multi-module aggregator
├── docker-compose.yml               # full local stack
├── shared/
│   ├── events/                      # Kafka event DTOs (shared by producers/consumers)
│   └── security/                    # JWT validator + UserContext helpers
├── infrastructure/
│   ├── config-server/               # Spring Cloud Config Server (native, classpath-served)
│   ├── discovery-server/            # Netflix Eureka standalone
│   └── api-gateway/                 # Spring Cloud Gateway + JWT filter
├── services/                        # domain services (added per phase)
├── monitoring/                      # Prometheus scrape config, Grafana provisioning
├── frontend/                        # React SPA (unchanged from monolith, points at gateway)
└── k8s/                             # Kubernetes manifests (added per phase)
```

## Tech choices (and why)

- **Spring Boot 3.4 + Spring Cloud 2024.0** — current LTS pair, what's actually shipping in enterprise Java today
- **Kafka (not RabbitMQ)** — more common in mid/senior backend job listings; demonstrates stream-processing knowledge
- **Eureka (not Consul)** — Spring-native, zero-friction setup; Consul is overkill for this scale
- **JWT validation in gateway only** — services trust propagated `X-User-Id` header; simpler, faster, demonstrates correct API gateway pattern
- **Resilience4j** — circuit breaker on Groq API + retry on Google OAuth (Phase 8); standard JVM resilience library
- **Database-per-service** — each service owns its schema; cross-service reads via REST or async events, never shared joins
- **CQRS-light** (Phase 5) — social feed built as read-model from Activity/Comment events, no joining across services
- **Saga pattern** (Phase 9) — activity creation → PR check → notification, demonstrated via Kafka event chain

## Not included (intentionally)

- gRPC — REST+JSON is fine at this scale, gRPC obscures the demo
- Istio / service mesh — Spring Cloud LoadBalancer + Eureka covers discovery + LB
- Full event sourcing — only the feed read-model is event-sourced
