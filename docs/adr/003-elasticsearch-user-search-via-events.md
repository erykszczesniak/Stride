# ADR-003 — Elasticsearch for user search, synced by Spring application events, SQL fallback

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-05 (#103), recorded 2026-09-12 |
| **Deciders** | domain |

## Context

Finding people by name, bio or city with typo tolerance is a product
feature; `ILIKE '%term%'` over `users` gives neither ranking nor fuzziness
and does not scale past a few thousand rows. The write model must stay in
PostgreSQL (ADR-002), and the app must keep working when the search engine
is down.

## Decision

- **Elasticsearch holds a read-only projection** of users: `UserDocument`
  (index `users`; `fullName^3`, `firstName^2`, `lastName^2`, `bio`, `city`
  with `fuzziness: AUTO`; `email` is a keyword, never free-text; a `must_not`
  on `hideFromSearch` enforces the privacy flag at query time). The JPA
  `User` entity remains the source of truth; hits are re-hydrated from
  PostgreSQL in Elasticsearch order (`UserServiceImpl.searchUsers`).
- **Sync by application events, not by a broker.** `UserServiceImpl`
  publishes `UserUpsertedEvent` / `UserDeletedEvent`;
  `UserSearchIndexSyncListener` (`@Async @EventListener`) indexes or
  deletes the document and swallows failures with a WARN — the index is an
  eventually consistent projection and must never fail a write.
- **Self-healing on boot:** `UserSearchIndexInitializer` creates the index
  and seeds up to 1000 users from PostgreSQL when the index is empty.
- **Degrade, do not fail:** `GET /api/v1/users/search` catches any
  Elasticsearch exception and answers from the SQL `ILIKE` implementation.

## Alternatives considered

- **PostgreSQL full-text search (`tsvector`, `pg_trgm`)** — no extra
  container and no sync; weaker fuzziness and ranking for names. The right
  choice if Elasticsearch ever becomes the operational burden.
- **Sync through Kafka / Debezium** — exactly-once and replayable; a broker
  and a connector to run for one index (ADR-005).
- **Synchronous indexing inside the user transaction** — consistent, but a
  slow or down Elasticsearch would block profile updates.

## Consequences

- Search quality comes from Elasticsearch; availability comes from the
  fallback. Both paths return the same DTO, so the frontend cannot tell.
- The listener is `@EventListener`, not `@TransactionalEventListener`: it
  fires when the event is published, before the transaction commits, and
  asynchronously. A rolled-back save can leave a phantom document until the
  next upsert.
- Elasticsearch is in `docker-compose.yml` (512 MB heap, health-checked)
  and `docker-compose.prod.yml`, but not in `k8s/` — there the seed and
  every sync log a WARN and search always takes the SQL path.
- Spring Data JPA and Spring Data Elasticsearch on one classpath needed
  explicit repository scoping (`JpaConfiguration`,
  `ElasticsearchConfiguration`, #110).
- The Elasticsearch health indicator is on, so an unreachable cluster turns
  the aggregate `/actuator/health` `DOWN` — see the note in
  [service-down.md](../runbooks/service-down.md).

## Known gaps

- Sync failures are visible only as WARN log lines; there is no metric and
  no alert for a stale index.
- One integration test (`UserSearchIntegrationTest`, real ES container,
  Docker-gated) covers the round trip; the fallback path is covered only by
  unit tests.
