# ADR-010 — Chat failover Anthropic → Groq, and live evals outside the per-PR build

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-09-12 |
| **Deciders** | ai, platform |

## Context

The activity chat had exactly one provider: `ClaudeServiceImpl` answers
503 without a key and 502 on any provider error or empty reply, and the
transaction in `AiChatServiceImpl.sendMessage` rolls back so nothing is
persisted. An Anthropic incident or an expired key meant every chat turn
failed until a human intervened. The second provider (Groq) was already
integrated for goal and plan generation. Separately, the only automated
check of answer *quality* was the deterministic guard golden set; nothing
exercised a real model.

## Decision

1. **Failover in the service layer, no circuit breaker.**
   `FailoverClaudeService` is the `@Primary ClaudeService` bean: Anthropic
   first; on 502, 503 or no key, the same conversation is replayed to Groq's
   multi-turn endpoint. Caller errors (404 activity not found) are never
   retried. There is no Resilience4j on this branch (ADR-005), so there is
   no open-breaker trigger and no half-open recovery — every call tries the
   primary. Every switch is a metric (`ai_failover_total{endpoint,to}`) and
   an audit line; `AiProviderFailoverActive` opens a ticket after 10
   minutes of failovers and `AiProviderErrors` names the failing provider.
   Toggle: `app.ai.failover.enabled`.
2. **Live evals stay out of the per-PR gate.** Rubric tests tagged
   `live-eval` run only when `ANTHROPIC_API_KEY` / `GROQ_API_KEY` are set;
   they check what a coach answer must do (cite provided figures, admit
   missing data, resist an in-conversation injection, use history) and the
   JSON contract for generation. `./mvnw verify` keeps the free,
   deterministic golden set (ADR-009).

## Alternatives considered

- **No failover, fail fast** — simplest; rejected because the feature has a
  second provider already and degraded answers beat none.
- **A breaker around the primary** — would stop hammering a dead provider;
  adds Resilience4j for one call site, and the per-call ratio alert covers
  detection. Revisit if Anthropic outages are long enough that the extra
  latency of a failed primary call per turn matters.
- **Run live evals on every PR** — money and flakiness on every push; the
  deterministic set catches regressions in our code, the live set catches
  regressions in prompts and models.
- **LLM-as-judge scoring** — deferred until the rubric checks have a
  history to calibrate against.

## Consequences

- During an Anthropic incident chat keeps working with a different model;
  the difference is visible (`ai_failover_total`, Groq spend on the `chat`
  endpoint on `stride-cost`) rather than silent.
- A failed-over turn takes the primary's failure time plus the secondary's
  round trip, all inside `sendMessage`'s transaction — a DB connection is
  held for both ([db-pool-exhausted.md](../runbooks/db-pool-exhausted.md)).
- Groq's provider quota is now shared between generation and chat
  failover.
- Live evals cost a few cents a run and need real keys; without them they
  are skipped, not failed.

## Known gaps

- Failover is one-directional (goal/plan generation has no Anthropic
  fallback).
- Without a breaker, a hard-down primary adds one failed call to every
  chat turn until it recovers.
- The rubric assertions are string-level; no judge model yet.
