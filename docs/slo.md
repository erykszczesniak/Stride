# Service level objectives

What "reliable" means for Stride, in numbers, and what happens when we miss
it. The rules that measure and alert on these live in
[`monitoring/alerts/slo.rules.yml`](../monitoring/alerts/slo.rules.yml) and are
unit-tested in `monitoring/alerts/tests/slo.test.yml`; the dashboard is
**Stride — SLOs & error budgets** (`stride-slo`) in Grafana.

## The objectives

| SLO | SLI (what we measure) | Target | 30-day error budget | Owner |
|---|---|---|---|---|
| **Edge availability** | share of gateway requests (excluding `/actuator`) that do not end in 5xx | 99.5 % | 0.5 % of requests | platform |
| **Edge latency** | share of gateway requests that complete within 500 ms (`le="0.5"` bucket) | 95 % | 5 % of requests may be slower | platform |
| **Service availability** | per domain service, share of its requests that do not end in 5xx | 99.5 % | 0.5 % per service | domain |
| **AI availability** | share of `/api/v1/ai/**` requests that do not end in 5xx | 98 % | 2 % of requests | ai |

Why these numbers: the gateway is the only thing users touch, so its two
SLOs are the user-facing promise. Domain services get the same availability
target because a failing hop becomes a gateway 5xx anyway; the per-service
SLO exists to name the culprit. The AI budget is looser because two external
providers sit behind it and their outages are not ours to fix, only to route
around (see the failover in ai-service).

Not covered on purpose: Kafka-driven flows (feed, notifications) have no
latency SLO yet — the consumer-lag alerts in `kafka.rules.yml` stand in until
there is an end-to-end freshness metric.

## How it is measured

Recording rules compute the error ratio per `app` over four windows
(5m, 30m, 1h, 6h) from `http_server_requests_seconds_count`, and the slow
ratio from the `le="0.5"` histogram bucket:

```
slo:http_error:ratio_rate1h{app}   = 5xx / all      (excluding /actuator)
slo:http_slow:ratio_rate1h{app}    = 1 - (<=500ms / all)
slo:error_budget_remaining:ratio   = 1 - (30d error ratio / 0.005)
```

Histogram buckets exist because Config Server enables
`management.metrics.distribution.percentiles-histogram.http.server.requests`
with SLO boundaries at 100ms, 250ms, 500ms, 1s and 2.5s for every service.

## Burn-rate alerting

A burn rate of 1 means the budget would be used up exactly at the end of the
30-day window. Alerts fire on *two* windows at once so that a long window
gives statistical confidence and a short window makes the alert resolve
quickly once the problem is fixed.

| Alert | Windows | Burn rate | Error ratio at which it fires (0.5 % budget) | Severity | Budget consumed if ignored |
|---|---|---|---|---|---|
| `SLOBurnRateFast` / `ServiceSLOBurnRateFast` / `AiSLOBurnRateFast` | 1h **and** 5m | > 14.4× | > 7.2 % (AI: > 28.8 %) | critical — page | 2 % of the month per hour |
| `SLOBurnRateSlow` / `ServiceSLOBurnRateSlow` | 6h **and** 30m | > 6× | > 3 % | warning — ticket | 5 % of the month per 6 h |
| `SLOLatencyBurnRateFast` | 1h and 5m | > 14.4× | > 72 % of requests slower than 500 ms | critical | |
| `SLOLatencyBurnRateSlow` | 6h and 30m | > 6× | > 30 % of requests slower than 500 ms | warning | |
| `SLOErrorBudgetExhausted` | 30d | — | remaining budget < 0 for 1 h | warning | policy below applies |

Alertmanager inhibits the slow-burn alert while the fast-burn alert for the
same `app` and `slo` is firing, so a real outage produces one page, not two.

## Error-budget policy

- **Budget above zero:** ship features. Reliability work competes on merit.
- **Budget below zero (`SLOErrorBudgetExhausted`):** the next sprint's work on
  that service is reliability first — the post-mortem actions, the missing
  test, the flaky dependency. New features for that service wait until the
  30-day budget is positive again.
- **Fast burn paged twice in a week:** the runbook for that alert gets
  reviewed and the threshold or the system gets fixed, never the alert
  silenced.

## Reviewing the SLOs

Targets are reviewed when a post-mortem shows the SLO did not reflect what
users felt (too strict → noise, too loose → users complained before we
paged). Changes go through a PR touching this file, the rules and the tests
together.
