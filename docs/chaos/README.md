# Chaos experiments

Alerts and runbooks are promises: "a dead backend pages after two minutes",
"a restart does not page", "a slow database shows up as a latency burn".
The promtool tests in `monitoring/alerts/tests/` prove the rule logic against
synthetic series; the experiments here prove the promise on the stack we
ship — scrape, rule, Alertmanager, restart policy, smoke test — and write
down how long each step took. Decision and alternatives:
[ADR 014](../adr/014-chaos-experiments-as-alert-tests.md).

## Results

Measured on the composed stack (`docker compose --profile observability up
--build --wait`), Docker Desktop, 2026-10-11. Full reports with the timeline
are in [`chaos/results/`](../../chaos/results/).

| Experiment | Fault | Expected | Time to alert | Time to recover | Verdict |
|---|---|---|---|---|---|
| [`backend-pause`](../../chaos/experiments/backend-pause.yaml) | `docker pause` backend (153 s) | `ServiceDown` firing in Alertmanager ≤ 240 s | **142 s** (pending at 25 s) | **16 s** after unpause; alert resolved at 16 s | held |
| [`backend-kill`](../../chaos/experiments/backend-kill.yaml) | SIGKILL to the JVM from the host PID namespace; restart policy recovers | `ServiceDown` never fires (held for the whole run); JVM restarted (`process_uptime_seconds` < 60) ≤ 60 s | no alert; JVM back **~1.5 s** after the kill (uptime 19.6 s at +21 s); one scrape failed, `ServiceDown` pending for exactly one evaluation | **22 s** (smoke) | held |
| `backend-kill` attempt 1 | `docker kill` | pending ≤ 60 s, restart ≤ 240 s | pending at 21 s, **no restart**: Docker ignores the restart policy after a manual kill (exit 137, `RestartCount 0`) | never | **disproven** |
| `backend-kill` attempt 2 | `kill -9 1` inside the container | pending ≤ 60 s | nothing happened: the kernel drops SIGKILL sent to a PID namespace's init from inside | — | **disproven** |
| [`postgres-netem-delay`](../../chaos/experiments/postgres-netem-delay.yaml) | netem 400 ms ± 50 ms on Postgres (641 s), 4 load workers | edge p95 > 1 s and `EdgeHighLatencyP95` pending ≤ 120 s, firing ≤ 720 s | p95 > 1 s at **24 s**, pending **34 s**, firing in Alertmanager **631 s** | **1 s** (warning alert, smoke does not wait for it); alert resolved 309 s after the qdisc was removed | held |
| `postgres-netem-delay` attempt 1 | same fault (484 s) | `SLOLatencyBurnRateFast` firing ≤ 360 s | never fired; slow ratio 5 m crossed 0.72 only at 321 s, 1 h ratio 0.27 | 0 s | **disproven** — see below |
| [`elasticsearch-pause`](../../chaos/experiments/elasticsearch-pause.yaml) | `docker pause` elasticsearch (104 s), search-heavy load | `up` stays 1 and no 5xx on search for the whole fault; search p95 > 1 s ≤ 180 s; `EdgeHighLatencyP95` pending ≤ 300 s; no alert fires | search p95 = 30 s at **68 s**, pending **93 s**; `/actuator/health` 503 DOWN after 30 s (probed at +20 s, +55 s) | **< 1 s** after unpause | held |
| `elasticsearch-pause` attempt 3 | same fault (207 s) | pending ≤ 180 s | pending at 197 s: the 5 m window still held ~600 fast requests from the previous experiment | 0 s | **disproven** — budget reset to 300 s, CI waits 5 min between experiments |

"Time to alert" is measured from the fault injection to the moment the alert
is in the expected state (for `firing`: firing in Prometheus **and** active in
Alertmanager, i.e. a receiver would have been called after `group_wait`).
"Time to recover" is measured from the end of the fault to the first passing
`scripts/smoke.sh`, which also requires that no critical alert is active.

## What the runs taught us

- **A slow database does not trigger the latency burn-rate alert.** The
  first `postgres-netem-delay` hypothesis expected `SLOLatencyBurnRateFast`
  and was disproven (report kept as
  `postgres-netem-delay-2026-10-11-attempt1-disproven.md`). With +400 ms per
  query the read endpoints took 3–22 s (dashboard p95 22 s), so the four load
  workers managed ~0.4 req/s instead of ~20: throughput collapsed. The SLI is
  request-weighted, so the fast requests from before the fault dominated the
  5 m ratio for a long time (0.5 after five minutes, above 0.72 only after
  321 s) and the 1 h ratio stayed at 0.27. The alert that reacts is
  `EdgeHighLatencyP95`, and its time-to-page is its own `for: 10m`.
- **Traffic floors hide the endpoint alert under collapse.**
  `EndpointHighLatencyP95` went pending 20 s into the fault and dropped out
  again once every endpoint fell below its 0.1 req/s floor. Correct by
  design (it must not fire on two slow requests a minute), but it means the
  per-endpoint name is missing exactly when the database is the cause.
- **`docker kill` is not a crash.** The first `backend-kill` attempt used
  `docker kill`; Docker treats that as a manual stop and ignores the
  `unless-stopped` restart policy (exit 137, `RestartCount 0`, container down
  until started by hand — report kept as
  `backend-kill-2026-10-11-attempt1-disproven.md`). An operator who "restarts"
  the backend that way leaves the stack down and pages in two minutes. The
  second attempt sent `kill -9 1` from inside the container and nothing
  happened: the kernel drops SIGKILL aimed at a PID namespace's init from
  within it (`backend-kill-2026-10-11-attempt2-disproven.md`; the JVM is PID 1
  in this image). The runner now sends the SIGKILL from the host PID namespace
  through a helper container with `--pid=host`, which Docker sees as the
  process dying — the crash the restart policy exists for.
- **A paused backend and a crashed backend are different alerts.** A pause
  pages in 142 s; a crash restarts in ~1.5 s, costs at most one failed scrape
  and leaves `ServiceDown` pending for a single evaluation — the intended
  behaviour of `for: 2m`, now measured instead of assumed. The flip side: a
  crash *loop* that always beats the 15 s scrape is invisible to
  `ServiceDown`; `changes(process_start_time_seconds[1h])` would see it and
  nothing alerts on that today.
- **Elasticsearch is a blind spot.** Nothing alerts on it. Search answers
  200 via the SQL fallback after the client's 30 s socket timeout, and
  `/actuator/health` answers `503 {"status":"DOWN"}` after the same 30 s
  (probed at +20 s and +55 s into the pause) — so the aggregate health does
  see it, as `docs/observability.md` says, but no rule reads that endpoint
  and `up` stays 1 because `/actuator/prometheus` does not touch
  Elasticsearch. The only trace in Prometheus is the search endpoint's p95.
  A blackbox probe on `/actuator/health` or an Elasticsearch scrape target
  would close the gap.

## How to run one locally

```bash
cp .env.example .env                      # once; set POSTGRES_PASSWORD and JWT_SECRET
docker compose --profile observability up -d --build --wait
chaos/run.sh chaos/experiments/backend-pause.yaml
```

The runner needs `docker` (compose v2), `curl`, `jq` and bash ≥ 3.2 (macOS's
`/bin/bash` is fine). YAML is read through the `mikefarah/yq` container; the
netem experiment uses `gaiaadm/pumba` with its `gaiadocker/iproute2` helper.
Set `YQ=/path/to/yq` to skip the container. `BASE_URL`, `PROM_URL` and
`ALERTMANAGER_URL` default to the compose ports (8081, 9091, 9093) like
`scripts/smoke.sh`.

What happens, in order:

1. **Steady state** — `scripts/smoke.sh` must pass; otherwise nothing is
   injected (exit 2).
2. **Load** — `load.workers` background curl loops cycle through
   `load.endpoints` (default: the authenticated read endpoints) with a token
   from `john@example.com`; the SLO and latency rules need traffic to say
   anything.
3. **Fault** — resolved with `docker compose ps -q <service>`, so only a
   container of *this* compose project is touched: `pause` (`docker pause`),
   `kill` (SIGKILL to the container's main process from the host PID
   namespace, via `alpine --pid=host`; neither `docker kill` nor a kill from
   inside the container produces a crash the restart policy reacts to),
   `netem-delay` (pumba, `delay_ms` ± `jitter_ms`) or `cpu-stress` (busy
   loops inside the container).
4. **Expectations** — every 5 s the runner reads `/api/v1/alerts` on
   Prometheus and `/api/v2/alerts` on Alertmanager and evaluates each
   `expect.queries` expression. Each expectation has a `within` budget in
   seconds from the fault start; meeting it late disproves the hypothesis.
   Every other alert that goes pending or firing during the fault is written
   to the timeline too.
5. **Recovery** — the fault ends (early, once every expectation is met plus
   `hold_after_expectations` seconds; or at `duration`). The runner then
   polls `scripts/smoke.sh` every 15 s up to `expect.recovery.smoke_within`
   and waits for the expected `firing` alerts to leave Alertmanager.
6. **Report** — `chaos/results/<name>-<date>.md`: hypothesis, measured
   table, time to alert / recover, timeline. Exit 0 when the hypothesis held,
   1 when disproven, 2 on a setup problem.

Cleanup runs from an `EXIT` trap: unpause, remove the netem qdisc (also if
pumba was interrupted), kill the busy loops, stop the load. A failed or
interrupted run leaves nothing behind.

## How to add an experiment

Copy one of the files in [`chaos/experiments/`](../../chaos/experiments/):

```yaml
name: postgres-netem-delay            # also the report's file name
hypothesis:                           # prose, copied into the report
  steady_state: …
  fault: …
  expected_alerts: …                  # name the alert AND why the budget is what it is
  expected_recovery: …
  notes: …                            # optional: what this verifies, known limits
action:
  type: netem-delay                   # kill | pause | netem-delay | cpu-stress
  service: postgres                   # compose service name
  duration: 480                       # max seconds the fault stays; ended early once expectations are met
  hold_after_expectations: 10         # seconds to keep the fault after the last expectation is met
  delay_ms: 400                       # netem-delay only (jitter_ms, default 50)
  cpu_workers: 2                      # cpu-stress only
load:
  enabled: true
  workers: 4
  endpoints: [/api/v1/users/me]       # optional; default = the read endpoints the smoke test uses
expect:
  alerts:
    - alertname: SLOLatencyBurnRateFast
      state: firing                   # firing (Prometheus + Alertmanager) | pending (Prometheus)
      within: 360                     # seconds after the fault start
  queries:                            # optional PromQL instant queries, same budget semantics
    - name: slow-ratio-5m-above-burn-threshold
      expr: slo:http_slow:ratio_rate5m{app="strava-backend"}
      op: ">"                         # > >= < <= == !=
      value: 0.72
      within: 150
      hold: false                     # true = must stay true until the fault ends
  recovery:
    smoke_within: 420                 # seconds after the fault end
    alerts_resolved: true             # expected firing alerts must leave Alertmanager
```

Derive `within` from the rule, do not guess: scrape interval (15 s) + scrape
timeout (10 s) for `up`-based rules, the rule's `for`, the group's
evaluation interval (15 s; recording rules 30 s), and a margin. If the rule's
windows make `firing` impossible inside a CI-sized run (`for: 10m`, 1 h
burn-rate windows on a stack with history), assert `pending` or the
recording-rule value and say so in `hypothesis.expected_alerts`. Never loosen
a rule to make an experiment pass; the experiment exists to measure the rule
as it is. Then add the file to the `experiment` choice list in
`.github/workflows/chaos.yml`, run it locally, commit the report, and add the
row above.

## In CI

`.github/workflows/chaos.yml` runs weekly (Sunday 03:30 UTC) and on
`workflow_dispatch` with an `experiment` choice (`all` or one). It boots the
composed stack like the E2E workflow, runs the experiments in sequence with
five minutes between them (the 5 m rate windows must drain: the previous
experiment's fast requests delayed `EdgeHighLatencyP95` from 47 s to 197 s in
one local run), writes a summary table and the full reports to the step
summary, uploads `chaos/results/` as an artifact and fails when any
hypothesis was disproven. Not on pull requests: 5–15 minutes per experiment.

## What it does not cover

- Kubernetes: `k8s/` has no monitoring stack to test against.
- Notification delivery: the runner stops at "active in Alertmanager"; the
  `alert-sink` log shows whether the webhook was called.
- Elasticsearch has no alert of its own; `elasticsearch-pause` records what
  the monitoring sees (latency on one endpoint, DOWN aggregate health) and
  what it does not.
