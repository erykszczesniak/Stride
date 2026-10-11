#!/usr/bin/env bash
# Lint for the Loki ruler files in monitoring/loki/rules/<tenant>/*.yml.
#
# There is no promtool for LogQL and the Loki image ships no lokitool, so the
# check is the real thing: start Loki (pinned image) with the repo's config and
# rule files, wait for /ready, then
#   1. every alert named in the files is listed by /loki/api/v1/rules
#      (the ruler drops a group it cannot parse and only logs the error), and
#   2. every `expr` is accepted by /loki/api/v1/query (a LogQL parse error is
#      an HTTP 400; an empty result is fine - there is no data).
# Also runs `loki -verify-config` on monitoring/loki/loki.yml first.
#
# Usage: monitoring/scripts/check_loki_rules.sh [monitoring/loki]
# Needs: docker, python3, curl. Exit 1 on any failure.
set -euo pipefail

LOKI_DIR="${1:-monitoring/loki}"
LOKI_IMAGE="${LOKI_IMAGE:-grafana/loki:3.3.2}"
NAME="loki-rules-lint-$$"
PORT="${LOKI_LINT_PORT:-33100}"

abs() { python3 -c 'import os,sys; print(os.path.abspath(sys.argv[1]))' "$1"; }
LOKI_DIR="$(abs "$LOKI_DIR")"

cleanup() { docker rm -f "$NAME" >/dev/null 2>&1 || true; }
trap cleanup EXIT

echo "== loki -verify-config"
docker run --rm -v "$LOKI_DIR/loki.yml:/etc/loki/loki.yml:ro" "$LOKI_IMAGE" \
  -config.file=/etc/loki/loki.yml -verify-config 2>&1 | grep -q 'config is valid' \
  || { echo "ERROR loki.yml is not valid"; exit 1; }

echo "== start $LOKI_IMAGE with $LOKI_DIR/rules"
docker run -d --name "$NAME" -p "127.0.0.1:$PORT:3100" \
  -v "$LOKI_DIR/loki.yml:/etc/loki/loki.yml:ro" \
  -v "$LOKI_DIR/rules:/loki/rules:ro" \
  "$LOKI_IMAGE" -config.file=/etc/loki/loki.yml >/dev/null

for _ in $(seq 1 60); do
  if curl -fs "http://127.0.0.1:$PORT/ready" >/dev/null 2>&1; then break; fi
  sleep 1
done
curl -fs "http://127.0.0.1:$PORT/ready" >/dev/null || { echo "ERROR Loki never became ready"; docker logs "$NAME" | tail -20; exit 1; }
# The ruler loads rule files on its first poll after start-up.
loaded=""
for _ in $(seq 1 30); do
  loaded="$(curl -fs "http://127.0.0.1:$PORT/loki/api/v1/rules" 2>/dev/null || true)"
  if echo "$loaded" | grep -q 'alert:'; then break; fi
  sleep 1
done

echo "== rules loaded by the ruler"
fail=0
expected="$(python3 - "$LOKI_DIR/rules" <<'PY'
import glob, os, sys, yaml
root = sys.argv[1]
for path in sorted(glob.glob(os.path.join(root, "*", "*.yml")) + glob.glob(os.path.join(root, "*", "*.yaml"))):
    with open(path, encoding="utf-8") as fh:
        doc = yaml.safe_load(fh) or {}
    for group in doc.get("groups", []):
        for rule in group.get("rules", []):
            if "alert" in rule:
                print(rule["alert"] + "\t" + rule["expr"].strip().replace("\n", " "))
PY
)"
[ -n "$expected" ] || { echo "ERROR no alert rules found under $LOKI_DIR/rules"; exit 1; }

while IFS=$'\t' read -r name expr; do
  if echo "$loaded" | grep -q "alert: $name\$"; then
    echo "  ok    $name loaded"
  else
    echo "  FAIL  $name is not listed by /loki/api/v1/rules"; fail=$((fail + 1))
  fi
  code="$(curl -s -o /dev/null -w '%{http_code}' -G "http://127.0.0.1:$PORT/loki/api/v1/query" --data-urlencode "query=$expr")"
  if [ "$code" = "200" ]; then
    echo "  ok    $name expression parses"
  else
    echo "  FAIL  $name expression rejected (HTTP $code): $expr"; fail=$((fail + 1))
  fi
done <<< "$expected"

if docker logs "$NAME" 2>&1 | grep -qi 'failed to load\|unable to load rules\|error parsing'; then
  echo "  FAIL  ruler logged a load error:"; docker logs "$NAME" 2>&1 | grep -i 'failed to load\|unable to load rules\|error parsing' | head -5; fail=$((fail + 1))
fi

echo
if [ "$fail" -eq 0 ]; then echo "LOKI RULES OK"; else echo "LOKI RULES FAILED: $fail problem(s)"; exit 1; fi
