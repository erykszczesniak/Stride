# Performance: k6 load profile gated by the SLOs

`perf/k6/user-journey.js` walks the path a signed-in user takes after opening
the app — profile, my activities, feed, dashboard, this week's stats, one
user search — against the composed stack, and fails when the result would
break an SLO from [`docs/slo.md`](../docs/slo.md). Read-only: nothing grows
the database, and the AI endpoints (external providers, their own SLO) are
left out.

It runs in CI as [`.github/workflows/perf.yml`](../.github/workflows/perf.yml):
nightly, on demand (choose VUs / ramp / hold), and on every push to a
`release/**` branch, where a threshold breach blocks the release.

## Run it locally

Stack up (`make up`, backend on `:8081`), then one container:

```bash
docker run --rm --network host -v "$PWD/perf/k6:/scripts" grafana/k6:0.54.0 \
  run --summary-export /scripts/out/summary.json /scripts/user-journey.js
```

On Docker Desktop (macOS / Windows) `--network host` does not reach the host's
ports; point the script at the gateway instead:

```bash
docker run --rm -v "$PWD/perf/k6:/scripts" -e BASE_URL=http://host.docker.internal:8081 \
  grafana/k6:0.54.0 run --summary-export /scripts/out/summary.json /scripts/user-journey.js
```

Then `perf/k6/summary-to-markdown.sh perf/k6/out/summary.json` prints the same
table the workflow writes to the run summary. `docker run --rm -v "$PWD/perf/k6:/scripts" grafana/k6:0.54.0 inspect /scripts/user-journey.js`
validates the script without running it.

| Variable | Default | Meaning |
|---|---|---|
| `BASE_URL` | `http://localhost:8081` | backend base URL |
| `VUS` | `10` | virtual users at plateau |
| `RAMP` | `30s` | ramp from 0 to `VUS` |
| `HOLD` | `2m` | plateau at `VUS` (then a fixed 10 s ramp-down) |
| `THINK_TIME` | `1` | seconds a VU pauses between iterations |
| `USER_EMAIL` / `USER_PASSWORD` | `john@example.com` / `password123` | seeded user (`V2__sample_data.sql`) |

Two limits set by the app: `/api/v1/auth/login` is rate-limited to 10
requests per minute per IP, so the script logs in **once** in `setup()` and
shares the token; the access token lives 30 minutes, so keep `RAMP + HOLD`
under about 25 minutes.

## What the thresholds mean

| Threshold | Value | Where it comes from |
|---|---|---|
| `http_req_failed` | `rate<0.005` | **Edge availability** SLO: 99.5 % of requests succeed, so at most 0.5 % may fail |
| `http_req_duration` | `p(95)<500` | **Edge latency** SLO: 95 % of requests complete within 500 ms (the `le="0.5"` histogram bucket the burn-rate rules use) |
| `http_req_duration{name:<endpoint>}` | `p(95)<500` | the same latency SLO per endpoint, so the summary names the slow path instead of averaging it away |
| `feed_duration` | `p(95)<500` | custom Trend for the slowest single path in the baseline (the feed joins followed users, activities, likes and comments) |
| `checks` | `rate==1` | every response had the status and shape the UI expects; a fast 200 with the wrong body is still a failure |

The load test and the production alerts therefore disagree only on *where*
the measurement happens, not on *what* "good" means: a release that fails
here would start burning the error budget as soon as it took traffic.

k6 exits with code 99 when a threshold is breached; the workflow publishes
the numbers first (`$GITHUB_STEP_SUMMARY`, `k6-summary` artifact) and then
fails.

## Baseline

Measured on 2026-10-11 against the full compose stack (`docker compose up
-d --build --wait`, PostgreSQL 16, Elasticsearch 8.11, sample data from the
Flyway migrations) on a developer laptop with Docker Desktop, k6 0.54.0 in a
container, `BASE_URL=http://host.docker.internal:8081`:

| Profile | Requests | Failed | p95 | p99 | max | Feed p95 | Throughput | Thresholds |
|---|---|---|---|---|---|---|---|---|
| 5 VUs, ramp 15 s, hold 30 s, think time 1 s | 1219 (203 iterations) | 0 (0.00 %) | 13.4 ms | 19.3 ms | 85.7 ms | 17.1 ms | 21.9 req/s | all 10 passed, 2032 / 2032 checks |

Per endpoint, p95 in that run: feed 17.1 ms, `users/me` 13.1 ms, my
activities 12.9 ms, user search 12.1 ms, dashboard 11.6 ms, weekly stats
3.3 ms. The stack has a few hundred sample rows, so these numbers say the
path works and where the relative cost is, not how production would behave
under real data volumes; treat a p95 that doubles between two runs on the
same hardware as the signal, not the absolute value.

Update this table when the profile, the data set or the hardware changes.
