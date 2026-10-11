# SLO definitions (OpenSLO v1)

One file per objective from [`docs/slo.md`](../docs/slo.md). These files are
the **source**; `monitoring/alerts/slo.rules.yml` is generated from them by
`monitoring/scripts/generate_slo_rules.py` and must not be edited by hand
(CI fails on drift). Numbered file names fix the order of the generated
groups.

```bash
make slo          # regenerate monitoring/alerts/slo.rules.yml
make slo-check    # validate slo/*.yaml and fail if the generated file is stale
```

## What the generator reads

Standard OpenSLO fields:

| Field | Used for |
|---|---|
| `metadata.name` | the `slo` label on the burn-rate alerts |
| `metadata.displayName`, `spec.description` | comments in the generated file |
| `metadata.labels.team / tier`, `spec.service` | the routing labels on every alert (`team`, `tier`, `service`) |
| `spec.indicator.spec.ratioMetric.{good|bad}.metricSource.spec.query` | the PromQL **selector** for good or bad events; the generator wraps it in `sum by (app) (rate(...[window]))` |
| `spec.indicator.spec.ratioMetric.total.metricSource.spec.query` | the selector for all events |
| `spec.objectives[0].target` | the target; `1 - target` is the error budget the burn rates divide by |
| `spec.timeWindow[0].duration` | the window of the error-budget recording rule (30d) |
| `spec.alertPolicies[].spec.conditions[0].spec` | `kind: burnrate`, `threshold`, `lookbackWindow` (the **long** window; the short one is a twelfth of it, 1h → 5m, 6h → 30m), `alertAfter` (`for`), `severity` |

Generator-specific keys live in `metadata.annotations` (OpenSLO allows any
string key there):

| Annotation | On | Meaning |
|---|---|---|
| `stride.io/recording-rule` | SLO | base name of the ratio recording rule, window appended (`slo:http_error:ratio_rate` → `slo:http_error:ratio_rate5m`) |
| `stride.io/windows` | SLO | comma-separated windows to record (`5m,30m,1h,6h`) |
| `stride.io/error-budget-rule` | SLO | optional; name of the 30-day budget-remaining recording rule |
| `stride.io/error-budget-slo-label` | SLO | the `slo` label that budget rule carries |
| `stride.io/alertname` | AlertPolicy | the Prometheus alert name |
| `stride.io/rule-kind` | AlertPolicy | `burn-rate` (default, two windows) or `budget-exhausted` (remaining budget below zero) |
| `stride.io/summary`, `stride.io/description`, `stride.io/runbook-url`, `stride.io/dashboard-url` | AlertPolicy | the alert annotations, verbatim |
| `stride.io/label.<name>` | AlertPolicy | overrides one routing label for that alert (the budget alert is tier 1 and templated on `app`) |

Anything else in the files (budgeting method, indicator metadata,
`notificationTargets`) is kept for OpenSLO tooling and ignored here.
