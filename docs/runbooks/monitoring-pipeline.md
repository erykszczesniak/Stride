---
name: MonitoringPipeline
description: The monitoring stack itself is broken — Prometheus has no targets, rules fail to evaluate, Alertmanager is down or cannot deliver, or the Watchdog heartbeat stopped arriving.
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
nothing*. The remaining alerts name the broken hop.

## Required inputs

- Which alert (or: the Watchdog stopped — how long ago at the receiver)
- `docker compose ps prometheus alertmanager alert-sink` / the monitoring namespace in k8s

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/alerts/self-observability.rules.yml` |
| Expression | `vector(1)`; `absent(up{job="stride-services"})`; `increase(prometheus_rule_evaluation_failures_total[10m]) > 0`; `prometheus_tsdb_head_series > 500000`; `up{job="alertmanager"} == 0`; `increase(alertmanager_notifications_failed_total[10m]) > 0` |
| Threshold / for | see rule file |

## Success criteria

- Watchdog arrives at the receiver every 30 minutes again.
- Prometheus → Status → Targets shows every service `UP`; Rules page shows every group `OK`.

## Investigation

### 1. Watchdog silent — walk the path

| Command | What to look for |
|---|---|
| `curl -s localhost:9090/-/healthy` | Prometheus up |
| `curl -s localhost:9090/api/v1/alerts \| jq '.data.alerts[] \| select(.labels.alertname=="Watchdog") \| .state'` | `firing` — if not, rules are not loaded (step 3) |
| `curl -s localhost:9093/-/healthy` and `curl -s localhost:9093/api/v2/alerts \| jq length` | Alertmanager up and receiving alerts |
| `docker compose logs --since 30m alert-sink \| grep -c Watchdog` | deliveries arriving; 0 = receiver or routing broken |
| `curl -s localhost:9093/api/v2/status \| jq '.config.original' \| head` | the config Alertmanager actually loaded |

### 2. No targets (`PrometheusTargetsMissing`)

| Command | What to look for |
|---|---|
| `curl -s localhost:8761/eureka/apps \| grep -c '<instance>'` | instances registered; 0 = every service lost Eureka, or Eureka restarted and leases have not renewed (30 s) |
| Prometheus → Status → Service Discovery → `stride-services` | discovered vs dropped targets; relabel errors |
| `docker compose logs --since 15m prometheus \| grep -i eureka` | `connection refused` to `discovery-server:8761` |

### 3. Rules failing (`PrometheusRuleEvaluationFailures`)

```bash
docker compose logs --since 15m prometheus | grep -i "Evaluating rule failed" | tail
docker run --rm -v "$PWD/monitoring:/m" --entrypoint promtool prom/prometheus:v2.55.0 check rules /m/alerts/*.rules.yml
```

A rule can pass `promtool check` and still fail at runtime (e.g. `many-to-many matching` once real labels appear). Fix the expression, add the case to the unit tests, `curl -X POST localhost:9090/-/reload`.

### 4. Cardinality (`PrometheusHighCardinality`)

```promql
topk(10, count by (__name__) ({__name__=~".+"}))
```

Usual offender: a `uri` label with ids in it (a controller mapping missing a path variable pattern) or `client_id` per request. Fix at the source (Micrometer `uri` tags), never by dropping the metric silently.

### 5. Deliveries failing (`AlertmanagerNotificationsFailing`)

```bash
docker compose logs --since 15m alertmanager | grep -i "notify" | tail
```

`connection refused` to the webhook = receiver down; `4xx` = payload rejected by the target (Teams/Slack schema) — test with `amtool alert add --alertmanager.url=http://localhost:9093 test severity=info`.

## Mitigation

- **Safe:** restart prometheus / alertmanager / alert-sink; `curl -X POST localhost:9090/-/reload` after fixing a rule.
- **Needs review:** editing `alertmanager.yml` routes/receivers (run `amtool check-config` first), changing scrape/SD config.

## Escalation / close

- Escalate to **platform**; this outranks every other open alert.
- Close when the Watchdog has arrived twice in a row at the receiver.
