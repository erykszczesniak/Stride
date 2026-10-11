# Chaos experiment: postgres-netem-delay

**Verdict: hypothesis disproven.** Run 2026-10-11T03:01:23Z on `Darwin arm64`, compose project `stride-chaos`, target `strava-postgres`.

## Hypothesis

- **Steady state:** scripts/smoke.sh passes; slo:http_slow:ratio_rate5m is low (the smoke requests are fast).
- **Fault:** pumba netem delay 400 ms ± 50 ms (normal distribution) on the postgres container for up to 480 s, with 4 load workers cycling through the authenticated read endpoints.
- **Expected alerts:** slo:http_slow:ratio_rate5m{app="strava-backend"} rises above 0.72 (the 14.4× burn threshold of the 95 %-under-500 ms SLO) within 150 s, and SLOLatencyBurnRateFast (critical) fires within 360 s: recording rules every 30 s, the alert needs both the 5 m and the 1 h ratio above 0.72 for 2 m, evaluation every 15 s. The 1 h window is dominated by the experiment's own traffic on a fresh stack; on a stack that served an hour of fast traffic before the experiment the 1 h ratio can stay below 0.72 and the alert would (correctly) not page — that is why the recording-rule value is asserted separately.
- **Expected recovery:** After the qdisc is removed, latency is normal immediately; the smoke test passes once the 5 m window of slow requests has drained and the critical alert resolved — within 420 s.

Verifies docs/runbooks/slo-burn-rate.md (fast burn pages, resolves when the short window clears) and the latency recording rules in monitoring/alerts/slo.rules.yml. EdgeHighLatencyP95 / DbPoolSaturated have for: 10m and are not expected within this budget; whatever they do is recorded in the timeline, not asserted.

## Measured

| Expectation | Result |
|---|---|
| alert `SLOLatencyBurnRateFast` firing within 360s | not seen |
| query `slow-ratio-5m-above-burn-threshold` > 0.72 within 150s | LATE: met after 321s (value 1), budget was 150s |
| recovery: smoke within 420s | smoke passes 0s after the fault ended (budget 420s) |
| recovery: alerts resolved | all expected alerts resolved in Alertmanager |

**Time to alert:** — · **Fault duration:** 484s · **Time to recover:** 0s

## Timeline

| UTC | Offset | Event |
|---|---|---|
| 02:53:09 | - | steady state: scripts/smoke.sh |
| 02:53:09 | - | steady state OK |
| 02:53:09 | - | load: 4 worker(s) over 7 endpoint(s) |
| 02:53:23 | +4s | fault: netem on strava-postgres — netem 8003: root refcnt 15 limit 1000 delay 400.0ms  50.0ms 20% |
| 02:53:43 | +24s | alerts now: EndpointHighLatencyP95(pending) |
| 02:54:39 | +80s | alerts now: EdgeHighLatencyP95(pending), EndpointHighLatencyP95(pending) |
| 02:58:24 | +305s | alerts now: EdgeHighLatencyP95(pending) |
| 02:58:40 | +321s | query 'slow-ratio-5m-above-burn-threshold' met late (+321s) |
| 02:59:46 | +387s | alerts now: EdgeHighLatencyP95(pending), SLOLatencyBurnRateSlow(pending) |
| 03:01:23 | +484s | fault budget of 480s exhausted |
| 03:01:23 | +484s | fault ended after 484s |
| 03:01:23 | +484s | recovery: smoke passes 0s after the fault ended (budget 420s) |
| 03:01:23 | +484s | alert SLOLatencyBurnRateFast resolved in Alertmanager (+0s after fault end) |
