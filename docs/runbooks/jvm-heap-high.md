---
name: JvmHeapHigh
description: The JVM has used more than 90% of its heap for 10 minutes, spends more than 25% of wall time in GC, or its modelled hourly cost doubled — OOMKill or a latency collapse is next.
metadata:
  owner: domain
  version: "1.0"
alert_type: saturation
severity: warning
actionability: "Ticket — same working day"
allowed-tools: []
---

# Alert: JvmHeapHigh / JvmGcPressure / InfraCostSpike

## Summary

The JVM sizes its heap from the container: `JAVA_OPTS` in the `Dockerfile`,
`docker-compose.prod.yml` and `k8s/backend/deployment.yaml` all set
`-XX:MaxRAMPercentage=75.0` (G1). k8s limits the pod to 1Gi → ~768 MiB heap;
`docker-compose.prod.yml` to 2G; the dev `docker-compose.yml` sets **no**
limit, so there the heap is 75 % of the host's RAM and 90 % of it is a lot
of memory. Heap above 90 % for 10 minutes is either a leak or a workload
that outgrew the limit — there is no cache framework in this app to
overflow. Heavy workloads in this codebase: the Strava bulk import
(`StravaImportRunner`, one streams call per activity when
`app.strava.import-streams=true`), file import (`POST /api/v1/import/activities`,
multipart up to 5 MB / 25 MB), exports (`/api/v1/export/activities/{json,csv,txt}`,
`/gpx/{activityId}`, `POST /api/v1/export/report/csv`), the Monday 09:00
weekly-summary job (`userRepository.findAll()`, one mail per user) and the
Elasticsearch seed at boot (up to 1000 users).

`JvmGcPressure` is the same story seen from the CPU side and usually
precedes latency alerts. `InfraCostSpike` is the same story priced: the
model is `cores × 0.04 + reserved GiB × 0.005` per hour and reserved memory
(heap max ÷ 0.75) is constant, so a spike is CPU — a hot loop, a retry
storm, or the GC itself.

## Required inputs

- `instance`; the "JVM Heap Memory" and "GC Pause Duration" panels on `stride-jvm` for the last 6 hours
- Whether a heavy operation was running (import, export, weekly summary)
- For `InfraCostSpike`: `team` / `cost_center` labels and the `stride-cost` dashboard

## Alert metadata

| Property | Value |
|---|---|
| Rule files | `monitoring/alerts/services.rules.yml` (`JvmHeapHigh`, `JvmGcPressure`), `monitoring/alerts/cost.rules.yml` (`InfraCostSpike`) |
| Expression | `sum by (app, instance) (jvm_memory_used_bytes{area="heap"}) / sum by (app, instance) (jvm_memory_max_bytes{area="heap"}) > 0.90`; `sum by (app, instance) (rate(jvm_gc_pause_seconds_sum[5m])) > 0.25`; `cost:infra_usd:rate1h > 2 * avg_over_time(cost:infra_usd:rate1h[24h]) and cost:infra_usd:rate1h > 0.05` |
| Threshold / for | 10m / 10m / 30m |
| Dashboard | http://localhost:3003/d/stride-jvm, http://localhost:3003/d/stride-cost |

## Success criteria

- Heap after GC settles below 70 % and stays there; GC time under 10 %.
- Leak vs. undersized vs. workload decided, with the evidence attached to the ticket.
- `InfraCostSpike`: hourly cost back at its 24 h average, and the cause named.

## Investigation

### 1. Leak or load?

There is no "heap after GC" panel; read the lower edge of the sawtooth on
"JVM Heap Memory", or:

```promql
min_over_time(jvm_memory_used_bytes{app="strava-backend",area="heap"}[1h])
```

- Lower edge climbing steadily over hours → leak. Go to step 2.
- Lower edge flat, peaks near max → the working set does not fit; go to step 3.
- A step up that coincides with an import or export → workload; wait for it to finish, then re-check.

### 2. What is growing

```promql
sum by (id) (jvm_memory_used_bytes{app="strava-backend",area="heap"})
```

`G1 Old Gen` growing = long-lived objects. The runtime image is a JRE
(`eclipse-temurin:21-jre-alpine`) and does not ship `jcmd`, so there is no
class histogram from inside the container. To capture a heap dump on the
next OOM, add `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp` to
`JAVA_OPTS` (compose `environment` / k8s `env`), then `docker cp` /
`kubectl cp` the file out for offline analysis.

Correlate with what was running:

```bash
docker compose logs --since 1h backend | grep -E "weekly summary|Seeded .* users|import" | tail
```

### 3. Undersized

| Command | What to look for |
|---|---|
| `docker inspect strava-backend --format '{{.HostConfig.Memory}}'` | the limit in bytes; `0` = no limit (dev compose) |
| `docker stats --no-stream strava-backend` | RSS at the limit → the next allocation is an OOMKill (exit 137) |
| `kubectl -n strava get pod <pod> -o jsonpath='{.status.containerStatuses[0].lastState}'` | `OOMKilled` in the last termination |

### 4. `InfraCostSpike` only

On `stride-cost`, compare "CPU cores consumed by service" with "Memory
reserved (GiB) by service": memory only moves when a limit changes
(`git log -p -- docker-compose.prod.yml k8s/backend/deployment.yaml`), so a
spike is CPU. Then:

```promql
topk(5, sum by (method, uri) (rate(http_server_requests_seconds_count{app="strava-backend"}[5m])))   # request storm?
sum(rate(logback_events_total{app="strava-backend",level="error"}[5m]))                             # retry loop?
sum(rate(jvm_gc_pause_seconds_sum{app="strava-backend"}[5m]))                                        # GC burning the CPU?
```

## Mitigation

- **Safe:** restart the instance to reclaim (buys hours, not a fix); let the import finish; stop a runaway export.
- **Needs review:** raising limits in `docker-compose.prod.yml` (`deploy.resources.limits`) and `k8s/backend/deployment.yaml` (`resources.limits`) — keep both in step so `MaxRAMPercentage` gives the same heap; the code fix for a leak; `STRAVA_IMPORT_STREAMS=false` for a 1000+ activity import (see the comment in `application.yml`).

## Escalation / close

- Escalate to **domain** with the heap graph, the `id` breakdown and the running-operation evidence.
- Close when the sawtooth is stable for an hour (and, for `InfraCostSpike`, the cost line is back at its 24 h average).
