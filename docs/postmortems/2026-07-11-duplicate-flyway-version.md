# Two PRs shipped Flyway V16; the backend could not boot against Postgres

**Date:** 2026-06-17 (introduced) → 2026-07-11 (fixed) · **Duration:** 24 days on `main` · **Severity:** would have been critical in prod (the one deployable does not start); no deployed users affected · **Author:** platform · **Status:** reviewed

Source of record: commit `9d4b165` (PR #113, `fix/duplicate-v16-migration`),
which renames `src/main/resources/db/migration/V16__add_ai_chat.sql` to
`V17__add_ai_chat.sql` and changes nothing else. Second occurrence of the
same failure: `6931e5e` (2026-04-21) resolved a triple collision on V12
between PRs #99, #100 and #102.

## Summary

PR #111 (`e5b3da4`, CQRS feed read model) and PR #112 (`13e9443`, AI
activity chat) were developed in parallel and merged on the same day,
2026-06-17. Each added the next free migration number at the time it was
written: `V16__add_user_feed_read_model.sql` and `V16__add_ai_chat.sql`.
Both files were valid, neither PR conflicted with the other in git, and CI
was green for both — the test profiles run with `spring.flyway.enabled=false`
(`application-test.yml`, `application-test.properties`,
`application-testcontainers.yml`) and let Hibernate `create-drop` the schema.
The first process to start the jar against a real database with Flyway on
(compose, k8s, or `make dev-backend` against Postgres) got
`FlywayException: Found more than one migration with version 16` and exited.

## Impact

- From 2026-06-17 to 2026-07-11 no build of `main` could start against
  PostgreSQL with Flyway enabled: `docker compose up` produced a backend in
  a restart loop, the frontend and Prometheus (both `depends_on` the backend
  being healthy) never came up.
- Unit and integration tests, CodeQL and the Docker image build were green
  the whole time.
- No production deployment existed for this branch; the cost was
  developer time and a broken composed stack.

## Timeline

| When | Event |
|---|---|
| 2026-04-21 | `6931e5e`: three PRs (#99, #100, #102) had each claimed V12; renamed to V12/V13/V14. The README gained a "Flyway migration order" note and a `flyway repair` hint |
| 2026-06-17 | PR #111 merged with `V16__add_user_feed_read_model.sql` |
| 2026-06-17 | PR #112 merged with `V16__add_ai_chat.sql` — git sees two new files, no conflict |
| 2026-06-17 → 2026-07-11 | `main` does not boot against Postgres. How many people hit it, and when, is not recorded |
| 2026-07-11 | `9d4b165`: the later-merged migration renamed to V17. One-line change |

## Root cause

Migration version numbers are allocated by hand at authoring time, and two
authors working from the same base pick the same number. Nothing in the
merge or the build checks uniqueness: git only conflicts on identical
paths, and every automated test disables Flyway. The failure surfaces only
where the jar meets a real database.

Contributing: the same thing had happened seven weeks earlier (`6931e5e`)
and the fix then was to rename the files and document the symptom, not to
add a check.

## What went well

- Flyway failed closed: a boot with an ambiguous migration order is
  refused, not applied. Both times the fix was a rename with no SQL edits
  and no history repair.
- The commit messages record the exact error text, so the runbook branch
  below is copy-paste searchable.

## What went badly

- Twenty-four days with an unbootable `main`. Green CI hid it because the
  tests never run the migration chain.
- The first occurrence was treated as a one-off.

## Where we got lucky

- The collision was between two *new* files. A collision that renumbered an
  already-applied migration on a long-lived database would have needed
  `flyway repair` on every environment.

## Actions

| Action | Owner | Status |
|---|---|---|
| Invariant in `AGENTS.md`: Flyway is append-only, new `V<n>__*.sql` only, H2 and PostgreSQL must both apply it | platform | done (`25f43ef`) |
| Runbook branch for `Found more than one migration with version N` in [service-down.md](../runbooks/service-down.md): rename the later file, never edit history | platform | done |
| Run Flyway in the `testcontainers` profile (today `spring.flyway.enabled=false` with `create-drop`) so `./mvnw verify` fails on a duplicate or invalid migration | domain | open |
| CI step that fails on duplicate `V<n>` prefixes under `src/main/resources/db/migration/` (a shell one-liner in `ci.yml`) | platform | open |
| Boot the jar against Postgres in CI (compose smoke) so "does not start" is a red check, not a Slack message | platform | open |

## Lessons for the runbooks / rules

- A green build says nothing about migrations while the tests skip Flyway.
  `ServiceDown` is the alert that catches this class; its runbook now lists
  the exact log line.
- Renumbering is safe only for migrations that have never been applied
  anywhere. Check `flyway_schema_history` on every long-lived database
  before renaming a file that already ran there.
