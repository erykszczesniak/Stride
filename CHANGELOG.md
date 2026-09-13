# Changelog

All notable changes to the `main` branch (the Spring Boot monolith). Format
follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions
follow [SemVer](https://semver.org/) and are cut by release-please from
Conventional Commit PR titles. The `microservices` branch keeps its own
changelog.

## [Unreleased]

### Fixed
- Frontend: `jspdf` 2.5 → 4.2 and `jspdf-autotable` 3.8 → 5.0 (CVE-2025-68428,
  CVE-2026-31938), `shell-quote` and `websocket-driver` bumped in the lockfile
  (CVE-2026-9277, CVE-2026-54466); the training-plan PDF export calls
  `autoTable(doc, …)` like the other pages. Trivy: 0 fixable CRITICAL.
- First real GitHub Actions run: `aquasecurity/trivy-action` pinned to an existing
  tag (`v0.36.0`), promtool globs expanded inside the container in the Monitoring
  workflow, and the Elasticsearch integration test loads the context with the MVC
  infrastructure the security filter chains require (it only ever ran where Docker
  was available, i.e. on the runner).
### Changed
- ADR 006 and 007 cite the current commit of the alerts-as-code change
  (`98f7b97`); the previous SHA no longer exists on `main` after the history
  rewrite of 12 September.

### Added
- README: table of contents, `Operations & quality` overview with the local
  verification commands, alerting section, `verify`-based test instructions and a
  documentation index.
- Documentation: 9 runbooks (one per alert), post-mortem index and the
  2026-07-11 duplicate-Flyway-version write-up, 10 ADRs, SLO, cost-attribution,
  AI-guardrails and observability docs for the monolith.
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
