# ADR-013 — OpenSLO documents as the source of the SLO rules

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-10-11 |
| **Deciders** | platform |

## Context

ADR-007 put the three SLOs into `docs/slo.md` (prose and a table) and into
`monitoring/alerts/slo.rules.yml` (eleven recording rules and six alerts,
written by hand). The same target appears in the doc, in the budget divisor
of every alert expression (`/ 0.005`), in the description text and in the
test file. A target review means editing four places and hoping they still
agree; the burn-rate recipe is repeated per SLO, so adding a fourth SLO is
copying sixty lines and changing the metric name in twelve of them. Nothing
checks that the rules still match the table.

The OpenSLO specification (`openslo/v1`) describes exactly what the table
says — service, indicator, objective, time window, alert policies — in a
vendor-neutral YAML that other tools can read.

## Decision

One OpenSLO v1 document per SLO in `slo/`. The Prometheus rules are
**generated** from them by `monitoring/scripts/generate_slo_rules.py`
(python3 and PyYAML, no other dependency) and committed; CI regenerates
and fails on drift, so the committed rules can never disagree with the
source. The generator reproduces the existing rule names, labels,
annotations and expressions exactly — the unit tests in
`monitoring/alerts/tests/slo.test.yml`, the contract check and the Grafana
dashboards did not change.

Everything OpenSLO can express is expressed in its fields (indicator
queries, target, window, burn-rate thresholds and lookback windows,
severity). What it cannot — the recording-rule names, the alert names and
texts, the budget-exhausted alert — lives in `metadata.annotations` under
the `stride.io/` prefix, so the documents remain valid OpenSLO for any
other consumer. The short burn-rate window is a convention of the generator
(a twelfth of the long one), not a field.

## Alternatives considered

- **Keep hand-written rules** — simplest, and fine for three SLOs. Rejected
  because the drift problem is real today (the doc, the divisor, the
  description and the test each carry the target) and grows with every SLO.
- **Sloth (OpenSLO plugin or its own spec)** — mature generator for exactly
  this recipe. Rejected for now because it names everything its own way
  (`slo:sli_error:ratio_rate5m{sloth_service, sloth_slo}`), which would
  break the existing dashboards, tests and the `slo:error_budget_remaining:ratio`
  panel; it also adds a binary to the toolchain. Revisit if the custom
  generator grows past a few hundred lines, or when a second service needs
  SLOs.
- **A SaaS SLO product (Nobl9, Datadog SLOs, Grafana SLO)** — gives
  reporting and policy workflows out of the box. Rejected: a vendor
  dependency for three objectives, and alert evaluation would move out of
  the Prometheus → Alertmanager path that ADR-006 made testable.
- **Generate from `docs/slo.md`** — the doc is prose; parsing a Markdown
  table is fragile and the doc would have to carry PromQL. OpenSLO is the
  structured form of that table.

## Consequences

- One place to change a target: the OpenSLO file. `make slo`, update the
  expected text in the test, done.
- The rules file is read-only in practice (header says so; CI enforces it).
  Reviewers read the small OpenSLO diff, not the regenerated rules.
- The SLO definitions can be consumed by anything that speaks OpenSLO
  (reporting, a future SLO tool) without a second description.
- The generator is one more script to maintain; it validates its input
  (required fields, windows divisible by 12, targets in (0, 1), one
  objective per SLO) and refuses anything it does not understand.

## Known gaps

- Only `Occurrences` budgeting and rolling windows are implemented; a
  calendar-aligned window or a `Timeslices` SLO is refused by the validator.
- Only ratio indicators over Prometheus counters; a threshold indicator
  (e.g. p95 latency directly) is not supported.
- The budget-remaining recording rule is implemented for `bad`-based
  ratios only, which is all the current SLOs need.
- The test file is still hand-written; a change of alert text is made in
  the OpenSLO annotation and in the test expectation.
