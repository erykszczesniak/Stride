---
applyTo: "monitoring/**"
---

# Alert rules, recording rules and dashboards

## Files

- `monitoring/alerts/<area>.rules.yml` — Prometheus rule groups (alerts + recording rules). One file per area: `platform`, `services`, `kafka`, `ai`, `slo`, `self-observability`, `cost`.
- `monitoring/alerts/tests/<area>.test.yml` — `promtool test rules` cases. Every alert needs at least one firing case and one non-firing case.
- `monitoring/alertmanager/alertmanager.yml` — routing. Routes match on `severity` and `team`.
- `monitoring/grafana/dashboards/*.json` — provisioned dashboards. Datasource must be referenced by type (`{"type":"prometheus","uid":"${DS_PROMETHEUS}"}`), never by a hardcoded uid.

## Required shape of an alert

```yaml
- alert: GatewayHighErrorRate
  expr: |
    sum(rate(http_server_requests_seconds_count{app="strava-backend",status=~"5.."}[5m]))
      /
    sum(rate(http_server_requests_seconds_count{app="strava-backend"}[5m])) > 0.05
  for: 5m
  labels:
    severity: critical          # critical | warning | info
    team: platform              # platform | domain | ai | data
    service: strava-backend
    tier: "2"                   # 1 = notify, 2 = actionable (page / ticket)
  annotations:
    summary: "Gateway 5xx ratio {{ $value | humanizePercentage }} over 5m"
    description: "More than 5% of edge requests fail. Users see errors."
    runbook_url: https://github.com/erykszczesniak/Stride/blob/main/docs/runbooks/edge-high-error-rate.md
    dashboard_url: http://localhost:3003/d/stride-slo
```

## Rules of thumb (learned the hard way)

- **Ratios, not counts.** `5xx / total`, never `5xx > 10`. Counts alert on traffic, not on health.
- **`for` longer than the flap.** Network blips last 1–3 min; a `for` of 5m removes most false positives. Only `ServiceDown` and `SLOBurnRateFast` may use `for: 2m`.
- **Debounce scheduled sources.** If the metric is refreshed on a schedule (batch job, exporter with cache), wrap it in `min_over_time(...[3× refresh interval])` before comparing.
- **Silence is a failure too.** Pair every "too much" alert with an `absent()` or traffic-collapse alert.
- **Correlate on the unit someone acts on.** Group by `app` (and `instance` for host-level alerts) so one incident does not swallow the others.
- **Do not raise thresholds to kill noise.** Find the benign condition (backup window, deploy, warm-up) and exclude it with `unless` / `and on()`.
- **New alerts start as `severity: info`** for one week before being promoted; put that in the PR.

## Verification

```bash
docker run --rm -v "$PWD/monitoring:/m" --entrypoint promtool prom/prometheus:v2.55.0 check rules /m/alerts/*.yml
docker run --rm -v "$PWD/monitoring:/m" --entrypoint promtool prom/prometheus:v2.55.0 test rules /m/alerts/tests/*.yml
docker run --rm -v "$PWD/monitoring:/m" --entrypoint amtool prom/alertmanager:v0.27.0 check-config /m/alertmanager/alertmanager.yml
```
