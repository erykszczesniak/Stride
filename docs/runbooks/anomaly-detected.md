---
name: Anomaly alerts (JvmHeapExhaustionPredicted, DbPoolExhaustionPredicted, TrafficDeviatesFromWeeklyBaseline, ErrorRatioDeviation)
description: Something about the backend deviates from its own recent behaviour — a resource trending towards its limit, or a request or error rate that does not look like last week — before any fixed threshold is crossed.
metadata:
  owner: domain (heap, pool, errors) / platform (traffic)
  version: "1.0"
alert_type: saturation | availability
severity: warning
actionability: "Ticket — same working day"
allowed-tools: []
---

# Alert: JvmHeapExhaustionPredicted / DbPoolExhaustionPredicted / TrafficDeviatesFromWeeklyBaseline / ErrorRatioDeviation

## Summary

These four rules (`monitoring/alerts/anomaly.rules.yml`) do not say "a
number is too high". They say "a number is heading somewhere bad" or "a
number is unusual for this service". Users are usually **not** affected
yet when they fire — that is the point: they buy the hour or the day that
the static rules (`JvmHeapHigh`, `DbPoolSaturated`, `EdgeTrafficCollapse`,
`SLOBurnRate*`) do not. Treat them as a ticket to investigate within the
working day, not a page. The usual causes are, in order: a deploy or
config change, a batch job (import, export, the Monday 09:00 weekly
summary), a client-side change (frontend, iOS app), a real change in user
behaviour, and only then a leak or a dependency misbehaving.

| Alert | What deviated | Compared with |
|---|---|---|
| `JvmHeapExhaustionPredicted` | heap used / max will cross 100 % within 4 h on the last hour's slope, and is already above 60 % | the service's own last hour |
| `DbPoolExhaustionPredicted` | active / max pool connections will cross 100 % within 1 h on the last 30 minutes' slope, and is already above 50 % | the service's own last 30 minutes |
| `TrafficDeviatesFromWeeklyBaseline` | request rate over the last hour is < 0.5× or > 3× the rate at the same hour seven days ago | the same hour one week ago |
| `ErrorRatioDeviation` | 5-minute 5xx ratio is > 3 standard deviations above its 24 h mean and above 1 % | the service's own last 24 hours |

## Required inputs

- `instance` (heap, pool) or `app` (traffic, errors) from the alert labels
- Alert start time (`startsAt` in the webhook payload, or the Alertmanager UI)
- What changed in the last hours: `git log --since=6h`, `docker compose ps --format '{{.Name}} {{.RunningFor}}'`, the frontend / iOS release notes
- Whether anything out of the ordinary is scheduled: a batch import, an event, a holiday, a marketing push

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/alerts/anomaly.rules.yml` |
| Expressions | `anomaly:jvm_heap_used:ratio_predict4h > 1 and anomaly:jvm_heap_used:ratio > 0.6`; `anomaly:db_pool_active:ratio_predict1h > 1 and anomaly:db_pool_active:ratio > 0.5`; `(anomaly:http_requests:weekly_ratio < 0.5 and anomaly:http_requests:rate1h_1w > 0.5) or (anomaly:http_requests:weekly_ratio > 3 and anomaly:http_requests:rate1h > 1)`; `anomaly:http_error:ratio_zscore24h > 3 and slo:http_error:ratio_rate5m > 0.01` |
| Threshold / for | 30m / 15m / 30m / 10m |
| Dashboard | http://localhost:3003/d/stride-jvm (heap, pool), http://localhost:3003/d/stride-slo (traffic, errors) |

## Success criteria

- The alert has resolved in Alertmanager (not silenced), **or** the deviation is explained and recorded as expected (holiday week, planned campaign, finished import).
- For the two predictions: the slope is flat or falling again and the static alert for the same resource never fired.
- For the two comparisons: the cause is named — a change on our side, a change on the client side, or a change in users — with the panel screenshot in the ticket.

## Investigation

### 1. Is the deviation real, or is the baseline odd?

Cheapest check first: look at the comparison the rule made, not just the
current value.

| Command | What to look for |
|---|---|
| `anomaly:jvm_heap_used:ratio{app="strava-backend"}` over 6 h | a sawtooth whose **lower** edge climbs = leak; a sawtooth whose lower edge is flat with one tall tooth = a batch job the regression caught mid-climb |
| `anomaly:db_pool_active:ratio{app="strava-backend"}` over 2 h | a staircase (one connection more every few minutes, never returned) vs. a spike that follows traffic |
| `anomaly:http_requests:rate1h` and `anomaly:http_requests:rate1h_1w` on one panel | is the *baseline* the odd one? A week ago may have been a holiday, an outage, a load test, or a day before the service had users |
| `slo:http_error:ratio_rate5m{app="strava-backend"}` over 24 h | a step up at a point in time (deploy) vs. a slow drift (dependency) vs. a day that was unusually *clean* making today's normal look unusual |

Expected result: you know which side of the comparison moved. If the
baseline is the odd one (last week was a holiday, the service had an
outage at that hour, the 24 h window contains a load test), close the
ticket with that note — the alert resolves on its own when the window
rolls past the odd period.

### 2. What changed on our side?

| Command | What to look for |
|---|---|
| `git log --since=6h --oneline` and the deploy time of the running image | a deploy within the hour before `startsAt` |
| `docker compose logs --since 2h backend \| grep -E "weekly summary\|Seeded .* users\|import\|export"` | a batch job running (heap, pool) |
| `topk(5, sum by (method, uri) (rate(http_server_requests_seconds_count{app="strava-backend"}[10m])))` | which endpoint carries the extra (or missing) traffic |
| `sum by (uri, status) (rate(http_server_requests_seconds_count{app="strava-backend",status=~"5.."}[10m]))` | which endpoint produces the new errors (`EndpointHighErrorRate` names it if it is above 20 %) |

### 3. What changed on the client side?

A traffic drop with a healthy backend is most often not the backend.
Check in this order: the frontend container (`curl -sf http://localhost/health`,
`docker compose logs --since 1h frontend`), a frontend or iOS release in
the last day (a changed path, a broken login flow sends users away
silently), DNS / ingress / certificate (`curl -sv https://<host>/actuator/health`).
A traffic surge with no campaign behind it: look at `User-Agent` in the
access logs for a scraper or a retry loop in a client.

### 4. For the predictions: confirm the trend, then hand over

`JvmHeapExhaustionPredicted` → continue with
[`jvm-heap-high.md`](jvm-heap-high.md) step 1 ("leak or load?") — you have
more time than that runbook assumes.
`DbPoolExhaustionPredicted` → continue with
[`db-pool-exhausted.md`](db-pool-exhausted.md); look for a transaction or
a stream that holds a connection open (`pg_stat_activity` with
`state = 'idle in transaction'`).

## False positives, and what to do about them

| Pattern | Why it fires | Action |
|---|---|---|
| Marketing push, event registration opening, a popular club challenge | traffic > 3× last week, errors may rise with load | expected; note it in the ticket. If it recurs, raise the surge multiplier or add a planned-event silence in Alertmanager, never delete the rule |
| Holiday week, long weekend, exam season | traffic < 0.5× last week on the quiet days, and > 3× a week later when the baseline *is* the holiday | expected both ways; a `7d`-offset baseline has no notion of holidays. Document the week; consider a second baseline (`offset 2w`) if this becomes frequent |
| Last week's outage or load test in the baseline | the ratio compares against a hole or a spike | nothing to fix today; the alert resolves when the window moves on |
| Bulk Strava import or a large file import | heap climbs steadily for the duration, pool fills with the import's connections | let it finish; if the prediction crosses 100 % before the import ends, follow `jvm-heap-high.md` |
| A clean day followed by the usual error level | 24 h mean ≈ 0, stddev floored at 0.005, so 1.6 % reads as > 3σ | expected; the 1 % absolute floor keeps single errors out, a day of near-zero errors does not |
| JVM restart | heap starts low and climbs fast | the 60 % guard stops this; if the service restarts *at* 60 % heap (very large working set), raise the guard, not the rule |

## Mitigation

- **Safe without review:** nothing needs to be done to the system for the
  two comparison alerts; record the explanation. For the predictions,
  restarting the backend buys time (`docker compose restart backend`) but
  is not a fix and should be paired with the investigation above.
- **Needs a change / second pair of eyes:** changing a multiplier, a floor
  or a `for` duration in `anomaly.rules.yml` — only with the unit test in
  `monitoring/alerts/tests/anomaly.test.yml` updated in the same PR, and
  only after two false positives of the same kind, not one.

## Escalation / close

- Escalate to **domain** (heap, pool, errors) or **platform** (traffic)
  with: the two series of the comparison on one panel, the deploy / batch /
  client evidence, what you already ruled out.
- Close: comment on the alert in Alertmanager with the explanation. No
  post-mortem for these on their own; if the static alert for the same
  resource fired later anyway, the post-mortem for *that* incident should
  say whether this alert gave usable lead time.
