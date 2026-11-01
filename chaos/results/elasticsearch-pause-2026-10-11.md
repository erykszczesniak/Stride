# Chaos experiment: elasticsearch-pause

**Verdict: hypothesis held.** Run 2026-10-11T03:54:08Z on `Darwin arm64`, compose project `stride-chaos`, target `strava-elasticsearch`.

## Hypothesis

- **Steady state:** scripts/smoke.sh passes (the search check returns 200 through Elasticsearch).
- **Fault:** docker pause on the elasticsearch container for up to 300 s, with 3 load workers that hit the search endpoint and one fast endpoint.
- **Expected alerts:** No alert fires. up{job="strava-backend"} stays 1 the whole time (the backend is up, so ServiceDown must not move), and the search endpoint produces no 5xx (the SQL fallback answers 200 after the Elasticsearch client's 30 s socket timeout). What the monitoring does see: the search endpoint's p95 goes far above 1 s within 180 s, and the edge p95 follows once enough 30 s searches sit in the 5 m window — EdgeHighLatencyP95 goes PENDING within 300 s (measured 47 s on a quiet window and 197 s when the window still held ~600 fast requests from the previous experiment). It needs 10 minutes to fire, longer than this budget, so pending is asserted and the gap is documented.
- **Expected recovery:** After docker unpause, searches go back to Elasticsearch, /actuator/health is UP again and the smoke test passes within 300 s (its search and health checks have a 15 s timeout, so they fail during the fault and pass after).

Documents a gap rather than a strength: a frozen Elasticsearch is visible as latency on one endpoint and as a DOWN aggregate health (/actuator/health answered 503 {"status":"DOWN"} after 30 s, probed at +20 s and +55 s during the 2026-10-11 run), and nothing alerts on either today. docs/observability.md already lists client spans for Elasticsearch and freshness metrics as not done; a blackbox probe on /actuator/health or an Elasticsearch scrape target would turn this into a real alert.

## Measured

| Expectation | Result |
|---|---|
| alert `EdgeHighLatencyP95` pending within 300s | pending in Prometheus after 93s (budget 300s) |
| query `backend-still-scraped` == 1 within 30s | met after 0s: value 1 == 1 (budget 30s); held until the fault ended |
| query `search-p95-above-1s` > 1 within 180s | met after 68s: value 30 > 1 (budget 180s) |
| query `no-5xx-on-search` == 0 within 180s | met after 0s: value 0 == 0 (budget 180s); held until the fault ended |
| recovery: smoke within 300s | smoke passes 0s after the fault ended (budget 300s) |
| recovery: alerts resolved (budget 120s) | all expected alerts resolved in Alertmanager |

**Time to alert:** EdgeHighLatencyP95 pending 93s · **Fault duration:** 104s · **Time to recover:** 0s

## Timeline

| UTC | Offset | Event |
|---|---|---|
| 03:52:14 | - | steady state: scripts/smoke.sh |
| 03:52:14 | - | steady state OK |
| 03:52:14 | - | load: 3 worker(s) over 2 endpoint(s) |
| 03:52:24 | +0s | fault: docker pause strava-elasticsearch |
| 03:52:24 | +0s | query 'backend-still-scraped' met: 1 == 1 (+0s) |
| 03:52:24 | +0s | query 'no-5xx-on-search' met: 0 == 0 (+0s) |
| 03:53:32 | +68s | query 'search-p95-above-1s' met: 30 > 1 (+68s) |
| 03:53:57 | +93s | alert EdgeHighLatencyP95 pending (+93s) |
| 03:53:58 | +94s | alerts now: EdgeHighLatencyP95(pending), EndpointHighLatencyP95(pending) |
| 03:54:08 | +104s | fault ended after 104s |
| 03:54:08 | +104s | recovery: smoke passes 0s after the fault ended (budget 300s) |
