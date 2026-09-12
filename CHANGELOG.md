# Changelog

All notable changes to the `microservices` branch. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[SemVer](https://semver.org/) and are cut by release-please from
Conventional Commit PR titles.

## [Unreleased]

### Added
- Repository standards: `CODEOWNERS`, PR template with a mandatory
  verification section, Dependabot (Maven, npm, Actions, Docker), release-please,
  `AGENTS.md` and per-path Copilot instructions.

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
