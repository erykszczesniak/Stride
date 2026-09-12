---
applyTo: "services/**/db/migration/**"
---

# Flyway migrations

- Never edit a migration that has been merged. Add a new `V<n>__<snake_case>.sql`; `n` is the next integer in that service's `common/` directory.
- `common/` must run on both PostgreSQL and H2 in PostgreSQL mode. Vendor-specific statements (identity restarts, `ON CONFLICT` variants, JSONB operators) go to `postgres/` and `h2/` siblings and are selected by `spring.flyway.locations`.
- Data fixes must be idempotent: content-keyed `UPDATE ... WHERE <old value>` rather than by id, so a re-run on an already-fixed database is a no-op (see `V9__fix_serpentine_swim_route.sql` in activity-service).
- Schema changes that a service reads in the same deploy must be backward compatible for one release: add column → deploy code → backfill → drop old column, in separate PRs.
- Every migration is validated by `hibernate.ddl-auto: validate` at boot and by the repository tests on H2; a Postgres-only migration also needs a Testcontainers test.
- Prod DDL never lives in `application.yml`; only in migrations.
