# ADR-007 — SLOs with multi-window burn-rate alerting

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-09-12 (#160) |
| **Deciders** | platform |

## Context

Symptom alerts (5xx ratio above 5 %, p95 above 1 s) say something is
broken now. They say nothing about whether the platform is keeping its
promise to users over a month, and they page for a five-minute blip as
readily as for a sustained degradation.

## Decision

Four objectives, written in [`docs/slo.md`](../slo.md): edge availability
99.5 %, edge latency 95 % under 500 ms, per-service availability 99.5 %,
AI availability 98 %. SLIs are recording rules over four windows; alerts
fire on **burn rate** with two windows each:

| Alert | Windows | Burn rate | Action |
|---|---|---|---|
| fast | 1h and 5m | > 14.4× | page |
| slow | 6h and 30m | > 6× | ticket |
| budget exhausted | 30d | remaining < 0 for 1h | error-budget policy |

The long window gives confidence, the short window makes the alert resolve
as soon as the cause is fixed. Alertmanager inhibits the slow alert while
the fast one fires for the same `app` and `slo`.

## Alternatives considered

- **Static thresholds only** — kept (they name the cause), but they do not
  express a monthly promise.
- **Single-window burn rate** — either pages on blips (short window) or
  keeps paging after recovery (long window).
- **A dedicated SLO tool (Sloth, Pyrra)** — generates the same rules from
  YAML; not worth a component for four SLOs, revisit above ~20.

## Consequences

- One page for an outage instead of one per symptom, and no page for
  errors too small to matter over a month.
- The error-budget policy in `docs/slo.md` gives a rule for prioritising
  reliability work over features.
- Burn rates on tiny traffic are noisy; the runbook says to check the
  request-rate panel first. A minimum-traffic guard is a candidate change.
- 30-day windows exceed the 15-day local retention; the budget panel is
  approximate locally.

## Known gaps

- No SLO for Kafka-driven flows (feed freshness, notification delay).
- SLO targets have not been through a full month of data yet; they will be
  reviewed after the first budget report.
