# ADR-002 — One PostgreSQL schema, Flyway migrations; H2 for unit tests, Testcontainers for integration

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-04 (V1), recorded 2026-09-12 |
| **Deciders** | platform |

## Context

One deployable (ADR-001) and one team. The schema is relational and
cross-feature: `activities.user_id → users.id`, `comments.activity_id →
activities.id`, `ai_conversations → users, activities`, `user_feed_entries
→ activities`. It has to evolve safely on a database that already holds
data, and the test suite has to run on a laptop without Docker.

## Decision

- **One PostgreSQL database** (`strava`), one Flyway history, 22
  migrations `V1__init_schema.sql` … `V22__fix_serpentine_swim_route.sql`
  in `src/main/resources/db/migration`, append-only. Foreign keys are real
  and cascade where a parent owns the child.
- **Hibernate validates, never generates:** `spring.jpa.hibernate.ddl-auto:
  validate` in every runtime profile; DDL lives only in migrations
  (`spring.flyway.baseline-on-migrate: true` for databases created before
  Flyway).
- **Data fixes are migrations too** (V19–V22), written content-keyed so a
  re-run is a no-op.
- **Tests:** the `test` profile runs on in-memory H2 with `create-drop`
  and Flyway off (`@DataJpaTest` repositories, `@WebMvcTest` controllers,
  service unit tests). Anything PostgreSQL-specific or end-to-end extends
  `AbstractIntegrationTest`: `postgres:16-alpine` via Testcontainers
  (`@ServiceConnection`, reused container, `disabledWithoutDocker = true`
  so a Docker-less laptop still passes).

## Alternatives considered

- **Schema per feature** (the `microservices` branch's database-per-service)
  — replaces foreign keys with runtime lookups and event-driven copies; the
  price of independent deploys, which one deployable does not need.
- **Postgres for every test** — 5–10× slower suite and a hard Docker
  dependency; H2 in memory plus one container base class is the trade-off.
- **Hibernate `update` in dev** — faster iteration, drifting schemas; the
  V12 and V16 collisions show that even append-only files need discipline,
  generated DDL would have hidden the drift entirely.

## Consequences

- A migration is reviewed as SQL in a PR and applied once per database;
  boot refuses an inconsistent state (`Validate failed`, `Found more than
  one migration`).
- Migration numbers are allocated by hand; parallel PRs collide (`6931e5e`,
  `9d4b165` — [post-mortem 2026-07-11](../postmortems/2026-07-11-duplicate-flyway-version.md)).
- Both H2 and PostgreSQL must accept every migration's SQL, which keeps
  vendor-specific syntax out of the schema.

## Known gaps

- No test runs the migration chain: the `testcontainers` profile also uses
  `create-drop` with Flyway disabled. A broken migration is found at boot,
  not in `verify` (open action in the post-mortem).
- No duplicate-version check in CI.
