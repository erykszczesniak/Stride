---
name: <AlertName or family>
description: <one sentence — what this alert means for users>
metadata:
  owner: <team label from the rule>
  version: "1.0"
alert_type: <availability | latency | saturation | pipeline | provider | slo | self-observability>
severity: <critical | warning>
actionability: <"Page — act now" | "Ticket — same working day" | "Investigate and escalate">
allowed-tools: []
---

# Alert: <AlertName>

## Summary

Two or three sentences: what fired, what the user experiences, what the
usual cause is.

## Required inputs

Capture before starting:

- `instance` (and any `uri` / `method` / `provider`) from the alert labels
- Environment (compose / k8s / prod)
- Alert start time (Alertmanager UI or `startsAt` in the webhook payload)
- Any deploy or config change in the last hour (`git log --since=1h`, `docker compose ps --format '{{.Name}} {{.RunningFor}}'`)

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/alerts/<file>.rules.yml` |
| Expression | `<expr>` |
| Threshold / for | `<threshold>` for `<duration>` |
| Dashboard | `<url>` |

## Success criteria

The runbook is done when all of these are true:

- The alert has resolved in Alertmanager (not silenced).
- The cause is identified or the incident is escalated with evidence.
- Users are no longer affected (state the metric that proves it).

## Investigation

### 1. <cheapest check first>

| Command | What to look for |
|---|---|
| `...` | ... |

Expected result: ...

### 2. ...

## Mitigation

- **Safe without review:** ...
- **Needs a change / second pair of eyes:** ...

## Escalation / close

- Escalate to `<team>` with: alert link, the panel screenshot, the log lines, what you already tried.
- Close: add a comment to the alert in Alertmanager, open a post-mortem if it paged.
