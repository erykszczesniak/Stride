# Unbounded Elasticsearch pinned the dev host; user-service retried it into the ground

**Date:** 2026-08-17 · **Duration:** several sessions before the cause was measured · **Severity:** dev-only; the composed stack was too slow to use for E2E work · **Author:** platform · **Status:** reviewed

Source of record: PR #151 ("memory limits for every JVM and opt-in heavy containers").

## Summary

After Phase 22 wired all 24 containers into one compose stack, the stack
became sluggish within minutes of booting: fans on, every request slow,
Kafka consumers lagging. Measuring instead of guessing showed two things.
Elasticsearch, unconstrained, sat at ~2 cores of CPU at idle, and
user-service spent another ~2 cores retrying its Elasticsearch mirror
(search index sync is async and swallow-on-failure, but each failure was
retried at full speed). The JVMs had no memory limits, so their heaps sized
themselves from the 24 GB host and the resident set kept growing.

## Impact

- The composed stack was effectively unusable for E2E verification on the development machine until containers were restarted, then degraded again.
- Kafka consumer lag grew because consumers were starved of CPU, which mimicked an application bug and cost investigation time.
- No production or user impact.

## Timeline

| Step | Observation |
|---|---|
| Boot | `docker stats` not consulted; symptoms attributed to "12 services is a lot" |
| Investigation | `docker stats --no-stream`: `elasticsearch` ~200 % CPU, `user-service` ~200 % CPU, others near idle |
| | user-service logs: continuous `ConnectException` to Elasticsearch followed by immediate retry — ES was restarting under memory pressure |
| Fix (PR #151) | `mem_limit` on every JVM (domain services 640m, gateway 512m, config/discovery 384/448m) with `-XX:MaxRAMPercentage=75`; Kafka capped at 768m with explicit heap; Elasticsearch behind a `search` profile; Jaeger/Prometheus/Grafana/Kafka-UI behind `observability` |
| Verification | idle CPU near zero; resident memory down ~1.6 GB; hard ceilings prevent regrowth |

## Root cause

No resource ceilings on any container, combined with a retry loop that had
no backoff. Each fault amplified the other: ES restarted because it was
memory-starved, user-service hammered it because retries were unbounded,
the CPU spent on both starved every other container.

## What went well

- The fix was measured before and after; the numbers are in the PR.
- Opt-in profiles turned the default `docker compose up` into a lean core that boots on a laptop.

## What went badly

- Hours were lost debugging "slow consumers" before anyone ran `docker stats`.
- The user-service → Elasticsearch retry had been reviewed as harmless ("swallow on failure") without asking how often it would retry.

## Where we got lucky

- The host had 24 GB; on a 16 GB machine the first symptom would have been the OOM killer taking a random JVM, which is much harder to read.

## Actions

| Action | Owner | Status |
|---|---|---|
| Memory limits on every container in compose and matching `resources.limits` in k8s manifests | platform | done in #151 / k8s |
| Heavy containers behind profiles; README documents the lean default | platform | done |
| Alert on the symptom: `JvmHeapHigh`, `JvmGcPressure`, `RetryBudgetExhausted` in `monitoring/alerts/services.rules.yml` — the last one would have fired on user-service within 5 minutes | platform | done |
| Add exponential backoff to the Elasticsearch mirror retry in user-service | domain | open |
| Runbook step "check `docker stats` first" in [high-latency.md](../runbooks/high-latency.md) and [jvm-heap-high.md](../runbooks/jvm-heap-high.md) | platform | done |

## Lessons for the runbooks / rules

- Resource saturation on a shared host looks like an application bug in whichever service you happen to be looking at. Saturation checks come before code checks.
- A "harmless" retry needs a number attached: attempts, backoff, and what happens after.
