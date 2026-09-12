# Cost attribution

Who pays for what, from metrics the backend already emits. The point is
not a precise bill — the point is that every hour of CPU, every GiB of
memory and every LLM token has a `team` and a `cost_center` attached, so a
cost question ("why did we spend 3× more on AI last week?") has an answer
without a spreadsheet.

## The label contract

Every meter the backend emits carries two tags, set once in
`application.yml`:

```yaml
management:
  metrics:
    tags:
      team: ${STRIDE_TEAM:domain}
      cost_center: ${STRIDE_COST_CENTER:CC-STRIDE-001}
```

One deployable, one owner: the defaults attribute the whole JVM to `domain`
/ `CC-STRIDE-001`, and a deployment overrides them through the two
environment variables. AI provider spend is the exception — the
`ai_request_cost_usd_total` series carries the same tags, but the
`AiSpendHigh` alert routes to the `ai` team because that is who can change
the prompt, the model or the limits. Ownership changes are a config change
with a diff, never a dashboard edit.

## The model

Recording rules in `monitoring/alerts/cost.rules.yml`:

| Series | Meaning | Source |
|---|---|---|
| `cost:cpu_cores:avg5m{app,team,cost_center}` | cores consumed (5-minute average) | `avg_over_time(process_cpu_usage[5m]) × system_cpu_count` |
| `cost:memory_gib:reserved{app,…}` | memory reserved, GiB | `jvm_memory_max_bytes{area="heap"} / 0.75` (the JVM runs with `MaxRAMPercentage=75`, so heap max ÷ 0.75 is the container limit) |
| `cost:infra_usd:rate1h{app,…}` | modelled USD per hour | `cores × 0.04 + GiB × 0.005` |
| `cost:team_infra_usd:rate1h{team,cost_center}` | the same, per team | sum |
| `cost:ai_usd:rate1h{provider,model}` | real provider spend per hour | `increase(ai_request_cost_usd_total[1h])` from token usage × the price table in `app.ai.pricing` |

Price constants are on-demand, general-purpose, rounded: **0.04 USD per
core-hour** and **0.005 USD per GiB-hour**. They are in the rule file with
the comment above them; change them there when the hosting changes.
Reserved memory is charged, not used memory — that is how cloud bills
work, and it is also why the memory limits in `docker-compose.prod.yml`
and `k8s/backend/deployment.yaml` matter: the dev `docker-compose.yml` sets
no limit, so locally "reserved" is 75 % of the host's RAM and the number
is meaningless in absolute terms.

What the model ignores: storage, network egress, managed Postgres
(RDS in `terraform/`), Elasticsearch, Jaeger, the frontend container,
licences. It attributes the backend's compute and the AI spend, which are
the two things a code review can change.

## Alerts

| Alert | Fires when | Why |
|---|---|---|
| `InfraCostSpike` | the backend's hourly cost exceeds 2× its own 24h average for 30 min (above a 0.05 USD/h floor) | runaway loop, retry storm, an unplanned limit bump — runbook [jvm-heap-high.md](runbooks/jvm-heap-high.md) |
| `AiSpendHigh` | provider spend above 5 USD in the trailing hour for 15 min | failover during a provider incident doubles calls — runbook [ai-provider-degraded.md](runbooks/ai-provider-degraded.md) |

## The dashboard

**Stride — Cost attribution** (`stride-cost`): "Infra cost per hour by
team", "Infra cost per hour by service", "CPU cores consumed by service",
"Memory reserved (GiB) by service", "AI spend per hour by model (red line =
5 USD alert)", "Tokens per second by provider and direction", and
"Projected 30-day spend (infra + AI)" (`sum(cost:infra_usd:rate1h) × 24 × 30`
plus the AI trailing-hour rate × 720).

## Reading it honestly

- Locally (compose) the numbers are tiny and dominated by reserved memory;
  the shape is what matters — when the CPU line moves and why.
- With one deployable, "by team" and "by service" are the same single bar
  until `STRIDE_TEAM` / `STRIDE_COST_CENTER` are set differently per
  deployment (e.g. a second environment).
- The AI panels are empty until the guardrails telemetry (`AiCallRecorder`,
  [`ai-guardrails.md`](ai-guardrails.md)) is deployed, and their prices are
  placeholders until verified.
