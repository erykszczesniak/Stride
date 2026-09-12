# Runbooks

One document per alert family. Every alert in `monitoring/alerts/*.rules.yml`
links here through its `runbook_url` annotation, and the CI contract check
warns when a link points at a file that does not exist.

| Runbook | Alerts | Severity |
|---|---|---|
| [service-down.md](service-down.md) | `ServiceDown`, `ServiceMissing` | critical |
| [gateway-high-error-rate.md](gateway-high-error-rate.md) | `GatewayHighErrorRate`, `GatewayElevatedErrorRate` | critical / warning |
| [gateway-traffic-collapse.md](gateway-traffic-collapse.md) | `GatewayTrafficCollapse` | warning |
| [high-latency.md](high-latency.md) | `GatewayHighLatencyP95`, `ServiceHighLatencyP95` | warning |
| [service-high-error-rate.md](service-high-error-rate.md) | `ServiceHighErrorRate`, `ErrorLogSpike` | critical / warning |
| [circuit-breaker-open.md](circuit-breaker-open.md) | `CircuitBreakerOpen`, `RetryBudgetExhausted` | warning |
| [jvm-heap-high.md](jvm-heap-high.md) | `JvmHeapHigh`, `JvmGcPressure` | warning |
| [db-pool-exhausted.md](db-pool-exhausted.md) | `DbPoolSaturated`, `DbPoolExhausted` | warning / critical |
| [kafka-consumer-lag.md](kafka-consumer-lag.md) | `KafkaConsumerLagHigh`, `KafkaConsumerLagCritical`, `KafkaConsumerStalled`, `KafkaListenerFailures` | warning / critical |
| [kafka-producer-errors.md](kafka-producer-errors.md) | `KafkaProducerErrors`, `KafkaNoEventsFlowing` | critical |
| [ai-provider-degraded.md](ai-provider-degraded.md) | `AiProviderCircuitOpen`, `AiChatErrorRate`, `AiChatLatencyP95`, `AiSLOBurnRateFast` | warning / critical |
| [slo-burn-rate.md](slo-burn-rate.md) | `SLOBurnRateFast/Slow`, `SLOLatencyBurnRateFast/Slow`, `ServiceSLOBurnRateFast/Slow`, `SLOErrorBudgetExhausted` | critical / warning |
| [monitoring-pipeline.md](monitoring-pipeline.md) | `Watchdog`, `PrometheusTargetsMissing`, `PrometheusRuleEvaluationFailures`, `PrometheusHighCardinality`, `AlertmanagerDown`, `AlertmanagerNotificationsFailing` | critical / warning |

## Format

Each runbook follows [`_template.md`](_template.md): YAML front matter that
an on-call tool or an AI agent can read (`name`, `alert_type`, `severity`,
`actionability`), then **Summary → Required inputs → Alert metadata →
Success criteria → Investigation (numbered steps, each with the command, what
to look for, and the expected result) → Mitigation → Escalation / close**.
Investigation steps are ordered from cheapest to most invasive; the mitigation
section is explicit about what is safe to do without a change review.

## Where things are

| Surface | Local (compose) | kind / k8s |
|---|---|---|
| Prometheus | http://localhost:9090 | `kubectl -n monitoring port-forward svc/prometheus 9090` |
| Alertmanager | http://localhost:9093 | `... svc/alertmanager 9093` |
| Grafana | http://localhost:3001 (`stride-jvm`, `stride-slo`) | `... svc/grafana 3000` |
| Jaeger | http://localhost:16686 | `... svc/jaeger-query 16686` |
| Eureka | http://localhost:8761 | `kubectl -n stride port-forward svc/discovery-server 8761` |
| Kafka UI | http://localhost:8090 (`--profile observability`) | — |
| Alert deliveries | `docker compose logs -f alert-sink` | receiver of your choice |
| Service logs | `docker compose logs --since 15m <service>` | `kubectl -n stride logs deploy/<service> --since=15m` |
| Actuator | `curl -s localhost:<port>/actuator/health` | `kubectl -n stride exec deploy/<service> -- wget -qO- localhost:<port>/actuator/health` |

Ports: gateway 8080, user 8081, activity 8082, notification 8083, social 8084,
club-event 8085, challenge-goal 8086, ai 8087, web-bff 8088, pipeline 8089,
config 8888, eureka 8761.

## After the incident

Anything that paged gets a post-mortem in [`../postmortems/`](../postmortems/)
within two working days, blameless, with at least one action that changes a
rule, a runbook, or the system. If a runbook step was wrong or missing, fix it
in the same PR as the post-mortem.
