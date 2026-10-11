# ADR-012 — Logs as a pipeline: collector in the trace path, Loki, log alerts as code

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-10-11 |
| **Deciders** | platform |

## Context

Metrics and traces were pipelines: Prometheus scrapes, rules are unit-tested
and contract-checked in CI ([ADR-006](006-alerts-as-code.md)), spans go over
OTLP to Jaeger. Logs were a file format. The backend could emit JSON under
`prod`, but in compose it printed coloured text, nothing collected it, and
"find the log lines for this trace" meant `docker compose logs | grep
<traceId>` on the host that happened to run the container. Three things
followed from that:

- **No correlation across signals.** A 5xx in Grafana, the trace in Jaeger
  and the stack trace in a terminal were three tools and a copy-paste. The
  `traceId` was already in every line (MDC) and in every span; nothing joined
  them.
- **No alerts from logs.** Failed logins, exception storms from one class,
  "the error stream is three times what it was an hour ago" are visible only
  in logs; `logback_events_total` gives one number per level and nothing
  about who or where.
- **Nothing between the application and the trace store.** The backend
  exported 100 % of spans straight to Jaeger, including whatever attribute
  Spring's observation convention puts on a span — the full request URL
  with its query string (OAuth `code`/`state` on the callback routes), and
  anything a future instrumentation adds. Sampling was a per-application
  setting (`application-prod.yml`: 10 %) that threw away errors and slow
  requests with the same probability as the rest.

## Decision

1. **An OpenTelemetry Collector (contrib) sits between the backend and
   Jaeger**, in compose next to Jaeger (not behind a profile: it is in the
   trace path). Policies live in `monitoring/otel/otel-collector.yml`, not in
   the application: `memory_limiter`; `attributes` dropping credential and
   session headers and `url.query`, hashing `enduser.id`; `redaction`
   masking bearer/JWT tokens, credential-bearing query parameters and e-mail
   addresses in any attribute value; `tail_sampling` keeping every trace with
   an error span, every request whose Micrometer `outcome` is a 4xx/5xx
   (Spring only marks the span ERROR for unhandled exceptions, while a
   handled 401/404 still writes an ERROR log line that must link to a trace)
   and every trace slower than 1 s, 10 % of the rest; `batch`.
   The backend keeps 100 % head sampling and exports to the collector; the
   collector decides. `OTEL_EXPORTER_OTLP_ENDPOINT` still accepts Jaeger
   directly for a process outside compose.
2. **Structured JSON logs in compose too.** The existing `LogstashEncoder`
   appender is reused: the `json` profile (added via `SPRING_PROFILES_INCLUDE`
   in compose) switches the dev levels to JSON output; `prod` is unchanged.
   `traceId`, `spanId`, `requestId`, `userId` stay as MDC fields.
3. **Grafana Alloy ships container logs to Loki** (`observability` profile).
   Docker service discovery over the socket, filtered to this stack's
   network; two index labels, `app` (same value as the Prometheus label) and
   `level`; `logger`, `traceId`, `spanId` as structured metadata — searchable
   without becoming a stream dimension. Loki keeps three days on a volume.
4. **Correlation is configured, not documented.** The Loki datasource has a
   derived field `traceId` → Jaeger; the Jaeger datasource has
   `tracesToLogsV2` → Loki (custom query `{app="strava-backend"} |=
   "<traceId>"`, ±5 min — the span's `service.name` is `strava`, the log
   label is the compose service name, so a tag mapping would not match). A
   `stride-logs` dashboard puts the alert signals and a logs panel on one
   page.
5. **Log alerts are code under the same contract.** Loki ruler rules in
   `monitoring/loki/rules/<tenant>/` carry the Prometheus label contract
   (`severity`, `team`, `tier`, `service`, `summary`, `description`,
   `runbook_url`), are delivered to the same Alertmanager, and
   `check_alert_rules.py` validates them together with the Prometheus rules —
   including alert-name uniqueness across the two sources. What differs is
   testing: there is no promtool for LogQL, so the contract check exempts them
   from the test-case rule *explicitly* (a `NOTE` per rule in the CI log), and
   `check_loki_rules.sh` loads the files into a real Loki in CI to prove they
   parse and are listed by the ruler.

## Alternatives considered

- **Keep exporting straight to Jaeger, no collector.** One container fewer,
  but every policy (what leaves the application, what is kept) stays
  application code and is re-implemented per service on the
  `microservices` branch. Tail sampling is impossible without a component
  that sees whole traces.
- **Elasticsearch + Logstash/Filebeat + Kibana.** Elasticsearch is already
  in the stack for user search, which made this tempting. Full-text indexing
  of every line costs an order of magnitude more memory than Loki's label
  index for the same retention, Kibana would be a second UI next to Grafana,
  and the trace ↔ log links would need to be built by hand. Loki queries by
  label and grep; that is what incident debugging looks like here.
- **A hosted logs/traces SaaS.** Correlation and alerting out of the box, no
  containers to run — and a bill, an account, data leaving the machine, and
  nothing that can run in CI. The repo's monitoring is deliberately
  reproducible from `docker compose up`.
- **Promtail instead of Alloy.** Same pipeline stages; Promtail is in
  maintenance (end of life announced), Alloy is its successor with the
  same `loki.*` components and an OTLP path for the day logs go through the
  collector too.
- **`traceId` as a Loki label.** Makes "logs for this trace" an index lookup,
  and creates one stream per request — the documented way to kill Loki.
  Structured metadata gives the same filter without the cardinality.
- **Head sampling at 10 % in the application** (what `prod` did). Simple,
  but it drops 90 % of the errors — exactly the traces a responder wants.

## Consequences

- Three more containers in the full stack (collector always; Loki and Alloy
  under `--profile observability`); Alloy needs read access to the Docker
  socket, which is a privilege to be aware of outside a laptop.
- Traces in Jaeger are now a sample: 100 % of errors and slow requests, 10 %
  of the rest. A span count in Jaeger no longer equals a request count — use
  Prometheus for counts. The collector's own metrics (`otelcol_*`, scraped
  by Prometheus) show received vs. sent spans.
- PII policy has one place and one reviewer. Adding an attribute that may
  carry personal data means adding a line to the collector config, not to
  the code. E-mail addresses and tokens are masked even if an instrumentation
  puts them somewhere unexpected.
- Log alerts have weaker tests than metric alerts: parse + load, not
  behaviour. Thresholds were set on compose traffic and are documented in
  the rule file and the runbook; the first weeks on real data will move them.
- Four more alerts to own (`docs/runbooks/log-alerts.md`); `LogPipelineSilent`
  carries `app` so `ServiceDown` inhibits it, otherwise a dead backend would
  page twice.
- Local disk: Loki keeps 72 h of every container's logs on `loki_data`; the
  Prometheus volume keeps 15 d. Both go with `docker compose down -v`.

## Known gaps

- Logs are shipped from the container runtime, not over OTLP from the
  application, so a line's resource attributes come from Docker labels, not
  from the SDK. Good enough for one deployable; the `microservices` branch
  will want the collector to receive logs too.
- No log-based *recording* rules yet (e.g. failed-login rate as a metric for
  the SLO dashboard); the ruler supports them.
- The ruler has no unit-test harness. If Loki ships one (or `lokitool`
  becomes available as an image), `check_loki_rules.sh` should be replaced
  by it and the `NOTE` exemption removed from `check_alert_rules.py`.
- Nothing in `k8s/` — same as the rest of the monitoring stack
  ([observability.md](../observability.md), "What's not done yet").
