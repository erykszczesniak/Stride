---
name: SyntheticProbes
description: An outside-in probe (blackbox exporter) cannot reach a target, gets the wrong status, is slow, sees a certificate about to expire, or the probe targets themselves are missing.
metadata:
  owner: domain (tier-1 targets) / platform (internal targets, probe plumbing)
  version: "1.0"
alert_type: availability
severity: critical
actionability: "Page — act now (ProbeFailed); Ticket — same working day (the rest)"
allowed-tools: []
---

# Alert: ProbeFailed / InternalProbeFailed / ProbeSlow / ProbeHttpStatusUnexpected / TlsCertificateExpiringSoon / ProbeTargetsMissing

## Summary

The blackbox exporter (compose service `blackbox-exporter`, Prometheus job
`blackbox`) opens a real HTTP or TCP connection to each target in
`monitoring/prometheus/targets/blackbox.json` every 15 s, the way a browser
or a load balancer would. `ProbeFailed` means a **user-facing** target
(frontend `/`, API health, API docs) has not answered a correct response
for 2 minutes: that is an outage from where the user stands, whatever the
application metrics say. `InternalProbeFailed` is the same for Grafana,
Prometheus, Postgres and Elasticsearch. The other four name a softer
problem: slow answers, an unexpected status code, a certificate with less
than 14 days left, or fewer probe targets than expected (the probes are
silently not running).

Usual causes, most to least frequent: the container restarted or is
unhealthy (`docker compose ps`), the path moved or was put behind
authentication (status 404 / 401), the health endpoint reports `DOWN`
because a dependency is unreachable (status 200, body not `UP`), a deploy
changed the port or the network, the targets file was edited by hand.

## Required inputs

- `instance` (the probed URL or host:port), `module`, `service`, `tier`
  from the alert labels
- Environment (compose / k8s / prod) and the alert start time
- Any deploy or compose change in the last hour
  (`git log --since=1h`, `docker compose ps --format '{{.Name}} {{.RunningFor}} {{.Status}}'`)

## Alert metadata

| Property | Value |
|---|---|
| Rule file | `monitoring/alerts/synthetic.rules.yml` |
| Expression | `probe_success{job="blackbox",tier="1"} == 0` (ProbeFailed); `tier="2"` (InternalProbeFailed); `avg_over_time(probe_duration_seconds[10m]) > 1`; `probe_http_status_code` outside 200–399; `probe_ssl_earliest_cert_expiry - time() < 14d`; `(count(up{job="blackbox"}) or vector(0)) < 7` |
| Threshold / for | ProbeFailed 2m; InternalProbeFailed 5m; ProbeSlow 10m; status 5m; certificate 1h; targets missing 5m |
| Dashboard | http://localhost:3003/d/stride-availability-outside-in |
| Exporter | http://127.0.0.1:9115 (compose, loopback only) |

## Success criteria

- `probe_success{instance="<target>"}` is 1 again and the alert resolved in
  Alertmanager (not silenced).
- The cause is named: the target, the exporter or the network — and if it
  was the target, the user-facing symptom is confirmed gone
  (`curl -i http://localhost/` or the equivalent for the target).
- For `ProbeTargetsMissing`: `count(up{job="blackbox"})` equals the number
  of entries in `targets/blackbox.json`.

## Investigation

### 1. Probe problem or real outage? Ask the exporter directly

| Command | What to look for |
|---|---|
| `curl -s 'localhost:9115/probe?module=<module>&target=<instance>&debug=true'` | The debug log at the top says exactly where the probe stopped: DNS, TCP connect, TLS, HTTP status, body regexp. `probe_success 1` here while Prometheus shows 0 means the exporter's view differs from the scrape (rare; check Prometheus targets page). |
| `curl -i <instance as seen from your machine>` (e.g. `curl -i http://localhost/`, `curl -i localhost:8081/actuator/health`) | Same answer as the exporter → the target is really broken, go to step 2. A good answer here but a failing probe → DNS or network inside the compose network (step 3). |
| `curl -s localhost:9091/api/v1/targets \| jq '.data.activeTargets[] \| select(.labels.job=="blackbox") \| {instance: .labels.instance, health, lastError}'` | Every target `up` with no `lastError`. A `lastError` on the **scrape** (not the probe) means Prometheus cannot reach the exporter itself. |

Expected result: you know whether the target, the exporter or the path
between them is at fault.

### 2. The target is really down or wrong

| Command | What to look for |
|---|---|
| `docker compose ps <service>` | `Up (healthy)`. `Restarting` or `unhealthy` → [service-down.md](service-down.md). |
| `docker compose logs --since 15m <service> \| tail -100` | Startup errors, port already in use, nginx `connect() failed` (frontend cannot reach the backend). |
| For `http://backend:8080/actuator/health` with status 200 but body `DOWN`: `curl -s -H "Authorization: Bearer $TOKEN" localhost:8081/actuator/health \| jq .components` | Which component is `DOWN` — `db` → Postgres, `elasticsearch` → ES (the app keeps serving; search falls back to SQL). |
| For a 404 / 401 on an API path: `git log -p --since=2d -- src/main/java/com/strava/backend/configuration/SecurityConfiguration.java src/main/resources/application.yml` | A moved path (`springdoc.api-docs.path`) or a `permitAll` that was removed. Fix the target in `blackbox.json` if the move was intended, otherwise fix the app. |

### 3. The exporter or the network is at fault

| Command | What to look for |
|---|---|
| `docker compose ps blackbox-exporter` and `docker compose logs --since 15m blackbox-exporter` | The container is up; `level=error` lines name a bad module or config. |
| `docker compose exec blackbox-exporter nslookup <host>` (BusyBox) | The compose DNS resolves the service name. If not: the target service is not on `strava-network` or has a different service name than the URL. |
| `docker run --rm -v "$PWD/monitoring/blackbox:/config:ro" prom/blackbox-exporter:v0.27.0 --config.check --config.file=/config/blackbox.yml` | `Config file is ok`. |

### 4. ProbeTargetsMissing

| Command | What to look for |
|---|---|
| `python3 -c 'import json; print(sum(len(g["targets"]) for g in json.load(open("monitoring/prometheus/targets/blackbox.json"))))'` | The number of targets in the file. Must equal the `< N` in the rule and the `of N` in its test. |
| `docker compose exec prometheus ls -l /etc/prometheus/targets/` | The file is mounted (`./monitoring/prometheus/targets` in `docker-compose.yml`). |
| `curl -s localhost:9091/api/v1/targets?state=any \| jq '[.data.activeTargets[] \| select(.labels.job=="blackbox")] \| length'` | The count Prometheus actually has. |

## Mitigation

- **Safe without review:** restart the failing container (`docker compose
  restart <service>`); restart `blackbox-exporter`; `curl -X POST
  localhost:9091/-/reload` after restoring the targets file from git.
- **Needs a change / second pair of eyes:** editing `blackbox.json` or
  `blackbox.yml` (a wrong regexp silences the health probe forever), lowering
  the expected target count, renewing or replacing a certificate, any change
  to `SecurityConfiguration`.

## Adding or changing a target

1. Add an entry to `monitoring/prometheus/targets/blackbox.json`: the URL
   (or `host:port` for `tcp_connect`), the `module`, and the labels
   `service`, `tier` (`"1"` pages, `"2"` tickets) and `team`.
2. Raise the expected count in `ProbeTargetsMissing`
   (`monitoring/alerts/synthetic.rules.yml`) and in its test case
   (`monitoring/alerts/tests/synthetic.test.yml`, the `of N` in the
   summary and the quiet case).
3. If the target needs a new module (POST, a different body check, a TLS
   client certificate), add it to `monitoring/blackbox/blackbox.yml` and run
   the `--config.check` command above.
4. `curl -X POST localhost:9091/-/reload` (the file is re-read every minute
   anyway) and check the new target on the dashboard.

## Escalation / close

- `ProbeFailed`: treat as [service-down.md](service-down.md) once the
  target is confirmed broken; escalate to `domain` with the exporter debug
  output and the target's logs.
- `InternalProbeFailed`, `ProbeTargetsMissing`: escalate to `platform`.
- `TlsCertificateExpiringSoon`: whoever owns the ingress or load balancer
  renews; close when `probe_ssl_earliest_cert_expiry - time()` is above 14 days.
- Close: comment on the alert in Alertmanager; a `ProbeFailed` that paged
  gets a post-mortem in `docs/postmortems/`.
