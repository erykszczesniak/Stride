---
name: ServiceDown
description: Prometheus cannot scrape the backend for 2 minutes — there is one deployable, so every feature is down and the frontend's nginx answers 502 for every API call.
metadata:
  owner: platform
  version: "1.0"
alert_type: availability
severity: critical
actionability: "Page — act now"
allowed-tools: []
---

# Alert: ServiceDown

## Summary

The scrape target is static (`backend:8080` in
`monitoring/prometheus/prometheus.yml`), so a dead backend keeps its `up`
series at 0 and this alert fires after 2 minutes. There is no discovery, no
gateway and no second service: when the backend is down, the SPA gets 502
from nginx for `/api/` and `/graphql`, the iOS client gets nothing from
`/pipeline/v1`. Alertmanager inhibits every warning-level alert for the same
`app` while this fires; `PrometheusTargetsMissing` inhibits *this* alert when
the scrape config itself is gone.

## Required inputs

- `instance` from the labels (`backend:8080` in compose)
- Environment: compose (`docker compose ps`) or k8s (`kubectl -n strava get pods`)
- Whether a deploy or a migration happened in the last 15 minutes

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/alerts/platform.rules.yml` |
| Expression | `up{job="strava-backend"} == 0` |
| Threshold / for | `for: 2m` |
| Dashboard | http://localhost:3003/d/stride-jvm |

## Success criteria

- `up{job="strava-backend"}` is 1 again and the alert resolved on its own.
- Direct: `curl -s -o /dev/null -w '%{http_code}\n' localhost:8081/actuator/health` → 200.
- Through the proxy: `curl -s -o /dev/null -w '%{http_code}\n' localhost/api/v1/activities/feed` → 401 (auth required = the path works), not 502.
- Cause written down (crash loop, OOM, migration, config, network) — not just "restarted it".

## Investigation

### 1. Is the process alive?

| Command | What to look for |
|---|---|
| `docker compose ps backend` | `STATUS`: `Restarting`, `Exited (137)` = OOM-killed, `unhealthy` (the compose healthcheck is `wget /actuator/health` every 30 s, 60 s start period, 3 retries) |
| `kubectl -n strava get pods -l app.kubernetes.io/name=backend` | `CrashLoopBackOff`; `kubectl -n strava describe pod <pod>` shows `OOMKilled` or a failing startup probe (`/actuator/health`, 60 s delay, up to 30 × 10 s) |

Expected: running and healthy. If restarting, go to step 3.

### 2. Is it reachable, and is it the scrape that fails?

| Command | What to look for |
|---|---|
| `curl -s localhost:8081/actuator/health` | `{"status":"UP"}`; 503 with `DOWN` = a health component failed (step 3) |
| `curl -s -H "Authorization: Bearer $TOKEN" localhost:8081/actuator/health \| jq '.components \| map_values(.status)'` | which component is `DOWN`: `db`, `elasticsearch`, `diskSpace` (details need a token) |
| `curl -s -o /dev/null -w '%{http_code}\n' localhost:8081/actuator/prometheus` | 200; 401 = someone removed `/actuator/prometheus` from `permitAll` in `SecurityConfiguration`; 404 = `prometheus` missing from `management.endpoints.web.exposure.include` |
| Prometheus → Status → Targets (http://localhost:9091/targets) | the scrape error: `connection refused` (process down), `context deadline exceeded` (JVM stalled — GC, see [jvm-heap-high.md](jvm-heap-high.md)), `401`/`404` (config, the app is fine) |

Note on health: nothing in `application.yml` disables Spring Boot's
Elasticsearch health indicator, so an unreachable Elasticsearch turns the
aggregate `/actuator/health` `DOWN` (503) even though every non-search
request works. The compose healthcheck and the k8s startup probe both read
that aggregate endpoint; `/actuator/prometheus` does not care. A container
that is `unhealthy` but scraped fine is this case — fix Elasticsearch, not
the backend.

### 3. Why did it die?

| Command | What to look for |
|---|---|
| `docker compose logs --since 30m backend \| tail -100` | the last exception before exit (table below) |
| `docker inspect strava-backend --format '{{.State.OOMKilled}} {{.State.ExitCode}}'` | `true 137` → memory limit too low for the heap (`MaxRAMPercentage=75` of the container limit; the dev compose sets no limit, `docker-compose.prod.yml` sets 2G, k8s 1Gi) |
| `git log --oneline -5` | a migration or config change in the last deploy |

| Log signature | Meaning |
|---|---|
| `Found more than one migration with version N` | two PRs shipped the same Flyway version — see [post-mortem 2026-07-11](../postmortems/2026-07-11-duplicate-flyway-version.md); rename the later one, never edit history |
| `Validate failed: Migration checksum mismatch` | a merged migration was edited — roll back the image, fix forward with a new `V<n>__*.sql` |
| `Schema-validation: missing column/table` | Hibernate `ddl-auto: validate` disagrees with the migrations — the entity change shipped without its migration |
| `Connection to postgres:5432 refused` | Postgres down or not yet healthy (`docker compose ps postgres`) |
| `OutOfMemoryError` | [jvm-heap-high.md](jvm-heap-high.md) |
| `docker compose up` refuses to start: `JWT_SECRET must be set in .env` / `POSTGRES_PASSWORD must be set` | the compose file requires both (`${VAR:?...}`); fill `.env` |

## Mitigation

- **Safe without review:** restart the one container/pod (`docker compose restart backend` / `kubectl -n strava rollout restart deploy/backend`). If it dies again within a minute, stop restarting and read the logs.
- **Flyway failure:** never delete rows from `flyway_schema_history` and never edit a merged migration. Roll back to the previous image; fix forward with a new migration (a duplicate version is fixed by renaming the later file, as in `9d4b165`).
- **Needs review:** raising memory limits (`docker-compose.prod.yml` `deploy.resources.limits`, `k8s/backend/deployment.yaml` `resources.limits`), rotating `JWT_SECRET` (logs every user out).

## Escalation / close

- Escalate to **platform** with the last 100 log lines and the `docker inspect` / `describe pod` output.
- Post-mortem required if the backend was down for more than 10 minutes — it is the whole product.
