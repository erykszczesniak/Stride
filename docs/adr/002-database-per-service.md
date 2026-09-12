# ADR-002 — One database per service, logical cross-service references

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-07 (Phase 0–3), recorded 2026-09-12 |
| **Deciders** | platform |

## Context

The monolith on `main` has one schema with foreign keys everywhere
(`activities.user_id → users.id`, `comments.activity_id → activities.id`).
Splitting into services while keeping one shared database would keep every
service coupled to every table and make independent deploys impossible.

## Decision

Each domain service owns a Postgres database and its Flyway migrations.
Cross-service references are plain columns (`BIGINT user_id`) with no
foreign key; the referenced entity is fetched over gRPC (ADR-003) or
mirrored locally from events (ADR-001) — e.g. `social-service.user_cache`
and `activity-service.user_cache` populated from `user.registered/updated`.
Joins across services happen in the BFFs or in read models, never in SQL.

## Alternatives considered

- **Shared database, separate schemas** — cheaper, but a migration in one
  service can break another and the "database is the integration point"
  anti-pattern returns.
- **One database, views per service** — same coupling, plus a false sense
  of isolation.

## Consequences

- Services deploy and migrate independently; a broken migration takes down
  one service, not the platform.
- Referential integrity is a runtime concern: a deleted user leaves
  `user_id`s behind; consumers of `user.deleted` clean up or mask.
- Eight Postgres containers locally; managed instances in production.
- Tests run on H2 in PostgreSQL mode; vendor-specific SQL lives in a
  `postgresql/` migration flavour and is exercised by a Testcontainers test
  (ADR-008).

## Known gaps

- No cross-service consistency checker; drift between `user_cache` copies
  and user-service is only detected when a name renders wrong.
