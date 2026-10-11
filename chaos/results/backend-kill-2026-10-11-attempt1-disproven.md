# Chaos experiment: backend-kill

**Verdict: hypothesis disproven.** Run 2026-10-11T03:33:15Z on `Darwin arm64`, compose project `stride-chaos`, target `strava-backend`.

## Hypothesis

- **Steady state:** scripts/smoke.sh passes.
- **Fault:** docker kill on the backend container; nothing else — the restart policy is the recovery mechanism under test.
- **Expected alerts:** ServiceDown goes PENDING in Prometheus within 60 s (first failed scrape → pending at the next 15 s evaluation). It must NOT be expected to fire: the JVM is back and scraped well before the 2 m "for" window ends, which is exactly the behaviour the rule was designed for (a restart is not a page). Asserting "pending" proves the scrape failure was seen without changing the rule.
- **Expected recovery:** The container is running again within seconds, the backend serves /actuator/health after the JVM boot (~30–60 s), and the smoke test passes within 240 s of the kill. No alert reaches Alertmanager.

Verifies the restart policy in docker-compose.yml and the "for: 2m" choice in monitoring/alerts/platform.rules.yml (docs/runbooks/service-down.md). A restart that took longer than two minutes would turn this into backend-pause.

## Measured

| Expectation | Result |
|---|---|
| alert `ServiceDown` pending within 60s | pending in Prometheus after 21s (budget 60s) |
| recovery: smoke within 240s | smoke did not pass within 240s |
| recovery: alerts resolved (budget 120s) | all expected alerts resolved in Alertmanager |

**Time to alert:** ServiceDown pending 21s · **Fault duration:** 0s · **Time to recover:** —

## Timeline

| UTC | Offset | Event |
|---|---|---|
| 03:28:09 | - | steady state: scripts/smoke.sh |
| 03:28:09 | - | steady state OK |
| 03:28:09 | - | load: 2 worker(s) over 7 endpoint(s) |
| 03:28:20 | +1s | fault: docker kill strava-backend (compose restart policy brings it back) |
| 03:28:20 | +1s | alerts now: SLOLatencyBurnRateSlow(firing) |
| 03:28:40 | +21s | alert ServiceDown pending (+21s) |
| 03:28:40 | +21s | alerts now: SLOLatencyBurnRateSlow(firing), ServiceDown(pending) |
| 03:28:40 | +21s | kill is instantaneous — recovery clock runs from the kill |
| 03:30:43 | +144s | container running again: false (restart policy unless-stopped) |
| 03:33:15 | +296s | recovery: smoke did not pass within 240s |
