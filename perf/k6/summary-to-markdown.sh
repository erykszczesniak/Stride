#!/usr/bin/env bash
# Turn a k6 `--summary-export` JSON file into a Markdown table for a PR
# comment or $GITHUB_STEP_SUMMARY. Prints the table; exits 1 if any threshold
# was breached (so a caller can fail a step after publishing the numbers).
#
#   perf/k6/summary-to-markdown.sh perf/k6/out/summary.json >> "$GITHUB_STEP_SUMMARY"
#
# In the legacy export format every metric carries `thresholds: {"<expr>": <bool>}`
# where the boolean is true when the threshold was *breached*.
set -euo pipefail

file="${1:?usage: summary-to-markdown.sh <summary.json>}"

reqs=$(jq -r '.metrics.http_reqs.count // 0' "$file")
iters=$(jq -r '.metrics.iterations.count // 0' "$file")
rps=$(jq -r '.metrics.http_reqs.rate // 0 | . * 100 | round / 100' "$file")
failed=$(jq -r '.metrics.http_req_failed.value // 0 | . * 10000 | round / 100' "$file")
p95=$(jq -r '.metrics.http_req_duration["p(95)"] // 0 | . * 100 | round / 100' "$file")
p99=$(jq -r '.metrics.http_req_duration["p(99)"] // 0 | . * 100 | round / 100' "$file")
checks=$(jq -r '.metrics.checks | "\(.passes // 0) / \((.passes // 0) + (.fails // 0))"' "$file")

echo "## k6 user journey"
echo
echo "| Metric | Value | SLO (docs/slo.md) |"
echo "|---|---|---|"
echo "| Requests | $reqs ($rps req/s, $iters iterations) | |"
echo "| Failed requests | ${failed} % | < 0.5 % (edge availability 99.5 %) |"
echo "| p95 latency | ${p95} ms | < 500 ms (edge latency) |"
echo "| p99 latency | ${p99} ms | |"
echo "| Checks passed | $checks | all |"
echo
echo "### Thresholds"
echo
echo "| Metric | Threshold | p95 / rate | Result |"
echo "|---|---|---|---|"
jq -r '
  .metrics | to_entries[]
  | select(.value.thresholds != null)
  | .key as $m
  | .value as $v
  | $v.thresholds | to_entries[]
  | "| `\($m)` | `\(.key)` | \(
        if $v["p(95)"] != null then ((($v["p(95)"] * 100) | round) / 100 | tostring) + " ms"
        elif $v.value != null then ((($v.value * 10000) | round) / 100 | tostring) + " %"
        else "-" end
      ) | \(if .value then "**FAIL**" else "pass" end) |"
' "$file" | sort

breached=$(jq -r '[.metrics[] | .thresholds // {} | to_entries[] | select(.value)] | length' "$file")
echo
if [ "$breached" -gt 0 ]; then
  echo "**$breached threshold(s) breached.**"
  exit 1
fi
echo "All thresholds passed."
