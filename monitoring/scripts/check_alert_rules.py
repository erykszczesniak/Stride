#!/usr/bin/env python3
"""Contract check for Prometheus alert rules (run in CI and locally).

Every alerting rule must carry the routing labels Alertmanager matches on, the
annotations a responder needs at 3 AM, and at least one unit-test case:
promtool proves the YAML and PromQL parse; this proves the rule is *operable*.

Usage: check_alert_rules.py [monitoring/alerts]
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


def main() -> int:
    rules_dir = sys.argv[1] if len(sys.argv) > 1 else "monitoring/alerts"
    repo_root = os.path.abspath(os.path.join(rules_dir, "..", ".."))
    errors: list[str] = []
    warnings: list[str] = []

    rules = load_rules(rules_dir)
    tested = tested_alertnames(os.path.join(rules_dir, "tests"))
    seen: dict[str, str] = {}

    for source, rule in rules:
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
        if labels.get("team") not in TEAMS:
            errors.append(f"{where}: team must be one of {sorted(TEAMS)}, got {labels.get('team')!r}")
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

        if name not in tested:
            errors.append(f"{where}: no firing test case in {rules_dir}/tests/*.test.yml")

    for w in warnings:
        print(f"WARN  {w}")
    for e in errors:
        print(f"ERROR {e}")
    print(f"\n{len(rules)} alert rules checked, {len(errors)} error(s), {len(warnings)} warning(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
