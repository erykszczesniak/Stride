# Changelog

All notable changes to the `main` branch (the Spring Boot monolith). Format
follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions
follow [SemVer](https://semver.org/) and are cut by release-please from
Conventional Commit PR titles. The `microservices` branch keeps its own
changelog.

## [Unreleased]

### Added
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
