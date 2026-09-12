# Changelog

All notable changes to the `microservices` branch. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[SemVer](https://semver.org/) and are cut by release-please from
Conventional Commit PR titles.

## [Unreleased]

### Added
- SLOs as code (`docs/slo.md`): edge availability 99.5 %, edge latency 95 %
  under 500 ms, per-service availability 99.5 %, AI availability 98 %;
  recording rules, multi-window burn-rate alerts (14.4× pages, 6× tickets),
  `SLOErrorBudgetExhausted`, promtool tests and the `stride-slo` Grafana
  dashboard with error-budget panels.
- Alerts as code: 30 Prometheus rules in five groups (platform, services,
  kafka, ai, self-observability) with promtool unit tests, a label/runbook
  contract enforced in CI, Alertmanager routing with inhibition, an alert
  echo receiver in compose, latency histograms enabled for every service,
  and the JVM dashboard fixed for the microservices `app` label.
- CI quality gates: JaCoCo per-module coverage floor (60 % lines / 40 % branches,
  lowest module today 68 % / 56 %), ArchUnit rules in activity-service and
  ai-service, a Testcontainers test running the production PostgreSQL
  migration set, frontend unit tests in CI, CodeQL and Trivy (filesystem +
  image) workflows.
- Repository standards: `CODEOWNERS`, PR template with a mandatory
  verification section, Dependabot (Maven, npm, Actions, Docker), release-please,
  `AGENTS.md` and per-path Copilot instructions.
- Runbooks for every alert family (`docs/runbooks/`, 13 documents with
  front matter and step-by-step investigation usable by people and agents)
  and two post-mortems reconstructed from #145 and #151 (`docs/postmortems/`).

## [1.0.0] - 2026-08-17

Baseline for the changelog: full product parity with the `main` monolith and
the completed gRPC migration.

### Added
- AI coach sees the athlete's recent training history via a new
  `ListRecentActivities` RPC; chat degrades gracefully when activity-service is
  down (#154).
- gRPC inside the cluster: typed contracts in `shared/grpc-contracts`, servers
  in user/activity/social, clients with per-call deadlines; REST `/internal`
  removed (#147–#150).
- Two BFFs: `web-bff` (dashboard composition, report GraphQL, CSV export) and
  `pipeline-service` (iOS facade with `tri_` personal tokens) (#142, #143).
- Full-stack wiring: compose, k8s manifests, CI on `microservices`, E2E
  verification on the 24-container stack (#145).

### Changed
- Memory limits for every JVM container; observability and search stacks are
  opt-in compose profiles (#151).

### Fixed
- Serpentine Swim sample route no longer crosses the Hyde Park lawns (#152).

[Unreleased]: https://github.com/erykszczesniak/Stride/compare/v1.0.0...microservices
[1.0.0]: https://github.com/erykszczesniak/Stride/tree/microservices
