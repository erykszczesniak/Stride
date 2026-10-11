# Service level objectives

What "reliable" means for Stride, in numbers, and what happens when we miss
it. The objectives are defined as OpenSLO documents in [`slo/`](../slo/)
(one file per SLO); the Prometheus rules that measure and alert on them,
[`monitoring/alerts/slo.rules.yml`](../monitoring/alerts/slo.rules.yml), are
**generated** from those files (see [How the rules are generated](#how-the-rules-are-generated))
and unit-tested in `monitoring/alerts/tests/slo.test.yml`; the dashboard is
**Stride — SLOs & error budgets** (`stride-slo`) in Grafana.

## The objectives

| SLO | SLI (what we measure) | Target | 30-day error budget | Owner (`team` label) |
|---|---|---|---|---|
| **Edge availability** | share of backend requests (excluding `/actuator`) that do not end in 5xx | 99.5 % | 0.5 % of requests | domain |
| **Edge latency** | share of backend requests that complete within 500 ms (`le="0.5"` bucket) | 95 % | 5 % of requests may be slower | domain |
| **AI availability** | share of `/api/v1/ai/**` requests that do not end in 5xx | 98 % | 2 % of requests | ai |

Why these numbers: there is one deployable and the backend is what users
touch (through the frontend's nginx or the ingress), so its two SLOs are
the user-facing promise; there is no per-service SLO because there is no
second service — the endpoint alerts (`EndpointHighErrorRate`,
`EndpointHighLatencyP95`) name the culprit instead. The AI budget is looser
because two external providers sit behind it and their outages are not ours
to fix, only to route around (see the failover in
[`ai-guardrails.md`](ai-guardrails.md)).

Not covered on purpose: the event-driven paths inside the process (feed
projection, SSE notifications, search-index sync) have no freshness metric
yet; `ErrorLogSpike` stands in until there is one.

## How it is measured

Recording rules compute the error ratio over four windows (5m, 30m, 1h, 6h)
from `http_server_requests_seconds_count`, and the slow ratio from the
`le="0.5"` histogram bucket:

```
slo:http_error:ratio_rate1h{app}   = 5xx / all      (excluding /actuator)
slo:http_slow:ratio_rate1h{app}    = 1 - (<=500ms / all)
slo:ai_error:ratio_rate1h{app}     = 5xx / all      (uri=~"/api/v1/ai.*")
slo:error_budget_remaining:ratio   = 1 - (30d error ratio / 0.005)
```

Histogram buckets exist because `application.yml` enables
`management.metrics.distribution.percentiles-histogram.http.server.requests`
with SLO boundaries at 100 ms, 250 ms, 500 ms, 1 s and 2.5 s.

## Burn-rate alerting

A burn rate of 1 means the budget would be used up exactly at the end of the
30-day window. Alerts fire on *two* windows at once so that a long window
gives statistical confidence and a short window makes the alert resolve
quickly once the problem is fixed.

| Alert | Windows | Burn rate | Error ratio at which it fires | Severity | Budget consumed if ignored |
|---|---|---|---|---|---|
| `SLOBurnRateFast` / `AiSLOBurnRateFast` | 1h **and** 5m | > 14.4× | > 7.2 % (AI: > 28.8 %) | critical — page | 2 % of the month per hour |
| `SLOBurnRateSlow` | 6h **and** 30m | > 6× | > 3 % | warning — ticket | 5 % of the month per 6 h |
| `SLOLatencyBurnRateFast` | 1h and 5m | > 14.4× | > 72 % of requests slower than 500 ms | critical | |
| `SLOLatencyBurnRateSlow` | 6h and 30m | > 6× | > 30 % of requests slower than 500 ms | warning | |
| `SLOErrorBudgetExhausted` | 30d | — | remaining budget < 0 for 1 h | warning | policy below applies |

Alertmanager inhibits `SLOBurnRateSlow` while `SLOBurnRateFast` is firing
for the same `app` and `slo`, so a real outage produces one page, not two.
The latency pair is not inhibited (ADR-007, known gap).

## Error budget policy

The budget is the share of the 30-day window we are allowed to fail
(0.5 % of requests for availability, 5 % slower than 500 ms, 2 % of AI
calls). `slo:error_budget_remaining:ratio` on the `stride-slo` dashboard
says how much is left: 1 = untouched, 0 = spent. What the number means for
the next two weeks of work is decided in advance, so nobody argues about it
during an incident:

| Remaining budget | What changes | Who decides | How it is lifted |
|---|---|---|---|
| **above 50 %** | Nothing. Features ship; reliability work competes on merit. | — | — |
| **50 % → 25 %** | *Yellow.* Every open post-mortem action for this SLO is scheduled into the current or next sprint; risky deploys (schema changes, dependency bumps, AI provider changes) go out early in the day with someone watching the burn-rate panel for an hour. | The owning team (`team` label) at its planning; no sign-off needed. | Automatically when the budget is back above 50 %. |
| **25 % → 0 %** | *Orange.* Reliability work first: the team picks at least one item per person per sprint from the SLO backlog (post-mortem actions, missing alerts, flaky dependency, capacity). Features still ship, but a feature that touches the failing path needs the on-call's OK. | The team lead for the owning team; disagreements go to the platform owner. | When the budget has been above 25 % for seven consecutive days. |
| **0 % (`SLOErrorBudgetExhausted`)** | *Red: feature freeze on the affected deployable.* Only reliability changes, security fixes and reverts merge to `main` until the budget is positive again. The post-mortem for whatever spent the budget is written within two working days and its actions are the sprint. | The platform owner declares and lifts the freeze; the owning team cannot lift it alone. | When `slo:error_budget_remaining:ratio` is above 0 and the post-mortem actions that address the cause are merged — or, if the budget was spent by an external provider (`ai-availability`), when the failover is proven to cover it. |

Two standing rules on top of the table:

- **Fast burn paged twice in a week:** the runbook for that alert gets
  reviewed and the threshold or the system gets fixed, never the alert
  silenced.
- **A disputed SLO is reviewed, not ignored:** if the team believes the
  target is wrong (users did not notice what the budget counted, or
  complained while it was green) the fix is a PR to the OpenSLO file under
  [Reviewing the SLOs](#reviewing-the-slos), not a silence.

Locally the 30-day numbers are approximate (15-day retention); the policy
is written for the environment where the SLO is a promise to users.

## How the rules are generated

Each SLO is one OpenSLO v1 document in [`slo/`](../slo/): the service, the
ratio indicator (the PromQL selectors for good or bad events and for all
events), the target, the 30-day rolling window and the alert policies with
their burn-rate thresholds and windows. Generator-specific details (the
recording-rule names, the alert names and texts) live in
`metadata.annotations` under the `stride.io/` prefix, documented in
[`slo/README.md`](../slo/README.md).

`monitoring/scripts/generate_slo_rules.py` (python3 + PyYAML, nothing else)
validates the documents and writes `monitoring/alerts/slo.rules.yml`:
one recording rule per window (`slo:http_error:ratio_rate5m` … `6h`), the
budget-remaining rule, and one alert per policy with the short window
derived as a twelfth of the long one (1h → 5m, 6h → 30m). The output is
deterministic; `make slo` regenerates it, `make slo-check` (and the
`Monitoring` workflow) fails if the committed file differs from what the
sources produce. The generated file is not edited by hand — change the
OpenSLO document, regenerate, update the test in
`monitoring/alerts/tests/slo.test.yml` if texts or thresholds changed.
Decision and alternatives: [ADR-013](adr/013-openslo-as-the-source-of-slo-rules.md).

## Reviewing the SLOs

Targets are reviewed when a post-mortem shows the SLO did not reflect what
users felt (too strict → noise, too loose → users complained before we
paged). Changes go through a PR touching this file, the OpenSLO document in
`slo/`, the regenerated rules, the tests and
[`runbooks/slo-burn-rate.md`](runbooks/slo-burn-rate.md) together.
