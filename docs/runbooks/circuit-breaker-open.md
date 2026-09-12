---
name: CircuitBreakerOpen
description: A Resilience4j breaker is open, so calls to a dependency are short-circuited and users get the fallback (degraded data or an error) instead of the real answer.
metadata:
  owner: domain
  version: "1.0"
alert_type: availability
severity: warning
actionability: "Ticket — same working day"
allowed-tools: []
---

# Alert: CircuitBreakerOpen / RetryBudgetExhausted

## Summary

The breaker did its job: the dependency named in `name` failed more than
50 % of its last calls and the caller stopped hammering it. The alert is
about the *dependency*, not the breaker. Breakers in this stack:

| Caller `app` | Breaker `name` | Fallback the user sees |
|---|---|---|
| challenge-goal-service | `activity-service` | goal progress shows 0 / cached value |
| activity-service | `user-service`, `social-service` | activity detail without author name / like counts |
| ai-service | `groq` | goal/plan generation returns 500 |
| ai-service | `activity-service` | chat works with single-activity context, no history |
| web-bff, pipeline-service | per domain service | partial dashboard, missing widgets |

`RetryBudgetExhausted` is the earlier signal: retries keep failing but the
breaker has not tripped yet.

## Required inputs

- `app` and `name` from the labels
- State of the dependency (`ServiceDown` for it? provider status page?)

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/alerts/services.rules.yml` |
| Expression | `resilience4j_circuitbreaker_state{state="open"} == 1`; `rate(resilience4j_retry_calls_total{kind="failed_with_retry"}[5m]) > 0.1` |
| Threshold / for | open for 2m; 0.1 failed/s for 5m |

## Success criteria

- Breaker back to `CLOSED` (it goes `HALF_OPEN` first after `waitDurationInOpenState`) and stays there 10 minutes.
- The dependency's own problem is identified.

## Investigation

### 1. Confirm and see why it opened

```bash
curl -s localhost:<caller-port>/actuator/circuitbreakers | jq '.circuitBreakers["<name>"] | {state, failureRate, bufferedCalls, notPermittedCalls}'
curl -s localhost:<caller-port>/actuator/circuitbreakerevents/<name> | jq '.circuitBreakerEvents[-10:] | .[] | {type, creationTime, errorMessage}'
```

`errorMessage` tells you what failed: `UNAVAILABLE` (peer down), `DEADLINE_EXCEEDED` (peer slow), `401`/`429` from a provider (key / quota).

### 2. Follow the dependency

- Internal peer: [service-down.md](service-down.md), then [high-latency.md](high-latency.md) for that peer.
- Groq / Anthropic: [ai-provider-degraded.md](ai-provider-degraded.md).

### 3. Is the breaker mis-tuned rather than the peer broken?

If the peer is healthy and the breaker still trips: `minimumNumberOfCalls: 3` with `failureRateThreshold: 50` means two failures in a row open it. That is deliberate in dev (fast feedback); prod overlays in Config Server raise the window. Check which profile the caller runs with before "fixing" the threshold.

## Mitigation

- **Safe:** nothing on the caller — fix the dependency; the breaker closes itself.
- **Emergency only:** `POST /actuator/circuitbreakers/<name>` with `{"updateState":"CLOSE"}` forces it closed; you are then hammering a sick dependency. Only with the owner's agreement.

## Escalation / close

- Escalate to the dependency's owner with the last 10 breaker events.
- Close when `resilience4j_circuitbreaker_state{state="closed"} == 1` for the pair.
