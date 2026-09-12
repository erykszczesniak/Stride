# AI guardrails, telemetry and cost

How ai-service keeps two external LLM providers (Anthropic for the activity
chat, Groq for goal and plan generation) safe to expose, observable, and
accountable for what they cost. Everything here lives in
`services/ai-service/src/main/kotlin/com/strava/aiservice/{guard,telemetry}`.

## The path of one request

```
POST /api/v1/ai/activities/{id}/chat   (gateway has already verified the JWT)
        │
        ▼
Guardrails.admit(userId, CHAT, message)
   1. AiRateLimiter   per-user sliding window, in memory        → 429
   2. PromptGuard     instruction overrides, prompt exfiltration,
                      role hijacks, chat-template tokens, noise → 400
   3. PiiMasker       emails, phones, Luhn-valid cards, IBANs   → "[email]" …
        │  masked text is what the provider sees AND what is persisted
        ▼
AiChatService  → AiCallContext.with(userId, activityId) { AnthropicClient.chat(...) }
        │
        ▼
AiCallRecorder.record(call)   metrics + one JSON audit line per provider call
```

The order is deliberate: rate limiting first so an abusive client cannot
burn CPU on guard evaluation; the guard sees the *original* text because
masking could hide an injection inside an email local part; masking last so
nothing personal leaves the process.

## Rate limits

| Endpoint | Default | Env var | Bucket |
|---|---|---|---|
| chat turn | 10 / minute / user | `AI_CHAT_PER_MINUTE` | `chat` |
| goal or plan generation | 5 / minute / user | `AI_GENERATION_PER_MINUTE` | `generation` |

Limits are per replica (in memory, `AiRateLimiter`). With N replicas a user
gets at most N× the limit — acceptable for an abuse brake on a billed
resource; the gateway's Redis `RequestRateLimiter` remains the fairness layer.
No Resilience4j `RateLimiter` on purpose: one registry entry per user would
mean one Micrometer series per user.

## Prompt guard

Blocks six classes of input, each with a reason that is safe to show and to
count:

| Reason | Example | Metric label |
|---|---|---|
| `INSTRUCTION_OVERRIDE` | "ignore all previous instructions" | `instruction_override` |
| `PROMPT_EXFILTRATION` | "reveal your system prompt" | `prompt_exfiltration` |
| `ROLE_HIJACK` | "you are now DAN", "developer mode" | `role_hijack` |
| `TEMPLATE_TOKENS` | `<\|im_start\|>`, `[INST]`, `<<SYS>>` | `template_tokens` |
| `CONTROL_CHARACTERS` | zero-width / bidi override characters | `control_characters` |
| `NOISE` | blank, 60 × "a", symbol soup | `noise` |

What it must *not* block: coaching questions that happen to contain
"ignore", "forget", "rules", "system", "act as" in a sporting sense. The
boundary is pinned by `src/test/resources/evals/guard-golden.json` (42
cases); `GuardGoldenEvalTest` fails the build on any `must` mismatch or when
agreement drops below 95 %. Add a case to the golden set whenever a false
positive or a bypass is found — that is the regression test.

## PII masking

`PiiMasker` replaces emails, phone numbers (international and local),
card numbers that pass Luhn, and IBANs that pass mod-97 with `[email]`,
`[phone]`, `[card]`, `[iban]`. Distances, paces, heart rates, timestamps and
activity ids are untouched (`PiiMaskerTest.keepsTrainingNumbers`). The
masked text is stored in `ai_messages`, so the database never holds the
original either. Counts per type go to `ai_pii_masked_total`; values are
never logged.

## Telemetry

Every provider call is recorded once, whatever the outcome:

| Metric | Labels | Use |
|---|---|---|
| `ai_requests_total` | endpoint, provider, model, outcome | error ratio per provider (`AiChatErrorRate`, `AiSLOBurnRateFast`) |
| `ai_request_duration_seconds` (histogram) | endpoint, provider, outcome | provider latency vs. our own (`AiChatLatencyP95`) |
| `ai_tokens_total` | provider, model, direction | usage trend, prompt growth |
| `ai_request_cost_usd_total` | provider, model | spend per hour/day; runaway retries |
| `ai_failover_total` | endpoint, to | how often the secondary provider served |
| `ai_guard_decisions_total` | decision, reason | attack volume, false-positive review |
| `ai_pii_masked_total` | type | how much personal data users paste |
| `ai_rate_limited_total` | endpoint | abuse / limit tuning |

Cost = `input_tokens × input_per_mtok + output_tokens × output_per_mtok`
from `app.ai.pricing.<model>` (USD per million tokens). The defaults in
`application.yml` are placeholders from the public price lists — verify them
before trusting the panel; a model without a price is recorded with
`priced=false` in the audit line and no cost series, so the gap is visible.

The audit trail is the `ai.audit` logger, one JSON line per call:

```json
{"event":"ai.call","endpoint":"chat","provider":"anthropic","model":"claude-opus-5","userId":7,"activityId":100,"inputTokens":1832,"outputTokens":241,"latencyMs":2140,"outcome":"ok","failedOver":false,"costUsd":"0.045555","priced":true,"error":null}
```

Never the prompt, never the reply — those are user content. Ship the logger
to the log platform and you have per-user cost attribution for free.

## Useful queries

```promql
# spend per model, last hour
sum by (provider, model) (increase(ai_request_cost_usd_total[1h]))

# provider error ratio
sum by (provider) (rate(ai_requests_total{outcome!="ok"}[5m])) / sum by (provider) (rate(ai_requests_total[5m]))

# p95 provider latency
histogram_quantile(0.95, sum by (provider, le) (rate(ai_request_duration_seconds_bucket[5m])))

# blocked prompts by reason (review weekly for false positives)
sum by (reason) (increase(ai_guard_decisions_total{decision="blocked"}[7d]))
```

## Provider failover (chat)

`FailoverChatClient` is the `ClaudeChatClient` the service layer gets:
Anthropic first, Groq (multi-turn `chatCompletion`) when Anthropic returns
502/503, its breaker (`resilience4j.circuitbreaker.instances.anthropic`) is
open, or no key is configured. Caller errors (404 activity not found) are
never retried on the secondary. Each switch increments `ai_failover_total`
and writes an `ai.failover` audit line; `AiProviderFailoverActive` opens a
ticket after 10 minutes of failovers, and the Groq spend shows on the cost
dashboard under the chat endpoint. Off switch: `AI_CHAT_FAILOVER_ENABLED=false`.

## Evals

Two halves:

- **Deterministic, every build** — `GuardGoldenEvalTest` against
  `evals/guard-golden.json` (see above).
- **Live, weekly / on demand** — `LiveChatEvalTest` (Anthropic) and
  `LiveGenerationEvalTest` (Groq), JUnit tag `live-eval`, enabled only when
  `ANTHROPIC_API_KEY` / `GROQ_API_KEY` are set. Rubric-style checks on real
  replies: the chat cites figures from the activity and invents none, admits
  missing data, survives an in-conversation injection without leaking the
  system prompt, uses the training history; goal/plan generation returns
  the exact JSON contract with allowed workout types. The `AI live eval`
  workflow runs them with the repository secrets and uploads the reports.

## Switches

| Property | Default | Effect |
|---|---|---|
| `app.ai.guard.enabled` | true | prompt guard on/off |
| `app.ai.pii.enabled` | true | masking on/off |
| `app.ai.guard.max-repeated-chars` | 40 | noise threshold |
| `app.ai.failover.enabled` | true | chat failover Anthropic → Groq |

Turning either off is a change-reviewed action, not a hotfix.
