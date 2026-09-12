# AI guardrails, telemetry and cost

How the backend keeps two external LLM providers (Anthropic for the activity
chat, Groq for goal and plan generation and as the chat fallback) safe to
expose, observable, and accountable for what they cost. The guard and
telemetry code lives in `com.strava.backend.ai.{guard,telemetry}`; the
provider clients are `ClaudeServiceImpl` (anthropic-java SDK) and
`GroqServiceImpl` (`RestTemplate`) in `service/impl`, with
`FailoverClaudeService` in front of the chat. The `ai_*` metrics feed the
alerts `AiProviderErrors`, `AiProviderFailoverActive` and `AiSpendHigh` and
the `stride-cost` dashboard.

## The path of one request

```
POST /api/v1/ai/activities/{activityId}/chat   (JwtAuthenticationFilter has set the user)
        │
        ▼
Guardrails.admit(userId, CHAT, message)
   1. rate limit     per-user sliding window, in memory            → 429
   2. PromptGuard    instruction overrides, prompt exfiltration,
                     role hijacks, chat-template tokens, noise      → 400
   3. PiiMasker      emails, phones, Luhn-valid cards, IBANs        → "[email]" …
        │  masked text is what the provider sees AND what is persisted (ai_messages)
        ▼
AiChatServiceImpl.sendMessage  → FailoverClaudeService.chat(systemPrompt, conversation)
                                    Anthropic first, Groq multi-turn on 502 / 503 / no key
        │
        ▼
AiCallRecorder.record(call)   metrics + one JSON audit line per provider call
```

The order is deliberate: rate limiting first so an abusive client cannot
burn CPU on guard evaluation; the guard sees the *original* text because
masking could hide an injection inside an email local part; masking last so
nothing personal leaves the process. Goal and plan generation
(`/api/v1/ai/generate-goal`, `/generate-plan`) go through the same facade
with the `generation` bucket before `GroqServiceImpl`.

## Rate limits

| Endpoint | Default | Property | Bucket |
|---|---|---|---|
| chat turn | 10 / minute / user | `app.ai.rate-limit.chat-per-minute` | `chat` |
| goal or plan generation | 5 / minute / user | `app.ai.rate-limit.generation-per-minute` | `generation` |

Limits are per replica (in memory). With N replicas a user gets at most N×
the limit — acceptable for an abuse brake on a billed resource. This is the
only per-user limiter in the app; `RateLimitingFilter` is per-IP and covers
only login and register. No Micrometer-backed limiter on purpose: one
registry entry per user would mean one series per user.

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

`PiiMasker` replaces emails, phone numbers (international and local), card
numbers that pass Luhn, and IBANs that pass mod-97 with `[email]`,
`[phone]`, `[card]`, `[iban]`. Distances, paces, heart rates, timestamps and
activity ids are untouched. The masked text is what `AiChatServiceImpl`
stores in `ai_messages` (migration `V17__add_ai_chat.sql`), so the database
never holds the original either. Counts per type go to
`ai_pii_masked_total`; values are never logged.

## Telemetry

Every provider call is recorded once, whatever the outcome:

| Metric | Labels | Use |
|---|---|---|
| `ai_requests_total` | endpoint, provider, model, outcome | error ratio per provider (`AiProviderErrors`, `AiSLOBurnRateFast`) |
| `ai_request_duration_seconds` (histogram) | endpoint, provider, outcome | provider latency vs. our own (`AiChatLatencyP95`) |
| `ai_tokens_total` | provider, model, direction | usage trend, prompt growth (the chat prompt carries up to 20 recent activities) |
| `ai_request_cost_usd_total` | provider, model | spend per hour/day; `AiSpendHigh`; `cost:ai_usd:rate1h` |
| `ai_failover_total` | endpoint, to | how often the secondary provider served (`AiProviderFailoverActive`) |
| `ai_guard_decisions_total` | decision, reason | attack volume, false-positive review |
| `ai_pii_masked_total` | type | how much personal data users paste |
| `ai_rate_limited_total` | endpoint | abuse / limit tuning |

Like every meter in the app, these carry the `team` and `cost_center` tags
from `application.yml` ([`cost-attribution.md`](cost-attribution.md)).

Cost = `input_tokens × input_per_mtok + output_tokens × output_per_mtok`
from `app.ai.pricing.<model>` (USD per million tokens). The defaults in
`application.yml` are placeholders from the public price lists — verify
them before trusting the panel; a model without a price is recorded with
`priced=false` in the audit line and no cost series, so the gap is visible.

The audit trail is the `ai.audit` logger, one JSON line per call:

```json
{"event":"ai.call","endpoint":"chat","provider":"anthropic","model":"claude-opus-4-8","userId":7,"activityId":100,"inputTokens":1832,"outputTokens":241,"latencyMs":2140,"outcome":"ok","failedOver":false,"costUsd":"0.045555","priced":true,"error":null}
```

Never the prompt, never the reply — those are user content. Under the
`prod` profile logs are already JSON (`logback-spring.xml`); ship them to
the log platform and you have per-user cost attribution for free.

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

`FailoverClaudeService` is the `@Primary ClaudeService` the service layer
gets: Anthropic first (`ClaudeServiceImpl`); Groq's multi-turn chat when
Anthropic returns 502 / 503 or no key is configured. There is no circuit
breaker on this branch, so every turn tries the primary. Caller errors (404
activity not found) are never retried on the secondary. Each switch
increments `ai_failover_total` and writes an `ai.failover` audit line;
`AiProviderFailoverActive` opens a ticket after 10 minutes of failovers, and
the Groq spend shows on `stride-cost` under the `chat` endpoint. Off
switch: `app.ai.failover.enabled=false`. Details and the trade-offs in
[ADR-010](adr/010-chat-provider-failover.md).

Two things to know when reading latency: the provider call runs inside
`sendMessage`'s database transaction (a failed-over turn holds a connection
for both round trips), and users stop waiting at 30 s (axios) / 60 s (nginx).

## Evals

Two halves:

- **Deterministic, every build** — `GuardGoldenEvalTest` against
  `evals/guard-golden.json` (see above), part of `./mvnw verify`.
- **Live, on demand** — tests tagged `live-eval`, enabled only when
  `ANTHROPIC_API_KEY` / `GROQ_API_KEY` are set, so a plain build never
  calls a provider. Rubric-style checks on real replies: the chat cites
  figures from the activity and invents none, admits missing data, survives
  an in-conversation injection without leaking the system prompt, uses the
  training history; goal/plan generation returns the exact JSON contract
  with allowed workout types.

## Switches

| Property | Default | Effect |
|---|---|---|
| `app.ai.guard.enabled` | true | prompt guard on/off |
| `app.ai.pii.enabled` | true | masking on/off |
| `app.ai.failover.enabled` | true | chat failover Anthropic → Groq |
| `app.ai.rate-limit.chat-per-minute` | 10 | per-user chat turns per minute |
| `app.ai.rate-limit.generation-per-minute` | 5 | per-user generations per minute |

All are Spring properties and can be set through the usual environment
binding. Turning a guard off is a change-reviewed action, not a hotfix.
