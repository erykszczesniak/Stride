---
name: HighLatencyP95
description: 95th-percentile latency above 1 s across the backend, or above 2 s on one endpoint, for 10 minutes — the app feels slow and browser timeouts follow.
metadata:
  owner: domain
  version: "1.0"
alert_type: latency
severity: warning
actionability: "Ticket — same working day"
allowed-tools: []
---

# Alert: EdgeHighLatencyP95 / EndpointHighLatencyP95

## Summary

Latency alerts rarely have a single cause. Work from the whole to the
endpoint, then decide where the time goes: in the JVM (GC, pool wait), in
Postgres (slow query), in Elasticsearch (user search), or in a provider
(Anthropic / Groq have their own 15 s alert, `AiChatLatencyP95`). Two things
skew p95 without being incidents: `GET /api/v1/notifications/stream` is an
SSE connection that lives up to 5 minutes (`SseEmitterServiceImpl.SSE_TIMEOUT`)
and is recorded when it ends, and exports / file imports are long by nature
(`/api/v1/export/**`, `POST /api/v1/import/activities`; the Strava bulk import
runs on an `@Async` thread and is not in the HTTP histogram at all). Users
stop waiting before you do: the SPA's axios client times out at 30 s
(`frontend/src/api/ApiClient.jsx`), nginx at 60 s (`proxy_read_timeout`).

**Verified by experiment** ([`chaos/experiments/postgres-netem-delay.yaml`](../../chaos/experiments/postgres-netem-delay.yaml),
2026-10-11): a 400 ms netem delay on Postgres under 4 load workers pushed the edge
p95 above 1 s in 24 s; `EdgeHighLatencyP95` was pending after 34 s and active in
Alertmanager after 631 s — the ten minutes are the rule's own `for`. The
smoke test was green 1 s after the delay was removed; the alert resolved
309 s later, once the slow samples left the 5 m window. Report in
[`chaos/results/`](../../chaos/results/).

## Required inputs

- `uri` / `method` from the alert (endpoint rule) or the `topk` below
- Traffic level — p95 on 2 requests a minute is noise, on 50 req/s it is real

## Alert metadata

| Property | Value |
|---|---|
| Rule files | `monitoring/alerts/platform.rules.yml` (`EdgeHighLatencyP95`), `monitoring/alerts/services.rules.yml` (`EndpointHighLatencyP95`) |
| Expression | `histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{app="strava-backend",uri!~"/actuator.*"}[5m]))) > 1`; endpoint: `sum by (app, uri, method, le)` `> 2 and rate > 0.1` |
| Threshold / for | edge > 1 s; endpoint > 2 s at > 0.1 req/s; both for 10m |
| Dashboard | http://localhost:3003/d/stride-slo, http://localhost:3003/d/stride-jvm ("HTTP Response Time (p95)") |

## Success criteria

- p95 back under threshold for 10 minutes.
- The slow endpoint and the component that spent the time are named in the ticket.

## Investigation

### 1. Which endpoint

```promql
topk(5, histogram_quantile(0.95, sum by (method, uri, le) (rate(http_server_requests_seconds_bucket{job="strava-backend",uri!~"/actuator.*"}[5m]))))
```

If `GET /api/v1/notifications/stream` is on top, exclude it
(`uri!~"/actuator.*|/api/v1/notifications/stream"`) and look again. Check
the traffic behind the number:

```promql
sum by (method, uri) (rate(http_server_requests_seconds_count{job="strava-backend",uri!~"/actuator.*"}[5m]))
```

### 2. Where the time goes — one trace

Jaeger (http://localhost:16686): service = the backend (the only entry
besides `jaeger-query`), min duration 1 s, last 15 minutes. What you will
see: the Spring MVC server span and `@Async` children (`AsyncConfiguration`
propagates the trace to the `stride-async-*` pool). What you will **not**
see: JDBC, Elasticsearch and provider calls are not instrumented as client
spans (`GroqServiceImpl` builds its own `RestTemplate`, the Anthropic SDK
brings its own HTTP client). A long server span with no children means the
time is in one of those — steps 3–5 tell which.

### 3. Postgres or the pool

```promql
hikaricp_connections_pending{app="strava-backend"}
```

`> 0` = threads waiting for a connection → [db-pool-exhausted.md](db-pool-exhausted.md).

```bash
docker compose exec postgres psql -U strava -d strava -c \
  "select pid, now()-query_start as age, state, left(query,120) from pg_stat_activity where datname='strava' and state='active' order by query_start;"
```

Queries running for seconds → `explain (analyze, buffers)` them; a seq scan
on a large table is a missing index (new Flyway migration).

### 4. Elasticsearch

| Command | What to look for |
|---|---|
| `curl -s localhost:9200/_cluster/health \| jq .status` | `red`/`yellow` with slow responses |
| `curl -s -o /dev/null -w '%{time_total}\n' 'localhost:9200/users/_search?q=a&size=1'` | the search round trip itself |
| `docker compose logs --since 15m backend \| grep -c "falling back to SQL ILIKE"` | ES *errors* fall back to SQL (`UserController.searchUsers`); a *slow* ES does not throw, it waits |

Only `GET /api/v1/users/search` touches Elasticsearch on the request path.

### 5. Resource pressure

| Command | What to look for |
|---|---|
| `docker stats --no-stream strava-backend strava-postgres strava-elasticsearch` | CPU pinned; memory at the limit (the dev compose sets no limit on the backend; Elasticsearch runs with a 512 MB heap) |
| `sum(rate(jvm_gc_pause_seconds_sum{app="strava-backend"}[5m]))` | > 0.25 = GC-bound → [jvm-heap-high.md](jvm-heap-high.md) |
| `kubectl -n strava top pods` | the HPA scales at 70 % CPU / 80 % memory between 2 and 5 replicas |

## Mitigation

- **Safe:** in k8s, scale out (`kubectl -n strava scale deploy/backend --replicas=3`; the HPA does this on its own within 2–5). Restart if GC-bound and leaking. The dev compose cannot scale the backend (fixed `container_name` and host port).
- **Needs review:** raising the nginx or axios timeouts (hides the problem); pool size; index migrations; anything that changes what a request does.

## Escalation / close

- Escalate to **domain** with the `topk` output and the trace link.
- Close when "Latency SLI: under 500 ms (1h)" on `stride-slo` is back above 95 %.
