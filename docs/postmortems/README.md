# Post-mortems

Blameless, written within two working days of anything that paged or lost
data, and for the near-misses that taught us something. One file per event:
`YYYY-MM-DD-<slug>.md`. Every post-mortem ends with actions that change a
rule, a runbook, a test, or the system — "be more careful" is not an action.

| Date | Title | Impact | Key action |
|---|---|---|---|
| 2026-08-16 | [Gateway ignored the public-path list: every login 401 on the composed stack](2026-08-16-gateway-public-paths.md) | auth unusable on the full stack; found before any user saw it | `@ConfigurationProperties` binder + test; E2E on the composed stack is part of the definition of done |
| 2026-08-17 | [Unbounded Elasticsearch pinned the dev host; user-service retried it into the ground](2026-08-17-dev-stack-resource-exhaustion.md) | dev stack unusable for hours, no prod impact | memory limits on every container, opt-in profiles, retry backoff review |

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
