# Saga: Activity created → Personal Record → Notification

The most-visible event chain in the stack. When a user records an activity,
three services react in sequence — none of them call each other directly,
all coordination happens through Kafka. This is the saga pattern in its
**choreography** form: no central orchestrator, each service publishes
events and reacts to events it cares about.

## Sequence

```
User → POST /api/activities             (via Gateway, X-User-Id propagated)
        │
        ▼
┌────────────────────────────────────────────────────────────────┐
│  activity-service                                              │
│  1. Persist Activity row in stride_activity                    │
│  2. Synchronously compute Personal Records for each            │
│     applicable DistanceType (1km, 5km, 10km, etc.)             │
│  3. Persist any new/improved PersonalRecord rows               │
│  4. Publish to Kafka:                                          │
│       · stride.activity.created (ActivityCreatedEvent)         │
│       · stride.pr.achieved      (PersonalRecordAchievedEvent)  │
│         — one per new PR                                       │
│  5. Return ActivityDto to the caller                           │
└────────────────────────────────────────────────────────────────┘
        │
        ▼ (Kafka, fan-out)
┌────────────────────────────┐  ┌────────────────────────────┐  ┌─────────────────────────────┐
│  social-service            │  │ challenge-goal-service     │  │ notification-service        │
│  group: social-service     │  │ group: challenge-goal-svc  │  │ group: notification-service │
│                            │  │                            │  │                             │
│  ActivityCreatedEvent →    │  │ ActivityCreatedEvent →     │  │ PersonalRecordAchieved →    │
│    INSERT feed_items       │  │   cache.evict("activity-   │  │   create Notification +     │
│    (denormalized snapshot, │  │   summary")                │  │   push via SSE to recipient │
│    user_id, sport,         │  │                            │  │                             │
│    distance, started_at)   │  │ (next read of /progress    │  │                             │
│                            │  │  recomputes from a fresh   │  │                             │
│                            │  │  call to activity-service) │  │                             │
└────────────────────────────┘  └────────────────────────────┘  └─────────────────────────────┘
        │                                                              │
        ▼ (read path)                                                   ▼ (SSE)
   GET /api/feed                                                  Browser tab gets
   shows the new activity                                         "New personal record!"
   in the dashboard                                               toast immediately
```

End-to-end latency from POST to a connected browser seeing the PR
notification is dominated by Kafka publish + consumer fetch, typically
< 1 second on local docker-compose.

## Why choreography (not orchestration)

The textbook alternative is an orchestrator service that explicitly calls
each step. We chose choreography here for three reasons specific to this
domain:

1. **Each step is genuinely autonomous.** The social feed and the
   notification have no business-logical dependency on each other — they're
   different reactions to the same fact. There's nothing for an orchestrator
   to orchestrate beyond "everyone subscribe to this topic."
2. **Adding consumers is zero-coupling.** Phase 9's hypothetical
   "leaderboard recomputation" or "weekly summary aggregation" only needs
   to add a `@KafkaListener` somewhere. The producer doesn't change. The
   other consumers don't know. With an orchestrator, every new step is a
   new call site in the orchestrator.
3. **The flow is fire-and-forget by design.** We don't need to know
   whether the notification was delivered to roll back the activity. If
   notification-service is down, the activity stands and the user just
   doesn't see a toast — graceful degradation, not transaction failure.

For workflows where you genuinely need rollback semantics across services
(e.g. "create order → reserve inventory → charge card → ship") the
orchestrated form with compensating actions is the right shape. This isn't
that workflow.

## At-least-once + idempotency

Spring Kafka uses auto-acknowledgement by default, which is at-least-once.
Each consumer in this saga is designed to be safely re-runnable:

- **social-service projection** — `feed_items` has a UNIQUE constraint on
  `activity_id`, and the consumer checks for existing rows before
  insertion. A duplicate `ActivityCreatedEvent` is a no-op.
- **social-service counts** — increments are NOT idempotent against
  duplicate events. A replayed `CommentAddedEvent` would over-count.
  We accept this for now because the producer side is also at-least-once
  but the dedup-on-id pattern is short — Phase 9 follow-up adds an
  `event_id` column on the producer side + a `processed_events` table
  on the consumer side.
- **challenge-goal cache eviction** — eviction is idempotent. Replays
  do nothing harmful.
- **notification-service** — currently no dedup. Duplicate PR events
  would create duplicate notifications. Follow-up: idempotency key on the
  notification row (one per `event_id`).

## Failure modes

| What fails           | What the user sees                                | Recovery                                  |
|----------------------|----------------------------------------------------|-------------------------------------------|
| Kafka broker down    | Activity creates, no propagation                  | Producer-side outbox table (Phase 9 todo) |
| social-service down  | Feed shows stale activity list                    | Consumer catches up from offset           |
| notification down    | No toast, but the PR is still saved               | Consumer catches up; toast is best-effort |
| challenge-goal down  | `/challenges/{id}/progress` returns 0 via R4j FB  | CB closes when service is healthy         |
| user-service down    | Login fails — that's the actual hard outage       | RDS multi-AZ in prod, can't avoid 100%    |

The system's overall availability is dominated by user-service (the only
service whose downtime causes total outage). Every other service can fail
without taking the rest of the app with it.
