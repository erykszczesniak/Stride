#!/usr/bin/env bash
# Chaos experiment runner for the composed Stride stack (docs/chaos/README.md).
#
#   chaos/run.sh chaos/experiments/backend-pause.yaml
#
# One experiment = one YAML file: a hypothesis (steady state → fault →
# expected alerts → expected recovery) and the data the runner needs. The
# runner checks the steady state with scripts/smoke.sh, starts light load,
# injects the fault into a container of THIS compose project (kill = SIGKILL to
# its main process from the host PID namespace, pause = docker pause,
# netem-delay = pumba, cpu-stress = busy loops inside it), polls Prometheus
# and Alertmanager until every expected alert is in the expected state (or
# the budget runs out), ends the fault, waits for the smoke test to pass again,
# writes chaos/results/<name>-<date>.md and exits 0 (hypothesis held),
# 1 (disproven) or 2 (setup problem). Cleanup runs on every exit path.
#
# Needs: docker (compose v2), curl, jq, bash ≥ 3.2 (macOS's /bin/bash works —
# no mapfile, no associative arrays). YAML is read through the mikefarah/yq
# image unless YQ points at a local binary.
set -euo pipefail

EXPERIMENT_FILE="${1:?usage: chaos/run.sh chaos/experiments/<name>.yaml}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE_URL="${BASE_URL:-http://localhost:8081}"
PROM_URL="${PROM_URL:-http://localhost:9091}"
ALERTMANAGER_URL="${ALERTMANAGER_URL:-http://localhost:9093}"
RESULTS_DIR="${RESULTS_DIR:-$REPO_ROOT/chaos/results}"
YQ_IMAGE="${YQ_IMAGE:-mikefarah/yq:4.44.3}"
PUMBA_IMAGE="${PUMBA_IMAGE:-gaiaadm/pumba:0.10.1}"
TC_IMAGE="${TC_IMAGE:-gaiadocker/iproute2}"
KILL_IMAGE="${KILL_IMAGE:-alpine:3.20}"
POLL_SECONDS="${POLL_SECONDS:-5}"
CURL="curl -sS --max-time 10"

# ---------------------------------------------------------------- helpers
now() { date +%s; }
ts() { date -u +%H:%M:%S; }
log() { printf '%s  %s\n' "$(ts)" "$*" >&2; }
die() { log "ERROR: $*"; exit 2; }

TIMELINE=()
T0=""
mark() { # event — appended to the timeline with the offset from T0
  local off="-"
  [ -n "$T0" ] && off="+$(( $(now) - T0 ))s"
  TIMELINE+=("| $(ts) | $off | $1 |")
  log "$1"
}

yaml_to_json() {
  if [ -n "${YQ:-}" ]; then "$YQ" -o=json '.' "$1"
  else docker run --rm -i "$YQ_IMAGE" -o=json '.' < "$1"
  fi
}
jqr() { jq -r "$@" <<< "$SPEC"; }

# The query helpers never fail the script: a transient API error is an empty
# answer, and the next poll tries again.
prom_query() { # expr → first value or empty
  $CURL -G "$PROM_URL/api/v1/query" --data-urlencode "query=$1" 2>/dev/null \
    | jq -r '.data.result[0].value[1] // empty' 2>/dev/null || true
}
prom_alert_state() { # alertname → firing|pending|"" (highest state wins)
  $CURL "$PROM_URL/api/v1/alerts" 2>/dev/null \
    | jq -r --arg a "$1" '[.data.alerts[] | select(.labels.alertname == $a) | .state] | if index("firing") then "firing" elif index("pending") then "pending" else "" end' 2>/dev/null || true
}
am_alert_active() { # alertname → count of active alerts in Alertmanager (0 on error)
  local n
  n="$($CURL "$ALERTMANAGER_URL/api/v2/alerts?active=true&silenced=false&inhibited=true&filter=alertname%3D$1" 2>/dev/null \
    | jq 'length' 2>/dev/null || true)"
  echo "${n:-0}"
}
observed_alerts() { # every non-inactive alert in Prometheus right now, "name(state)" list
  $CURL "$PROM_URL/api/v1/alerts" 2>/dev/null \
    | jq -r '[.data.alerts[] | select(.labels.alertname != "Watchdog") | "\(.labels.alertname)(\(.state))"] | unique | join(", ")' 2>/dev/null || true
}

idx() { # 0..n-1, one per line (BSD seq counts down when n is 0)
  local i=0
  while [ "$i" -lt "$1" ]; do echo "$i"; i=$((i + 1)); done
}

compare() { # value op threshold
  awk -v v="$1" -v op="$2" -v t="$3" 'BEGIN {
    if (v == "") exit 1
    if (op == ">")  exit !(v >  t); if (op == ">=") exit !(v >= t)
    if (op == "<")  exit !(v <  t); if (op == "<=") exit !(v <= t)
    if (op == "==") exit !(v == t); if (op == "!=") exit !(v != t)
    exit 1 }'
}

# ---------------------------------------------------------------- spec
[ -f "$EXPERIMENT_FILE" ] || die "no such file: $EXPERIMENT_FILE"
command -v jq >/dev/null || die "jq is required"
SPEC="$(yaml_to_json "$EXPERIMENT_FILE")"
NAME="$(jqr '.name')"
ACTION="$(jqr '.action.type')"
SERVICE="$(jqr '.action.service')"
DURATION="$(jqr '.action.duration // 300')"
HOLD="$(jqr '.action.hold_after_expectations // 15')"
NETEM_MS="$(jqr '.action.delay_ms // 400')"
NETEM_JITTER_MS="$(jqr '.action.jitter_ms // 50')"
CPU_WORKERS="$(jqr '.action.cpu_workers // 2')"
LOAD_ENABLED="$(jqr '.load.enabled // true')"
LOAD_WORKERS="$(jqr '.load.workers // 2')"
LOAD_ENDPOINTS=()
while IFS= read -r line; do [ -n "$line" ] && LOAD_ENDPOINTS+=("$line"); done < <(jqr '.load.endpoints[]? // empty')
[ "${#LOAD_ENDPOINTS[@]}" -gt 0 ] || LOAD_ENDPOINTS=(
  "/api/v1/users/me" "/api/v1/activities/my?page=0&size=5" "/api/v1/activities/feed?page=0&size=5"
  "/api/v1/dashboard" "/api/v1/clubs?page=0&size=5" "/api/v1/goals" "/api/v1/notifications?page=0&size=5")
SMOKE_WITHIN="$(jqr '.expect.recovery.smoke_within // 300')"
ALERTS_RESOLVED="$(jqr '.expect.recovery.alerts_resolved // true')"
RESOLVE_WITHIN="$(jqr '.expect.recovery.alerts_resolve_within // 120')"
N_ALERTS="$(jqr '.expect.alerts | length')"
N_QUERIES="$(jqr '.expect.queries | length')"
N_OBSERVE="$(jqr '.expect.observe | length')"   # reported at the end of the fault, never asserted
case "$ACTION" in kill|pause|netem-delay|cpu-stress) ;; *) die "unknown action.type '$ACTION' (kill|pause|netem-delay|cpu-stress)";; esac
[ "$N_ALERTS" -gt 0 ] || [ "$N_QUERIES" -gt 0 ] || die "the experiment expects nothing (expect.alerts or expect.queries)"

# ---------------------------------------------------------------- target (scoped to this compose project)
cd "$REPO_ROOT"
CID="$(docker compose ps -q "$SERVICE" 2>/dev/null || true)"
[ -n "$CID" ] || die "compose service '$SERVICE' is not running in this project (docker compose ps)"
PROJECT="$(docker inspect -f '{{ index .Config.Labels "com.docker.compose.project" }}' "$CID")"
CNAME="$(docker inspect -f '{{ .Name }}' "$CID" | sed 's#^/##')"
[ -n "$PROJECT" ] || die "container $CID carries no compose project label; refusing to touch it"
log "experiment $NAME: $ACTION on $SERVICE ($CNAME, project $PROJECT)"

# ---------------------------------------------------------------- cleanup (idempotent, runs on every exit)
LOAD_PIDS=()
PUMBA_CONTAINER=""
FAULT_ACTIVE=0
STRESS_STARTED=0
stop_load() {
  for p in "${LOAD_PIDS[@]:-}"; do
    if [ -n "$p" ]; then kill "$p" 2>/dev/null || true; fi
  done
  LOAD_PIDS=()
}
end_fault() {
  [ "$FAULT_ACTIVE" = 1 ] || return 0
  case "$ACTION" in
    pause)
      if [ "$(docker inspect -f '{{ .State.Paused }}' "$CID" 2>/dev/null)" = "true" ]; then docker unpause "$CID" >/dev/null; fi ;;
    kill) ;;  # the compose restart policy brings the container back; nothing to undo
    netem-delay)
      # pumba removes the qdisc when it ends normally; make sure it is gone either way
      if [ -n "$PUMBA_CONTAINER" ]; then docker rm -f "$PUMBA_CONTAINER" >/dev/null 2>&1 || true; fi
      docker run --rm --net="container:$CID" --cap-add NET_ADMIN "$TC_IMAGE" qdisc del dev eth0 root >/dev/null 2>&1 || true ;;
    cpu-stress)
      if [ "$STRESS_STARTED" = 1 ]; then docker exec "$CID" sh -c 'pkill -f "yes > /dev/null" || true' >/dev/null 2>&1 || true; fi ;;
  esac
  FAULT_ACTIVE=0
}
cleanup() { stop_load; end_fault; }
trap cleanup EXIT INT TERM

# ---------------------------------------------------------------- steady state
mkdir -p "$RESULTS_DIR"
mark "steady state: scripts/smoke.sh"
if ! scripts/smoke.sh >"$RESULTS_DIR/.smoke-before.log" 2>&1; then
  cat "$RESULTS_DIR/.smoke-before.log" >&2
  die "steady state not met — smoke test fails before the fault; nothing injected"
fi
rm -f "$RESULTS_DIR/.smoke-before.log"
mark "steady state OK"

# ---------------------------------------------------------------- load
TOKEN=""
login() {
  TOKEN="$($CURL -X POST "$BASE_URL/api/v1/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"${SMOKE_USER:-john@example.com}\",\"password\":\"${SMOKE_PASSWORD:-password123}\"}" 2>/dev/null \
    | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')"
}
load_worker() { # endless loop over the endpoints; the token is refreshed on 401
  local tok="$1" code
  while :; do
    for ep in "${LOAD_ENDPOINTS[@]}"; do
      code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 -H "Authorization: Bearer $tok" "$BASE_URL$ep" || true)"
      if [ "$code" = "401" ]; then login; tok="$TOKEN"; fi
      sleep 0.2
    done
  done
}
if [ "$LOAD_ENABLED" = "true" ]; then
  login
  [ -n "$TOKEN" ] || die "login failed; cannot generate load"
  for _ in $(seq 1 "$LOAD_WORKERS"); do load_worker "$TOKEN" & LOAD_PIDS+=("$!"); done
  mark "load: $LOAD_WORKERS worker(s) over ${#LOAD_ENDPOINTS[@]} endpoint(s)"
  sleep 10
fi

# ---------------------------------------------------------------- fault
T0=$(now)
FAULT_ACTIVE=1
case "$ACTION" in
  pause) docker pause "$CID" >/dev/null; mark "fault: docker pause $CNAME" ;;
  kill)
    # SIGKILL to the container's main process, sent from the host PID namespace
    # (a helper container with --pid=host), so the process dies the way an
    # OOM-kill or a JVM crash would and the restart policy applies. Two things
    # that do not work, both measured (chaos/results/backend-kill-*-disproven.md):
    # `docker kill` is a manual stop and Docker ignores the restart policy
    # afterwards; `kill -9 1` from inside the container is dropped by the
    # kernel, because a PID namespace's init cannot be SIGKILLed from within.
    HPID="$(docker inspect -f '{{ .State.Pid }}' "$CID")"
    [ "${HPID:-0}" -gt 0 ] || die "cannot find the host PID of $CNAME"
    docker run --rm --pid=host "$KILL_IMAGE" kill -9 "$HPID"
    mark "fault: SIGKILL to $CNAME's main process (host pid $HPID) from the host PID namespace — a crash; the restart policy is the recovery under test" ;;
  netem-delay)
    PUMBA_CONTAINER="chaos-pumba-$NAME-$T0"
    # pumba does what `tc qdisc add ... netem delay` would do, inside the target's
    # network namespace, and removes the qdisc when the duration is over.
    docker run --rm -d --name "$PUMBA_CONTAINER" -v /var/run/docker.sock:/var/run/docker.sock "$PUMBA_IMAGE" \
      netem --duration "${DURATION}s" --tc-image "$TC_IMAGE" \
      delay --time "$NETEM_MS" --jitter "$NETEM_JITTER_MS" --distribution normal "$CNAME" >/dev/null
    sleep 3
    QDISC="$(docker run --rm --net="container:$CID" --cap-add NET_ADMIN "$TC_IMAGE" qdisc show dev eth0 2>/dev/null | grep -o 'netem.*' || true)"
    [ -n "$QDISC" ] || die "netem qdisc not present on $CNAME after starting pumba"
    mark "fault: netem on $CNAME — $QDISC" ;;
  cpu-stress)
    for _ in $(seq 1 "$CPU_WORKERS"); do docker exec -d "$CID" sh -c 'yes > /dev/null' ; done
    STRESS_STARTED=1; mark "fault: $CPU_WORKERS busy loop(s) inside $CNAME" ;;
esac

# ---------------------------------------------------------------- expectations during the fault
ALERT_SEEN_AT=(); ALERT_RESULT=()
QUERY_SEEN_AT=(); QUERY_RESULT=()
for i in $(idx "$N_ALERTS"); do ALERT_RESULT[i]="not seen"; done
for i in $(idx "$N_QUERIES"); do QUERY_RESULT[i]="not met"; done
DEADLINE=$((T0 + DURATION))
OBSERVED=""
all_met=0
while :; do
  elapsed=$(( $(now) - T0 ))
  pending_any=0
  for i in $(idx "$N_ALERTS"); do
    [ -z "${ALERT_SEEN_AT[$i]:-}" ] || continue
    a="$(jqr ".expect.alerts[$i].alertname")"; want="$(jqr ".expect.alerts[$i].state // \"firing\"")"
    within="$(jqr ".expect.alerts[$i].within // $DURATION")"
    pstate="$(prom_alert_state "$a")"
    if [ "$want" = "firing" ]; then
      if [ "$pstate" = "firing" ] && [ "$(am_alert_active "$a")" -gt 0 ]; then
        ALERT_SEEN_AT[i]=$elapsed
        if [ "$elapsed" -le "$within" ]; then ALERT_RESULT[i]="firing in Prometheus and active in Alertmanager after ${elapsed}s (budget ${within}s)"; mark "alert $a firing, in Alertmanager (+${elapsed}s)"
        else ALERT_RESULT[i]="LATE: firing after ${elapsed}s, budget was ${within}s"; mark "alert $a firing but late (+${elapsed}s > ${within}s)"; fi
      fi
    else
      if [ "$pstate" = "pending" ] || [ "$pstate" = "firing" ]; then
        ALERT_SEEN_AT[i]=$elapsed
        if [ "$elapsed" -le "$within" ]; then ALERT_RESULT[i]="$pstate in Prometheus after ${elapsed}s (budget ${within}s)"; mark "alert $a $pstate (+${elapsed}s)"
        else ALERT_RESULT[i]="LATE: $pstate after ${elapsed}s, budget was ${within}s"; mark "alert $a $pstate but late (+${elapsed}s > ${within}s)"; fi
      fi
    fi
    [ -n "${ALERT_SEEN_AT[$i]:-}" ] || pending_any=1
  done
  for i in $(idx "$N_QUERIES"); do
    expr="$(jqr ".expect.queries[$i].expr")"; op="$(jqr ".expect.queries[$i].op")"; val="$(jqr ".expect.queries[$i].value")"
    qname="$(jqr ".expect.queries[$i].name")"; within="$(jqr ".expect.queries[$i].within // $DURATION")"
    hold="$(jqr ".expect.queries[$i].hold // false")"
    if [ -n "${QUERY_SEEN_AT[$i]:-}" ]; then
      # hold: true — a condition that must stay true for the whole fault, re-checked every poll
      if [ "$hold" = "true" ] && ! compare "$(prom_query "$expr")" "$op" "$val"; then
        QUERY_RESULT[i]="VIOLATED after ${elapsed}s: '$expr' no longer $op $val"; mark "query '$qname' violated (+${elapsed}s)"
      fi
      continue
    fi
    v="$(prom_query "$expr")"
    if compare "$v" "$op" "$val"; then
      QUERY_SEEN_AT[i]=$elapsed
      if [ "$elapsed" -le "$within" ]; then QUERY_RESULT[i]="met after ${elapsed}s: value $v $op $val (budget ${within}s)"; mark "query '$qname' met: $v $op $val (+${elapsed}s)"
      else QUERY_RESULT[i]="LATE: met after ${elapsed}s (value $v), budget was ${within}s"; mark "query '$qname' met late (+${elapsed}s)"; fi
    fi
    [ -n "${QUERY_SEEN_AT[$i]:-}" ] || pending_any=1
  done
  obs="$(observed_alerts)"
  if [ -n "$obs" ] && [ "$obs" != "$OBSERVED" ]; then OBSERVED="$obs"; mark "alerts now: $obs"; fi
  if [ "$pending_any" = 0 ]; then all_met=1; break; fi
  if [ "$(now)" -ge "$DEADLINE" ]; then mark "fault budget of ${DURATION}s exhausted"; break; fi
  sleep "$POLL_SECONDS"
done
if [ "$all_met" = 1 ] && [ "$HOLD" -gt 0 ]; then sleep "$HOLD"; fi
for i in $(idx "$N_QUERIES"); do   # a held condition that was never violated held for the whole fault
  if [ "$(jqr ".expect.queries[$i].hold // false")" = "true" ]; then
    case "${QUERY_RESULT[$i]}" in met*) QUERY_RESULT[i]="${QUERY_RESULT[$i]}; held until the fault ended";; esac
  fi
done
OBSERVE_ROWS=()
for i in $(idx "$N_OBSERVE"); do
  v="$(prom_query "$(jqr ".expect.observe[$i].expr")")"
  OBSERVE_ROWS+=("| \`$(jqr ".expect.observe[$i].name")\` | ${v:-no data} |")
  mark "observed '$(jqr ".expect.observe[$i].name")' = ${v:-no data} at the end of the fault"
done

# ---------------------------------------------------------------- recovery
end_fault
FAULT_END=$(now)   # the fault is over once the cleanup (unpause, qdisc removal) has returned
if [ "$ACTION" = "kill" ]; then
  FAULT_END=$T0   # the kill is instantaneous; recovery is measured from it
  mark "kill is instantaneous — recovery clock runs from the kill"
else
  mark "fault ended after $((FAULT_END - T0))s"
fi
stop_load
RECOVERED=""
RECOVERY_RESULT="smoke did not pass within ${SMOKE_WITHIN}s"
if [ "$ACTION" = "kill" ]; then
  for _ in $(seq 1 60); do
    [ "$(docker inspect -f '{{ .State.Running }}' "$CID" 2>/dev/null)" = "true" ] && break; sleep 2
  done
  mark "container running again: $(docker inspect -f '{{ .State.Running }}' "$CID" 2>/dev/null) (restart policy $(docker inspect -f '{{ .HostConfig.RestartPolicy.Name }}' "$CID" 2>/dev/null))"
fi
while [ $(( $(now) - FAULT_END )) -le "$SMOKE_WITHIN" ]; do
  if scripts/smoke.sh >"$RESULTS_DIR/.smoke-after.log" 2>&1; then
    RECOVERED=$(( $(now) - FAULT_END ))
    RECOVERY_RESULT="smoke passes ${RECOVERED}s after the fault ended (budget ${SMOKE_WITHIN}s)"
    mark "recovery: $RECOVERY_RESULT"
    break
  fi
  sleep 15
done
[ -n "$RECOVERED" ] || { mark "recovery: $RECOVERY_RESULT"; tail -20 "$RESULTS_DIR/.smoke-after.log" >&2 || true; }
rm -f "$RESULTS_DIR/.smoke-after.log"
RESOLVED_RESULT="not checked"
if [ "$ALERTS_RESOLVED" = "true" ] && [ "$N_ALERTS" -gt 0 ]; then
  RESOLVED_RESULT="all expected alerts resolved in Alertmanager"
  for i in $(idx "$N_ALERTS"); do
    a="$(jqr ".expect.alerts[$i].alertname")"; want="$(jqr ".expect.alerts[$i].state // \"firing\"")"
    [ "$want" = "firing" ] || continue
    ok=0
    while [ $(( $(now) - FAULT_END )) -le "$RESOLVE_WITHIN" ]; do
      if [ "$(am_alert_active "$a")" -eq 0 ]; then ok=1; break; fi; sleep 5
    done
    if [ "$ok" = 1 ]; then mark "alert $a resolved in Alertmanager (+$(( $(now) - FAULT_END ))s after fault end)"
    else RESOLVED_RESULT="alert $a still active in Alertmanager ${RESOLVE_WITHIN}s after the fault ended"; mark "$RESOLVED_RESULT"; fi
  done
fi

# ---------------------------------------------------------------- verdict + report
VERDICT="held"
for i in $(idx "$N_ALERTS"); do case "${ALERT_RESULT[$i]}" in "not seen"|LATE:*) VERDICT="disproven";; esac; done
for i in $(idx "$N_QUERIES"); do case "${QUERY_RESULT[$i]}" in "not met"|LATE:*|VIOLATED*) VERDICT="disproven";; esac; done
[ -n "$RECOVERED" ] || VERDICT="disproven"
[ "$RESOLVED_RESULT" = "not checked" ] || [ "$RESOLVED_RESULT" = "all expected alerts resolved in Alertmanager" ] || VERDICT="disproven"

mkdir -p "$RESULTS_DIR"
OUT="$RESULTS_DIR/$NAME-$(date -u +%Y-%m-%d).md"
{
  echo "# Chaos experiment: $NAME"
  echo
  echo "**Verdict: hypothesis $VERDICT.** Run $(date -u +%Y-%m-%dT%H:%M:%SZ) on \`$(uname -s) $(uname -m)\`, compose project \`$PROJECT\`, target \`$CNAME\`."
  echo
  echo "## Hypothesis"
  echo
  echo "- **Steady state:** $(jqr '.hypothesis.steady_state')"
  echo "- **Fault:** $(jqr '.hypothesis.fault')"
  echo "- **Expected alerts:** $(jqr '.hypothesis.expected_alerts')"
  echo "- **Expected recovery:** $(jqr '.hypothesis.expected_recovery')"
  n="$(jqr '.hypothesis.notes // empty')"; [ -n "$n" ] && { echo; echo "$n"; }
  echo
  echo "## Measured"
  echo
  echo "| Expectation | Result |"
  echo "|---|---|"
  for i in $(idx "$N_ALERTS"); do
    echo "| alert \`$(jqr ".expect.alerts[$i].alertname")\` $(jqr ".expect.alerts[$i].state // \"firing\"") within $(jqr ".expect.alerts[$i].within // $DURATION")s | ${ALERT_RESULT[$i]} |"
  done
  for i in $(idx "$N_QUERIES"); do
    echo "| query \`$(jqr ".expect.queries[$i].name")\` $(jqr ".expect.queries[$i].op") $(jqr ".expect.queries[$i].value") within $(jqr ".expect.queries[$i].within // $DURATION")s | ${QUERY_RESULT[$i]} |"
  done
  echo "| recovery: smoke within ${SMOKE_WITHIN}s | $RECOVERY_RESULT |"
  echo "| recovery: alerts resolved (budget ${RESOLVE_WITHIN}s) | $RESOLVED_RESULT |"
  echo
  if [ "$N_OBSERVE" -gt 0 ]; then
    echo "Observed at the end of the fault (reported, not asserted):"
    echo
    echo "| Query | Value |"
    echo "|---|---|"
    printf '%s\n' "${OBSERVE_ROWS[@]}"
    echo
  fi
  tta=""
  for i in $(idx "$N_ALERTS"); do
    if [ -n "${ALERT_SEEN_AT[$i]:-}" ]; then tta="${tta:+$tta, }$(jqr ".expect.alerts[$i].alertname") $(jqr ".expect.alerts[$i].state // \"firing\"") ${ALERT_SEEN_AT[$i]}s"; fi
  done
  tta="${tta:-—}"
  echo "**Time to alert:** $tta · **Fault duration:** $((FAULT_END - T0))s · **Time to recover:** ${RECOVERED:-—}${RECOVERED:+s}"
  echo
  echo "## Timeline"
  echo
  echo "| UTC | Offset | Event |"
  echo "|---|---|---|"
  printf '%s\n' "${TIMELINE[@]}"
} > "$OUT"
log "report: $OUT"
echo "RESULT $NAME $VERDICT time_to_alert=$(printf '%s' "$tta" | tr ' ' '_') time_to_recover=${RECOVERED:-none}"
[ "$VERDICT" = "held" ]
