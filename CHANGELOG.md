# Changelog

All notable changes to the `main` branch (the Spring Boot monolith). Format
follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions
follow [SemVer](https://semver.org/) and are cut by release-please from
Conventional Commit PR titles. The `microservices` branch keeps its own
changelog.

## [1.1.0](https://github.com/erykszczesniak/Stride/compare/v1.0.0...v1.1.0) (2026-11-01)


### Added

* **activity-details:** add per-kilometre splits table ([8434286](https://github.com/erykszczesniak/Stride/commit/84342866562b910037a85d4a37caef32a4b27adb))
* **activity-details:** add per-kilometre splits table ([a9b0879](https://github.com/erykszczesniak/Stride/commit/a9b0879c1520f154e27ad68ca9d0bd31259b537f))
* add GPX/FIT/TCX activity import with sample files ([#71](https://github.com/erykszczesniak/Stride/issues/71)) ([cac0097](https://github.com/erykszczesniak/Stride/commit/cac009799a9bf8d78d0acd11f977332431853a2a))
* add personal records detection for standard distances ([#72](https://github.com/erykszczesniak/Stride/issues/72)) ([d4577e0](https://github.com/erykszczesniak/Stride/commit/d4577e09f4231d70c2e331c3ec92d3e207447d5a))
* **ai:** guardrails, telemetry and chat failover — rate limit, prompt guard, PII masking, cost per call, Anthropic→Groq ([2cda216](https://github.com/erykszczesniak/Stride/commit/2cda2162aa34e497c1a5cd77b16ba579bdfb3db5))
* **auth:** add "Continue with Strava" OAuth2 login provider ([#107](https://github.com/erykszczesniak/Stride/issues/107)) ([acaf513](https://github.com/erykszczesniak/Stride/commit/acaf51362dc9a5d9927af8457c66fbab76a22be8))
* compute real per-km splits from imported GPS timestamps ([b8b134c](https://github.com/erykszczesniak/Stride/commit/b8b134ceb371459fa898c50d3f5694eb1a47190c))
* **data:** seed eleven weeks of sample activity history ([bba14ba](https://github.com/erykszczesniak/Stride/commit/bba14badf5b55de08f3d90320c2aec90fd12d7f1))
* elevation profile chart on activity detail page ([f1c9e13](https://github.com/erykszczesniak/Stride/commit/f1c9e13b8411fc5e918549100edb59f13832d982))
* elevation profile chart on activity detail page ([40fa43d](https://github.com/erykszczesniak/Stride/commit/40fa43d117777d5330f73259b1ae9a3069146a41))
* **feed:** switch activity feed to CQRS read model with event-driven fan-out ([#111](https://github.com/erykszczesniak/Stride/issues/111)) ([e5b3da4](https://github.com/erykszczesniak/Stride/commit/e5b3da4d4944a4648d93ebb8c118a6646cca7dbe))
* **frontend:** add Personal Records section to profile page ([#74](https://github.com/erykszczesniak/Stride/issues/74)) ([f166d26](https://github.com/erykszczesniak/Stride/commit/f166d2664a1967086629623b1150db67bd1a7f58))
* **frontend:** file import UI on Record Activity page ([#73](https://github.com/erykszczesniak/Stride/issues/73)) ([96e88d1](https://github.com/erykszczesniak/Stride/commit/96e88d1e7b102b7ac03b594b0fd7ef53173ff1a0))
* **gear:** add shoe/bike/gear tracking with per-item mileage totals ([a5d09bb](https://github.com/erykszczesniak/Stride/commit/a5d09bb7a063a85d4384e5f344a950c00d497487))
* **gear:** shoe/bike/gear tracking with per-item mileage ([49b63e6](https://github.com/erykszczesniak/Stride/commit/49b63e607871fd9f4f4c326dba294f11677c1adf))
* **i18n:** full frontend internationalization with 6 languages ([#79](https://github.com/erykszczesniak/Stride/issues/79)) ([22485fb](https://github.com/erykszczesniak/Stride/commit/22485fbf084cc92f39f9c5ee699aa2202c0bf70a))
* **monitoring:** alerts as code — 30 unit-tested rules, SLO burn-rate, cost attribution, Alertmanager routing ([98f7b97](https://github.com/erykszczesniak/Stride/commit/98f7b97941fa9778fdd0b089cb88ef82d5a25cde))
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
* **web:** iOS-style Weekly Volume replaces the daily activity chart ([8c001fa](https://github.com/erykszczesniak/Stride/commit/8c001fa1d7212da2874f4505dbadc013c767cc6a))
* **web:** media row on feed cards + start-time dating ([7011835](https://github.com/erykszczesniak/Stride/commit/7011835d607904f44d0ae3f0d426de6984fdc5a8))
* **web:** pace and heart-rate chart on the activity detail ([d629e57](https://github.com/erykszczesniak/Stride/commit/d629e57a091541c2b6c200f155ee0646a93da0e2))
* **web:** photo deletion from the activity lightbox ([b2585de](https://github.com/erykszczesniak/Stride/commit/b2585dedcccb29207d355e61307669666f53b88c))
* **web:** photo lightbox on activity detail ([09c5b14](https://github.com/erykszczesniak/Stride/commit/09c5b14fcd5621dbef93293de78769fb640f8e9e))
* **web:** Training Load (CTL/ATL) chart on the dashboard ([48e16b6](https://github.com/erykszczesniak/Stride/commit/48e16b6081af59255bb26d5a0d7a8a0fb71d40f1))
* **web:** training-form gauge (CTL/ATL/TSB) on the dashboard ([967ae85](https://github.com/erykszczesniak/Stride/commit/967ae85a52c4b7206408e044b8d0c8504b0505bf))
* wire privacy settings to backend ([#85](https://github.com/erykszczesniak/Stride/issues/85)) ([bb41afd](https://github.com/erykszczesniak/Stride/commit/bb41afdb511b433188497dffff916ccb96656cb3))


### Fixed

* **ci:** smoke test — URL-encode the Prometheus query ([237f3c1](https://github.com/erykszczesniak/Stride/commit/237f3c1f8a28e72febe1585a98ea5375550b8796))
* **dashboard:** survive more than one active goal in a period ([08a240e](https://github.com/erykszczesniak/Stride/commit/08a240ee7a3412261a9bb4c874104f45f548ca25))
* **data:** normalize legacy JSON route polylines to the encoded format ([2b14e83](https://github.com/erykszczesniak/Stride/commit/2b14e834a61023d75f554fa33fdcf6c12f70161b))
* **dev:** silence OTel OTLP export retries when Jaeger is not running ([c74a60c](https://github.com/erykszczesniak/Stride/commit/c74a60cd00d9df1113b1a5487a79069fe6511cb0))
* **docker:** frontend health check targets 127.0.0.1, not localhost ([159ce50](https://github.com/erykszczesniak/Stride/commit/159ce509a15d76dbda97cb7d304f1804c1e9681e))
* duration with seconds + pace display for running ([#75](https://github.com/erykszczesniak/Stride/issues/75)) ([c0c980c](https://github.com/erykszczesniak/Stride/commit/c0c980c4e4d65919c0f946ff3d4c4f93fa1ab84c))
* **e2e:** a base64 JWT secret in the sample env and patient monitoring checks ([f1319aa](https://github.com/erykszczesniak/Stride/commit/f1319aa1714efcddf586e8f6cd82a1214c54bf4f))
* **e2e:** the workflow's JWT_SECRET must be base64 too ([5fa514a](https://github.com/erykszczesniak/Stride/commit/5fa514a763e4f95cf2154faefdd5e16218182b1b))
* **flyway:** resolve V12 triple-collision — rename gear to V13, strava to V14 ([6931e5e](https://github.com/erykszczesniak/Stride/commit/6931e5ea9ccbb5e2be96dcfa05e3cc66bc8e5288))
* **frontend:** patch the two fixable CRITICAL findings in the lockfile ([9d17325](https://github.com/erykszczesniak/Stride/commit/9d17325a6a01b4749d56fe647810267eb43e24ca))
* **i18n:** make English the default for new users regardless of browser locale ([#84](https://github.com/erykszczesniak/Stride/issues/84)) ([5d007ef](https://github.com/erykszczesniak/Stride/commit/5d007effb475ac81b5525ec3bfc93871402e3e92))
* **import:** make FIT parser actually parse real device files ([#76](https://github.com/erykszczesniak/Stride/issues/76)) ([eb11248](https://github.com/erykszczesniak/Stride/commit/eb112481e70a17b5421624d12619c8bd2a11caa6))
* **navbar:** hide add-activity and notifications on the Settings page ([afcbd07](https://github.com/erykszczesniak/Stride/commit/afcbd074dfd7e52c02ad1d9c2740f06d311bd765))
* **navbar:** show search on mobile and space the icons properly ([b011436](https://github.com/erykszczesniak/Stride/commit/b011436ca757d170e38eaab9b54317d7eb91120b))
* **navbar:** strip add-activity + notifications on the Settings page ([9b77960](https://github.com/erykszczesniak/Stride/commit/9b77960dd6b254fb462ced0a7f35aab2bd69ec84))
* **personal-records:** make recalculate not blow up on unique constraint ([#77](https://github.com/erykszczesniak/Stride/issues/77)) ([6049fbe](https://github.com/erykszczesniak/Stride/commit/6049fbe6adf81787df7cc707051488be5a9bc0fc))
* **personal-records:** polyline format detection + samples + tests ([#78](https://github.com/erykszczesniak/Stride/issues/78)) ([fb38874](https://github.com/erykszczesniak/Stride/commit/fb3887402008ee28e89f6b9f1fde17a9c1e95b1d))
* **pipeline:** serve viewable foreign activities in activity detail ([31f0d12](https://github.com/erykszczesniak/Stride/commit/31f0d1283596fe2d991012346cb7b8cb28d240a3))
* **privacy:** missing follow-ups — PR endpoint privacy, locked screen, auto-refresh ([#87](https://github.com/erykszczesniak/Stride/issues/87)) ([4f3c88f](https://github.com/erykszczesniak/Stride/commit/4f3c88f90421288478b6372ff4b7c525d15b3692))
* **pr:** skip non-distance sports and fix swim/ride pace labels ([#92](https://github.com/erykszczesniak/Stride/issues/92)) ([e400149](https://github.com/erykszczesniak/Stride/commit/e4001496d07072fc5bf1a0c75c3b7c9cf8479370))
* **pr:** stop the GPS sliding window from inventing impossibly-fast PRs ([#95](https://github.com/erykszczesniak/Stride/issues/95)) ([69df508](https://github.com/erykszczesniak/Stride/commit/69df5086bba33d99cef4dbbf4fea561a506804b8))
* **record-activity:** stop the form from overflowing on mobile ([#93](https://github.com/erykszczesniak/Stride/issues/93)) ([96cbc77](https://github.com/erykszczesniak/Stride/commit/96cbc7797bce54c0f568be2cafa2f3e066e2d6f2))
* remove obsolete Pace/Speed Display dropdown from preferences ([#83](https://github.com/erykszczesniak/Stride/issues/83)) ([98957b2](https://github.com/erykszczesniak/Stride/commit/98957b2b37bc7096ad930c43fdf29d78c30174eb))
* **search:** make the search modal resizable ([aebf42a](https://github.com/erykszczesniak/Stride/commit/aebf42ac8ea9136998e41283045b2ed97feb793b))
* **search:** make the search modal resizable by the user ([9199126](https://github.com/erykszczesniak/Stride/commit/91991262b167854e8c67963caf17f766a672a1b3))
* **security:** visibility gate on GET /api/v1/activities/{id} ([128e448](https://github.com/erykszczesniak/Stride/commit/128e448eb33b04f58020a91529f2e911579d81a8))
* **settings:** stop the layout from overflowing on mobile ([94b6eda](https://github.com/erykszczesniak/Stride/commit/94b6eda121fb200b3cc33eb0abb295b30dcfc642))
* **settings:** stop the layout from overflowing on mobile ([e1e8ccd](https://github.com/erykszczesniak/Stride/commit/e1e8ccdbd3b8c7ea5d505f4d13c32fd2e6ec6429))
* show lifetime stats on profile and display avatar everywhere ([#109](https://github.com/erykszczesniak/Stride/issues/109)) ([5395bd7](https://github.com/erykszczesniak/Stride/commit/5395bd70a9029069f6fa9b95bee2fa719125d887))
* **splits:** only show the Splits section on distance-based activities ([#105](https://github.com/erykszczesniak/Stride/issues/105)) ([1e4f2eb](https://github.com/erykszczesniak/Stride/commit/1e4f2eb4975c3af7535926de617803a991b94341))
* **web:** zoom the elevation chart to the altitude band ([71c1286](https://github.com/erykszczesniak/Stride/commit/71c12862ef1aef04d0aa4a07b35dba9c7bc01eb0))


### Changed

* **i18n:** move DistanceType display names from backend enum to frontend locales ([#104](https://github.com/erykszczesniak/Stride/issues/104)) ([4c7d3d3](https://github.com/erykszczesniak/Stride/commit/4c7d3d3b8e7317169a24f55742705b4dddba5625))
* make GraphQL controllers extend BaseController ([#80](https://github.com/erykszczesniak/Stride/issues/80)) ([8bfa198](https://github.com/erykszczesniak/Stride/commit/8bfa198255f2848b322c3fbdc574e2d3ccdcd338))
* port PersonalRecordService to Kotlin ([45ed001](https://github.com/erykszczesniak/Stride/commit/45ed001dbf9806f6829eb8c4cd60092381174922))
* port PersonalRecordService to Kotlin ([3108e18](https://github.com/erykszczesniak/Stride/commit/3108e18aa65658989509b40d840528994c14256e))


### Documentation

* add "Recent Features — Configuration & Testing" section to README ([12c6e1c](https://github.com/erykszczesniak/Stride/commit/12c6e1cc2c45e103fc4bd0e717988877503f1f03))
* **adr:** cite the current commit of the alerts-as-code change ([f63b918](https://github.com/erykszczesniak/Stride/commit/f63b9189f209353f8205b7879e480fe1ea682d1f))
* **agents:** no tool names in the instructions header ([025e2e0](https://github.com/erykszczesniak/Stride/commit/025e2e0e22622d7eb7595361b2a4c5ee8947ccf1))
* Document Jackson boolean field serialization issue ([b09672d](https://github.com/erykszczesniak/Stride/commit/b09672d267f75b31f6e4459ddf699defc24b7989))
* README table of contents, operations & quality overview and documentation index ([c750fcd](https://github.com/erykszczesniak/Stride/commit/c750fcdd7c7822d3c338ee293ae9c9a88757264b))
* runbooks, post-mortem, ADRs, SLO, cost attribution, AI guardrails and observability for the monolith ([7650c3d](https://github.com/erykszczesniak/Stride/commit/7650c3d6a179c744b727619b1664935c3e22595c))
* simplify monolith README ([#185](https://github.com/erykszczesniak/Stride/issues/185)) ([43abf47](https://github.com/erykszczesniak/Stride/commit/43abf47c77bbe7a63fddd74d8c1ffbd1fc23f9fb))

## [Unreleased]

### Security
- Strava OAuth `state` is no longer signed with the literal `changeme` when
  neither `STRAVA_STATE_SECRET` nor `jwt.secret` is set; the connect flow now
  answers 503 "Strava integration is not configured" instead of issuing a
  forgeable state. The `dev` profile falls back to an obviously-dev JWT key
  (the one from `.env.example`) instead of a random-looking one, and
  `.gitignore` covers key material (`*.pem`, `*.key`, `*.p12`, `*.jks`) and
  Terraform state.

### Fixed
- Frontend: `dompurify` 3.4.15 → 3.4.17 (GHSA-cg2g-xh64-v4q7, SAFE_FOR_TEMPLATES
  bypass), a transitive dependency of `jspdf`; the fixable CRITICAL Trivy
  gate reported on the lockfile.
- Frontend: the two fixable CRITICAL findings Trivy reports in the lockfile,
  `proxy-addr` 2.0.7 → 2.0.8 (CVE-2026-90711) and `shell-quote` 1.10 → 1.12
  (CVE-2026-102422). Both are build-time dependencies of `react-scripts`;
  the bundle does not change.
- Dashboard returned 500 for anyone with more than one active goal in a period
  (a weekly run goal and a weekly ride goal, as the sample data has): the query
  returned an `Optional` and threw `NonUniqueResultException`. It returns the
  active goals ordered now and the service takes the first.
- Smoke test: the activities check called `/api/v1/activities`, which no endpoint
  serves; it checks `/api/v1/activities/my`.
- E2E on a fresh host: the sample `JWT_SECRET` is base64 (the signing key is
  base64-decoded, so the placeholder made every login fail with 500), and the
  smoke test waits up to a minute for the first scrape and the Watchdog after a
  cold start.
- Frontend container health check: `127.0.0.1` instead of `localhost` (BusyBox
  wget tries `::1` first, where nginx did not listen), nginx also listens on IPv6,
  and the compose check gets a start period. The E2E stack timed out on this.
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
- Sample data: eleven weeks of training history for the four sample users
  (`V23`, 166 manual activities dated relative to the first start), so the
  dashboard month cards, the month-over-month trend and the 12-week volume
  chart have data on any day. Seeded personal records are unchanged;
  `SampleActivityHistoryIntegrationTest` runs the full Flyway set on
  PostgreSQL and recalculates the records to prove it. `make db-reset`
  re-dates the sample data.
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
