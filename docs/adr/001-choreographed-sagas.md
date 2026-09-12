# ADR-001 — Choreographed sagas over Kafka instead of an orchestrator

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-07 (Phase 9), recorded 2026-09-12 |
| **Deciders** | platform |

## Context

Creating an activity has several downstream effects owned by other services:
personal-record detection, feed fan-out (social-service), notifications and
email (notification-service), goal progress and cache eviction
(challenge-goal-service). They must not run inside activity-service's
transaction, and they must survive a downstream service being down.

## Decision

`activity-service` publishes `activity.created` (and the other topics in
`shared/events/Topics.java`) and forgets. Every interested service subscribes
with its own consumer group and reacts independently. There is no
orchestrator, no reply topic, no compensation flow. Consumers are
idempotent because delivery is at-least-once (details and the walkthrough
in [`docs/saga.md`](../saga.md)).

## Alternatives considered

- **Orchestrator service** calling each step and compensating on failure —
  right for order/payment style workflows with rollback semantics; wrong
  here, because no step depends on another and nothing needs undoing.
- **Synchronous fan-out over gRPC** from activity-service — couples the
  write path to every consumer's availability and latency.
- **Transactional outbox** — would remove the "event lost if the broker is
  unreachable" window. Not implemented; see gaps.

## Consequences

- Adding a consumer is one `@KafkaListener` and zero producer changes.
- A dead consumer means stale feeds or missing toasts, never a failed write
  (graceful degradation, `KafkaConsumerLag*` alerts).
- Every consumer carries idempotency logic and its own retry/DLT story.
- Debugging a flow means following a trace across topics; the Kafka trace
  propagation is still incomplete (`docs/observability.md`).

## Known gaps

- No outbox: a producer error loses the event (`KafkaProducerErrors` pages,
  `docs/runbooks/kafka-producer-errors.md` describes the replay).
- Kafka-driven flows have no latency SLO yet (ADR-007).
