---
name: GatewayTrafficCollapse
description: Requests through the gateway dropped to almost zero although there was traffic an hour ago — users cannot reach the platform even though every health check is green.
metadata:
  owner: platform
  version: "1.0"
alert_type: availability
severity: warning
actionability: "Investigate and escalate"
allowed-tools: []
---

# Alert: GatewayTrafficCollapse

## Summary

"Silence is a failure too." Every service reports healthy, no error ratio is
elevated, yet nobody is arriving. Typical causes: ingress or DNS broken, TLS
certificate expired, a frontend deploy pointing at the wrong API base URL,
or the load balancer draining the gateway. The metric cannot tell these
apart; the checks below can.

## Required inputs

- Alert start time
- Whether traffic is expected at this hour (a demo stack at night legitimately has none — the rule requires >0.5 req/s in the previous hour precisely to avoid that)

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/alerts/platform.rules.yml` |
| Expression | `rate(all[10m]) < 0.05 and rate(all[1h] offset 1h) > 0.5` on the gateway |
| Threshold / for | 10m |

## Success criteria

- Edge request rate back to the previous hour's level.
- The broken hop identified (ingress / DNS / frontend / gateway).

## Investigation

### 1. Can you reach the gateway from outside?

| Command | What to look for |
|---|---|
| `curl -s -o /dev/null -w '%{http_code} %{time_total}\n' http://stride.local/actuator/health` (kind) / `localhost:8080/actuator/health` (compose) | 200 within a second. Connection refused or timeout = ingress/network |
| `kubectl -n stride get ingress stride-gateway` and `kubectl -n ingress-nginx get pods` | address assigned, controller running |
| `openssl s_client -connect <host>:443 -servername <host> </dev/null 2>/dev/null \| openssl x509 -noout -dates` | `notAfter` in the past = expired certificate |

### 2. Is the frontend sending requests anywhere?

| Command | What to look for |
|---|---|
| Browser → DevTools → Network on the SPA | requests going to the wrong host/port (CRA builds bake `REACT_APP_API_URL` at build time) |
| `docker compose logs --since 30m frontend \| tail` | nginx serving, no 404 on `/static/*` |

### 3. Is the gateway accepting connections but rejecting everything?

```promql
sum by (status) (rate(http_server_requests_seconds_count{app="api-gateway"}[10m]))
```

Only 401/403? The JWT secret changed (Config Server vs user-service mismatch) — every token is now invalid. Only 429? Rate limiter misconfigured to a near-zero replenish rate.

## Mitigation

- **Safe:** restart the ingress controller pod; redeploy the frontend with the correct API URL.
- **Needs review:** rotating `JWT_SECRET` (all users are logged out), changing rate-limiter values in `api-gateway.yml`.

## Escalation / close

- Escalate to **platform** with the three checks above and their outputs.
- Close when the request-rate panel on `stride-slo` recovers.
