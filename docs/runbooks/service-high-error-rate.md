---
name: ServiceHighErrorRate
description: One service fails more than 5% of its requests with 5xx (or logs more than one ERROR per second) — its callers and the gateway propagate the failure to users.
metadata:
  owner: domain
  version: "1.0"
alert_type: availability
severity: critical
actionability: "Page — act now"
allowed-tools: []
---

# Alert: ServiceHighErrorRate / ErrorLogSpike

## Summary

The service is up and answering, but wrongly. The three usual causes, in
order of frequency on this stack: a dependency the service needs is down
(Postgres, Kafka, Redis, Elasticsearch, a gRPC peer), a bad deploy
(exception on a new code path, failed Flyway validation on startup), or a
bad request pattern from one client hitting an unhandled edge case.
`ErrorLogSpike` is the same family seen from the logs; it also catches
failing background jobs that have no HTTP signal.

## Required inputs

- `app`; which `uri`/`status` (step 1)
- Last deploy time for the service (`docker compose ps --format '{{.Name}} {{.RunningFor}}'`)

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/alerts/services.rules.yml` |
| Expression | `sum by (app)(rate(5xx[5m])) / sum by (app)(rate(all[5m])) > 0.05`; `rate(logback_events_total{level="error"}[5m]) > 1` |
| Threshold / for | 5 % for 5m; 1 error/s for 10m |
| Dashboard | http://localhost:3001/d/stride-jvm |

## Success criteria

- Error ratio under 0.5 % and no new ERROR lines for 10 minutes.
- Root cause named; if it was a deploy, the fix is a new PR, not a hot edit.

## Investigation

### 1. Which endpoint

```promql
topk(5, sum by (uri, status, exception) (rate(http_server_requests_seconds_count{app="<service>",status=~"5.."}[5m])))
```

The `exception` tag (Spring Boot adds it) names the class — often enough on its own.

### 2. Read the logs, first exception first

```bash
docker compose logs --since 15m <service> | grep -n -m 5 -B 2 -A 25 "ERROR"
```

| Log signature | Meaning |
|---|---|
| `PSQLException: Connection refused` / `HikariPool ... timeout` | Postgres down or pool exhausted → [db-pool-exhausted.md](db-pool-exhausted.md) |
| `StatusRuntimeException: UNAVAILABLE` | gRPC peer down → [service-down.md](service-down.md) for the peer; the breaker should open |
| `TimeoutException` on Kafka send | broker unreachable → [kafka-producer-errors.md](kafka-producer-errors.md) |
| `FlywayValidateException` | migration mismatch after a deploy — roll back |
| `NullPointerException` / `IllegalArgumentException` on one `uri` | code bug on a new path; check the payload in Jaeger |

### 3. Dependencies from the service's point of view

| Command | What to look for |
|---|---|
| `curl -s localhost:<port>/actuator/health \| jq` | which component is `DOWN` (db, kafka, redis, elasticsearch) |
| `curl -s localhost:<port>/actuator/circuitbreakers \| jq '.circuitBreakers[] \| {name: .name, state: .state}'` | `OPEN` = the peer is the problem |

## Mitigation

- **Safe:** roll back the last deploy of this service; restart the dependency that is down.
- **Not safe:** silencing the alert while the ratio is high; editing data by hand.

## Escalation / close

- Escalate to the owning team with the `topk` line, the first stack trace, and the health JSON.
- Post-mortem if it paged.
