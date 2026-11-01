# Chaos experiment: backend-kill

**Verdict: hypothesis held.** Run 2026-10-11T03:55:46Z on `Darwin arm64`, compose project `stride-chaos`, target `strava-backend`.

## Hypothesis

- **Steady state:** scripts/smoke.sh passes.
- **Fault:** SIGKILL to the backend's main process (the JVM is PID 1 of the container) from the host PID namespace; nothing else — the restart policy is the recovery mechanism under test.
- **Expected alerts:** None. ServiceDown must never fire (asserted for the whole run through the ALERTS series), and in practice it does not even go pending: Docker restarts the container within a second and the JVM serves /actuator/prometheus again before the next 15 s scrape, so `up` never records a 0 (attempt 3 on 2026-10-11: RestartCount 0 → 1, StartedAt 0.4 s after the kill, up == 1 on every scrape). The restart itself is proven through process_uptime_seconds dropping below 60 s within 60 s of the kill; whether a scrape failed is recorded (min_over_time of up), not asserted.
- **Expected recovery:** The container is running again within seconds and the smoke test passes within 120 s of the kill.

Verifies the restart policy in docker-compose.yml and the "for: 2m" choice in monitoring/alerts/platform.rules.yml (docs/runbooks/service-down.md): a single crash is not a page, and it is not even a pending alert at a 15 s scrape interval. The flip side is a gap: a crash *loop* would also be invisible to ServiceDown as long as each restart beats the scrape; `changes(process_start_time_seconds[1h])` would catch it and is not alerted on today. Attempt 1 showed what `docker kill` does (nothing restarts; ServiceDown would have fired after 2 minutes), attempt 2 that `kill -9 1` inside a container is a no-op.

## Measured

| Expectation | Result |
|---|---|
| query `service-down-never-fires` == 1 within 5s | met after 0s: value 1 == 1 (budget 5s); held until the fault ended |
| query `jvm-restarted` < 60 within 60s | met after 21s: value 19.594 < 60 (budget 60s) |
| recovery: smoke within 120s | smoke passes 22s after the fault ended (budget 120s) |
| recovery: alerts resolved (budget 120s) | not checked |

Observed at the end of the fault (reported, not asserted):

| Query | Value |
|---|---|
| `min up over the last 2 m (0 = a scrape failed)` | 0 |
| `ServiceDown pending or firing at the end of the fault window (1 = yes)` | 0 |

**Time to alert:** — · **Fault duration:** 0s · **Time to recover:** 22s

## Timeline

| UTC | Offset | Event |
|---|---|---|
| 03:55:13 | - | steady state: scripts/smoke.sh |
| 03:55:14 | - | steady state OK |
| 03:55:14 | - | load: 2 worker(s) over 7 endpoint(s) |
| 03:55:24 | +0s | fault: SIGKILL to strava-backend's main process (host pid 73927) from the host PID namespace — a crash; the restart policy is the recovery under test |
| 03:55:24 | +0s | query 'service-down-never-fires' met: 1 == 1 (+0s) |
| 03:55:24 | +0s | alerts now: EdgeHighLatencyP95(pending), EndpointHighLatencyP95(pending) |
| 03:55:45 | +21s | query 'jvm-restarted' met: 19.594 < 60 (+21s) |
| 03:55:45 | +21s | observed 'min up over the last 2 m (0 = a scrape failed)' = 0 at the end of the fault |
| 03:55:45 | +21s | observed 'ServiceDown pending or firing at the end of the fault window (1 = yes)' = 0 at the end of the fault |
| 03:55:45 | +21s | kill is instantaneous — recovery clock runs from the kill |
| 03:55:45 | +21s | container running again: true (restart policy unless-stopped) |
| 03:55:46 | +22s | recovery: smoke passes 22s after the fault ended (budget 120s) |
