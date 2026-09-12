---
name: KafkaConsumerLag
description: A consumer is behind on a topic (lag high, growing, or a listener throwing on every poll) — feeds, notifications and cache evictions are late or stuck.
metadata:
  owner: data
  version: "1.0"
alert_type: pipeline
severity: warning
actionability: "Ticket — same working day; page at KafkaConsumerLagCritical"
allowed-tools: []
---

# Alert: KafkaConsumerLagHigh / KafkaConsumerLagCritical / KafkaConsumerStalled / KafkaListenerFailures

## Summary

The saga (`docs/saga.md`) is choreographed: `activity.created` fans out to
social-service (feed), notification-service (push + email),
challenge-goal-service (cache eviction, progress). Lag means those effects
arrive late; a *stalled* consumer means they do not arrive at all. Three
distinct situations:

| Alert | Situation |
|---|---|
| `KafkaConsumerLagHigh` / `Critical` | consumer alive but slower than the producer |
| `KafkaConsumerStalled` | lag climbing for 15 min — consumer stuck (poison pill, blocked thread) |
| `KafkaListenerFailures` | listener throws on every poll — with the default error handler the same record is retried forever |

## Required inputs

- `app`, `topic`, `client_id` from the labels
- Kafka UI (http://localhost:8090, `--profile observability`): consumer group, per-partition lag

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/alerts/kafka.rules.yml` |
| Expression | `kafka_consumer_fetch_manager_records_lag_max > 1000` / `> 10000`; `deriv(...[15m]) > 0`; `rate(spring_kafka_listener_seconds_count{result="failure"}[5m]) > 0` |
| Threshold / for | 10m / 5m / 15m / 10m |

## Success criteria

- Lag back under 100 and decreasing on every partition.
- If a record was skipped, its key and offset are in the ticket and the owning team knows.

## Investigation

### 1. Stuck or slow?

| Command | What to look for |
|---|---|
| Kafka UI → Consumers → `<app>` group | one partition with lag, offset not moving = stuck; all partitions moving but behind = slow |
| `docker compose logs --since 15m <app> \| grep -A 20 -m 3 "KafkaListener\|Listener method"` | the exception the listener throws (`JsonProcessingException` = schema drift from `shared/events`, `DataIntegrityViolation` = idempotency gap) |
| `docker exec stride-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group <app>` | `CURRENT-OFFSET` vs `LOG-END-OFFSET` per partition; `CONSUMER-ID` empty = no member |

### 2. Poison pill

If the same offset fails repeatedly, fetch it:

```bash
docker exec stride-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic <topic> --partition <p> --offset <o> --max-messages 1
```

Decide with the owner: fix the consumer (preferred — the record is valid but the code is not), or skip it (commit the next offset with `kafka-consumer-groups.sh --reset-offsets --to-offset <o+1> --execute` while the consumer is stopped) and replay later.

### 3. Just slow

| Command | What to look for |
|---|---|
| Jaeger: service `<app>`, operation `<topic> process` | where the handler spends time (DB write, gRPC call per record) |
| `spring_kafka_listener_seconds` p95 in Prometheus | handler duration; > 1 s per record means batching or async is needed |

## Mitigation

- **Safe:** restart the consumer (rebalance, resumes from committed offset — at-least-once, handlers are idempotent per `docs/saga.md`). Scale consumers up to the partition count.
- **Needs review:** offset resets, skipping records, changing the error handler / DLT configuration.

## Escalation / close

- Escalate to **data** (pipeline owner) with the `--describe` output and the failing record.
- Close when lag < 100 for 15 minutes and `KafkaListenerFailures` is quiet.
