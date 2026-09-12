---
name: AiProviderDegraded
description: An LLM provider behind the backend (Anthropic for chat, Groq for goals/plans and as chat fallback) is failing, slow, being failed over, or expensive — AI features return 5xx, time out, or cost more than planned.
metadata:
  owner: ai
  version: "1.0"
alert_type: provider
severity: warning
actionability: "Ticket — same working day; page on AiSLOBurnRateFast"
allowed-tools: []
---

# Alert: AiProviderFailoverActive / AiProviderErrors / AiChatErrorRate / AiChatLatencyP95 / AiSLOBurnRateFast / AiSpendHigh

## Summary

Two external providers sit behind `/api/v1/ai/**`: Anthropic
(`ClaudeServiceImpl`, chat on `/api/v1/ai/activities/{activityId}/chat`) and
Groq (`GroqServiceImpl`, `/api/v1/ai/generate-goal`, `/generate-plan`, and
the multi-turn fallback for chat through `FailoverClaudeService`). Their
failures are not ours to fix, only to detect and route around. There is no
circuit breaker on this branch: the provider's own error ratio
(`AiProviderErrors`) is the signal, and failover happens per call.

| Status from `/api/v1/ai/**` | Meaning | Where to look |
|---|---|---|
| 503 `AI chat is not configured` | no `ANTHROPIC_API_KEY` (and failover could not serve it) | environment, not the provider — step 3 |
| 502 `Failed to generate AI response` / `Claude returned an empty response` | the provider returned an error or an empty reply | step 2 |
| 5xx on `generate-goal` / `generate-plan` | Groq failed (`Error calling Groq API` in the log); there is no fallback for generation | step 2 |
| 429 | per-user rate limit (`Guardrails.admit`, 10 chat / 5 generation per minute) — expected behaviour, a client is over quota | not an incident |
| 400 from the prompt guard | the input was blocked | not an incident; review `ai_guard_decisions_total` if it spikes |
| 200 but slow | provider latency; `AiChatLatencyP95` at 15 s. The SPA gives up at 30 s (axios), nginx at 60 s — and the backend keeps waiting, holding a DB connection ([db-pool-exhausted.md](db-pool-exhausted.md)) | step 4 |

The `ai_*` series come from `AiCallRecorder` (see
[`docs/ai-guardrails.md`](../ai-guardrails.md)). Until that code is deployed,
`AiProviderFailoverActive`, `AiProviderErrors` and `AiSpendHigh` cannot fire
and step 1 returns nothing; `AiChatErrorRate`, `AiChatLatencyP95` and
`AiSLOBurnRateFast` read the HTTP metrics and work regardless.

## Required inputs

- Which endpoint (`chat` = Anthropic first, Groq on failover; `generate-goal` / `generate-plan` = Groq only) and which `provider` label
- Provider status pages: https://status.anthropic.com, https://groqstatus.com
- Whether failover is already serving (`ai_failover_total`)

## Alert metadata

| Property | Value |
|---|---|
| Rule files | `monitoring/alerts/ai.rules.yml`, `slo.rules.yml` (`AiSLOBurnRateFast`), `cost.rules.yml` (`AiSpendHigh`) |
| Expression | failover: `sum(rate(ai_failover_total[15m])) > 0`; provider: `sum by (provider) (rate(ai_requests_total{outcome=~"provider_error\|timeout"}[10m])) / sum by (provider) (rate(ai_requests_total[10m])) > 0.5`; HTTP 5xx ratio on `uri=~"/api/v1/ai.*"` `> 0.10`; p95 on the same `> 15`; burn: `(slo:ai_error:ratio_rate1h / 0.02) > 14.4 and (…rate5m / 0.02) > 14.4`; spend: `sum(increase(ai_request_cost_usd_total[1h])) > 5` |
| Threshold / for | 10m / 5m / 5m / 10m / 2m / 15m |
| Dashboard | http://localhost:3003/d/stride-cost (providers, spend), http://localhost:3003/d/stride-slo (AI SLO) |

## Success criteria

- AI 5xx ratio under 2 % and p95 under 15 s for 10 minutes; no failovers in the last 15 minutes.
- Provider incident linked, or our own misconfiguration fixed.
- `AiSpendHigh`: hourly spend back under 5 USD and the multiplier (retries, failover) named.

## Investigation

### 1. Which provider, which outcome

```promql
sum by (provider, outcome) (rate(ai_requests_total[10m]))
sum by (endpoint, to) (rate(ai_failover_total[15m]))
```

`outcome="provider_error"` with failover to `groq` = Anthropic incident,
chat still answering (degraded). Errors on `provider="groq"` = generation
is down *and* the chat fallback is gone.

### 2. Provider error text

```bash
docker compose logs --since 15m backend | grep -E "Claude chat request failed|Error calling Groq API" | tail -10
```

`401` = key rotated or expired; `429` = provider quota; `5xx` / timeout =
provider outage. Compare with the status pages.

### 3. Is it our configuration?

| Command | What to look for |
|---|---|
| `docker compose exec backend sh -c 'test -n "$ANTHROPIC_API_KEY" && echo set \|\| echo missing'` | `docker-compose.yml` passes `GROQ_API_KEY` but **not** `ANTHROPIC_API_KEY`; k8s `backend-secret` has `GROQ_API_KEY` only. A 503 on chat there is configuration |
| `docker compose logs backend \| grep -m1 "Claude AI chat"` | the startup line: `configured (model=…)` or `not configured — set ANTHROPIC_API_KEY` |
| `app.ai.failover.enabled` (default true) and `GROQ_API_KEY` set | with both, chat keeps working on Groq while Anthropic is unavailable |

### 4. Latency

```promql
histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{app="strava-backend",uri=~"/api/v1/ai.*"}[10m])))
histogram_quantile(0.95, sum by (provider, le) (rate(ai_request_duration_seconds_bucket[10m])))
```

The difference between the two is our own time: the ownership check, the
20-activity history query and persisting the turn (`AiChatServiceImpl`,
`HISTORY_LIMIT = 20`) — all inside one transaction. If the provider p95 is
the whole number, it is theirs.

### 5. Cost sanity (`AiSpendHigh`)

```promql
sum by (provider, model) (increase(ai_request_cost_usd_total[1h]))
sum by (provider, outcome) (increase(ai_requests_total[1h]))
```

A latency incident with failover multiplies spend: every failed Anthropic
call is followed by a Groq call. If error / timeout outcomes rose with the
spend, consider `app.ai.failover.enabled=false` until the primary recovers
(change-reviewed; the alert description says the same).

## Mitigation

- **Safe:** nothing to restart — there is no breaker to reset and a restart does not help a provider; failover is automatic. Note that the prices in `app.ai.pricing.*` are placeholders, so `AiSpendHigh` is a relative signal until they are verified.
- **Needs review:** rotating keys (`ANTHROPIC_API_KEY`, `GROQ_API_KEY` — compose env, k8s `backend-secret`, Terraform SSM `groq_api_key`); changing models (`ANTHROPIC_MODEL`, `GROQ_MODEL`); raising the per-user limits (`app.ai.rate-limit.*`); disabling failover.

## Escalation / close

- Escalate to **ai** with the `outcome` breakdown, the log excerpt and the provider status link.
- Close when the AI SLI (`1 - slo:ai_error:ratio_rate1h`) is back above 98 % and p95 under 15 s.
