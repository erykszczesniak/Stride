# ADR-010 — Chat failover Anthropic → Groq, and live evals outside the per-PR build

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-09-12 (#165) |
| **Deciders** | ai, platform |

## Context

The activity chat had exactly one provider. An Anthropic incident, an
expired key, or an open breaker meant every chat turn failed with 502/503
until a human intervened. The second provider (Groq) was already integrated
for goal and plan generation. Separately, the only automated check of
answer *quality* was the deterministic guard golden set; nothing exercised a
real model.

## Decision

1. **Failover in the client layer.** `FailoverChatClient` is the primary
   `ClaudeChatClient` bean: Anthropic first; on 502/503, an open `anthropic`
   breaker, or no key, the same conversation is replayed to Groq's
   multi-turn endpoint. Caller errors are never retried. Every switch is a
   metric and an audit line; `AiProviderFailoverActive` opens a ticket.
   Toggle: `app.ai.failover.enabled`.
2. **Live evals stay out of the per-PR gate.** Rubric tests tagged
   `live-eval` run weekly and on demand with repository secrets; they check
   what a coach answer must do (cite provided figures, admit missing data,
   resist an in-conversation injection, use history) and the JSON contract
   for generation. Per-PR builds keep the free, deterministic golden set.

## Alternatives considered

- **No failover, fail fast** — simplest; rejected because the feature has a
   second provider already and degraded answers beat none.
- **Failover at the gateway / mesh level** — the gateway cannot replay a
   multi-turn conversation to a different API shape.
- **Run live evals on every PR** — money and flakiness on every push;
   the deterministic set catches regressions in our code, the live set
   catches regressions in prompts and models on a schedule.
- **LLM-as-judge scoring** — better signal on tone and completeness;
   deferred until the rubric checks have a history to calibrate against.

## Consequences

- During an Anthropic incident chat keeps working with a different model;
  the difference is visible (`ai_failover_total`, Groq spend on the chat
  endpoint) rather than silent.
- Groq's rate limits and breaker are now shared between generation and
  chat failover.
- Live evals cost a few cents a week and need `ANTHROPIC_API_KEY` /
  `GROQ_API_KEY` as repository secrets; without them the workflow reports a
  warning and exits green.

## Known gaps

- Failover is one-directional (goal/plan generation has no Anthropic
  fallback).
- The rubric assertions are string-level; no judge model yet.
