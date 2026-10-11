---
name: MonitoringPipeline
description: The monitoring stack itself is broken — Prometheus has no target, rules fail to evaluate, Alertmanager is down or cannot deliver, or the Watchdog heartbeat stopped arriving.
metadata:
  owner: platform
  version: "1.0"
alert_type: self-observability
severity: critical
actionability: "Page — act now (everything else is blind)"
allowed-tools: []
---

# Alert: Watchdog / PrometheusTargetsMissing / PrometheusRuleEvaluationFailures / PrometheusHighCardinality / AlertmanagerDown / AlertmanagerNotificationsFailing

## Summary

The one failure no other alert can report. `Watchdog` is always firing by
design — if the receiver stops getting it, the path Prometheus → Alertmanager
→ receiver is broken somewhere, and *silence from every other alert means
nothing*. The remaining alerts name the broken hop. The whole stack lives in
`docker-compose.yml`: Prometheus (`monitoring/prometheus/prometheus.yml` and
`monitoring/alerts/` mounted read-only, `--web.enable-lifecycle`),
Alertmanager (`monitoring/alertmanager/alertmanager.yml`) and `alert-sink`,
an HTTP echo container that logs every delivery. There is no monitoring in
`k8s/`.

Two more hops exist since ADR-012 and are **not** covered by these alerts:
the OpenTelemetry Collector between the backend and Jaeger
(`monitoring/otel/otel-collector.yml`; its own metrics are scraped as job
`otel-collector`, so a dead collector shows up as `up{job="otel-collector"} == 0`
on the Prometheus targets page, and Jaeger simply stops receiving traces),
and the log pipeline Alloy → Loki → ruler → Alertmanager, whose silence is
reported by `LogPipelineSilent` and handled in [log-alerts.md](log-alerts.md).
A Loki alert never arriving while `Watchdog` does means the Loki ruler, not
Alertmanager: `curl -s localhost:3100/loki/api/v1/rules` and
`docker compose logs --since 15m loki | grep -i ruler`.

## Required inputs

- Which alert (or: the Watchdog stopped — how long ago at the receiver)
- `docker compose ps backend prometheus alertmanager alert-sink`

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/alerts/self-observability.rules.yml` |
| Expression | `vector(1)`; `absent(up{job="strava-backend"})`; `increase(prometheus_rule_evaluation_failures_total[10m]) > 0`; `prometheus_tsdb_head_series > 500000`; `up{job="alertmanager"} == 0`; `sum by (integration) (increase(alertmanager_notifications_failed_total[10m])) > 0` |
| Threshold / for | Watchdog: none (info, delivered every 30 min); targets missing 5m; rule failures 0m; cardinality 15m; Alertmanager down 2m; notifications failing 0m |

## Success criteria

- Watchdog arrives at the receiver (`/watchdog` in the `alert-sink` log) every 30 minutes again.
- Prometheus → Status → Targets shows `strava-backend`, `prometheus` and `alertmanager` `UP`; the Rules page shows every group `OK`.

## Investigation

### 1. Watchdog silent — walk the path

| Command | What to look for |
|---|---|
| `curl -s localhost:9091/-/healthy` | Prometheus up. If the container does not exist at all: `prometheus` has `depends_on: backend: condition: service_healthy`, so an unhealthy backend at `docker compose up` keeps Prometheus from ever starting |
| `curl -s localhost:9091/api/v1/alerts \| jq '.data.alerts[] \| select(.labels.alertname=="Watchdog") \| .state'` | `firing` — if empty, rules are not loaded (step 3) |
| `curl -s localhost:9093/-/healthy` and `curl -s localhost:9093/api/v2/alerts \| jq length` | Alertmanager up and holding alerts |
| `docker compose logs --since 30m alert-sink \| grep -c '/watchdog'` | deliveries arriving (the echo logs the request path); 0 = routing or delivery broken |
| `curl -s localhost:9093/api/v2/status \| jq -r '.config.original' \| head -40` | the config Alertmanager actually loaded |

### 2. No target (`PrometheusTargetsMissing`)

| Command | What to look for |
|---|---|
| `curl -s localhost:9091/api/v1/targets \| jq '.data.activeTargets[] \| {job: .labels.job, health: .health, lastError: .lastError}'` | the `strava-backend` job present at all; if present but down, that is [service-down.md](service-down.md), not this alert |
| `docker run --rm -v "$PWD/monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro" -v "$PWD/monitoring/alerts:/etc/prometheus/rules:ro" --entrypoint promtool prom/prometheus:v2.55.0 check config /etc/prometheus/prometheus.yml` | the config parses (same command as `.github/workflows/monitoring.yml`) |
| `docker compose config \| grep -A2 'prometheus.yml'` | Prometheus mounts the file you think it does |

The target is static and the `app="strava-backend"` label is attached in the
scrape config; renaming the job or the label breaks every rule
(`job="strava-backend"`, `app="strava-backend"` selectors).

### 3. Rules failing (`PrometheusRuleEvaluationFailures`)

```bash
docker compose logs --since 15m prometheus | grep -i "Evaluating rule failed" | tail
docker run --rm -v "$PWD/monitoring:/m" --entrypoint promtool prom/prometheus:v2.55.0 check rules /m/alerts/*.rules.yml
docker run --rm -v "$PWD/monitoring:/m" --entrypoint promtool prom/prometheus:v2.55.0 test rules /m/alerts/tests/*.test.yml
python3 monitoring/scripts/check_alert_rules.py monitoring/alerts
```

A rule can pass `promtool check` and still fail at runtime (e.g.
`many-to-many matching` once real labels appear). Fix the expression, add
the case to `monitoring/alerts/tests/`, then `curl -X POST localhost:9091/-/reload`.
Rules over series that do not exist yet (the `ai_*` metrics before the
guardrails code is deployed) do not fail — they evaluate to nothing.

### 4. Cardinality (`PrometheusHighCardinality`)

```promql
topk(10, count by (__name__) ({__name__=~".+"}))
```

Usual offender: a label with unbounded values (a per-user id, a raw path
instead of the handler pattern in `uri`). The latency histograms enabled in
`application.yml` multiply series per endpoint by the bucket count — a new
endpoint costs tens of series, not thousands; 500k means an unbounded label.
Fix at the source (Micrometer tags), never by dropping the metric silently.

### 5. Deliveries failing (`AlertmanagerNotificationsFailing`)

```bash
docker compose logs --since 15m alertmanager | grep -i notify | tail
docker compose ps alert-sink
```

Every receiver here is a `webhook` to `alert-sink:8000`; `connection refused`
= the sink is down. Test the path end to end:

```bash
docker compose exec alertmanager amtool alert add --alertmanager.url=http://localhost:9093 test severity=info
docker compose logs --since 2m alert-sink
```

## Mitigation

- **Safe:** restart `prometheus` / `alertmanager` / `alert-sink`; `curl -X POST localhost:9091/-/reload` after fixing a rule.
- **Needs review:** editing `alertmanager.yml` routes / receivers (run `amtool check-config` first — the command is in `.github/workflows/monitoring.yml`), changing the scrape config.

## Escalation / close

- Escalate to **platform**; this outranks every other open alert.
- Close when the Watchdog has arrived twice in a row at `/watchdog`.
