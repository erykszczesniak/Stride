---
name: KafkaProducerErrors
description: A service cannot publish events (producer errors) or publishes nothing while writes succeed (no events flowing) — downstream effects of the saga are lost, not delayed.
metadata:
  owner: data
  version: "1.0"
alert_type: pipeline
severity: critical
actionability: "Page — act now"
allowed-tools: []
---

# Alert: KafkaProducerErrors / KafkaNoEventsFlowing

## Summary

Unlike consumer lag, producer failures *lose* data: an activity is created
in activity-service's database but `activity.created` never reaches the
feed, the PR detection, or the notification. There is no outbox table in
this branch (documented trade-off in `docs/saga.md`), so the only recovery
is replaying from the source. Treat as critical even when nobody has
noticed yet.

`KafkaNoEventsFlowing` is the silence check: POST `/api/v1/activities`
succeeds but `kafka_producer_record_send_total` does not move.

## Required inputs

- `app` from the labels; alert start time (= start of the data gap)
- Broker state (`docker compose ps kafka`)

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/alerts/kafka.rules.yml` |
| Expression | `rate(kafka_producer_record_error_total[5m]) > 0`; `rate(POST 2xx[30m]) > 0 and rate(kafka_producer_record_send_total[30m]) == 0` |
| Threshold / for | 5m; 15m |

## Success criteria

- `kafka_producer_record_send_total` moving again, error rate 0.
- The gap window is known and the missed events are replayed or explicitly written off with the owner.

## Investigation

### 1. Broker

| Command | What to look for |
|---|---|
| `docker compose ps kafka` / `kubectl -n stride get pods -l app.kubernetes.io/name=kafka` | running, healthy |
| `docker exec stride-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list` | topics exist (`activity.created`, ...) — auto-create is on in compose, off in prod |
| `docker compose logs --since 15m kafka \| grep -i -E "error\|exception" \| tail` | disk full, controller election loop |

### 2. Producer side

```bash
docker compose logs --since 15m <app> | grep -E "KafkaProducer|TimeoutException|NotLeaderOrFollower|RecordTooLarge" | tail -20
```

| Log signature | Cause |
|---|---|
| `TimeoutException: Topic ... not present in metadata` | wrong bootstrap servers (`KAFKA_BOOTSTRAP_SERVERS`) or topic missing |
| `RecordTooLargeException` | an event with an embedded stream/polyline exceeds `max.request.size` — code fix, not ops |
| `NotLeaderOrFollowerException` bursts | broker restart; transient, should self-heal |
| nothing at all, yet no sends | publishing disabled by config or the publisher bean not wired — check `ActivityEventPublisher` logs at startup |

### 3. Size the gap

```promql
sum(increase(http_server_requests_seconds_count{app="<app>",method="POST",uri=~"/api/v1/activities.*",status=~"2.."}[<alert duration>]))
```

That is how many events were not published.

## Mitigation

- **Safe:** restart the producer service once the broker is healthy.
- **Replay (needs the owner):** re-emit events for the gap window. For activities: `select id from activities where created_at between <start> and <end>` and trigger the publisher per id (admin endpoint or a one-off script — do not hand-craft Kafka messages).

## Escalation / close

- Escalate to **data** immediately with the gap window and the count.
- Post-mortem mandatory: data loss.
