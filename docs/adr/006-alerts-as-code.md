# ADR-006 — Prometheus rules + Alertmanager, tested and contract-checked in CI

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-09-12 (#159) |
| **Deciders** | platform |

## Context

The stack scraped every metric it needed (HTTP, JVM, HikariCP,
Resilience4j, Kafka client) and nothing alerted on any of them. Two ways to
add alerting: Grafana unified alerting (rules in the Grafana database,
edited in the UI) or Prometheus rule files with Alertmanager.

## Decision

Alerts are Prometheus rule files under `monitoring/alerts/`, routed by
Alertmanager, and treated like code:

- every alert carries a label contract (`severity`, `team`, `tier`,
  `service`) and annotations (`summary`, `description`, `runbook_url`),
  enforced by `monitoring/scripts/check_alert_rules.py`;
- every alert has at least one firing case in `promtool test rules`;
- `promtool check rules`, `amtool check-config` and `dashboard-linter` run
  on every PR that touches `monitoring/`;
- a rule change is a reviewed diff, never a UI click.

Routing is by `severity` (page vs ticket vs log) and `team`; inhibition
rules suppress consequences while their cause fires. Locally every
notification lands in an HTTP echo container so delivery is observable.

## Alternatives considered

- **Grafana unified alerting** — friendlier editing, but rules live in a
  database, unit testing is not first-class, and the label discipline that
  makes routing predictable is optional. Used as the *viewing* surface
  (Grafana reads Alertmanager), not as the source.
- **Alerting inside the application (e.g. Micrometer + custom notifier)** —
  couples alert delivery to the thing being monitored.

## Consequences

- Rules are reviewable and testable; a threshold change shows the case
  that motivated it.
- Every alert has an owner and a runbook by construction; the contract
  check fails the build otherwise.
- Histogram buckets had to be enabled for every service (Config Server),
  which raises series count per endpoint.
- The Eureka `app` label needed normalising at scrape time; every rule
  depends on that relabel.

## Known gaps

- Alertmanager receivers are echo webhooks; wiring Teams/Slack/PagerDuty is
  configuration, not code, and is not done.
- No alert on Kafka broker health itself (only client-side lag/errors).
