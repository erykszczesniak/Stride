---
name: SLOBurnRate
description: An SLO is burning its 30-day error budget faster than sustainable (fast burn pages, slow burn tickets) or the budget is already spent.
metadata:
  owner: platform
  version: "1.0"
alert_type: slo
severity: critical
actionability: "Page on *Fast; ticket on *Slow and SLOErrorBudgetExhausted"
allowed-tools: []
---

# Alert: SLOBurnRateFast / SLOBurnRateSlow / SLOLatencyBurnRate* / ServiceSLOBurnRate* / SLOErrorBudgetExhausted

## Summary

Burn-rate alerts do not tell you *what* is broken, only that the user-facing
promise in `docs/slo.md` is being missed fast enough to matter. A fast burn
(14.4×) means 2 % of the monthly budget disappears every hour; a slow burn
(6×) means a week of this empties the budget. The value in the summary is
the burn rate over the long window.

The symptom alerts (`GatewayHighErrorRate`, `ServiceHighErrorRate`,
`*LatencyP95`, `AiChatErrorRate`) fire for the same event with the concrete
cause; start there. If a burn-rate alert fires *without* a symptom alert,
the errors are spread thin (many endpoints, each under the symptom
threshold) — step 1 finds them.

## Required inputs

- `slo` and `app` labels; the `stride-slo` dashboard
- Which symptom alerts are firing at the same time (Alertmanager UI, grouped by `app`)

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/alerts/slo.rules.yml` |
| Expression | `(slo:http_error:ratio_rate1h / budget) > 14.4 and (…rate5m / budget) > 14.4`; slow: 6h/30m > 6 |
| Threshold / for | fast: `for: 2m`; slow: `for: 15m`; budget exhausted: `< 0 for 1h` |
| Dashboard | http://localhost:3001/d/stride-slo |

## Success criteria

- Burn rate below 1 on the 1h window (the budget is no longer being consumed faster than earned).
- Cause identified via the symptom runbook; for budget exhaustion, the policy in `docs/slo.md` is applied and recorded in the ticket.

## Investigation

### 1. Where the errors are

```promql
topk(10, sum by (app, uri, status) (rate(http_server_requests_seconds_count{job="stride-services",status=~"5..",uri!~"/actuator.*"}[1h])))
```

For latency SLOs:

```promql
topk(10, 1 - sum by (app, uri) (rate(http_server_requests_seconds_bucket{le="0.5",uri!~"/actuator.*"}[1h])) / sum by (app, uri) (rate(http_server_requests_seconds_count{uri!~"/actuator.*"}[1h])))
```

Then follow the matching runbook: [gateway-high-error-rate.md](gateway-high-error-rate.md), [service-high-error-rate.md](service-high-error-rate.md), [high-latency.md](high-latency.md), [ai-provider-degraded.md](ai-provider-degraded.md).

### 2. Is it real traffic?

A burn rate computed on 3 requests is noise. Check the request-rate panel on `stride-slo`; below ~0.1 req/s treat the alert as informational and note it in the ticket (candidate for a minimum-traffic guard in the rule).

### 3. Budget exhausted

`slo:error_budget_remaining:ratio{app="<service>"} < 0` — open the panel "Error budget remaining per service", find the days that spent it (`increase(...5xx[1d])` per day), link the post-mortems. The policy: reliability work before features for that service until the ratio is positive again.

## Mitigation

- Whatever the symptom runbook says. There is no mitigation for a burn-rate alert itself.
- **Never** widen the SLO to make the alert stop; change it only through a PR that touches `docs/slo.md`, the rules and the tests together.

## Escalation / close

- Fast burn: page **platform** (edge) or the owning team (service SLO); include the `topk` output.
- Close when the 1h burn rate is below 1 and the 6h window is trending down.
