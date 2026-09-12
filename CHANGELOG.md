# Changelog

All notable changes to the `main` branch (the Spring Boot monolith). Format
follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions
follow [SemVer](https://semver.org/) and are cut by release-please from
Conventional Commit PR titles. The `microservices` branch keeps its own
changelog.

## [Unreleased]

### Added
- `scripts/smoke.sh` (API, one request per feature area, AI guard, Prometheus
  target and rules, Watchdog, no critical alert) and a nightly Playwright e2e
  workflow on the composed stack; `make smoke` / `make e2e-up`.
- AI guardrails and telemetry: per-user rate limits (429), prompt guard (400),
  PII masking before text leaves the app, cost/tokens/latency per provider call
  with an `ai.audit` trail, Anthropic→Groq chat failover, golden-set and live evals.
- Alerts as code: 30 unit-tested Prometheus rules (availability, per-endpoint
  errors/latency, JVM, DB pool, SLO burn-rate, AI provider, cost, self-observability),
  Alertmanager routing with inhibition, JVM / SLO / cost Grafana dashboards,
  request-latency histograms and team/cost_center tags on every meter.
- Quality gates in the build: JaCoCo coverage floor (60% line / 40% branch),
  ArchUnit layer rules with a frozen store for the three legacy controllers,
  frontend Jest job in CI, CodeQL and Trivy scans.
- Repository standards: `CODEOWNERS`, PR template with a mandatory
  verification section, Dependabot (Maven, npm, Actions, Docker), release-please,
  `AGENTS.md` and per-path Copilot instructions.

## [1.0.0] - 2026-08-17

Baseline for the changelog: the monolith as of the last merged feature PR
(#139), with the React SPA, the iOS `/pipeline/v1` client, AI coach chat
(Anthropic) and goal/plan generation (Groq), Elasticsearch user search,
Testcontainers integration tests and the observability stack (Prometheus,
Grafana, Jaeger).

[Unreleased]: https://github.com/erykszczesniak/Stride/compare/v1.0.0...main
[1.0.0]: https://github.com/erykszczesniak/Stride/tree/main
