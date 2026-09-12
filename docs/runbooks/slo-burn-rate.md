---
name: SLOBurnRate
description: An SLO is burning its 30-day error budget faster than sustainable (fast burn pages, slow burn tickets) or the budget is already spent.
metadata:
  owner: domain
  version: "1.0"
alert_type: slo
severity: critical
actionability: "Page on *Fast; ticket on *Slow and SLOErrorBudgetExhausted"
allowed-tools: []
---

# Alert: SLOBurnRateFast / SLOBurnRateSlow / SLOLatencyBurnRateFast / SLOLatencyBurnRateSlow / SLOErrorBudgetExhausted

## Summary

Burn-rate alerts do not tell you *what* is broken, only that a promise in
[`docs/slo.md`](../slo.md) is being missed fast enough to matter. A fast
burn (14.4×) means 2 % of the monthly budget disappears every hour; a slow
burn (6×) means a week of this empties the budget. The value in the summary
is the burn rate over the long window. There is one deployable, so there is
one availability SLO and one latency SLO (`slo` label `edge-availability` /
`edge-latency`); the AI endpoints have their own budget and their own alert
(`AiSLOBurnRateFast` → [ai-provider-degraded.md](ai-provider-degraded.md)).

The symptom alerts (`EdgeHighErrorRate`, `EndpointHighErrorRate`,
`*LatencyP95`, `AiChatErrorRate`) fire for the same event with the concrete
cause; start there. If a burn-rate alert fires *without* a symptom alert,
the errors are spread thin (many endpoints, each under the symptom
threshold) — step 1 finds them.

## Required inputs

- `slo` label; the `stride-slo` dashboard
- Which symptom alerts are firing at the same time (Alertmanager UI, grouped by `alertname`, `app`)

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/alerts/slo.rules.yml` |
| Expression | fast: `(slo:http_error:ratio_rate1h / 0.005) > 14.4 and (…rate5m / 0.005) > 14.4`; slow: 6h and 30m `> 6`; latency: the same on `slo:http_slow:ratio_rate*` over `0.05`; exhausted: `slo:error_budget_remaining:ratio < 0` |
| Threshold / for | fast: `for: 2m`; slow: `for: 15m`; budget exhausted: `for: 1h` |
| Dashboard | http://localhost:3003/d/stride-slo |

Alertmanager inhibits `SLOBurnRateSlow` while `SLOBurnRateFast` fires for
the same `app` and `slo`. The latency pair has no such rule: expect both a
page and a ticket for one latency outage.

## Success criteria

- Burn rate below 1 on the 1h window (the budget is no longer being consumed faster than earned).
- Cause identified via the symptom runbook; for budget exhaustion, the policy in `docs/slo.md` applied and recorded in the ticket.

## Investigation

### 1. Where the errors are

```promql
topk(10, sum by (method, uri, status) (rate(http_server_requests_seconds_count{job="strava-backend",status=~"5..",uri!~"/actuator.*"}[1h])))
```

For the latency SLO:

```promql
topk(10, 1 - sum by (method, uri) (rate(http_server_requests_seconds_bucket{job="strava-backend",le="0.5",uri!~"/actuator.*"}[1h])) / sum by (method, uri) (rate(http_server_requests_seconds_count{job="strava-backend",uri!~"/actuator.*"}[1h])))
```

Then follow the matching runbook: [edge-high-error-rate.md](edge-high-error-rate.md),
[high-latency.md](high-latency.md), [db-pool-exhausted.md](db-pool-exhausted.md),
[ai-provider-degraded.md](ai-provider-degraded.md).

### 2. Is it real traffic?

A burn rate computed on 3 requests is noise. Check "Request rate monolith
(context for the ratios above)" on `stride-slo`; below ~0.1 req/s treat the
alert as informational and note it in the ticket (the rules have no
minimum-traffic guard yet — candidate change).

### 3. Budget exhausted

`slo:error_budget_remaining:ratio{app="strava-backend"} < 0` — open "Error
budget remaining monolith (30d)", find the days that spent it
(`increase(http_server_requests_seconds_count{job="strava-backend",status=~"5.."}[1d])`
per day), link the post-mortems. Locally the 30-day window exceeds the 15-day
Prometheus retention (`--storage.tsdb.retention.time=15d`), so the number is
approximate. The policy: reliability work before features until the ratio
is positive again.

## Mitigation

- Whatever the symptom runbook says. There is no mitigation for a burn-rate alert itself.
- **Never** widen the SLO to make the alert stop; change it only through a PR that touches `docs/slo.md`, the rules, the tests and this runbook together.

## Escalation / close

- Fast burn: page **domain** (the `team` label — one deployable, one owner); include the `topk` output.
- Close when the 1h burn rate is below 1 and the 6h window is trending down.
