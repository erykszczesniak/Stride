---
name: EdgeTrafficCollapse
description: Requests to the backend dropped to almost zero although there was traffic an hour ago — users cannot reach the app even though the backend reports healthy.
metadata:
  owner: platform
  version: "1.0"
alert_type: availability
severity: warning
actionability: "Investigate and escalate"
allowed-tools: []
---

# Alert: EdgeTrafficCollapse

## Summary

"Silence is a failure too." `up` is 1, no error ratio is elevated, yet
nobody is arriving. Users reach the backend only through the frontend's
nginx (`/api/` and `/graphql` proxied to `backend:8080`) in compose, or the
ingress `strava.local` (`/api/(.*)` → backend, everything else → frontend)
in k8s; the iOS client calls `/pipeline/v1` directly. Typical causes: the
frontend container is down or nginx cannot resolve its upstream, ingress /
DNS / TLS broken, or a frontend build that points at another API base
(CRA bakes `REACT_APP_API_URL` at build time; `docker-compose.yml` passes
`/api/v1`). The metric cannot tell these apart; the checks below can.

## Required inputs

- Alert start time
- Whether traffic is expected at this hour (a demo stack at night legitimately has none — the rule requires > 0.5 req/s in the previous hour precisely to avoid that)

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/alerts/platform.rules.yml` |
| Expression | `sum(rate(all[10m])) < 0.05 and sum(rate(all[1h] offset 1h)) > 0.5` on `app="strava-backend"`, `uri!~"/actuator.*"` |
| Threshold / for | `for: 10m` |
| Dashboard | http://localhost:3003/d/stride-slo ("Request rate monolith") |

## Success criteria

- Request rate back to the previous hour's level.
- The broken hop identified (frontend / nginx / ingress / DNS / build).

## Investigation

### 1. Can you reach the backend directly and through the proxy?

| Command | What to look for |
|---|---|
| `curl -s -o /dev/null -w '%{http_code} %{time_total}\n' localhost:8081/actuator/health` | 200 within a second — the backend itself |
| `curl -s -o /dev/null -w '%{http_code}\n' localhost/health` | 200 (`OK`) — nginx alive |
| `curl -s -o /dev/null -w '%{http_code}\n' localhost/api/v1/activities/feed` | 401 = the proxy path works (auth required); 502 = nginx cannot reach `backend:8080`; connection refused = frontend container down |
| `docker compose ps frontend` / `docker compose logs --since 30m frontend \| tail` | `unhealthy`; `host not found in upstream "backend"` |
| `kubectl -n strava get ingress strava-ingress` and `kubectl -n ingress-nginx get pods` | address assigned, controller running; then `curl -s -o /dev/null -w '%{http_code}\n' http://strava.local/api/v1/activities/feed` |
| `openssl s_client -connect <host>:443 -servername <host> </dev/null 2>/dev/null \| openssl x509 -noout -dates` | only if TLS was enabled on the ingress (it is commented out in `k8s/ingress.yaml`): `notAfter` in the past = expired certificate |

### 2. Is the SPA sending requests anywhere?

| Command | What to look for |
|---|---|
| Browser → DevTools → Network on the SPA | requests going to `/api/v1/...` on the same origin. Another host or port = a build with a different `REACT_APP_API_URL` |
| `docker compose logs --since 30m frontend \| grep -c ' /api/'` | nginx access log: are API requests arriving at nginx at all? |

### 3. Traffic low but not zero — what is left?

```promql
sum by (status) (rate(http_server_requests_seconds_count{app="strava-backend",uri!~"/actuator.*"}[10m]))
```

- Only 401: every token is invalid — `JWT_SECRET` changed (compose `.env`, k8s `backend-secret`). Every session is logged out; expected after a deliberate rotation, an incident otherwise.
- Only 429 on `/api/v1/auth/login`: the per-IP limiter (`RateLimitingFilter`, 10 logins / 5 registrations per minute per IP). Behind a proxy that does not set `X-Forwarded-For`, every user shares one IP and the whole site gets 10 logins a minute. The frontend's nginx and the ingress do set it; a new hop in front may not.

## Mitigation

- **Safe:** `docker compose restart frontend`; restart the ingress controller pod; rebuild the frontend image with the correct `REACT_APP_API_URL`.
- **Needs review:** rotating `JWT_SECRET` (all users are logged out); changing the limiter constants — they are code in `RateLimitingFilter`, not configuration.

## Escalation / close

- Escalate to **platform** with the three checks above and their outputs.
- Close when "Request rate monolith" on `stride-slo` recovers.
