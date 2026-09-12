# Cost attribution

Who pays for what, from metrics the services already emit. The point is
not a precise bill — the point is that every hour of CPU, every GiB of
memory and every LLM token has a `team` and a `cost_center` attached, so
a cost question ("why did we spend 3× more on AI last week?") has an
answer without a spreadsheet.

## The label contract

Every meter every service emits carries two tags, set once per service in
Config Server (`infrastructure/config-server/.../<service>.yml`):

```yaml
stride:
  team: domain            # platform | domain | ai
  cost-center: CC-DOMAIN-002
```

The shared `application.yml` turns them into Micrometer common tags
(`management.metrics.tags.team`, `cost_center`). A service that does not
set them shows up as `unassigned` on the dashboard instead of disappearing.
Ownership changes are a config change with a diff, never a dashboard edit.

| Team | Services | Cost center |
|---|---|---|
| platform | api-gateway, web-bff, pipeline-service | CC-PLATFORM-001 |
| domain | user, activity, social, club-event, challenge-goal, notification | CC-DOMAIN-002 |
| ai | ai-service | CC-AI-003 |

## The model

Recording rules in `monitoring/alerts/cost.rules.yml`:

| Series | Meaning | Source |
|---|---|---|
| `cost:cpu_cores:avg5m{app,team,cost_center}` | cores consumed (5-minute average) | `process_cpu_usage × system_cpu_count` |
| `cost:memory_gib:reserved{app,…}` | memory reserved, GiB | `jvm_memory_max_bytes{area="heap"} / 0.75` (every JVM runs with `MaxRAMPercentage=75`, so heap max ÷ 0.75 is the container limit) |
| `cost:infra_usd:rate1h{app,…}` | modelled USD per hour | `cores × 0.04 + GiB × 0.005` |
| `cost:team_infra_usd:rate1h{team,cost_center}` | the same, per team | sum |
| `cost:ai_usd:rate1h{provider,model}` | real provider spend per hour | `increase(ai_request_cost_usd_total[1h])` from token usage × the price table in ai-service |

Price constants are on-demand, general-purpose, rounded: **0.04 USD per
core-hour** and **0.005 USD per GiB-hour**. They are in the rule file with
the comment above them; change them there when the hosting changes.
Reserved memory is charged, not used memory — that is how cloud bills
work, and it is also why the memory limits in compose and k8s matter.

What the model ignores: storage, network egress, managed Postgres/Kafka/
Redis, licences. It attributes compute and AI spend, which are the two
things a team can change with a code review.

## Alerts

| Alert | Fires when | Why |
|---|---|---|
| `InfraCostSpike` | a service's hourly cost exceeds 2× its own 24h average for 30 min (above a 0.05 USD/h floor) | runaway loop, retry storm, an unplanned limit bump |
| `AiSpendHigh` | provider spend above 5 USD in the trailing hour for 15 min | retries during a provider incident multiply spend |

## The dashboard

**Stride — Cost attribution** (`stride-cost`): hourly infra cost stacked by
team, per-service table, AI spend by model, tokens by direction, and a
30-day projection (`sum(cost:infra_usd:rate1h) × 24 × 30` plus the AI
trailing-hour rate × 720). Use the `team` variable to answer a team's
question, the `app` variable for a service's.

## Reading it honestly

- Locally (compose) the numbers are tiny and dominated by reserved memory;
  the shape is what matters — which service moves when.
- Attribution only works for services that go through Config Server.
  config-server and discovery-server are infrastructure and are not tagged.
- If a new service appears as `unassigned`, that is the alert: give it a
  team in its Config Server file.
