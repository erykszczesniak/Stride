# Chaos experiment: backend-pause

**Verdict: hypothesis held.** Run 2026-10-11T02:52:52Z on `Darwin arm64`, compose project `stride-chaos`, target `strava-backend`.

## Hypothesis

- **Steady state:** scripts/smoke.sh passes (API, Prometheus scrapes the backend, Watchdog in Alertmanager, no critical alert).
- **Fault:** docker pause on the backend container, up to 300 s.
- **Expected alerts:** ServiceDown (critical, team platform) is firing in Prometheus and active in Alertmanager within 240 s: the scrape fails after the 10 s scrape timeout (≤ 25 s after the pause), the rule has for: 2m, groups are evaluated every 15 s, and Alertmanager shows the alert as soon as it arrives (group_wait only delays the notification). Expected ~165 s, budget 240 s.
- **Expected recovery:** After docker unpause the backend answers immediately; the smoke test passes within 300 s and ServiceDown resolves in Alertmanager (the smoke test refuses to pass while a critical alert is active).

The runbook promise this verifies: docs/runbooks/service-down.md — "a dead backend keeps its up series at 0 and this alert fires after 2 minutes". A paused container is the strictest version of "dead": Docker reports it as running, so only the scrape can see it.

## Measured

| Expectation | Result |
|---|---|
| alert `ServiceDown` firing within 240s | firing in Prometheus and active in Alertmanager after 142s (budget 240s) |
| recovery: smoke within 300s | smoke passes 16s after the fault ended (budget 300s) |
| recovery: alerts resolved | all expected alerts resolved in Alertmanager |

**Time to alert:** ServiceDown firing 142s · **Fault duration:** 153s · **Time to recover:** 16s

## Timeline

| UTC | Offset | Event |
|---|---|---|
| 02:49:53 | - | steady state: scripts/smoke.sh |
| 02:49:53 | - | steady state OK |
| 02:49:53 | - | load: 2 worker(s) over 7 endpoint(s) |
| 02:50:04 | +1s | fault: docker pause strava-backend |
| 02:50:29 | +26s | alerts now: ServiceDown(pending) |
| 02:52:26 | +143s | alert ServiceDown firing, in Alertmanager (+142s) |
| 02:52:26 | +143s | alerts now: ServiceDown(firing) |
| 02:52:36 | +153s | fault ended after 153s |
| 02:52:52 | +169s | recovery: smoke passes 16s after the fault ended (budget 300s) |
| 02:52:52 | +169s | alert ServiceDown resolved in Alertmanager (+16s after fault end) |
