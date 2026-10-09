# Architecture decision records

One file per decision that had real alternatives. An ADR says what we
chose, what we gave up, and what still hurts — not how the code works
(`architecture.md` does that). Records are immutable once accepted; a change
of mind is a new ADR that supersedes the old one.

| # | Decision | Status |
|---|---|---|
| [001](001-choreographed-sagas.md) | Choreographed sagas over Kafka instead of an orchestrator | Accepted |
| [002](002-database-per-service.md) | One database per service, logical cross-service references | Accepted |
| [003](003-grpc-inside-rest-at-the-edge.md) | gRPC for cluster-internal calls, REST for the public contract | Accepted (supersedes REST `/internal`) |
| [004](004-jwt-at-the-gateway.md) | JWT validated only at the gateway; identity propagated as headers | Accepted |
| [005](005-what-we-do-not-run.md) | No service mesh, no full event sourcing, no EKS | Accepted |
| [006](006-alerts-as-code.md) | Prometheus rules + Alertmanager, tested and contract-checked in CI | Accepted |
| [007](007-slo-burn-rate-alerting.md) | SLOs with multi-window burn-rate alerting | Accepted |
| [008](008-quality-gates-in-the-build.md) | Coverage floors, ArchUnit rules and a real-database test in `verify` | Accepted |
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
