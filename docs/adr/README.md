# Architecture decision records

One file per decision that had real alternatives. An ADR says what we
chose, what we gave up, and what still hurts — not how the code works
(`README.md` and `AGENTS.md` do that). Records are immutable once accepted;
a change of mind is a new ADR that supersedes the old one.

| # | Decision | Status |
|---|---|---|
| [001](001-one-deployable.md) | One deployable: a layered Spring Boot monolith; the `microservices` branch is the same product split | Accepted |
| [002](002-one-postgres-schema-with-flyway.md) | One PostgreSQL schema, Flyway migrations; H2 for unit tests, Testcontainers for integration | Accepted |
| [003](003-elasticsearch-user-search-via-events.md) | Elasticsearch for user search, synced by Spring application events, SQL fallback | Accepted |
| [004](004-jwt-in-the-app-security-filter.md) | JWT issued and validated in the app's own security filter | Accepted |
| [005](005-what-we-do-not-run.md) | No gateway, no discovery, no config server, no Kafka, no breakers, no Redis | Accepted |
| [006](006-alerts-as-code.md) | Prometheus rules + Alertmanager, tested and contract-checked in CI | Accepted |
| [007](007-slo-burn-rate-alerting.md) | SLOs with multi-window burn-rate alerting | Accepted |
| [008](008-quality-gates-in-the-build.md) | Coverage floor, ArchUnit rules and a real-database test in `verify` | Accepted |
| [009](009-ai-guardrails.md) | LLM guardrails and cost telemetry in the service layer | Accepted |
| [010](010-chat-provider-failover.md) | Chat failover Anthropic → Groq, and live evals outside the per-PR build | Accepted |
| [011](011-sonarcloud-quality-gate.md) | SonarCloud quality gate on every pull request, scanned from CI, skipped without a token | Accepted |

## Format

```markdown
# ADR-NNN — Title

| | |
|---|---|
| **Status** | Proposed / Accepted / Superseded by ADR-NNN |
| **Date** | YYYY-MM-DD |
| **Deciders** | who |

## Context
## Decision
## Alternatives considered
## Consequences
## Known gaps
```

Status changes and supersessions are edits to the table above plus the
status row of the record; the body stays as written.
