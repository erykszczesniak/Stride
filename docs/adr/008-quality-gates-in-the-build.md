# ADR-008 — Coverage floors, ArchUnit rules and a real-database test in `verify`

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-09-12 (#158) |
| **Deciders** | platform |

## Context

Nine hundred tests ran in CI and none of them said whether a module's
coverage had dropped, whether a controller had started calling a repository
directly, or whether the PostgreSQL-only migrations actually applied. The
first ArchUnit run found exactly that: `AdminActivityController` bypassing
the service layer.

## Decision

`./mvnw verify` is the gate:

- **JaCoCo** with a reactor-wide floor (60 % lines, 40 % branches) checked
  per module; modules without executable logic opt out explicitly with a
  comment. The floor only moves up.
- **ArchUnit** rules in the two modules with the most structure
  (activity-service, ai-service): persistence only in `domain`, controllers
  never touch repositories, transports never leak into the domain, provider
  SDKs only in `client/`. Violations are fixed, not frozen — the one found
  was a five-line refactor.
- **Testcontainers** for what H2 cannot prove: the `postgresql/` migration
  flavour and identity sequences run on `postgres:16` in CI; skipped
  without Docker so a laptop build still works.
- Frontend Jest tests, CodeQL and Trivy run alongside.

## Alternatives considered

- **Coverage as a report only** — informative, ignored within a month.
- **A single global coverage number** — hides a module at 20 % behind one
  at 95 %.
- **Freezing existing ArchUnit violations** (`FreezingArchRule`) — the
  right tool for a large legacy codebase; here the violation count was one.
- **Postgres for every test** — 5–10× slower suite; H2 in PostgreSQL mode
  plus one container test per Postgres-specific feature is the trade-off.

## Consequences

- A PR that lowers a module below the floor fails; the number to raise is
  in the root pom with a comment saying why.
- Package boundaries are documented as executable rules, which is also how
  new contributors learn them.
- Testcontainers pinned above the Spring Boot BOM (Docker Engine 29 API
  version); the dependency comment records the reason.

## Known gaps

- The floors are below where the modules are (68–94 %); they catch
  regressions, not complacency. Raise them module by module.
- ArchUnit covers two of nine services.
