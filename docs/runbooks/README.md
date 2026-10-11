# Runbooks

One document per alert family. Every alert in `monitoring/alerts/*.rules.yml`
links here through its `runbook_url` annotation, and the CI contract check
(`monitoring/scripts/check_alert_rules.py`, run by
`.github/workflows/monitoring.yml`) warns when a link points at a file that
does not exist.

| Runbook | Alerts | Severity |
|---|---|---|
| [service-down.md](service-down.md) | `ServiceDown` | critical |
| [edge-high-error-rate.md](edge-high-error-rate.md) | `EdgeHighErrorRate`, `EdgeElevatedErrorRate`, `EndpointHighErrorRate`, `ErrorLogSpike` | critical / warning |
| [edge-traffic-collapse.md](edge-traffic-collapse.md) | `EdgeTrafficCollapse` | warning |
| [high-latency.md](high-latency.md) | `EdgeHighLatencyP95`, `EndpointHighLatencyP95` | warning |
| [jvm-heap-high.md](jvm-heap-high.md) | `JvmHeapHigh`, `JvmGcPressure`, `InfraCostSpike` | warning |
| [db-pool-exhausted.md](db-pool-exhausted.md) | `DbPoolSaturated`, `DbPoolExhausted` | warning / critical |
| [ai-provider-degraded.md](ai-provider-degraded.md) | `AiProviderFailoverActive`, `AiProviderErrors`, `AiChatErrorRate`, `AiChatLatencyP95`, `AiSLOBurnRateFast`, `AiSpendHigh` | warning / critical |
| [slo-burn-rate.md](slo-burn-rate.md) | `SLOBurnRateFast/Slow`, `SLOLatencyBurnRateFast/Slow`, `SLOErrorBudgetExhausted` | critical / warning |
| [anomaly-detected.md](anomaly-detected.md) | `JvmHeapExhaustionPredicted`, `DbPoolExhaustionPredicted`, `TrafficDeviatesFromWeeklyBaseline`, `ErrorRatioDeviation` | warning |
| [monitoring-pipeline.md](monitoring-pipeline.md) | `Watchdog`, `PrometheusTargetsMissing`, `PrometheusRuleEvaluationFailures`, `PrometheusHighCardinality`, `AlertmanagerDown`, `AlertmanagerNotificationsFailing` | critical / warning / info |

## Format

Each runbook follows [`_template.md`](_template.md): YAML front matter that
an on-call tool or an AI agent can read (`name`, `alert_type`, `severity`,
`actionability`), then **Summary → Required inputs → Alert metadata →
Success criteria → Investigation (numbered steps, each with the command, what
to look for, and the expected result) → Mitigation → Escalation / close**.
Investigation steps are ordered from cheapest to most invasive; the mitigation
section is explicit about what is safe to do without a change review.

## Where things are

There is one deployable, `strava-backend` (compose service `backend`). The
monitoring stack exists only in `docker-compose.yml`; `k8s/` has manifests for
the backend, the frontend and Postgres, nothing else.

| Surface | Local (compose) | Kubernetes (`k8s/`, namespace `strava`) |
|---|---|---|
| Prometheus | http://localhost:9091 | not deployed |
| Alertmanager | http://localhost:9093 | not deployed |
| Grafana | http://localhost:3003 (`stride-jvm`, `stride-slo`, `stride-cost`) | not deployed |
| Jaeger | http://localhost:16686 | not deployed |
| Alert deliveries | `docker compose logs -f alert-sink` | — |
| Backend logs | `docker compose logs --since 15m backend` | `kubectl -n strava logs deploy/backend --since=15m` |
| Actuator | `curl -s localhost:8081/actuator/health` | `kubectl -n strava port-forward svc/backend 8080:8080`, then `localhost:8080` |
| Postgres | `docker compose exec postgres psql -U strava -d strava` (`make shell-db`) | `kubectl -n strava exec deploy/postgres -- psql -U strava -d strava` |
| Elasticsearch | http://localhost:9200 | not deployed (user search falls back to SQL) |
| Frontend | http://localhost (nginx; proxies `/api/` and `/graphql` to `backend:8080`) | ingress `strava.local` (`/api/(.*)` → backend, rest → frontend) |

Ports, host → container: backend 8081 → 8080, frontend 80, Postgres
127.0.0.1:5433 → 5432, Elasticsearch 9200, Jaeger 16686 (UI) / 4318 (OTLP
HTTP), Prometheus 9091 → 9090, Alertmanager 9093, Grafana 3003 → 3000.

Actuator access: `/actuator/health/**` and `/actuator/prometheus` are public.
Health *components* (db, elasticsearch, diskSpace) appear only with a Bearer
token (`show-details: when_authorized`); `/actuator/metrics/**` needs a token
too and is not exposed at all under the `prod` profile — use Prometheus for
metrics.

## After the incident

Anything that paged gets a post-mortem in [`../postmortems/`](../postmortems/)
within two working days, blameless, with at least one action that changes a
rule, a runbook, or the system. If a runbook step was wrong or missing, fix it
in the same PR as the post-mortem.
