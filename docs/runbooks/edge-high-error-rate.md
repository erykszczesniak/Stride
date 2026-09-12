---
name: EdgeHighErrorRate
description: More than 5% (critical) or 1% (warning) of backend requests end in 5xx, one endpoint fails more than 20% of the time, or the log streams more than one ERROR per second — users are seeing errors.
metadata:
  owner: domain
  version: "1.0"
alert_type: availability
severity: critical
actionability: "Page — act now (EdgeHighErrorRate); ticket for the other three"
allowed-tools: []
---

# Alert: EdgeHighErrorRate / EdgeElevatedErrorRate / EndpointHighErrorRate / ErrorLogSpike

## Summary

The backend *is* the edge. Nothing sits in front of it except the
frontend's nginx (`/api/`, `/graphql` → `backend:8080`) and, in k8s, the
ingress — so the 5xx ratio is the application's own. A 500 is an unhandled
exception (`ApiExceptionHandler.handleGenericException` answers
`{"error":"An unexpected error occurred"}` and logs `Unexpected error` with
the stack trace); 502 and 503 come only from the AI chat path. nginx's own
502/504 (backend down, or slower than `proxy_read_timeout 60s`) never reach
this metric — that is [service-down.md](service-down.md) and
[high-latency.md](high-latency.md).

| Alert | Says | Threshold |
|---|---|---|
| `EdgeHighErrorRate` | bad enough to page | > 5 % of requests over 5m, for 5m — critical |
| `EdgeElevatedErrorRate` | the error budget is burning | > 1 % over 10m, for 10m — warning |
| `EndpointHighErrorRate` | *which* `method uri` is broken | > 20 % of that endpoint at > 0.1 req/s over 10m, for 5m — warning |
| `ErrorLogSpike` | something fails with no HTTP signal | > 1 ERROR line/s over 5m, for 10m — warning |

Usual causes, most to least frequent on this stack: Postgres unreachable or
the pool exhausted; a bad deploy (exception on a new code path); one client
hitting an unhandled edge case; a provider failure on `/api/v1/ai/**`.
Elasticsearch failures do **not** produce 5xx — user search falls back to SQL
(`UserController.searchUsers`) and the index sync only logs a WARN.

## Required inputs

- Which `method` / `uri` / `status` (step 1)
- Last deploy time (`docker compose ps --format '{{.Name}} {{.RunningFor}}'`)
- The "Error ratio monolith (1h)" panel on `stride-slo` for the same window

## Alert metadata

| Property | Value |
|---|---|
| Rule files | `monitoring/alerts/platform.rules.yml` (`Edge*`), `monitoring/alerts/services.rules.yml` (`EndpointHighErrorRate`, `ErrorLogSpike`) |
| Expression | `sum(rate(5xx[5m])) / sum(rate(all[5m])) > 0.05` on `app="strava-backend"`, `uri!~"/actuator.*"`; endpoint: `sum by (app, uri, method)` of the same over 10m `> 0.20 and rate > 0.1`; logs: `sum by (app) (rate(logback_events_total{level="error"}[5m])) > 1` |
| Threshold / for | 5 % for 5m; 1 % for 10m; 20 % for 5m; 1 error/s for 10m |
| Dashboard | http://localhost:3003/d/stride-slo (edge), http://localhost:3003/d/stride-jvm ("HTTP Errors (4xx / 5xx)") |

## Success criteria

- Ratio under 0.5 % (the budget line on "Error ratio monolith (1h)") for 10 minutes and no new ERROR lines.
- The failing endpoint and root cause named; if it was a deploy, the fix is a new PR, not a hot edit.

## Investigation

### 1. Which endpoint, which status

Prometheus (http://localhost:9091):

```promql
topk(10, sum by (method, uri, status) (rate(http_server_requests_seconds_count{app="strava-backend",status=~"5..",uri!~"/actuator.*"}[5m])))
```

The `uri` label is the handler pattern (`/api/v1/activities/{activityId}`),
so one broken id does not become one series per id.

| Status / uri | Meaning |
|---|---|
| 500 anywhere | unhandled exception → step 2 |
| 502 on `/api/v1/ai/activities/{activityId}/chat` | Anthropic (or the Groq fallback) failed or returned an empty reply → [ai-provider-degraded.md](ai-provider-degraded.md) |
| 503 on the same | `AI chat is not configured`: no `ANTHROPIC_API_KEY`. `docker-compose.yml` does not pass that variable (only `GROQ_API_KEY`), so on a composed stack this is configuration, not an incident |
| 5xx on `/api/v1/ai/generate-goal` or `/generate-plan` | Groq (`Error calling Groq API` in the log) → [ai-provider-degraded.md](ai-provider-degraded.md) |
| 401 / 403 / 429 | not 5xx. Only-401 traffic = JWT problem ([edge-traffic-collapse.md](edge-traffic-collapse.md)); 429 on `/api/v1/auth/login` or `/register` = the per-IP `RateLimitingFilter` (10 / 5 per minute) |

### 2. Read the logs, first exception first

```bash
docker compose logs --since 15m backend | grep -n -m 5 -B 2 -A 25 "ERROR"
```

| Log signature | Meaning |
|---|---|
| `Unexpected error` + stack trace | the 500s; the first `com.strava.backend` frame names the code path |
| `HikariPool-1 - Connection is not available, request timed out after 30000ms` / `PSQLException: Connection ... refused` | pool exhausted or Postgres down → [db-pool-exhausted.md](db-pool-exhausted.md) |
| `Claude chat request failed: ...` / `Error calling Groq API: ...` | provider → [ai-provider-degraded.md](ai-provider-degraded.md) |
| `WeakKeyException` or `DecodingException` on `/api/v1/auth/login` | `JWT_SECRET` is not valid base64 or shorter than 256 bits — every login fails, everything else works (`JwtProvider` decodes the key per request, so this does not fail at boot) |
| `<== XxxController.method() threw exception: ...` | `LoggingAspect` logs at ERROR for **every** exception thrown through a controller — including the expected 404/400 `ApiRequestException`s. This feeds `ErrorLogSpike` without any 5xx |
| `Failed to send weekly summary for user N` | the Monday 09:00 job (`WeeklySummaryServiceImpl`) cannot send mail — no HTTP signal, `ErrorLogSpike` only |
| `Failed to index user N into Elasticsearch` | WARN, not ERROR; search still works (SQL fallback) |

### 3. `ErrorLogSpike` without 5xx

```promql
sum(rate(logback_events_total{app="strava-backend",level="error"}[5m]))
sum by (uri, status) (rate(http_server_requests_seconds_count{app="strava-backend",status=~"4.."}[5m]))
```

If the 5xx rate is flat, the ERROR lines are the aspect logging 4xx (a client
probing ids → a 404 storm on one `uri`) or a background job. Neither is a
user-facing outage; the first may be abuse.

### 4. Dependencies from the app's point of view

| Command | What to look for |
|---|---|
| `curl -s -H "Authorization: Bearer $TOKEN" localhost:8081/actuator/health \| jq '.components \| map_values(.status)'` | `db` or `elasticsearch` `DOWN` |
| `docker compose ps postgres elasticsearch` | `unhealthy`, `Restarting` |

### 5. Correlate with a deploy

`docker compose ps --format '{{.Name}} {{.RunningFor}}'` and `git log --oneline -5`
— anything recreated right before the alert started is the prime suspect.
Roll it back first, investigate second.

## Mitigation

- **Safe:** roll back the most recent deploy; restart the dependency that is down (Hikari reconnects on its own).
- **Not safe:** silencing the alert while the ratio is high; `docker compose restart backend` when the cause is a dependency (it comes back into the same state); editing data by hand.

## Escalation / close

- Escalate to **domain** with the `topk` line, the first stack trace and the health JSON.
- Close when "Error ratio monolith (1h)" on `stride-slo` is back under 0.5 %. Post-mortem if it paged.
