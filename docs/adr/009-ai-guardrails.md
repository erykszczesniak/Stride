# ADR-009 — LLM guardrails and cost telemetry in the service layer

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-09-12 (#162) |
| **Deciders** | ai, platform |

## Context

ai-service forwards athlete-typed text to two external providers and stores
the conversation. Before this decision: no per-user limit on a billed
resource beyond the gateway's global rate limiter, no defence against
prompt injection, personal data (emails, phone numbers pasted into chat)
sent to the provider and persisted verbatim, and no idea what a day of chat
costs.

## Decision

Guardrails are a single facade (`Guardrails.admit`) called by the service
layer before any provider call, in this order:

1. **Per-user sliding-window rate limit**, in memory per replica.
2. **Prompt guard** for instruction overrides, prompt exfiltration, role
   hijacks, chat-template tokens, control characters and noise; narrow by
   design, with a golden set that fails the build on regressions.
3. **PII masking** (emails, phones, Luhn-valid cards, mod-97 IBANs) —
   the masked text is what the provider receives *and* what is persisted.

Every provider call is recorded once by `AiCallRecorder`: bounded-label
metrics (requests, latency histogram, tokens, cost, failover, guard
decisions) and one JSON audit line per call with user, activity, tokens
and cost — never the prompt or the reply. Cost comes from a configurable
price table; unpriced models are flagged, not hidden.

## Alternatives considered

- **Guardrails at the gateway** — the gateway cannot know which body field
  is free text for which route; masking there would also mask activity
  descriptions the product needs verbatim.
- **Resilience4j `RateLimiter` per user** — one registry entry and one
  Micrometer series per user; unbounded cardinality.
- **Distributed rate limit in Redis** — exact across replicas; deferred,
  the per-replica N× bound is acceptable for an abuse brake.
- **LLM-based injection classifier** — better recall, adds latency and a
  second billed call per message; the regex guard plus golden set is the
  first line, a classifier can be added behind the same facade.
- **Store the original text and mask only outbound** — leaves PII at rest
  for no product benefit.

## Consequences

- Provider spend and error ratios are visible per model; `AiChatErrorRate`
  and the AI SLO have a provider-side counterpart.
- Blocked requests are user-visible 400s with a reason; the weekly
  `ai_guard_decisions_total` review is how false positives are found.
- Masked text changes what the coach sees ("[email]"), which is the point.
- The audit logger is a compliance artefact: it must go to the log
  platform, not to stdout only.

## Known gaps

- Guard coverage is regex-based; adversarial paraphrases will get through
  until a classifier is added behind the facade.
- Prices in `application.yml` are placeholders to verify against the
  current price lists.
- No provider failover yet for chat (Anthropic only) — next decision.
