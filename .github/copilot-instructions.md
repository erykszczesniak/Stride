# Copilot instructions

Read `AGENTS.md` at the repo root first — it has the map, the commands, the
invariants and the definition of done. This file only adds what Copilot needs
to get the mechanics right.

- Propose changes as one PR per concern, titled as a Conventional Commit.
- Before writing an alert rule, dashboard panel or PromQL, check the metric
  exists: `curl -s localhost:9090/api/v1/label/__name__/values` on the running
  stack, or grep `monitoring/alerts/tests/` for a fixture that already uses it.
- Never add a dependency to `pom.xml` without a one-line comment saying what
  it is for.
- Java files use Lombok; the Kotlin files under `src/main/kotlin` do not.
- When a task touches `monitoring/`, `k8s/` or Flyway migrations, the
  path-specific instructions in `.github/instructions/` apply on top of this
  file.
- Always run the narrowest verification that proves the change
  (`./mvnw -q test -Dtest=...`, `promtool test rules`, `npm test`) and paste
  the result into the PR's Verification section.
