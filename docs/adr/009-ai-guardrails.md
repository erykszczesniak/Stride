# ADR-009 — LLM guardrails and cost telemetry in the service layer

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-09-12 |
| **Deciders** | ai, platform |

## Context

The backend forwards athlete-typed text to two external providers
(Anthropic for the activity chat, Groq for goal and plan generation) and
stores the conversation in `ai_messages`. Before this decision: no per-user
limit on a billed resource (the only limiter, `RateLimitingFilter`, is
per-IP and covers login and register), no defence against prompt
injection, personal data pasted into chat sent to the provider and
persisted verbatim, and no idea what a day of chat costs.

## Decision

Guardrails are a single facade, `Guardrails.admit(userId, endpoint, text)`
in `com.strava.backend.ai.guard`, called by the service layer
(`AiChatServiceImpl`, the Groq generation path) before any provider call,
in this order:

1. **Per-user sliding-window rate limit**, in memory per replica
   (`app.ai.rate-limit.chat-per-minute` 10, `generation-per-minute` 5) → 429.
2. **Prompt guard** (`PromptGuard`) for instruction overrides, prompt
   exfiltration, role hijacks, chat-template tokens, control characters
   and noise → 400; narrow by design, with a golden set
   (`src/test/resources/evals/guard-golden.json`, 42 cases) that fails the
   build below 95 % agreement (`GuardGoldenEvalTest`).
3. **PII masking** (`PiiMasker`: emails, phones, Luhn-valid cards, mod-97
   IBANs) — the masked text is what the provider receives *and* what is
   persisted.

Every provider call is recorded once by
`com.strava.backend.ai.telemetry.AiCallRecorder`: bounded-label metrics
(requests, latency histogram, tokens, cost, failover, guard decisions,
masked PII, rate limits) and one JSON line on the `ai.audit` logger with
user, activity, tokens and cost — never the prompt or the reply. Cost comes
from `app.ai.pricing.<model>.{input,output}-per-mtok`; unpriced models are
flagged, not hidden. Switches: `app.ai.guard.enabled`, `app.ai.pii.enabled`.

## Alternatives considered

- **Guardrails in a servlet filter** (next to `RateLimitingFilter`) — a
  filter cannot know which body field is free text for which route, and
  masking there would also mask activity descriptions the product needs
  verbatim.
- **A Micrometer / Resilience4j rate limiter per user** — one registry
  entry and one series per user; unbounded cardinality
  (`PrometheusHighCardinality`).
- **Distributed rate limit in Redis** — exact across replicas; deferred
  (ADR-005), the per-replica N× bound is acceptable for an abuse brake.
- **LLM-based injection classifier** — better recall, adds latency and a
  second billed call per message; the regex guard plus golden set is the
  first line, a classifier can be added behind the same facade.
- **Store the original text and mask only outbound** — leaves PII at rest
  for no product benefit.

## Consequences

- Provider spend and error ratios are visible per model; `AiProviderErrors`,
  `AiSpendHigh` and the AI SLO have a provider-side counterpart.
- Blocked requests are user-visible 400s with a reason; the weekly
  `ai_guard_decisions_total` review is how false positives are found.
- Masked text changes what the coach sees ("[email]"), which is the point.
- The audit logger is a compliance artefact: under the `prod` profile it
  is JSON on stdout like everything else and must reach the log platform.

## Known gaps

- Guard coverage is regex-based; adversarial paraphrases will get through
  until a classifier is added behind the facade.
- Prices in `application.yml` are placeholders to verify against the
  current price lists.
- Every meter carries `team` / `cost_center` from `application.yml`, so AI
  spend is attributed to the one deployable's owner, not to a feature team.
