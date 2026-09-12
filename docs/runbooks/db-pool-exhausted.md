---
name: DbPoolExhausted
description: HikariCP connections are all busy (saturated) or requests timed out waiting for one (exhausted) — those requests failed with 5xx.
metadata:
  owner: domain
  version: "1.0"
alert_type: saturation
severity: critical
actionability: "Page — act now"
allowed-tools: []
---

# Alert: DbPoolSaturated / DbPoolExhausted

## Summary

Each service owns one Postgres and one Hikari pool (default max 10). Pool
saturation means requests are queueing for a connection; a timeout
(`connectionTimeout`, 30 s default) means they gave up and the user got a
500. Causes, most to least common here: a slow query holding connections
(missing index after a data growth), a transaction that stays open around a
remote call (gRPC or provider call inside `@Transactional`), Postgres itself
saturated (CPU, locks), or simply more traffic than the pool size allows.

## Required inputs

- `app` / `instance`; the pool panel on the JVM dashboard
- Whether Postgres for that service is healthy (`docker compose ps postgres-<svc>`)

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/alerts/services.rules.yml` |
| Expression | `hikaricp_connections_active / hikaricp_connections_max > 0.9`; `increase(hikaricp_connections_timeout_total[5m]) > 0` |
| Threshold / for | 90 % for 10m (warning); any timeout, immediately (critical) |
| Dashboard | http://localhost:3001/d/stride-jvm |

## Success criteria

- `hikaricp_connections_pending` is 0 and no new timeouts for 10 minutes.
- The query or transaction that held the pool is identified.

## Investigation

### 1. What is holding the connections

```bash
docker exec stride-postgres-<svc> psql -U stride -d stride_<svc> -c \
  "select pid, now()-xact_start as xact_age, state, wait_event_type, left(query,120) from pg_stat_activity where datname='stride_<svc>' and state <> 'idle' order by xact_start;"
```

| Pattern | Meaning |
|---|---|
| Several rows `active` on the same query for seconds | slow query → step 2 |
| `idle in transaction` with old `xact_age` | app holds a transaction open while doing something else (remote call, file I/O) — code fix |
| `wait_event_type = Lock` | lock contention; find the blocker with `pg_blocking_pids(pid)` |

### 2. The slow query

```bash
docker exec stride-postgres-<svc> psql -U stride -d stride_<svc> -c "explain (analyze, buffers) <the query>"
```

Seq scan on a large table = missing index → new Flyway migration (`CREATE INDEX CONCURRENTLY` in the `postgresql/` flavor).

### 3. Is it just traffic?

```promql
sum by (app) (rate(http_server_requests_seconds_count{app="<service>"}[5m]))
```

Pool of 10 sustains roughly `10 / avg_query_time` requests/s. If traffic legitimately exceeds that, size the pool (`spring.datasource.hikari.maximum-pool-size`) *and* check Postgres `max_connections` across all services first.

## Mitigation

- **Safe:** terminate one runaway backend: `select pg_terminate_backend(<pid>)` (the request fails, the pool frees). Restart the service if `idle in transaction` rows keep accumulating.
- **Needs review:** pool size changes; index migrations; moving remote calls out of transactions.

## Escalation / close

- Escalate to the owning team with the `pg_stat_activity` output and the explain plan.
- Post-mortem if timeouts were user-visible for more than 5 minutes.
