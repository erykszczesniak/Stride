#!/usr/bin/env python3
"""Contract check for alert rules (run in CI and locally).

Every alerting rule must carry the routing labels Alertmanager matches on, the
annotations a responder needs at 3 AM, and at least one unit-test case:
promtool proves the YAML and PromQL parse; this proves the rule is *operable*.

Two rule sources share one Alertmanager and therefore one contract:

* Prometheus rules in monitoring/alerts/*.rules.yml (PromQL), unit-tested
  with promtool in monitoring/alerts/tests/.
* Loki ruler rules in monitoring/loki/rules/<tenant>/*.yml (LogQL). There
  is no promtool for LogQL, so these are exempt from the test-case rule;
  the exemption is printed as a NOTE per rule so it never goes unnoticed, and
  monitoring/scripts/check_loki_rules.sh covers syntax by loading the files
  into a real Loki. Alert names must be unique across both sources.

Usage: check_alert_rules.py [monitoring/alerts] [--loki-rules monitoring/loki/rules | --no-loki]
The Loki directory defaults to <repo>/monitoring/loki/rules when it exists.
Exit 1 on any error. Warnings (missing runbook file) do not fail the build yet.
"""
from __future__ import annotations

import glob
import os
import re
import sys

import yaml

SEVERITIES = {"critical", "warning", "info"}
TEAMS = {"platform", "domain", "ai", "data"}
TIERS = {"1", "2"}
REQUIRED_LABELS = ("severity", "team", "tier", "service")
REQUIRED_ANNOTATIONS = ("summary", "description", "runbook_url")
RUNBOOK_PREFIX = "https://github.com/erykszczesniak/Stride/blob/"
RUNBOOK_RE = re.compile(r"/docs/runbooks/([a-z0-9-]+\.md)$")


def load_rules(rules_dir: str) -> list[tuple[str, dict]]:
    out: list[tuple[str, dict]] = []
    for path in sorted(glob.glob(os.path.join(rules_dir, "*.rules.yml"))):
        with open(path, encoding="utf-8") as fh:
            doc = yaml.safe_load(fh) or {}
        for group in doc.get("groups", []):
            for rule in group.get("rules", []):
                if "alert" in rule:
                    out.append((os.path.basename(path), rule))
    return out


def load_loki_rules(loki_rules_dir: str) -> list[tuple[str, dict]]:
    """Loki ruler layout: <dir>/<tenant>/<file>.yml, Prometheus rule-group format."""
    out: list[tuple[str, dict]] = []
    paths = glob.glob(os.path.join(loki_rules_dir, "*", "*.yml")) + glob.glob(os.path.join(loki_rules_dir, "*", "*.yaml"))
    for path in sorted(paths):
        with open(path, encoding="utf-8") as fh:
            doc = yaml.safe_load(fh) or {}
        tenant = os.path.basename(os.path.dirname(path))
        for group in doc.get("groups", []):
            for rule in group.get("rules", []):
                if "alert" in rule:
                    out.append((f"loki/{tenant}/{os.path.basename(path)}", rule))
    return out


def tested_alertnames(tests_dir: str) -> set[str]:
    names: set[str] = set()
    for path in glob.glob(os.path.join(tests_dir, "*.test.yml")):
        with open(path, encoding="utf-8") as fh:
            doc = yaml.safe_load(fh) or {}
        for test in doc.get("tests", []):
            for case in test.get("alert_rule_test", []) or []:
                # A case counts only if it expects the alert to fire at least once.
                if case.get("exp_alerts"):
                    names.add(case["alertname"])
    return names


def parse_args(argv: list[str]) -> tuple[str, str | None]:
    rules_dir = "monitoring/alerts"
    loki_dir: str | None = None
    no_loki = False
    positional: list[str] = []
    i = 0
    while i < len(argv):
        if argv[i] == "--loki-rules":
            loki_dir = argv[i + 1]
            i += 2
        elif argv[i] == "--no-loki":
            no_loki = True
            i += 1
        else:
            positional.append(argv[i])
            i += 1
    if positional:
        rules_dir = positional[0]
    if loki_dir is None and not no_loki:
        candidate = os.path.join(rules_dir, "..", "loki", "rules")
        if os.path.isdir(candidate):
            loki_dir = os.path.normpath(candidate)
    return rules_dir, loki_dir


def main() -> int:
    rules_dir, loki_dir = parse_args(sys.argv[1:])
    repo_root = os.path.abspath(os.path.join(rules_dir, "..", ".."))
    errors: list[str] = []
    warnings: list[str] = []
    notes: list[str] = []

    rules = [("prometheus", s, r) for s, r in load_rules(rules_dir)]
    loki_rules = [("loki", s, r) for s, r in load_loki_rules(loki_dir)] if loki_dir else []
    tested = tested_alertnames(os.path.join(rules_dir, "tests"))
    seen: dict[str, str] = {}

    for kind, source, rule in rules + loki_rules:
        name = rule["alert"]
        where = f"{source}:{name}"
        if name in seen:
            errors.append(f"{where}: duplicate alert name (also in {seen[name]})")
        seen[name] = source

        labels = rule.get("labels") or {}
        ann = rule.get("annotations") or {}

        for key in REQUIRED_LABELS:
            if key not in labels:
                errors.append(f"{where}: missing label '{key}'")
        if labels.get("severity") not in SEVERITIES:
            errors.append(f"{where}: severity must be one of {sorted(SEVERITIES)}, got {labels.get('severity')!r}")
        team = labels.get("team")
        # A templated team ("{{ $labels.team }}") is allowed for alerts owned by
        # whichever team the series belongs to (cost attribution); everything
        # else must name a team so Alertmanager routing is static.
        if team not in TEAMS and team != "{{ $labels.team }}":
            errors.append(f"{where}: team must be one of {sorted(TEAMS)} or '{{{{ $labels.team }}}}', got {team!r}")
        if str(labels.get("tier")) not in TIERS:
            errors.append(f"{where}: tier must be \"1\" or \"2\", got {labels.get('tier')!r}")

        for key in REQUIRED_ANNOTATIONS:
            if not ann.get(key):
                errors.append(f"{where}: missing annotation '{key}'")
        runbook = ann.get("runbook_url", "")
        if runbook and not runbook.startswith(RUNBOOK_PREFIX):
            errors.append(f"{where}: runbook_url must point into this repo ({RUNBOOK_PREFIX}...)")
        m = RUNBOOK_RE.search(runbook)
        if m:
            runbook_path = os.path.join(repo_root, "docs", "runbooks", m.group(1))
            if not os.path.exists(runbook_path):
                warnings.append(f"{where}: runbook file docs/runbooks/{m.group(1)} does not exist yet")

        if name != "Watchdog" and "for" not in rule:
            warnings.append(f"{where}: no 'for' duration — fires on a single sample")
        if labels.get("severity") == "critical" and str(rule.get("for", "0m")) in {"0m", "0s"} and name not in {
            "DbPoolExhausted", "AlertmanagerNotificationsFailing", "PrometheusRuleEvaluationFailures"
        }:
            warnings.append(f"{where}: critical with for: 0m — make sure this cannot flap")

        if kind == "loki":
            expr = str(rule.get("expr", ""))
            if "{" not in expr or "}" not in expr:
                errors.append(f"{where}: LogQL expr has no stream selector")
            # No promtool for LogQL: say so for every rule rather than silently
            # skipping, so the gap stays visible in the CI log.
            notes.append(f"{where}: LogQL rule, no unit test (not supported); syntax covered by check_loki_rules.sh")
        elif name not in tested:
            errors.append(f"{where}: no firing test case in {rules_dir}/tests/*.test.yml")

    for n in notes:
        print(f"NOTE  {n}")
    for w in warnings:
        print(f"WARN  {w}")
    for e in errors:
        print(f"ERROR {e}")
    loki_summary = f" + {len(loki_rules)} Loki rules ({loki_dir})" if loki_dir else ""
    print(f"\n{len(rules)} Prometheus alert rules{loki_summary} checked, {len(errors)} error(s), {len(warnings)} warning(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
