---
name: HighLatencyP95
description: 95th-percentile latency above 1 s at the gateway or 500 ms in a service for 10 minutes — the app feels slow, timeouts follow.
metadata:
  owner: domain
  version: "1.0"
alert_type: latency
severity: warning
actionability: "Ticket — same working day"
allowed-tools: []
---

# Alert: GatewayHighLatencyP95 / ServiceHighLatencyP95

## Summary

Latency alerts rarely have a single cause. Work from the outside in: which
service is slow (gateway p95 is the max of its hops), then which endpoint,
then whether the time is spent in the service (GC, DB pool) or in a
dependency (gRPC deadline, Groq/Anthropic, Elasticsearch). Jaeger shows the
split in one trace.

## Required inputs

- `app` from the alert; the SLO dashboard latency panels
- Traffic level — p95 on 2 requests/min is noise, on 50 req/s it is real

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/alerts/platform.rules.yml`, `services.rules.yml` |
| Expression | `histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket[5m])))` |
| Threshold / for | gateway > 1 s; service > 0.5 s; both for 10m |
| Dashboard | http://localhost:3001/d/stride-jvm |

## Success criteria

- p95 back under threshold for 10 minutes.
- The slow endpoint and the component that spent the time are named in the ticket.

## Investigation

### 1. Which service and endpoint

```promql
topk(5, histogram_quantile(0.95, sum by (app, uri, le) (rate(http_server_requests_seconds_bucket{job="stride-services",uri!~"/actuator.*"}[5m]))))
```

### 2. Where the time goes — one trace

Jaeger (http://localhost:16686): service = the slow `app`, min duration = 1 s, last 15 minutes. Read the longest trace:

| Span pattern | Cause |
|---|---|
| Long `jdbc` / `SELECT` spans | slow query or pool wait → [db-pool-exhausted.md](db-pool-exhausted.md) |
| Long gRPC client span (`ActivityInternalService/...`) | the callee is slow — repeat this runbook for that service |
| Long `HTTP POST api.groq.com` / `api.anthropic.com` | provider latency → [ai-provider-degraded.md](ai-provider-degraded.md) |
| Gaps between spans | GC pauses → [jvm-heap-high.md](jvm-heap-high.md); check `JvmGcPressure` |
| Many short repeated spans | N+1 queries — code fix, not ops |

### 3. Resource pressure on the instance

| Command | What to look for |
|---|---|
| `docker stats --no-stream stride-<service>` | CPU pinned at the limit; memory at `mem_limit` |
| `curl -s localhost:<port>/actuator/metrics/jvm.gc.pause \| jq '.measurements'` | total pause time climbing |
| `curl -s localhost:<port>/actuator/metrics/hikaricp.connections.pending` | > 0 = threads waiting for a connection |

## Mitigation

- **Safe:** scale the slow service (`docker compose up -d --scale <service>=2` needs the HPA/replicas in k8s); restart if GC-bound and leaking.
- **Needs review:** raising Resilience4j timeouts or RestTemplate timeouts (hides the problem), changing pool sizes.

## Escalation / close

- Escalate to the owning team with the trace link and the `topk` output.
- Close when the latency SLI on `stride-slo` is back above 95 % under 500 ms.
