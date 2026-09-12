---
applyTo: "src/main/resources/db/migration/**"
---

# Flyway migrations

- Never edit a migration that has been merged. Add a new `V<n>__<snake_case>.sql`; `n` is the next integer in that service's `common/` directory.
- `common/` must run on both PostgreSQL and H2 in PostgreSQL mode. Vendor-specific statements (identity restarts, `ON CONFLICT` variants, JSONB operators) go to `postgres/` and `h2/` siblings and are selected by `spring.flyway.locations`.
- Data fixes must be idempotent: content-keyed `UPDATE ... WHERE <old value>` rather than by id, so a re-run on an already-fixed database is a no-op (see `V22__fix_serpentine_swim_route.sql`).
- Schema changes that a service reads in the same deploy must be backward compatible for one release: add column → deploy code → backfill → drop old column, in separate PRs.
- Every migration is validated by `hibernate.ddl-auto: validate` at boot and by the repository tests on H2 and by the Testcontainers integration tests (`AbstractIntegrationTest`).
- Prod DDL never lives in `application.yml`; only in migrations.
