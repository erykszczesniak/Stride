# Copilot instructions

Read `AGENTS.md` at the repo root first — it has the map, the commands, the
invariants and the definition of done. This file only adds what Copilot needs
to get the mechanics right.

- Propose changes as one PR per concern, titled as a Conventional Commit.
- Before writing an alert rule, dashboard panel or PromQL, check the metric
  exists: `curl -s localhost:9090/api/v1/label/__name__/values` on the composed
  stack, or grep `monitoring/alerts/tests/` for a fixture that already uses it.
- Never add a dependency to a service pom without a one-line comment saying
  what it is for (see existing poms for the style).
- For Kotlin services do not introduce Lombok; for Java services do not
  introduce Kotlin.
- When a task touches `monitoring/`, `k8s/` or Flyway migrations, the
  path-specific instructions in `.github/instructions/` apply on top of this
  file.
- Always run the narrowest verification that proves the change
  (`./mvnw -pl <module> -am verify`, `promtool test rules`, `npm test`) and
  paste the result into the PR's Verification section.
