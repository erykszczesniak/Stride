---
name: AiProviderDegraded
description: ai-service's LLM provider (Anthropic for chat, Groq for goals/plans) is failing, slow, or short-circuited — AI features return 5xx or time out.
metadata:
  owner: ai
  version: "1.0"
alert_type: provider
severity: warning
actionability: "Ticket — same working day; page on AiSLOBurnRateFast"
allowed-tools: []
---

# Alert: AiProviderCircuitOpen / AiChatErrorRate / AiChatLatencyP95 / AiSLOBurnRateFast

## Summary

Two external providers sit behind ai-service. Their failures are not ours
to fix, only to detect and route around. Map the status to the cause:

| Status from `/api/v1/ai/**` | Meaning | Where to look |
|---|---|---|
| 503 `AI chat is not configured` | no `ANTHROPIC_API_KEY` in this environment | env / Config Server, not the provider |
| 503 (breaker) | Resilience4j breaker open on `groq` / `anthropic` | provider outage or bad key; step 1 |
| 502 `Failed to generate AI response` | provider returned an error or an empty reply | step 2 |
| 429 | per-user rate limit (guardrails) — expected behaviour, a client is over quota | not an incident |
| 400 `Request blocked` | prompt guard rejected the input | not an incident; review if it spikes |
| 200 but slow | provider latency; watch `AiChatLatencyP95` | step 3 |

## Required inputs

- Which endpoint (`chat` = Anthropic, `generate-goal` / `generate-plan` = Groq)
- Provider status pages: https://status.anthropic.com, https://groqstatus.com
- `ai_failover_total` (once the failover PR is merged): whether the secondary provider is already serving

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/alerts/ai.rules.yml`, `slo.rules.yml` |
| Expression | breaker `state="open"`; AI 5xx ratio > 10 %; AI p95 > 15 s; burn rate > 14.4× of a 2 % budget |
| Threshold / for | 2m / 5m / 10m / 2m |
| Dashboard | http://localhost:3001/d/stride-slo |

## Success criteria

- AI 5xx ratio under 2 % and p95 under 15 s for 10 minutes.
- Provider incident linked, or our own misconfiguration fixed.

## Investigation

### 1. Breaker events say why

```bash
curl -s localhost:8087/actuator/circuitbreakerevents | jq '.circuitBreakerEvents[-10:] | .[] | {circuitBreakerName, type, errorMessage}'
```

`401` = key rotated/expired; `429` = provider quota; `5xx` / timeout = provider outage.

### 2. Provider error text

```bash
docker compose logs --since 15m ai-service | grep -E "Claude chat request failed|Error calling Groq API" | tail -10
```

### 3. Latency

```promql
histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{app="ai-service",uri=~"/api/v1/ai.*"}[5m])))
```

Compare with `ai_request_duration_seconds` per provider (guardrails PR) to separate provider time from our own (activity-service gRPC context fetch, DB write).

### 4. Cost sanity (guardrails PR)

```promql
sum by (provider, model) (increase(ai_request_cost_usd_total[1h]))
```

A latency incident with retries can multiply spend; if the hourly cost is 5× normal, consider disabling retries before the provider recovers.

## Mitigation

- **Safe:** nothing to restart — the breaker recovers on its own; the failover client (Anthropic → Groq for chat) already serves degraded answers if enabled.
- **Needs review:** rotating keys (`ANTHROPIC_API_KEY`, `GROQ_API_KEY`), changing models, raising per-user limits.

## Escalation / close

- Escalate to **ai** with the breaker events and the provider status link.
- Close when the AI SLI on `stride-slo` is back above 98 %.
