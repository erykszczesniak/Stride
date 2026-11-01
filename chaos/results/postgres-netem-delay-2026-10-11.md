# Chaos experiment: postgres-netem-delay

**Verdict: hypothesis held.** Run 2026-10-11T03:27:14Z on `Darwin arm64`, compose project `stride-chaos`, target `strava-postgres`.

## Hypothesis

- **Steady state:** scripts/smoke.sh passes; edge p95 is well under 1 s.
- **Fault:** pumba netem delay 400 ms ± 50 ms (normal distribution) on the postgres container for up to 780 s, with 4 load workers cycling through the authenticated read endpoints.
- **Expected alerts:** Edge p95 (histogram_quantile over all non-actuator requests, 5 m window) crosses 1 s within 120 s and EdgeHighLatencyP95 (warning, team domain) goes PENDING within 120 s; it FIRES and is active in Alertmanager within 720 s — the rule has for: 10m, evaluation every 15 s, and Alertmanager shows it on arrival (group_wait only delays the notification). Expected ~680 s, budget 720 s. SLOLatencyBurnRateFast is not expected: with throughput collapsed the request-weighted slow ratio stays dominated by the pre-fault traffic in its 1 h window (measured 0.27 after 5 minutes of fault); the ratios are recorded below instead of asserted.
- **Expected recovery:** After the qdisc is removed, latency is normal immediately. The smoke test passes right away (the alert is a warning, not critical); EdgeHighLatencyP95 resolves once the slow samples leave its 5 m window — within 420 s.

Verifies docs/runbooks/high-latency.md ("the app feels slow … for 10 minutes") and measures the real time-to-page for a slow database: ~11 minutes, all of it the rule's own "for". Also seen: EndpointHighLatencyP95 goes pending in 20 s and then drops out again because the throughput collapse puts every endpoint below its 0.1 req/s traffic floor; DbPoolSaturated does not move (4 of 10 connections busy at this load); the SLO latency burn-rate alerts are the wrong tool for this fault at this traffic level.

## Measured

| Expectation | Result |
|---|---|
| alert `EdgeHighLatencyP95` pending within 120s | pending in Prometheus after 34s (budget 120s) |
| alert `EdgeHighLatencyP95` firing within 720s | firing in Prometheus and active in Alertmanager after 631s (budget 720s) |
| query `edge-p95-above-1s` > 1 within 120s | met after 24s: value 3.035110220680002 > 1 (budget 120s) |
| recovery: smoke within 300s | smoke passes 1s after the fault ended (budget 300s) |
| recovery: alerts resolved (budget 420s) | all expected alerts resolved in Alertmanager |

Observed at the end of the fault (reported, not asserted):

| Query | Value |
|---|---|
| `slo:http_slow:ratio_rate5m` | 1 |
| `slo:http_slow:ratio_rate1h` | 0.45189504373177847 |
| `requests per second (5m, non-actuator)` | 0.45262999077196225 |
| `edge p95 seconds (5m)` | 20.444044328769998 |
| `HikariCP active connections` | 4 |

**Time to alert:** EdgeHighLatencyP95 pending 34s, EdgeHighLatencyP95 firing 631s · **Fault duration:** 641s · **Time to recover:** 1s

## Timeline

| UTC | Offset | Event |
|---|---|---|
| 03:11:13 | - | steady state: scripts/smoke.sh |
| 03:11:14 | - | steady state OK |
| 03:11:14 | - | load: 4 worker(s) over 7 endpoint(s) |
| 03:11:27 | +3s | fault: netem on strava-postgres — netem 8004: root refcnt 15 limit 1000 delay 400.0ms  50.0ms 20% |
| 03:11:27 | +3s | alerts now: SLOLatencyBurnRateSlow(pending) |
| 03:11:48 | +24s | query 'edge-p95-above-1s' met: 3.035110220680002 > 1 (+24s) |
| 03:11:58 | +34s | alert EdgeHighLatencyP95 pending (+34s) |
| 03:11:58 | +34s | alerts now: EdgeHighLatencyP95(pending), EndpointHighLatencyP95(pending) |
| 03:13:20 | +116s | alerts now: EdgeHighLatencyP95(pending), EndpointHighLatencyP95(pending), SLOLatencyBurnRateSlow(pending) |
| 03:16:28 | +304s | alerts now: EdgeHighLatencyP95(pending), SLOLatencyBurnRateSlow(pending) |
| 03:21:55 | +631s | alert EdgeHighLatencyP95 firing, in Alertmanager (+631s) |
| 03:21:55 | +631s | alerts now: EdgeHighLatencyP95(firing), SLOLatencyBurnRateSlow(pending) |
| 03:22:05 | +641s | observed 'slo:http_slow:ratio_rate5m' = 1 at the end of the fault |
| 03:22:05 | +641s | observed 'slo:http_slow:ratio_rate1h' = 0.45189504373177847 at the end of the fault |
| 03:22:05 | +641s | observed 'requests per second (5m, non-actuator)' = 0.45262999077196225 at the end of the fault |
| 03:22:05 | +641s | observed 'edge p95 seconds (5m)' = 20.444044328769998 at the end of the fault |
| 03:22:05 | +641s | observed 'HikariCP active connections' = 4 at the end of the fault |
| 03:22:05 | +641s | fault ended after 641s |
| 03:22:06 | +642s | recovery: smoke passes 1s after the fault ended (budget 300s) |
| 03:27:14 | +950s | alert EdgeHighLatencyP95 resolved in Alertmanager (+309s after fault end) |
