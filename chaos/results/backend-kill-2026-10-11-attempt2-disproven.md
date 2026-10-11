# Chaos experiment: backend-kill

**Verdict: hypothesis disproven.** Run 2026-10-11T03:39:10Z on `Darwin arm64`, compose project `stride-chaos`, target `strava-backend`.

## Hypothesis

- **Steady state:** scripts/smoke.sh passes.
- **Fault:** SIGKILL to PID 1 inside the backend container; nothing else — the restart policy is the recovery mechanism under test.
- **Expected alerts:** ServiceDown goes PENDING in Prometheus within 60 s (first failed scrape → pending at the next 15 s evaluation). It must NOT be expected to fire: the JVM is back and scraped well before the 2 m "for" window ends, which is exactly the behaviour the rule was designed for (a restart is not a page). Asserting "pending" proves the scrape failure was seen without changing the rule.
- **Expected recovery:** The container is running again within seconds, the backend serves /actuator/health after the JVM boot (~30–60 s), and the smoke test passes within 240 s of the kill. No alert reaches Alertmanager.

Verifies the restart policy in docker-compose.yml and the "for: 2m" choice in monitoring/alerts/platform.rules.yml (docs/runbooks/service-down.md). A restart that took longer than two minutes would turn this into backend-pause. Attempt 1 also showed what `docker kill` does: nothing restarts, and ServiceDown would have fired after 2 minutes.

## Measured

| Expectation | Result |
|---|---|
| alert `ServiceDown` pending within 60s | not seen |
| recovery: smoke within 240s | smoke passes 92s after the fault ended (budget 240s) |
| recovery: alerts resolved (budget 120s) | all expected alerts resolved in Alertmanager |

**Time to alert:** — · **Fault duration:** 0s · **Time to recover:** 92s

## Timeline

| UTC | Offset | Event |
|---|---|---|
| 03:37:27 | - | steady state: scripts/smoke.sh |
| 03:37:28 | - | steady state OK |
| 03:37:28 | - | load: 2 worker(s) over 7 endpoint(s) |
| 03:37:38 | +0s | fault: kill -9 PID 1 inside strava-backend (a crash; the restart policy is the recovery under test) |
| 03:37:38 | +0s | alerts now: EdgeHighLatencyP95(pending), EndpointHighLatencyP95(pending), SLOLatencyBurnRateSlow(firing) |
| 03:38:14 | +36s | alerts now: EndpointHighLatencyP95(pending), SLOLatencyBurnRateSlow(firing) |
| 03:38:49 | +71s | alerts now: EndpointHighLatencyP95(pending) |
| 03:39:09 | +91s | fault budget of 90s exhausted |
| 03:39:09 | +91s | kill is instantaneous — recovery clock runs from the kill |
| 03:39:10 | +92s | container running again: true (restart policy unless-stopped) |
| 03:39:10 | +92s | recovery: smoke passes 92s after the fault ended (budget 240s) |
