---
applyTo: "src/main/resources/db/migration/**"
---

# Flyway migrations

- Never edit a migration that has been merged. Add a new `V<n>__<snake_case>.sql`; `n` is the next integer in `src/main/resources/db/migration/` **on `main` at merge time** — two PRs claiming the same number is how post-mortem `2026-07-11-duplicate-flyway-version` happened. Check `git log main -- src/main/resources/db/migration` before picking `n`.
- One `db/migration` directory, PostgreSQL only (`spring.flyway.locations: classpath:db/migration`). H2 is used by the unit-test profile with `spring.flyway.enabled=false` and `ddl-auto: create-drop`, so a migration is **not** exercised by `./mvnw test`; it runs at boot against Postgres (`hibernate.ddl-auto: validate` then rejects any drift between entities and schema). Boot the compose stack (`make up`, then `docker compose logs backend | grep -i flyway`) before merging a schema change.
- Data fixes must be idempotent: content-keyed `UPDATE ... WHERE <old value>` rather than by id, so a re-run on an already-fixed database is a no-op.
- Schema changes the running code reads in the same deploy must be backward compatible for one release: add column → deploy code → backfill → drop old column, in separate PRs.
- Prod DDL never lives in `application.yml`; only in migrations. `baseline-on-migrate: true` is for existing databases, not a reason to skip a migration.
