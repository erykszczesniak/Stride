# Observability

Three pillars for one deployable: **metrics** (Prometheus + Grafana +
Alertmanager), **traces** (OpenTelemetry + Jaeger), **logs** (structured
JSON under the `prod` profile). Everything below runs from
`docker-compose.yml`; `k8s/` deploys the app with health probes only.

## Tracing

The backend has Micrometer Tracing on the OpenTelemetry bridge
(`micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`), exporting
spans over OTLP HTTP to Jaeger. Configuration in `application.yml`:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0           # 100% in dev; application-prod.yml sets 0.1
    propagation:
      type: W3C
  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}
```

What a trace contains: the Spring MVC server span per request, and the
`@Async` work it triggers — `AsyncConfiguration` wraps the `stride-async-*`
executor in a Micrometer `ContextExecutorService`, so `NotificationEventListener`
(SSE push) and `UserSearchIndexSyncListener` (Elasticsearch) become child
spans instead of new roots. What it does not contain: JDBC, Elasticsearch
and provider calls are not instrumented as client spans (`GroqServiceImpl`
builds its own `RestTemplate`; the Anthropic SDK brings its own HTTP
client). A long server span with no children means time spent in one of
those — the runbooks say how to tell which.

Jaeger UI at http://localhost:16686 — one service besides `jaeger-query`.
Grafana has Jaeger as a datasource too (`monitoring/grafana/provisioning/datasources/jaeger.yml`).

The console log pattern in `logback-spring.xml` injects `traceId` / `spanId`
into every line:

```
%d %5p [<application name>,%X{traceId:-},%X{spanId:-}] ...
```

So `docker compose logs backend | grep <traceId>` assembles one request's
path, including its async listeners.

## Metrics

The backend exposes `/actuator/prometheus` (public; `/actuator/metrics/**`
needs a token and is not exposed under `prod`). Prometheus scrapes one
static target every 15 s — `monitoring/prometheus/prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'strava-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']
        labels:
          app: 'strava-backend'
```

The `app` label is set here so every rule and dashboard can use the same
selectors as the `microservices` branch (where it comes from Eureka).
Prometheus also scrapes itself and Alertmanager for the self-observability
rules, keeps 15 days, and accepts `curl -X POST localhost:9091/-/reload`.

Every meter carries `application`, `team` and `cost_center` tags
(`management.metrics.tags`, [`cost-attribution.md`](cost-attribution.md)),
and `http.server.requests` records a latency histogram with SLO buckets at
100 ms, 250 ms, 500 ms, 1 s and 2.5 s ([`slo.md`](slo.md)).

### What to look at

| Metric (PromQL) | Meaning |
|---|---|
| `sum(rate(http_server_requests_seconds_count{app="strava-backend",uri!~"/actuator.*"}[1m]))` | request rate at the edge |
| `histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{app="strava-backend"}[5m])))` | p95 latency |
| `sum by (method, uri, status) (rate(http_server_requests_seconds_count{app="strava-backend",status=~"5.."}[5m]))` | which endpoint fails |
| `hikaricp_connections_active`, `hikaricp_connections_pending`, `hikaricp_connections_timeout_total` | the one DB pool (max 10 by default) |
| `jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}` | heap pressure (max = 75 % of the container limit) |
| `rate(jvm_gc_pause_seconds_sum[5m])` | share of wall time in GC |
| `rate(logback_events_total{level="error"}[5m])` | ERROR lines per second (`ErrorLogSpike`) |
| `ai_requests_total`, `ai_request_duration_seconds`, `ai_request_cost_usd_total`, `ai_failover_total` | provider calls: outcome, latency, cost, failover ([`ai-guardrails.md`](ai-guardrails.md)) |
| `slo:http_error:ratio_rate1h`, `slo:error_budget_remaining:ratio` | the SLIs ([`slo.md`](slo.md)) |

Dashboards (provisioned from `monitoring/grafana/dashboards/`): `stride-jvm`
(HTTP rate / errors / p95, JVM heap, GC, threads, CPU, HikariCP),
`stride-slo` (SLIs, burn rates, budget), `stride-cost` (modelled infra cost,
AI spend). Grafana at http://localhost:3003 (`admin` / `admin`).

### Alerting

30 rules in `monitoring/alerts/*.rules.yml`, unit-tested and contract-checked
in CI ([ADR-006](adr/006-alerts-as-code.md)), routed by Alertmanager
(http://localhost:9093) to `alert-sink`, an echo container:
`docker compose logs -f alert-sink` shows exactly what a pager or a Teams
channel would receive. Every alert links to a runbook in
[`runbooks/`](runbooks/).

## Logs

Two profiles in `logback-spring.xml`:

- `default | dev | test | testcontainers`: coloured console with the
  trace pattern above; `com.strava`, Spring Security and Hibernate SQL at
  DEBUG.
- `prod`: JSON via `logstash-logback-encoder` (`LogstashEncoder`), UTC,
  with the MDC keys `requestId`, `userId`, `traceId`, `spanId`; `com.strava`
  at INFO, Spring Security at WARN.

Drop the JSON into ELK, Loki, CloudWatch Logs, or any structured log
aggregator without preprocessing. Locally, `docker compose logs --since 15m backend`
is sufficient. Two things that shape the ERROR stream: `LoggingAspect` logs
every exception thrown through a controller at ERROR (including expected
404s), and `ApiExceptionHandler` logs `Unexpected error` with a stack trace
for every 500. The `ai.audit` logger adds one JSON line per provider call
([`ai-guardrails.md`](ai-guardrails.md)).

## Health & readiness

Spring Boot Actuator separates liveness and readiness probes
(`management.endpoint.health.probes.enabled: true`):

- `/actuator/health/liveness` — is the JVM up? K8s liveness probe
  (`periodSeconds: 15`).
- `/actuator/health/readiness` — ready to serve? K8s readiness probe
  (`periodSeconds: 10`).
- `/actuator/health` — the aggregate, used by the compose healthcheck and
  the k8s **startup** probe (60 s initial delay, up to 30 × 10 s). It
  includes the database and Elasticsearch indicators, so an unreachable
  Elasticsearch makes it `DOWN` while the app keeps serving — see
  [service-down.md](runbooks/service-down.md).

Components are shown only to an authenticated caller
(`show-details: when_authorized`); the mail indicator is disabled.

## What's not done yet

- **No monitoring in `k8s/`** — Prometheus, Alertmanager and Grafana exist
  only in compose; a cluster deployment runs blind except for probes.
- **Client spans** for JDBC, Elasticsearch and the two providers.
- **Freshness metrics** for the in-process event paths (feed projection,
  SSE delivery, search-index sync) — today their failures are WARN or ERROR
  log lines, nothing more.
- **Real receivers** — Alertmanager delivers to the echo container; Teams /
  Slack / PagerDuty is a configuration change in
  `monitoring/alertmanager/alertmanager.yml`.
