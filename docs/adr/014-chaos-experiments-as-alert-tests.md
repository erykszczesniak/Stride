# ADR-014 — Chaos experiments as tests of the alerts and the recovery

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-10-11 |
| **Deciders** | platform |

## Context

Every alert in `monitoring/alerts/` has a promtool unit test, an owner and a
runbook (ADR 006), and the SLO rules follow the burn-rate recipe (ADR 007).
What none of that proves is that the chain works end to end on the stack we
actually ship: that a frozen backend turns into `ServiceDown` in Alertmanager
in the time the runbook promises, that a restart does *not* page, that a slow
database shows up as a latency burn before users give up, and that the stack
is whole again when the fault is gone. The promtool tests feed the rules
synthetic series; the compose health checks look at one container at a time.
The gap between "the rule evaluates correctly" and "someone would have been
paged" had never been measured.

## Decision

1. **Experiments are data, the runner is one script.** Each experiment in
   `chaos/experiments/*.yaml` states a hypothesis (steady state → fault →
   expected alerts → expected recovery) and the data needed to run it: the
   compose service, the action (`kill`, `pause`, `netem-delay`,
   `cpu-stress`), the time budgets, the alert names and states, optional
   PromQL assertions. `chaos/run.sh` executes any of them the same way and
   writes a Markdown report with the timeline and the measured numbers.
2. **The assertion is the alert, not the metric.** An experiment passes
   when the named alert is `firing` in Prometheus *and* active in
   Alertmanager — the point where a receiver would have been called — within
   a budget derived from the rule's `for`, the evaluation interval and the
   scrape interval. Where a rule's windows are longer than a CI-sized run
   allows, the experiment asserts the `pending` state or the recording-rule
   value instead and says so in its hypothesis; the rules are never relaxed
   to make an experiment pass.
3. **Recovery is part of the hypothesis.** After the fault ends the runner
   waits for `scripts/smoke.sh` to pass and for the expected alerts to
   resolve. A fault that is cleanly injected but leaves the stack degraded
   disproves the hypothesis.
4. **Scoped and self-cleaning.** The target is resolved with
   `docker compose ps -q <service>` and must carry this project's compose
   label; a trap unpauses, removes the netem qdisc and stops the load on
   every exit path, so a failed run never leaves a paused container behind.
5. **Weekly and on demand, never per PR.** `.github/workflows/chaos.yml`
   boots the composed stack like the E2E workflow and runs the experiments
   on a Sunday schedule and on `workflow_dispatch` (one or all). Results are
   an artifact and a step summary. A disproven hypothesis fails the run.
6. **Runbooks cite the experiment.** A runbook that an experiment has
   verified says so, with the measured time-to-alert, so the on-call reader
   knows which promises are tested and which are still assumptions.

## Alternatives considered

- **Keep the promtool tests only.** Cheap and already there, but they prove
  rule logic against synthetic series, not the scrape → rule → Alertmanager
  path on the real stack, and nothing about recovery.
- **A chaos platform (Chaos Mesh, Litmus, Gremlin).** Built for Kubernetes
  fleets with steady-state probes and schedules; the monitoring stack here
  exists only in compose (there is no Prometheus in `k8s/`), so a platform
  would add a cluster to test a stack that does not run on one.
- **Ad-hoc `docker pause` by hand before a release.** What happened until
  now, when someone remembered. Not repeatable, never measured, never
  compared with the runbook's promise.
- **Pumba alone as the runner.** It injects faults well (and is used for
  netem here), but has no notion of an expected alert or a recovery check;
  the hypothesis half is the part that was missing.

## Consequences

- Alert latency becomes a measured number per experiment
  (`docs/chaos/README.md`) and the runbooks can quote it.
- Adding an alert that can be exercised on compose means adding a YAML file,
  not code; `docs/chaos/README.md` describes the fields.
- A weekly job that takes 30–60 minutes of runner time and builds the
  images; acceptable for a schedule, which is why it is not on PRs.
- The experiments document gaps as well as strengths: a frozen
  Elasticsearch is visible only as latency on one endpoint and a DOWN
  aggregate health, and no rule fires for it today.

## Known gaps

- Only compose is covered; the Kubernetes manifests have no monitoring to
  test against.
- `cpu-stress` is implemented but no experiment uses it yet: the JVM alerts
  (`JvmGcPressure`, `JvmHeapHigh`) need 10 minutes and a memory-shaped
  fault that a busy loop does not produce.
- Time-to-notification (receiver webhook delivery) is not measured; the
  runner stops at "active in Alertmanager". The `alert-sink` log would allow
  it.
