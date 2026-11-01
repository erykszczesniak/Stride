# Chaos experiment: elasticsearch-pause

**Verdict: hypothesis disproven.** Run 2026-10-11T03:44:05Z on `Darwin arm64`, compose project `stride-chaos`, target `strava-elasticsearch`.

## Hypothesis

- **Steady state:** scripts/smoke.sh passes (the search check returns 200 through Elasticsearch).
- **Fault:** docker pause on the elasticsearch container for up to 300 s, with 3 load workers that hit the search endpoint and one fast endpoint.
- **Expected alerts:** No alert fires. up{job="strava-backend"} stays 1 the whole time (the backend is up, so ServiceDown must not move), and the search endpoint produces no 5xx (the SQL fallback answers 200 after the Elasticsearch client's 30 s socket timeout). What the monitoring does see: the search endpoint's p95 goes far above 1 s, so EdgeHighLatencyP95 goes PENDING within 180 s — it needs 10 minutes to fire, longer than this budget, so pending is asserted and the gap is documented.
- **Expected recovery:** After docker unpause, searches go back to Elasticsearch and the smoke test passes within 300 s (its search check has a 15 s timeout, so it fails during the fault and passes after).

Documents a gap rather than a strength: a frozen Elasticsearch is only visible as latency on one endpoint. /actuator/health answered 200 {"status":"UP"} 15 s into the pause (probe during the 2026-10-11 run), so neither the compose health check nor a health probe would notice; docs/observability.md's statement that an unreachable Elasticsearch makes the aggregate DOWN did not hold for a paused container. An Elasticsearch scrape target or a synthetic search probe would turn this into a real alert.

## Measured

| Expectation | Result |
|---|---|
| alert `EdgeHighLatencyP95` pending within 180s | LATE: pending after 197s, budget was 180s |
| query `backend-still-scraped` == 1 within 30s | met after 0s: value 1 == 1 (budget 30s) |
| query `search-p95-above-1s` > 1 within 180s | met after 36s: value 10.856722885583338 > 1 (budget 180s) |
| query `no-5xx-on-search` == 0 within 180s | met after 0s: value 0 == 0 (budget 180s) |
| recovery: smoke within 300s | smoke passes 0s after the fault ended (budget 300s) |
| recovery: alerts resolved (budget 120s) | all expected alerts resolved in Alertmanager |

**Time to alert:** EdgeHighLatencyP95 pending 197s · **Fault duration:** 207s · **Time to recover:** 0s

## Timeline

| UTC | Offset | Event |
|---|---|---|
| 03:40:28 | - | steady state: scripts/smoke.sh |
| 03:40:28 | - | steady state OK |
| 03:40:28 | - | load: 3 worker(s) over 2 endpoint(s) |
| 03:40:38 | +0s | fault: docker pause strava-elasticsearch |
| 03:40:38 | +0s | query 'backend-still-scraped' met: 1 == 1 (+0s) |
| 03:40:38 | +0s | query 'no-5xx-on-search' met: 0 == 0 (+0s) |
| 03:41:15 | +37s | query 'search-p95-above-1s' met: 10.856722885583338 > 1 (+36s) |
| 03:41:40 | +62s | alerts now: EndpointHighLatencyP95(pending) |
| 03:43:55 | +197s | alert EdgeHighLatencyP95 pending but late (+197s > 180s) |
| 03:43:55 | +197s | alerts now: EdgeHighLatencyP95(pending), EndpointHighLatencyP95(pending) |
| 03:44:05 | +207s | fault ended after 207s |
| 03:44:05 | +207s | recovery: smoke passes 0s after the fault ended (budget 300s) |
