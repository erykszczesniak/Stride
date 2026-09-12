---
name: ServiceDown
description: A Stride service is not being scraped (ServiceDown) or has no registered instance at all (ServiceMissing); every request routed to it fails at the gateway.
metadata:
  owner: platform
  version: "1.0"
alert_type: availability
severity: critical
actionability: "Page — act now"
allowed-tools: []
---

# Alert: ServiceDown / ServiceMissing

## Summary

`ServiceDown` means Prometheus still knows a target but cannot scrape it for
2 minutes. `ServiceMissing` means Eureka has no instance of the service at
all for 3 minutes — the process died and its lease expired, or it never
registered after a deploy. Either way the gateway answers 503 for that
service's routes and dependent services trip their circuit breakers
(`CircuitBreakerOpen` follows within minutes; it is inhibited while this
alert fires).

## Required inputs

- `app` (and `instance` for ServiceDown) from the labels
- Environment: compose (`docker compose ps`) or kind (`kubectl -n stride get pods`)
- Whether a deploy happened in the last 15 minutes

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/alerts/platform.rules.yml` |
| Expression | `up{job=~"stride-services\|discovery-server\|config-server"} == 0` / `absent(up{app="<service>"})` |
| Threshold / for | `for: 2m` / `for: 3m` |
| Dashboard | http://localhost:3001/d/stride-jvm |

## Success criteria

- `up{app="<service>"}` is 1 again and the alert resolved on its own.
- The gateway route answers 2xx: `curl -s -o /dev/null -w '%{http_code}' localhost:8080/api/v1/<route>`.
- Cause written down (crash loop, OOM, config, network) — not just "restarted it".

## Investigation

### 1. Is the process alive?

| Command | What to look for |
|---|---|
| `docker compose ps <service>` | `STATUS` — `Restarting`, `Exited (137)` = OOM-killed, `unhealthy` |
| `kubectl -n stride get pods -l app=<service>` | `CrashLoopBackOff`, `OOMKilled` in `kubectl describe pod` |

Expected: running and healthy. If restarting, go to step 3.

### 2. Is it registered and reachable?

| Command | What to look for |
|---|---|
| `curl -s localhost:8761/eureka/apps/<SERVICE> \| head -30` | instance status `UP`; missing = never registered |
| `curl -s localhost:<port>/actuator/health/readiness` | `{"status":"UP"}`; `OUT_OF_SERVICE` = readiness failing (DB, Kafka) |
| Prometheus → Status → Targets | scrape error text (connection refused, timeout, 404) |

A service that is up but not in Eureka usually cannot reach `discovery-server:8761` — check `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`.

### 3. Why did it die?

| Command | What to look for |
|---|---|
| `docker compose logs --since 30m <service> \| tail -100` | the last exception before exit; `OutOfMemoryError`; Flyway `Validate failed`; `Connection refused` to Postgres/Kafka |
| `docker inspect stride-<service> --format '{{.State.OOMKilled}}'` | `true` → memory limit too low for the heap (see `mem_limit`, `MaxRAMPercentage=75`) |
| `git log --oneline -5` | a migration or config change in the last deploy |

## Mitigation

- **Safe without review:** restart the one container/pod (`docker compose restart <service>` / `kubectl -n stride rollout restart deploy/<service>`). If it dies again within a minute, stop restarting and read the logs.
- **Flyway validation failure:** never delete the migration history. Roll back the deploy to the previous image; fix forward with a new migration.
- **Needs review:** raising `mem_limit` / k8s limits, changing Config Server values.

## Escalation / close

- Escalate to **platform** with the last 100 log lines and the `describe pod` / `docker inspect` output.
- Post-mortem required if the service was down for more than 10 minutes or if it was the gateway.
