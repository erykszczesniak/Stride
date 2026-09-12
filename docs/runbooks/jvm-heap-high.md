---
name: JvmHeapHigh
description: A JVM has used more than 90% of its heap for 10 minutes, or spends more than 25% of wall time in GC — OOMKill or a latency collapse is next.
metadata:
  owner: domain
  version: "1.0"
alert_type: saturation
severity: warning
actionability: "Ticket — same working day"
allowed-tools: []
---

# Alert: JvmHeapHigh / JvmGcPressure

## Summary

Every JVM in compose and k8s sizes its heap from the container limit
(`-XX:MaxRAMPercentage=75`, PR #151). Heap above 90 % of max for a
sustained period is either a leak, a workload that outgrew the limit (bulk
Strava import, big CSV export), or a cache without a bound. GC pressure is
the same story seen from the CPU side and usually precedes latency alerts.

## Required inputs

- `app` / `instance`; the JVM dashboard heap and GC panels for the last 6 hours
- Whether a heavy operation was running (import, export, weekly summary job)

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/alerts/services.rules.yml` |
| Expression | `jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.90`; `rate(jvm_gc_pause_seconds_sum[5m]) > 0.25` |
| Threshold / for | 10m |
| Dashboard | http://localhost:3001/d/stride-jvm |

## Success criteria

- Heap after GC settles below 70 % and stays there.
- Leak vs. undersized decided, with the evidence attached to the ticket.

## Investigation

### 1. Leak or load?

On the JVM dashboard, look at **heap after GC** (the sawtooth's lower edge):

- Lower edge climbing steadily over hours → leak. Go to step 2.
- Lower edge flat, peaks near max → the working set does not fit; go to step 3.

```promql
min_over_time(jvm_memory_used_bytes{app="<service>",area="heap"}[1h])
```

### 2. What is growing

| Command | What to look for |
|---|---|
| `curl -s localhost:<port>/actuator/metrics/jvm.memory.used?tag=area:heap&tag=id:G1%20Old%20Gen` | old gen growth = long-lived objects |
| `docker exec stride-<service> jcmd 1 GC.class_histogram \| head -20` | the class with the most instances (byte[] from streams, HashMap nodes from an unbounded cache) |
| `docker exec stride-<service> jcmd 1 GC.heap_dump /tmp/heap.hprof` then `docker cp` | for offline analysis (Eclipse MAT) — only when the histogram is not enough |

Known suspects in this codebase: activity streams kept in memory during import; `@Cacheable` without a Redis TTL in challenge-goal-service.

### 3. Undersized

| Command | What to look for |
|---|---|
| `docker inspect stride-<service> --format '{{.HostConfig.Memory}}'` | the limit; heap max = 75 % of it |
| `docker stats --no-stream stride-<service>` | RSS at the limit → the next allocation is an OOMKill (exit 137) |

## Mitigation

- **Safe:** restart the instance to reclaim (buys hours, not a fix); pause the heavy job.
- **Needs review:** raising `mem_limit` in `docker-compose.yml` / `resources.limits` in `k8s/services/<service>.yaml` (keep both in sync, see the k8s instructions); code fix for the leak.

## Escalation / close

- Escalate to the owning team with the histogram output and the heap-after-GC graph.
- Close when the sawtooth is stable for an hour.
