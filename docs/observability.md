# Observability

Three pillars: **metrics** (Prometheus + Grafana), **traces** (OpenTelemetry
+ Jaeger), **logs** (structured JSON, ready for any log aggregator).

## Tracing

Every service has Micrometer Tracing on the OpenTelemetry bridge, exporting
spans over OTLP HTTP to Jaeger. Trace IDs propagate through Spring Cloud
Gateway's WebFlux pipeline, through Spring MVC controllers on downstream
services, into Kafka producers and consumers, and into outbound `RestTemplate`
calls (challenge-goal → activity-service, ai-service → Groq).

Configuration is shared in `infrastructure/config-server/src/main/resources/
config/application.yml` so every service picks it up automatically:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0           # 100% in dev — sample down for prod
  otlp:
    tracing:
      endpoint: http://jaeger:4318/v1/traces
```

A typical trace for `POST /api/activities` (the saga from `docs/saga.md`)
shows 6+ spans:

```
api-gateway      [POST /api/activities]   ────────────────────────────
└─ activity-svc  [persist Activity]       ──────────
   └─ activity-svc [compute PRs]               ──────────
      └─ Kafka producer [stride.activity.created]   ─
      └─ Kafka producer [stride.pr.achieved]        ─
                                                    └─ social-svc [project to feed_items] ─
                                                    └─ challenge-goal-svc [evict cache] ─
                                                    └─ notification-svc [create + SSE push] ──
```

Jaeger UI at http://localhost:16686 — filter by `service=api-gateway`,
look at recent traces, drill into the cross-service waterfall.

The MDC logging pattern in the shared config injects `traceId`/`spanId`
into every log line:

```
%5p [${spring.application.name},%X{traceId:-},%X{spanId:-}]
```

So you can grep logs by `traceId` to assemble a single request's path
across all services that touched it.

## Metrics

Each service exposes `/actuator/prometheus`. Prometheus discovers them via
**Eureka service discovery** (not static targets) — see `monitoring/
prometheus.yml`:

```yaml
scrape_configs:
  - job_name: stride-services
    eureka_sd_configs:
      - server: http://discovery-server:8761/eureka
    relabel_configs:
      - source_labels: [__meta_eureka_app_name]
        target_label: app
      - source_labels: [__meta_eureka_app_instance_ip_addr,
                        __meta_eureka_app_instance_port]
        separator: ':'
        target_label: __address__
      - target_label: __metrics_path__
        replacement: /actuator/prometheus
```

This means new services automatically show up in Prometheus the moment
they register with Eureka — no Prometheus config change required.

### What to look at

| Metric (PromQL)                                                       | Meaning                                          |
|-----------------------------------------------------------------------|--------------------------------------------------|
| `rate(http_server_requests_seconds_count{app="api-gateway"}[1m])`     | Request rate at the edge                         |
| `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{}[5m]))` | P95 latency per service               |
| `resilience4j_circuitbreaker_state{name="activity-service"}`          | challenge-goal's CB on activity-service          |
| `resilience4j_circuitbreaker_state{name="groq"}`                      | ai-service's CB on Groq                          |
| `kafka_consumer_records_lag_max{client_id=~"...-consumer-.*"}`        | Kafka consumer lag — alert if > 1000             |
| `cache_gets_total{result="hit"} / cache_gets_total`                   | challenge-goal's Redis cache hit ratio           |

## Logs

Structured JSON via `logstash-logback-encoder`. Each log event carries:

- `@timestamp`
- `level`
- `service` (from `spring.application.name`)
- `traceId`, `spanId` (MDC)
- `message`, `stack_trace` (if any)
- `logger_name`, `thread_name`

Drop into ELK, Loki, CloudWatch Logs, or any structured log aggregator
without preprocessing. For local docker-compose, `docker compose logs -f
<service>` is sufficient.

## Health & readiness

Spring Boot Actuator separates liveness and readiness probes
(`management.endpoint.health.probes.enabled: true`):

- `/actuator/health/liveness` — is the JVM up? Returning 200 means
  "don't restart me." K8s liveness probe uses this.
- `/actuator/health/readiness` — am I ready to serve traffic? Returns 200
  only after Spring context refresh completes AND dependencies (DB,
  Redis, Eureka registration) are healthy. K8s readiness probe uses this
  to gate inclusion in the Service's endpoint list.

The K8s manifests in `k8s/services/*` wire these properly with sensible
`initialDelaySeconds` — Spring Boot startup is ~30-60s, so liveness
probes wait that long before checking.

## What's not done yet

- **Saga observability** — per-event tracing of the Kafka chain end-to-end
  in a single Jaeger trace requires explicit context propagation through
  the Kafka producer/consumer record headers. Spring Cloud Sleuth and
  Micrometer Tracing both support this; needs enabling in
  `KafkaTemplate` config. (Phase 9b follow-up.)

Closed since this list was written: SLO dashboards and burn-rate alerting
([`docs/slo.md`](slo.md), `stride-slo`), Alertmanager with 41 tested rules
([`monitoring/alerts/`](../monitoring/alerts), [`docs/runbooks/`](runbooks)).
