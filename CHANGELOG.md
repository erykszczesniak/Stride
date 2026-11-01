# Changelog

All notable changes to the `microservices` branch. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[SemVer](https://semver.org/) and are cut by release-please from
Conventional Commit PR titles.

## [1.1.0](https://github.com/erykszczesniak/Stride/compare/v1.0.0...v1.1.0) (2026-11-01)


### Added

* **activity-details:** add per-kilometre splits table ([8434286](https://github.com/erykszczesniak/Stride/commit/84342866562b910037a85d4a37caef32a4b27adb))
* **activity-details:** add per-kilometre splits table ([a9b0879](https://github.com/erykszczesniak/Stride/commit/a9b0879c1520f154e27ad68ca9d0bd31259b537f))
* **activity:** seed eleven weeks of sample activity history ([928041e](https://github.com/erykszczesniak/Stride/commit/928041e6c05db1456713255c8c9d2efd68cb85b3))
* add GPX/FIT/TCX activity import with sample files ([#71](https://github.com/erykszczesniak/Stride/issues/71)) ([cac0097](https://github.com/erykszczesniak/Stride/commit/cac009799a9bf8d78d0acd11f977332431853a2a))
* add personal records detection for standard distances ([#72](https://github.com/erykszczesniak/Stride/issues/72)) ([d4577e0](https://github.com/erykszczesniak/Stride/commit/d4577e09f4231d70c2e331c3ec92d3e207447d5a))
* **ai:** chat failover Anthropic → Groq, breaker on Anthropic, live rubric evals ([6b025b2](https://github.com/erykszczesniak/Stride/commit/6b025b2c4730baeef3d0ed0fd0e266c84a82ee09))
* **ai:** guardrails — per-user rate limit, prompt guard, PII masking, token/cost telemetry and audit log ([d0d7d45](https://github.com/erykszczesniak/Stride/commit/d0d7d45232a329eb0f0afc8eaeb93b25b6a3add9))
* **auth:** add "Continue with Strava" OAuth2 login provider ([#107](https://github.com/erykszczesniak/Stride/issues/107)) ([acaf513](https://github.com/erykszczesniak/Stride/commit/acaf51362dc9a5d9927af8457c66fbab76a22be8))
* compute real per-km splits from imported GPS timestamps ([b8b134c](https://github.com/erykszczesniak/Stride/commit/b8b134ceb371459fa898c50d3f5694eb1a47190c))
* elevation profile chart on activity detail page ([f1c9e13](https://github.com/erykszczesniak/Stride/commit/f1c9e13b8411fc5e918549100edb59f13832d982))
* elevation profile chart on activity detail page ([40fa43d](https://github.com/erykszczesniak/Stride/commit/40fa43d117777d5330f73259b1ae9a3069146a41))
* **frontend:** add Personal Records section to profile page ([#74](https://github.com/erykszczesniak/Stride/issues/74)) ([f166d26](https://github.com/erykszczesniak/Stride/commit/f166d2664a1967086629623b1150db67bd1a7f58))
* **frontend:** file import UI on Record Activity page ([#73](https://github.com/erykszczesniak/Stride/issues/73)) ([96e88d1](https://github.com/erykszczesniak/Stride/commit/96e88d1e7b102b7ac03b594b0fd7ef53173ff1a0))
* **gear:** add shoe/bike/gear tracking with per-item mileage totals ([a5d09bb](https://github.com/erykszczesniak/Stride/commit/a5d09bb7a063a85d4384e5f344a950c00d497487))
* **gear:** shoe/bike/gear tracking with per-item mileage ([49b63e6](https://github.com/erykszczesniak/Stride/commit/49b63e607871fd9f4f4c326dba294f11677c1adf))
* **i18n:** full frontend internationalization with 6 languages ([#79](https://github.com/erykszczesniak/Stride/issues/79)) ([22485fb](https://github.com/erykszczesniak/Stride/commit/22485fbf084cc92f39f9c5ee699aa2202c0bf70a))
* **microservices:** Phase 1 — Config Server, Eureka, API Gateway ([899b71b](https://github.com/erykszczesniak/Stride/commit/899b71bcf6c90e8557c385a57eac876f7bb47783))
* **microservices:** Phase 10 — sync frontend with main (web features from PRs [#110](https://github.com/erykszczesniak/Stride/issues/110)-[#139](https://github.com/erykszczesniak/Stride/issues/139)) ([a91f0a9](https://github.com/erykszczesniak/Stride/commit/a91f0a9b847879391d4fdbbd7c974ad6bb31dbb0))
* **microservices:** Phase 11 — shared events v2 + gateway /api/v1 contract ([4ef85b5](https://github.com/erykszczesniak/Stride/commit/4ef85b54f1506f45a765b60571c900c095b115b1))
* **microservices:** Phase 12 — user-service full parity with main ([646a386](https://github.com/erykszczesniak/Stride/commit/646a38622045627b5cb9b2ab1bd4ffd923e65544))
* **microservices:** Phase 13 — activity-service core parity with main ([306b3e1](https://github.com/erykszczesniak/Stride/commit/306b3e18cc00a26c1dc467cd39fa00218aa3d99f))
* **microservices:** Phase 17 — notification-service parity with main ([4d7a526](https://github.com/erykszczesniak/Stride/commit/4d7a52688154221dff11941ed2db3689b904b05f))
* **microservices:** Phase 18 — challenge-goal + club-event parity with main ([db2a66c](https://github.com/erykszczesniak/Stride/commit/db2a66c6723230698612e100970da9d49f733d59))
* **microservices:** Phase 2 — user-service ([1d3b9d1](https://github.com/erykszczesniak/Stride/commit/1d3b9d110406c2f17dcc127ab80dc0f420453ded))
* **microservices:** Phase 3 — activity-service + extract auth filter to shared ([731ce72](https://github.com/erykszczesniak/Stride/commit/731ce726138836c79e02e4c2dfa34b9ec4f961fe))
* **microservices:** Phase 4 — notification-service in Kotlin ([93a9a90](https://github.com/erykszczesniak/Stride/commit/93a9a9060328fcb97cfdbdff51d1ac44933f4046))
* **microservices:** Phase 5 — social-service (CQRS-light) ([ae26987](https://github.com/erykszczesniak/Stride/commit/ae26987d46a5490779030dc51d03e75f5fbce70d))
* **microservices:** Phase 6 — club-event-service (combined service) ([86f2ce5](https://github.com/erykszczesniak/Stride/commit/86f2ce58f191a61926a8667b0e7688476da7fbb2))
* **microservices:** Phase 7 — challenge-goal-service + Resilience4j + Redis cache ([ecba2b0](https://github.com/erykszczesniak/Stride/commit/ecba2b0926e0c6d41cbc53c6dfd6a8357b1763cd))
* **microservices:** Phase 8 — ai-service (Kotlin, Groq, Resilience4j on external API) ([7239306](https://github.com/erykszczesniak/Stride/commit/723930658b77e2e86968267ca8ba3dd545a9e0f8))
* **microservices:** Phase 9 — K8s, observability, AWS Terraform, docs ([1a035af](https://github.com/erykszczesniak/Stride/commit/1a035afab695130aad1a2643a9146835ba201e25))
* **monitoring:** alerts as code — 30 Prometheus rules with unit tests, Alertmanager routing, rule contract in CI ([9508976](https://github.com/erykszczesniak/Stride/commit/95089765c5b9fb03c62e4e4f21d10b9b90d304f7))
* **monitoring:** cost attribution — team/cost_center on every metric, modelled infra cost, real AI spend, alerts and dashboard ([10d3383](https://github.com/erykszczesniak/Stride/commit/10d338378a4c2b52a83166bbe73d8cc3dac0fd79))
* **monitoring:** SLOs as code — recording rules, multi-window burn-rate alerts, error-budget dashboard ([f3beffb](https://github.com/erykszczesniak/Stride/commit/f3beffb4f61345f3bac0700e21e9c8f449051435))
* **observability:** add OpenTelemetry distributed tracing exported to Jaeger ([169dcd3](https://github.com/erykszczesniak/Stride/commit/169dcd386fc92ce65de125795a57682eec7287ca))
* **observability:** OpenTelemetry distributed tracing with Jaeger ([63a6600](https://github.com/erykszczesniak/Stride/commit/63a660027f01069e378f67c696e6d19a2f20205a))
* **pr:** sport-specific PR distances — add 50/100/200/400m + 3km for Swimming ([#96](https://github.com/erykszczesniak/Stride/issues/96)) ([2a225f8](https://github.com/erykszczesniak/Stride/commit/2a225f85c5a2bc37097861da709ae283270bf6c5))
* real per-km splits computed from GPS timestamps ([8a861f6](https://github.com/erykszczesniak/Stride/commit/8a861f6eafce67fca5413d877c23942a1298f2d3))
* respect units toggle for Weight and Height on Profile tab ([#94](https://github.com/erykszczesniak/Stride/issues/94)) ([ef65ca1](https://github.com/erykszczesniak/Stride/commit/ef65ca1c72d1a9000fd9d4539f5ede3f7f18bf70))
* **search:** Elasticsearch-backed full-text user search ([188bfdb](https://github.com/erykszczesniak/Stride/commit/188bfdbda1e01bd9171ef4f6fdc01077adb6b659))
* **search:** Elasticsearch-backed full-text user search ([e68d777](https://github.com/erykszczesniak/Stride/commit/e68d777a3e40c6113dd71d7c2a3c74b48ce73a7f))
* **strava:** fetch activity streams for GPS, timestamps, and elevation ([#108](https://github.com/erykszczesniak/Stride/issues/108)) ([6d1cde5](https://github.com/erykszczesniak/Stride/commit/6d1cde544846c9d3f93783f6bdddd843a73c6906))
* **strava:** OAuth2 integration and async activity import ([280619f](https://github.com/erykszczesniak/Stride/commit/280619f57e5c216a1f3257df1da061733d4cf0a0))
* **strava:** OAuth2 integration and async activity import ([3cf2471](https://github.com/erykszczesniak/Stride/commit/3cf2471abb9abccd724a2f3ad76518b47696ff44))
* wire privacy settings to backend ([#85](https://github.com/erykszczesniak/Stride/issues/85)) ([bb41afd](https://github.com/erykszczesniak/Stride/commit/bb41afdb511b433188497dffff916ccb96656cb3))


### Fixed

* **ci:** smoke test — URL-encode the Prometheus query ([b55ee81](https://github.com/erykszczesniak/Stride/commit/b55ee81a5caa8c35d403ab5d6fac0bd424b50783))
* **dev:** silence OTel OTLP export retries when Jaeger is not running ([c74a60c](https://github.com/erykszczesniak/Stride/commit/c74a60cd00d9df1113b1a5487a79069fe6511cb0))
* duration with seconds + pace display for running ([#75](https://github.com/erykszczesniak/Stride/issues/75)) ([c0c980c](https://github.com/erykszczesniak/Stride/commit/c0c980c4e4d65919c0f946ff3d4c4f93fa1ab84c))
* **flyway:** resolve V12 triple-collision — rename gear to V13, strava to V14 ([6931e5e](https://github.com/erykszczesniak/Stride/commit/6931e5ea9ccbb5e2be96dcfa05e3cc66bc8e5288))
* **frontend:** patch the fixable CRITICAL findings Trivy reports ([acf2b2f](https://github.com/erykszczesniak/Stride/commit/acf2b2fb51d8bf84ad090e2c81e4fd23c8b526c9))
* **i18n:** make English the default for new users regardless of browser locale ([#84](https://github.com/erykszczesniak/Stride/issues/84)) ([5d007ef](https://github.com/erykszczesniak/Stride/commit/5d007effb475ac81b5525ec3bfc93871402e3e92))
* **import:** make FIT parser actually parse real device files ([#76](https://github.com/erykszczesniak/Stride/issues/76)) ([eb11248](https://github.com/erykszczesniak/Stride/commit/eb112481e70a17b5421624d12619c8bd2a11caa6))
* **navbar:** hide add-activity and notifications on the Settings page ([afcbd07](https://github.com/erykszczesniak/Stride/commit/afcbd074dfd7e52c02ad1d9c2740f06d311bd765))
* **navbar:** show search on mobile and space the icons properly ([b011436](https://github.com/erykszczesniak/Stride/commit/b011436ca757d170e38eaab9b54317d7eb91120b))
* **navbar:** strip add-activity + notifications on the Settings page ([9b77960](https://github.com/erykszczesniak/Stride/commit/9b77960dd6b254fb462ced0a7f35aab2bd69ec84))
* **personal-records:** make recalculate not blow up on unique constraint ([#77](https://github.com/erykszczesniak/Stride/issues/77)) ([6049fbe](https://github.com/erykszczesniak/Stride/commit/6049fbe6adf81787df7cc707051488be5a9bc0fc))
* **personal-records:** polyline format detection + samples + tests ([#78](https://github.com/erykszczesniak/Stride/issues/78)) ([fb38874](https://github.com/erykszczesniak/Stride/commit/fb3887402008ee28e89f6b9f1fde17a9c1e95b1d))
* **privacy:** missing follow-ups — PR endpoint privacy, locked screen, auto-refresh ([#87](https://github.com/erykszczesniak/Stride/issues/87)) ([4f3c88f](https://github.com/erykszczesniak/Stride/commit/4f3c88f90421288478b6372ff4b7c525d15b3692))
* **pr:** skip non-distance sports and fix swim/ride pace labels ([#92](https://github.com/erykszczesniak/Stride/issues/92)) ([e400149](https://github.com/erykszczesniak/Stride/commit/e4001496d07072fc5bf1a0c75c3b7c9cf8479370))
* **pr:** stop the GPS sliding window from inventing impossibly-fast PRs ([#95](https://github.com/erykszczesniak/Stride/issues/95)) ([69df508](https://github.com/erykszczesniak/Stride/commit/69df5086bba33d99cef4dbbf4fea561a506804b8))
* **record-activity:** stop the form from overflowing on mobile ([#93](https://github.com/erykszczesniak/Stride/issues/93)) ([96cbc77](https://github.com/erykszczesniak/Stride/commit/96cbc7797bce54c0f568be2cafa2f3e066e2d6f2))
* remove obsolete Pace/Speed Display dropdown from preferences ([#83](https://github.com/erykszczesniak/Stride/issues/83)) ([98957b2](https://github.com/erykszczesniak/Stride/commit/98957b2b37bc7096ad930c43fdf29d78c30174eb))
* **search:** make the search modal resizable ([aebf42a](https://github.com/erykszczesniak/Stride/commit/aebf42ac8ea9136998e41283045b2ed97feb793b))
* **search:** make the search modal resizable by the user ([9199126](https://github.com/erykszczesniak/Stride/commit/91991262b167854e8c67963caf17f766a672a1b3))
* **settings:** stop the layout from overflowing on mobile ([94b6eda](https://github.com/erykszczesniak/Stride/commit/94b6eda121fb200b3cc33eb0abb295b30dcfc642))
* **settings:** stop the layout from overflowing on mobile ([e1e8ccd](https://github.com/erykszczesniak/Stride/commit/e1e8ccdbd3b8c7ea5d505f4d13c32fd2e6ec6429))
* show lifetime stats on profile and display avatar everywhere ([#109](https://github.com/erykszczesniak/Stride/issues/109)) ([5395bd7](https://github.com/erykszczesniak/Stride/commit/5395bd70a9029069f6fa9b95bee2fa719125d887))
* **splits:** only show the Splits section on distance-based activities ([#105](https://github.com/erykszczesniak/Stride/issues/105)) ([1e4f2eb](https://github.com/erykszczesniak/Stride/commit/1e4f2eb4975c3af7535926de617803a991b94341))


### Changed

* **i18n:** move DistanceType display names from backend enum to frontend locales ([#104](https://github.com/erykszczesniak/Stride/issues/104)) ([4c7d3d3](https://github.com/erykszczesniak/Stride/commit/4c7d3d3b8e7317169a24f55742705b4dddba5625))
* make GraphQL controllers extend BaseController ([#80](https://github.com/erykszczesniak/Stride/issues/80)) ([8bfa198](https://github.com/erykszczesniak/Stride/commit/8bfa198255f2848b322c3fbdc574e2d3ccdcd338))
* port PersonalRecordService to Kotlin ([45ed001](https://github.com/erykszczesniak/Stride/commit/45ed001dbf9806f6829eb8c4cd60092381174922))
* port PersonalRecordService to Kotlin ([3108e18](https://github.com/erykszczesniak/Stride/commit/3108e18aa65658989509b40d840528994c14256e))


### Documentation

* add "Recent Features — Configuration & Testing" section to README ([12c6e1c](https://github.com/erykszczesniak/Stride/commit/12c6e1cc2c45e103fc4bd0e717988877503f1f03))
* **agents:** no tool names in the instructions header ([31c7bbf](https://github.com/erykszczesniak/Stride/commit/31c7bbf8b7a23596c596ad3406ac78d6fce182f1))
* architecture decision records ([2fbbb93](https://github.com/erykszczesniak/Stride/commit/2fbbb93a93c339c77787fcbea174b00f512d8a37))
* Document Jackson boolean field serialization issue ([b09672d](https://github.com/erykszczesniak/Stride/commit/b09672d267f75b31f6e4459ddf699defc24b7989))
* README table of contents, Operations & quality section, documentation index ([6d95508](https://github.com/erykszczesniak/Stride/commit/6d9550876a185a9aa46dcb67d25768b883f70882))
* runbooks for every alert family and two post-mortems ([ab13262](https://github.com/erykszczesniak/Stride/commit/ab13262f6232f35871d9f57f08c5dcf4dcef7e91))
* simplify microservices README ([#184](https://github.com/erykszczesniak/Stride/issues/184)) ([0af5cd5](https://github.com/erykszczesniak/Stride/commit/0af5cd5933db1c03c5fd8b32efc3b261e659b2cf))

## [Unreleased]

### Security
- `JWT_SECRET` has no built-in fallback any more: Config Server (`api-gateway`,
  `user-service`, `pipeline-service`) and `docker-compose.yml` require it,
  the same way `main` does (`cp .env.example .env` sets a dev value). The
  `standalone` profile keeps its explicit dev secret.
- Strava OAuth `state` is no longer signed with the literal `changeme` when
  neither `STRAVA_STATE_SECRET` nor `jwt.secret` is set; user-service and
  activity-service answer 503 "Strava integration is not configured" instead
  of issuing a forgeable state.
- `/actuator/env` is no longer exposed by the shared actuator config; the
  dashboards and alerts only read `/actuator/prometheus`.
- `.gitignore` covers key material (`*.pem`, `*.key`, `*.p12`, `*.jks`) and
  Terraform state.

### Fixed
- club-event-service: `GET /api/v1/clubs` failed with `LazyInitializationException`
  (`Club.memberIds` read after the read-only transaction) and surfaced as a
  403; the collection is now loaded eagerly in batches.
- shared-security: `HeaderAuthenticationFilter` also runs on the ERROR
  dispatch, so a 405 or 500 of an authenticated call is reported as such
  instead of an anonymous 403 on `/error`.
- `scripts/smoke.sh`: checks `/api/v1/activities/my` (the public contract has
  no `GET /api/v1/activities`) and takes `SMOKE_ACTIVITY` (default 7, owned by
  the smoke user) for the AI chat, as the `main` script does.
- Frontend: `dompurify` 3.4.16 → 3.4.17 (GHSA-cg2g-xh64-v4q7, SAFE_FOR_TEMPLATES
  bypass), a transitive dependency of `jspdf`; the fixable CRITICAL Trivy
  gate reported on the lockfile.
- Frontend: the fixable CRITICAL findings Trivy reports in the lockfile, the
  same set `main` patched in `bbc96d5`: `jspdf` 2.5 → 4.2 and `jspdf-autotable`
  3.8 → 5.0 (CVE-2025-68428, CVE-2026-31938), `proxy-addr` 2.0.8
  (CVE-2026-90711), `shell-quote` 1.8 → 1.12 (CVE-2026-9277) and
  `websocket-driver` 0.7.5 (CVE-2026-54466). The training-plan PDF export calls
  `autoTable(doc, …)` like the other pages.

### Added
- activity-service: eleven weeks of training history for the four sample
  users (`V10`, 166 manual activities dated relative to the first start, same
  rows as `main`), so the dashboard month cards, the month-over-month trend
  and the 12-week volume chart have data on any day. Seeded personal records
  are unchanged; `ActivityRepositoryTest` recalculates them on H2 to prove it.
- Chat provider failover (Anthropic → Groq on 502/503, open breaker or no
  key) with `ai_failover_total` and the `AiProviderFailoverActive` alert;
  live rubric evals for chat and generation (JUnit tag `live-eval`, weekly
  `AI live eval` workflow, keys required) alongside the deterministic guard
  golden set.
- README: table of contents, an "Operations & quality" section mapping every
  area to its files, a documentation index; `docs/observability.md` gap list
  updated (SLOs and Alertmanager are done).
- `scripts/smoke.sh` (gateway health, login, one request per service, guard
  check, Eureka registrations, Prometheus targets and rule groups, Watchdog in
  Alertmanager, no critical alert firing) and a nightly / on-demand `E2E`
  workflow that boots the composed stack, runs the smoke test and the
  Playwright suite against the SPA.
- AI guardrails in ai-service: per-user rate limit (429), prompt guard
  (400 with reason), PII masking before the provider and before persistence;
  token, latency, cost and failover metrics plus a JSON audit line per
  provider call; a 42-case golden eval that gates the build
  (`docs/ai-guardrails.md`).
- Cost attribution: every metric carries `team` and `cost_center` (set per
  service in Config Server), recording rules model infra USD/hour from CPU
  consumed and memory reserved, AI spend comes from token accounting;
  `InfraCostSpike` and `AiSpendHigh` alerts; `stride-cost` dashboard;
  `docs/cost-attribution.md`.
- Architecture decision records (`docs/adr/`, 9 records): choreographed
  sagas, database per service, gRPC inside / REST at the edge, JWT at the
  gateway, what we deliberately do not run, alerts as code, SLO burn-rate
  alerting, quality gates in the build, AI guardrails.
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
