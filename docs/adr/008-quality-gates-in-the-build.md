# ADR-008 — Coverage floor, ArchUnit rules and a real-database test in `verify`

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-09-12 (`f96266a`) |
| **Deciders** | platform |

## Context

The suite ran in CI and nothing said whether coverage had dropped, whether
a controller had started calling a repository directly, or whether the
frontend tests had run at all (only `npm run build` did). The first
ArchUnit run found three controllers reaching past the service layer.

## Decision

`./mvnw verify` is the gate (`.github/workflows/ci.yml`, job `backend-test`):

- **JaCoCo** with a bundle-wide floor — 60 % lines, 40 % branches
  (`jacoco.min.line`, `jacoco.min.branch` in `pom.xml`, `jacoco:check` in
  `verify`; `StravaApplication` and generated GraphQL classes excluded).
  The floor only moves up.
- **ArchUnit** (`ArchitectureTest`, six rules): entities only in `domain`;
  repositories only in `repository` or `search`; `domain` depends on
  neither web, services, search nor Elasticsearch; controllers end in
  `Controller`; services never read `jakarta.servlet`; controllers do not
  use repositories — this last one is a `FreezingArchRule` with the three
  pre-existing violations (`AdminController`, `ExportController`,
  `SearchController`) recorded in `src/test/resources/archunit_store`. A
  new violation fails the build; fixing a frozen one shrinks the store.
- **Testcontainers** for what H2 cannot prove: `AbstractIntegrationTest`
  on `postgres:16-alpine` (skipped without Docker), pinned to 1.21.3 for the
  Docker Engine 29 API.
- Alongside: frontend Jest job (`frontend-test`), CodeQL (`java-kotlin`,
  `javascript-typescript`, weekly), Trivy filesystem and image scans
  (`security-scan.yml`), and the monitoring gate (ADR-006).

## Alternatives considered

- **Coverage as a report only** — informative, ignored within a month.
- **Fixing the three controllers instead of freezing** — each is a
  refactor of a controller that composes several repositories; freezing
  stops the bleeding today and turns the fixes into tracked debt.
- **Postgres for every test** — 5–10× slower suite; H2 plus one container
  base class is the trade-off (ADR-002).

## Consequences

- A PR that lowers coverage below the floor fails; the number to raise is
  in `pom.xml` with a comment saying why.
- Package boundaries are documented as executable rules, which is also how
  new contributors learn them (`AGENTS.md` invariant 1).
- The frozen store is committed; a run with `freeze.store.default.allowStoreUpdate=true`
  rewrites it when a violation disappears.

## Known gaps

- One module, one floor: it catches regressions, not the untested corners.
- The Testcontainers profile does not run Flyway (ADR-002, post-mortem
  2026-07-11).
