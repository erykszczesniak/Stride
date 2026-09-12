# ADR-006 — Prometheus rules + Alertmanager, tested and contract-checked in CI

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-09-12 (`42fd577`) |
| **Deciders** | platform |

## Context

The stack scraped every metric it needed (HTTP, JVM, HikariCP, logback)
into Prometheus and showed them on one Grafana dashboard, and nothing
alerted on any of them. Two ways to add alerting: Grafana unified alerting
(rules in the Grafana database, edited in the UI) or Prometheus rule files
with Alertmanager.

## Decision

Alerts are Prometheus rule files under `monitoring/alerts/` — six files,
30 alerts (`platform`, `services`, `slo`, `ai`, `cost`,
`self-observability`) — routed by Alertmanager, and treated like code:

- every alert carries a label contract (`severity`, `team`, `tier`,
  `service`) and annotations (`summary`, `description`, `runbook_url`),
  enforced by `monitoring/scripts/check_alert_rules.py`;
- every alert has at least one firing case in
  `monitoring/alerts/tests/*.test.yml` (`promtool test rules`);
- `.github/workflows/monitoring.yml` runs `promtool check rules`,
  `promtool test rules`, `promtool check config`, `amtool check-config`,
  the contract script and `dashboard-linter` on every PR that touches
  `monitoring/`;
- a rule change is a reviewed diff, never a UI click.

Routing is by `severity` (critical → `pager`, warning → the team's
receiver, info → log) and `team`; inhibition suppresses consequences while
their cause fires (`ServiceDown` → every warning for the same `app`,
`SLOBurnRateFast` → `SLOBurnRateSlow`, `PrometheusTargetsMissing` →
`ServiceDown`). Locally every receiver is a webhook to `alert-sink`, an HTTP
echo container, so delivery is observable with `docker compose logs`.

## Alternatives considered

- **Grafana unified alerting** — friendlier editing, but rules live in a
  database, unit testing is not first-class, and the label discipline that
  makes routing predictable is optional. Grafana stays the *viewing*
  surface (`stride-jvm`, `stride-slo`, `stride-cost`), not the source.
- **Alerting inside the application** (Micrometer + a custom notifier) —
  couples alert delivery to the thing being monitored.

## Consequences

- Rules are reviewable and testable; a threshold change shows the case
  that motivated it.
- Every alert has an owner and a runbook by construction; the contract
  check fails the build otherwise (a missing runbook *file* is a warning).
- Latency histograms had to be enabled in `application.yml`
  (`percentiles-histogram` and SLO buckets at 100 ms–2.5 s for
  `http.server.requests`), which multiplies series per endpoint.
- One static target: the `app="strava-backend"` label is set in
  `monitoring/prometheus/prometheus.yml`, and every rule depends on it.

## Known gaps

- Alertmanager receivers are echo webhooks; wiring Teams / Slack /
  PagerDuty is configuration, not code, and is not done.
- The monitoring stack exists only in `docker-compose.yml`; `k8s/` has no
  Prometheus, so the rules do not run there.
- No alert on Elasticsearch or Postgres health directly — only through the
  backend's symptoms.
