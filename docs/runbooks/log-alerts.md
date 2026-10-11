---
name: LogAlerts
description: A log-derived signal on the backend is abnormal — failed logins spiking, one logger flooding ERROR, ERROR volume far above its own baseline — or the log pipeline has gone silent and every log-based alert is blind.
metadata:
  owner: platform (LogAuthFailureSpike, LogPipelineSilent) / domain (LogExceptionRateSpikePerLogger, LogErrorVolumeAboveBaseline)
  version: "1.0"
alert_type: pipeline
severity: warning
actionability: "Ticket — same working day; LogAuthFailureSpike: investigate now"
allowed-tools: []
---

# Alert: LogAuthFailureSpike / LogExceptionRateSpikePerLogger / LogErrorVolumeAboveBaseline / LogPipelineSilent

## Summary

These four alerts are evaluated by the **Loki ruler** over the backend's JSON
logs (`monitoring/loki/rules/fake/stride-logs.rules.yml`), not by Prometheus.
They answer questions metrics cannot: *who* is failing to log in, *which class*
writes the errors, is the error stream abnormal *for this service*, and is the
log pipeline itself alive. Users are usually not affected yet when they fire;
the point is to find the cause before `EdgeHighErrorRate` does. Every backend
line carries a `traceId`, so the path is always alert → logs panel → trace in
Jaeger ([observability.md](../observability.md#logs)).

## Required inputs

- Which alert, and `logger` from the labels for `LogExceptionRateSpikePerLogger`
- Alert start time (Alertmanager UI or `startsAt` in the webhook payload)
- `docker compose --profile observability ps backend alloy loki otel-collector`
- Any deploy in the last hour (`git log --since=1h`, `docker compose ps --format '{{.Name}} {{.RunningFor}}'`)

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/loki/rules/fake/stride-logs.rules.yml` (Loki ruler, 1 m interval → Alertmanager) |
| `LogAuthFailureSpike` | `sum(count_over_time({app="strava-backend", level="ERROR"} \|= "AuthenticationController.login() threw exception: Bad credentials" [5m])) > 20` for 5m — warning, platform |
| `LogExceptionRateSpikePerLogger` | `sum by (logger) (count_over_time({app="strava-backend", level="ERROR"} \| logger != "" \| logger !~ ".*LoggingAspect" [5m])) > 30` for 10m — warning, domain |
| `LogErrorVolumeAboveBaseline` | last 10 m of ERROR lines `> 3 ×` the previous hour's average 10-minute slice, and `> 20` — for 10m, warning, domain |
| `LogPipelineSilent` | `absent_over_time({app="strava-backend"}[10m]) == 1` for 5m — warning, platform; inhibited by `ServiceDown` (same `app`) |
| Dashboard | http://localhost:3003/d/stride-logs |

No promtool unit tests exist for these (LogQL): CI loads the file into a real
Loki and checks every alert is listed and every expression parses
(`monitoring/scripts/check_loki_rules.sh`); the contract check prints a
`NOTE` per rule for the missing test case. Thresholds were set against the
compose stack under `scripts/smoke.sh` traffic — revisit them with real data.

## Success criteria

- The alert has resolved in Alertmanager (not silenced).
- The logger / account / pipeline hop is identified, with a trace or a log
  excerpt attached to the ticket.
- For `LogPipelineSilent`: `sum(count_over_time({app="strava-backend"}[5m]))`
  in Loki is above zero again and `LogPipelineSilent` is gone from
  http://localhost:3100/loki/api/v1/rules?type=alert state.

## Investigation

### 1. Is the pipeline the problem? (always first, cheapest)

| Command | What to look for |
|---|---|
| `curl -s localhost:3100/ready` | `ready`. Anything else: Loki is starting or broken — `docker compose logs --since 10m loki` |
| `curl -s -G localhost:3100/loki/api/v1/query --data-urlencode 'query=sum by (app) (count_over_time({app=~".+"}[5m]))' \| jq '.data.result[] \| [.metric.app, .value[1]]'` | lines per app in the last 5 minutes. `strava-backend` missing while other apps are present = Alloy is not tailing the backend (step 5); everything missing = Alloy → Loki is down |
| `curl -s localhost:3100/loki/api/v1/rules` | the `stride-logs` group with four alerts. Empty = the ruler did not load the file (mount, YAML) |
| `docker compose logs --since 15m backend \| head -3` | lines are JSON (`{"@timestamp":...,"level":...}`). Plain text = the `json` profile is not active (`SPRING_PROFILES_INCLUDE`), so `level` is never set and every rule sees nothing |

If this step finds the fault, go to Mitigation; the other three alerts cannot
be trusted until it is fixed.

### 2. `LogAuthFailureSpike` — who, and how many accounts

Dashboard → set *Level* to `ERROR`; or in Explore (Loki):

```logql
{app="strava-backend", level="ERROR"} |= "AuthenticationController.login() threw exception: Bad credentials" | json
```

| Look for | Meaning |
|---|---|
| Many lines, same minute, bursts of exactly N | a script. Correlate with `rate(http_server_requests_seconds_count{uri="/api/v1/auth/login",status="401"}[5m])` in Prometheus and the login rate limit (429s) to see whether it holds |
| Lines spread evenly after a deploy / app release | a client shipped with a broken login flow (wrong field, stale token exchange). Check the frontend or iOS release time |
| One `traceId` → Jaeger | the request span shows the client IP (`client.address` / `net.sock.peer.addr`) and user agent after redaction; the e-mail is masked by the collector, by design |

The backend does not log the attempted e-mail (the exception message is the
constant `Bad credentials`), so "how many accounts" comes from Postgres if it
matters: failed logins are not persisted, so the answer is the 401 count by
client IP from the access path, not from the database.

### 3. `LogExceptionRateSpikePerLogger` — one class

```logql
{app="strava-backend", level="ERROR"} | logger="<logger from the alert>" | json | line_format "{{.message}} {{.stack_trace}}"
```

Pick one line, expand it, click **Trace in Jaeger** next to `traceId`. The
trace shows the request path and whether the error comes from a controller,
an async listener (`NotificationEventListener`, `UserSearchIndexSyncListener`)
or a provider call. A logger that is an async listener with no parent request
in the trace means the event source is the problem, not the request.

Usual offenders and where they lead:

| Logger | Follow |
|---|---|
| `...exception.ApiExceptionHandler` (`Unexpected error`) | a 500 — [edge-high-error-rate.md](edge-high-error-rate.md) |
| `...search.UserSearchIndexSyncListener` | Elasticsearch down or red — `curl localhost:9200/_cluster/health` |
| `...ai.*` / `...service.impl.GroqServiceImpl` | provider errors — [ai-provider-degraded.md](ai-provider-degraded.md) |
| `...event.NotificationEventListener` | SSE emitter failures; usually clients disconnecting, harmless unless it coincides with user reports |

### 4. `LogErrorVolumeAboveBaseline` — abnormal for this service

The top-loggers panel on the dashboard answers "what grew" in one look. Then
step 3 for that logger. Check also whether the *baseline* is the anomaly: a
quiet previous hour (deploy, night) makes a normal morning look like a spike —
the `> 20` floor exists for this, but a low-traffic environment may need it
raised. If `EdgeHighErrorRate` or `EdgeElevatedErrorRate` fires at the same
time, treat this alert as context for that one.

### 5. `LogPipelineSilent` — which hop

| Command | What to look for |
|---|---|
| `curl -s localhost:9091/api/v1/query --data-urlencode 'query=up{job="strava-backend"}'` | `1` = the backend is up and this is a pipeline fault; `0` = `ServiceDown`, this alert is noise and should be inhibited |
| `curl -s localhost:12345/api/v0/web/components \| jq '.[] \| {name: .localID, health: .health.state}'` | Alloy components healthy; `loki.source.docker.stack` exists |
| `docker compose logs --since 10m alloy \| grep -i 'error\|permission denied'` | `permission denied` on `/var/run/docker.sock` = the socket mount; `connection refused` to `loki:3100` = Loki |
| `docker inspect strava-backend --format '{{json .NetworkSettings.Networks}}' \| jq 'keys'` | the backend is on a network whose name ends in `strava-network` — Alloy keeps only those containers (`monitoring/alloy/config.alloy`) |
| `curl -s localhost:3100/metrics \| grep -E 'loki_distributor_lines_received_total\|loki_ingester_streams_created_total'` | lines arriving at Loki at all |

## Mitigation

- **Safe without review:** `docker compose --profile observability restart alloy`
  (re-discovers containers, no data loss beyond the gap); `restart loki`
  (chunks are on `loki_data`); `docker compose up -d --force-recreate backend`
  when the `json` profile is missing (set `SPRING_PROFILES_INCLUDE=json`).
  For `LogAuthFailureSpike` from one IP: block it at the edge (nginx / ingress)
  — the backend's own login rate limit already slows it.
- **Needs a change / second pair of eyes:** editing a rule threshold (run
  `monitoring/scripts/check_loki_rules.sh` and `check_alert_rules.py`
  first), changing the Alloy relabel rules or the Loki retention, lowering
  logger levels in `logback-spring.xml` / `application-*.yml` to silence a
  logger (fix the cause instead).

## Escalation / close

- `LogAuthFailureSpike` with evidence of many accounts targeted: escalate to
  **platform** now with the IP list, the 401 rate and the time window; it is a
  security event, not an outage.
- The others: ticket to the owning team (`team` label) with the dashboard
  link, the logger, one `traceId` and the Jaeger link for it.
- Close when the alert has resolved in Alertmanager. If a threshold was wrong,
  change it in the rule file in the same PR as the fix, and say so in the
  post-mortem if one is written.
