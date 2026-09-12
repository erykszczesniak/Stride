# Post-mortems

Blameless, written within two working days of anything that paged or lost
data, and for the near-misses that taught us something. One file per event:
`YYYY-MM-DD-<slug>.md`. Every post-mortem ends with actions that change a
rule, a runbook, a test, or the system — "be more careful" is not an action.

| Date | Title | Impact | Key action |
|---|---|---|---|
| 2026-07-11 | [Two PRs shipped Flyway V16; the backend could not boot against Postgres](2026-07-11-duplicate-flyway-version.md) | every fresh Postgres boot failed for 24 days while CI stayed green; no deployed users affected | Flyway append-only invariant in `AGENTS.md`; runbook branch for `Found more than one migration`; open: run Flyway in the Testcontainers profile and a CI check for duplicate versions |

## Template

```markdown
# <Title: what the user saw>

**Date:** · **Duration:** · **Severity:** · **Author:** · **Status:** draft / reviewed

## Summary
## Impact
## Timeline (UTC)
## Root cause
## What went well / what went badly
## Where we got lucky
## Actions (owner, due, link)
## Lessons for the runbooks / rules
```
