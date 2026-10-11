# Observability

Three pillars for one deployable, joined by the trace id: **metrics**
(Prometheus + Grafana + Alertmanager), **traces** (OpenTelemetry Collector →
Jaeger), **logs** (JSON → Alloy → Loki, with log-based alerts). Everything
below runs from `docker-compose.yml` (`--profile observability` adds Loki
and Alloy); `k8s/` deploys the app with health probes only. The design and
its alternatives are in [ADR-012](adr/012-logs-as-a-pipeline.md).

## Tracing

The backend has Micrometer Tracing on the OpenTelemetry bridge
(`micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`), exporting
spans over OTLP HTTP to the **OpenTelemetry Collector**, which forwards them
to Jaeger. In compose `OTEL_EXPORTER_OTLP_ENDPOINT` points at
`otel-collector:4318`; a backend started outside compose (`make dev-backend`)
still hits Jaeger's published `localhost:4318` directly. Configuration in
`application.yml`:

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

### The collector: what is redacted, what is kept

`monitoring/otel/otel-collector.yml` (image
`otel/opentelemetry-collector-contrib:0.115.1`, validated in CI with
`otelcol-contrib validate`) is the one place where trace policy lives:

```
backend ──OTLP/HTTP──▶ otel-collector ──OTLP/gRPC──▶ jaeger
                        memory_limiter
                        attributes/pii      delete authorization/cookie/set-cookie/x-api-key
                                            headers and url.query; hash enduser.id / user.id
                        redaction           mask bearer/JWT tokens, token/code/state/password/
                                            secret/api_key query params, e-mail addresses — in
                                            any attribute value (Spring puts the full URL with
                                            its query string in http.url)
                        tail_sampling       keep every trace with an ERROR span, every request
                                            whose outcome is CLIENT_ERROR/SERVER_ERROR (4xx/5xx —
                                            a handled 401/404 still writes an ERROR log line that
                                            must link somewhere), every trace slower than 1 s;
                                            10 % of the rest (decision 10 s after the first span)
                        batch
```

Why: the e-mail address is the login identifier here, so it would otherwise
appear in a span the moment an instrumentation records a request body or a
query; OAuth callback routes carry `code`/`state` in the query string; and
head sampling at 10 % in the application (what `prod` used to do) throws
away 90 % of the errors — the traces a responder actually wants. The
backend therefore samples at 100 % and lets the collector decide. The cost:
a span count in Jaeger is no longer a request count — use Prometheus for
counts, and the collector's own metrics (job `otel-collector`,
`otelcol_receiver_accepted_spans` vs `otelcol_exporter_sent_spans`,
`otelcol_processor_tail_sampling_count_traces_sampled` by policy) to
see what the sampler kept. The `stride-logs` dashboard shows both.

Jaeger UI at http://localhost:16686 — one service besides `jaeger-query`,
named `strava` (`spring.application.name` in `application.properties`; the
log label `app` is the compose service, `strava-backend`). Grafana has Jaeger
as a datasource too (`monitoring/grafana/provisioning/datasources/jaeger.yml`),
with **Logs for this span** wired to Loki (`tracesToLogsV2`, custom query
`{app="strava-backend"} |= "<traceId>"`, ±5 min).

Every log line carries `traceId` / `spanId` — in the console pattern for a
local `dev` run, as JSON fields in compose and `prod` (see Logs) — so one
request's path, including its async listeners, is one query:
`{app="strava-backend"} |= "<traceId>"` in Loki, or `docker compose logs
backend | grep <traceId>` without the profile.

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
AI spend), `stride-logs` (error lines, top loggers, log volume, a logs panel
with trace links, collector throughput and sampling decisions). Grafana at
http://localhost:3003 (`admin` / `admin`).

### Alerting

30 rules in `monitoring/alerts/*.rules.yml`, unit-tested and contract-checked
in CI ([ADR-006](adr/006-alerts-as-code.md)), plus four log-based rules in
`monitoring/loki/rules/` (see Logs), all routed by Alertmanager
(http://localhost:9093) to `alert-sink`, an echo container:
`docker compose logs -f alert-sink` shows exactly what a pager or a Teams
channel would receive. Every alert links to a runbook in
[`runbooks/`](runbooks/).

## Logs

```
backend (JSON, one line per event, traceId/spanId/requestId/userId from MDC)
   │  stdout → Docker json-file log driver
   ▼
alloy  (grafana/alloy:v1.5.1, --profile observability)      monitoring/alloy/config.alloy
   │  discovery.docker over /var/run/docker.sock, keep containers on *strava-network
   │  app = compose service (backend → strava-backend, same value as Prometheus)
   │  stage.json: level, logger_name, traceId, spanId
   │  labels: app, level            structured metadata: logger, traceId, spanId
   ▼
loki   (grafana/loki:3.3.2, :3100)                           monitoring/loki/loki.yml
   │  tsdb/v13, filesystem, retention 72 h (compactor)
   │  ruler: monitoring/loki/rules/fake/*.yml, 1 m ──▶ alertmanager:9093
   ▼
grafana  Explore → Loki; dashboard stride-logs; derived field traceId → Jaeger
```

**Format.** `logback-spring.xml` has two appenders. `(default | dev | test |
testcontainers) & !json` is the coloured console with the trace pattern;
`prod | json` is `logstash-logback-encoder` (`LogstashEncoder`, UTC, MDC
keys `requestId`, `userId`, `traceId`, `spanId`, logger names shortened to
36 characters). Compose adds `SPRING_PROFILES_INCLUDE=json`, so the stack
logs JSON with the `dev` levels; `prod` is unchanged; a local `make
dev-backend` keeps the console. The encoder's field names are the ones the
pipeline and the rules depend on: `level`, `logger_name`, `message`,
`stack_trace`, `traceId`.

**Labels.** Only `app` and `level` are index labels — five levels times a
handful of containers. `traceId` is unique per request; as a label it would
create one stream per request, so it is *structured metadata*: filterable
(`| traceId="…"`), aggregatable, linkable, not indexed. `logger` likewise
(`sum by (logger)` works without a parser). Lines from the other containers
(Postgres, Prometheus, nginx) are not JSON and keep only `app`; so do the
backend's own non-JSON lines — `spring.jpa.show-sql: true` in `dev` prints
SQL through `System.out` — which is why the rules and panels select
`level="…"` rather than the bare stream.

**Retention** is 72 hours (`limits_config.retention_period`, enforced by the
compactor) — enough to work an incident and write the post-mortem;
Prometheus keeps 15 days of the metrics.

**From an alert to the trace to the logs.**

1. A log alert (`LogAuthFailureSpike`, `LogExceptionRateSpikePerLogger`,
   `LogErrorVolumeAboveBaseline`, `LogPipelineSilent` —
   [`runbooks/log-alerts.md`](runbooks/log-alerts.md)) or a metric alert
   links to a dashboard. On `stride-logs`, set *Level* to `ERROR`, pick the
   logger from the top-loggers panel.
2. Expand a line in the logs panel: next to `traceId` there is a **Trace in
   Jaeger** button (Loki datasource, derived field → Jaeger datasource). The
   trace is always there for an error — tail sampling keeps every trace with
   an ERROR span.
3. In the trace, **Logs for this span** runs `{app="strava-backend"} |=
   "<traceId>"` in Loki, ±5 minutes, so the async listeners' lines show up
   too.

Useful queries:

| LogQL | Meaning |
|---|---|
| `{app="strava-backend"} \|= "<traceId>"` | every line of one request, async work included |
| `sum by (level) (count_over_time({app="strava-backend"}[5m]))` | volume per level |
| `topk(10, sum by (logger) (count_over_time({app="strava-backend", level="ERROR"}[1h])))` | who writes the errors |
| `{app="strava-backend", level="ERROR"} \|= "AuthenticationController.login() threw exception: Bad credentials"` | failed password logins (the 401 itself is not logged) |
| `{app="strava-backend"} \| json \| logger_name=~".*ai.audit"` | one line per provider call ([`ai-guardrails.md`](ai-guardrails.md)) |

Two things that shape the ERROR stream: `LoggingAspect` logs every exception
thrown through a controller at ERROR (including expected 404s and the
`Bad credentials` of a failed login), and `ApiExceptionHandler` logs
`Unexpected error` with a stack trace for every 500.

**Log alerts as code.** `monitoring/loki/rules/fake/stride-logs.rules.yml`
(`fake` is Loki's tenant id when `auth_enabled: false`) carries the same
labels and annotations as the Prometheus rules and is routed by the same
Alertmanager; `check_alert_rules.py` checks both sources and rejects a
duplicate alert name across them. There is no promtool for LogQL, so the
rules are exempt from the unit-test rule — explicitly: the check prints a
`NOTE` per rule — and `monitoring/scripts/check_loki_rules.sh` instead starts
Loki with the repo's config and rule files and asserts every alert is listed
by `/loki/api/v1/rules` and every expression is accepted by
`/loki/api/v1/query`. Both run in `.github/workflows/monitoring.yml`.

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

- **No monitoring in `k8s/`** — Prometheus, Alertmanager, Grafana, the
  collector, Loki and Alloy exist only in compose; a cluster deployment runs
  blind except for probes.
- **Logs are tailed from Docker, not sent over OTLP** — resource attributes
  on a line come from compose labels; the collector handles traces only.
- **No behavioural tests for the Loki rules** — parse + load in CI, thresholds
  tuned on compose traffic ([ADR-012](adr/012-logs-as-a-pipeline.md), Known gaps).
- **Client spans** for JDBC, Elasticsearch and the two providers.
- **Freshness metrics** for the in-process event paths (feed projection,
  SSE delivery, search-index sync) — today their failures are WARN or ERROR
  log lines, nothing more.
- **Real receivers** — Alertmanager delivers to the echo container; Teams /
  Slack / PagerDuty is a configuration change in
  `monitoring/alertmanager/alertmanager.yml`.
