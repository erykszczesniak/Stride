---
name: DbPoolExhausted
description: HikariCP connections are all busy (saturated) or requests timed out waiting for one (exhausted) — those requests failed with 500.
metadata:
  owner: domain
  version: "1.0"
alert_type: saturation
severity: critical
actionability: "Page — act now (DbPoolExhausted); ticket for DbPoolSaturated"
allowed-tools: []
---

# Alert: DbPoolSaturated / DbPoolExhausted

## Summary

One PostgreSQL database (`strava`), one HikariCP pool with Spring Boot's
defaults — `application.yml` sets no `spring.datasource.hikari.*`, so
**10 connections** and a **30 s** `connectionTimeout`. Saturation means
requests are queueing for a connection; a timeout means one gave up
(`HikariPool-1 - Connection is not available, request timed out after
30000ms`) and the user got a 500. Causes, most to least common here: a slow
query holding connections (missing index after data growth), a transaction
kept open around something slow, Postgres itself saturated (CPU, locks), or
more traffic than ten connections sustain. With 2–5 replicas in k8s that is
20–50 connections against Postgres's `max_connections`.

Transactions in this codebase that hold a connection for a long time:

| Where | Why it holds |
|---|---|
| `AiChatServiceImpl.sendMessage` (`@Transactional`) | the provider call (Anthropic, then Groq on failover) runs *inside* the transaction — every in-flight chat turn holds one connection for the whole round trip (p95 alert is at 15 s; failover doubles it) |
| `WeeklySummaryServiceImpl.sendWeeklySummaries` (`@Transactional(readOnly = true)`) | Monday 09:00: one SMTP send per user inside a single read-only transaction |
| `UserFeedProjector` (`@TransactionalEventListener(AFTER_COMMIT)` + `REQUIRES_NEW`) | an activity write briefly needs a second connection while the first is still bound |

## Required inputs

- `instance`; the "HikariCP Connection Pool" panel on `stride-jvm`
- Whether Postgres is healthy (`docker compose ps postgres`)
- Whether chat traffic or the weekly job was running

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/alerts/services.rules.yml` |
| Expression | `sum by (app, instance) (hikaricp_connections_active) / sum by (app, instance) (hikaricp_connections_max) > 0.90`; `sum by (app, instance) (increase(hikaricp_connections_timeout_total[5m])) > 0` |
| Threshold / for | 90 % for 10m (warning); any timeout, immediately (critical, `for: 0m`) |
| Dashboard | http://localhost:3003/d/stride-jvm |

## Success criteria

- `hikaricp_connections_pending` is 0 and no new timeouts for 10 minutes.
- The query or transaction that held the pool is identified.

## Investigation

### 1. What is holding the connections

```bash
docker compose exec postgres psql -U strava -d strava -c \
  "select pid, now()-xact_start as xact_age, state, wait_event_type, left(query,120) from pg_stat_activity where datname='strava' and state <> 'idle' order by xact_start;"
```

(k8s: `kubectl -n strava exec deploy/postgres -- psql -U strava -d strava -c "..."`.)

| Pattern | Meaning |
|---|---|
| Several rows `active` on the same query for seconds | slow query → step 2 |
| `idle in transaction` with old `xact_age` | the app holds a transaction open while doing something else — a chat turn waiting for the provider, the weekly job sending mail |
| `wait_event_type = Lock` | lock contention; find the blocker with `select pg_blocking_pids(<pid>)` |

### 2. The slow query

```bash
docker compose exec postgres psql -U strava -d strava -c "explain (analyze, buffers) <the query>"
```

Seq scan on a large table = missing index → a new Flyway migration
(`V<n>__add_<index>.sql`; migrations are append-only, see `AGENTS.md`).

### 3. Is it just traffic?

```promql
sum(rate(http_server_requests_seconds_count{app="strava-backend"}[5m]))
# average concurrent chat turns — each one is a held connection:
sum(rate(http_server_requests_seconds_sum{app="strava-backend",uri=~"/api/v1/ai.*"}[5m]))
```

A pool of 10 sustains roughly `10 / avg_query_time` requests per second,
minus whatever the chat turns hold. If traffic legitimately exceeds that,
set `spring.datasource.hikari.maximum-pool-size` (not set today) *and* check
`show max_connections;` on Postgres across all replicas first.

## Mitigation

- **Safe:** terminate one runaway backend: `select pg_terminate_backend(<pid>);` (that request fails, the pool frees). Restart the backend if `idle in transaction` rows keep accumulating.
- **Needs review:** pool size changes; index migrations; moving the provider call out of `sendMessage`'s transaction (call first, persist the turn afterwards in its own transaction).

## Escalation / close

- Escalate to **domain** with the `pg_stat_activity` output and the explain plan.
- Post-mortem if timeouts were user-visible for more than 5 minutes.
