---
name: GatewayHighErrorRate
description: More than 5% (critical) or 1% (warning) of requests through the API gateway end in 5xx — users are seeing errors.
metadata:
  owner: platform
  version: "1.0"
alert_type: availability
severity: critical
actionability: "Page — act now"
allowed-tools: []
---

# Alert: GatewayHighErrorRate / GatewayElevatedErrorRate

## Summary

The edge 5xx ratio (excluding `/actuator`) is above threshold. The gateway
itself rarely produces 5xx; almost always it is relaying a failing
downstream (`503` when Eureka has no instance, `500`/`502` when the service
threw). The job is to find *which route*, then follow that service's runbook.

## Required inputs

- Alert start time; the SLO dashboard's error-ratio panel for the same window
- Which routes fail (step 1)
- Deploys in the last hour

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/alerts/platform.rules.yml` |
| Expression | `sum(rate(5xx[5m])) / sum(rate(all[5m])) > 0.05` on `app="api-gateway"`, `uri!~"/actuator.*"` |
| Threshold / for | 5 % for 5m (critical); 1 % for 10m (warning) |
| Dashboard | http://localhost:3001/d/stride-slo |

## Success criteria

- Ratio below 1 % for 10 minutes and the alert resolved.
- The failing route and root cause identified; a post-mortem opened if it paged.

## Investigation

### 1. Which route, which status?

Prometheus (http://localhost:9090):

```promql
topk(5, sum by (uri, status) (rate(http_server_requests_seconds_count{app="api-gateway",status=~"5.."}[5m])))
```

| Status | Meaning |
|---|---|
| 503 | no healthy instance behind the route (Eureka) → [service-down.md](service-down.md) |
| 502 / 500 | the downstream returned an error → that service's `ServiceHighErrorRate`, [service-high-error-rate.md](service-high-error-rate.md) |
| 504 | downstream too slow → [high-latency.md](high-latency.md) |
| 429 | not 5xx; a client is being rate-limited by `RequestRateLimiter` (Redis) — check the caller, not the gateway |

### 2. Is the gateway itself healthy?

| Command | What to look for |
|---|---|
| `curl -s localhost:8080/actuator/health` | `UP`; `redis` component — the rate limiter needs Redis, without it every request may fail |
| `docker compose logs --since 15m api-gateway \| grep -i -E "error\|exception" \| tail -20` | `JwtException` bursts (bad secret after a config change), `Connection refused: redis` |
| `curl -s localhost:8888/api-gateway/default \| head` | Config Server serves the routes; a broken YAML here breaks every route |

### 3. Correlate with a deploy

`docker compose ps --format '{{.Name}} {{.RunningFor}}'` — anything recreated right before the alert started is the prime suspect. Roll it back first, investigate second.

## Mitigation

- **Safe:** roll back the most recent deploy of the failing downstream; restart Redis if the health check shows it down.
- **Needs review:** editing `api-gateway.yml` routes or `jwt.public-paths` (see post-mortem [2026-08-16-gateway-public-paths](../postmortems/2026-08-16-gateway-public-paths.md) for how a list binding silently broke auth).

## Escalation / close

- Escalate to the owning team of the failing route with the `topk` output and the gateway log excerpt.
- Close when the SLO dashboard shows the error ratio back under 0.5 % (the budget line).
