#!/usr/bin/env python3
"""Generate monitoring/alerts/slo.rules.yml from the OpenSLO files in slo/.

The OpenSLO documents are the single source of the SLOs (docs/slo.md explains
them); this script turns them into Prometheus recording and alerting rules
following the multi-window, multi-burn-rate recipe. It is deterministic: the
same input always produces the same bytes, so CI regenerates the file and fails
on `git diff`.

    generate_slo_rules.py                # write monitoring/alerts/slo.rules.yml
    generate_slo_rules.py --check        # validate and exit 1 if the file is stale
    generate_slo_rules.py --slo-dir slo --output monitoring/alerts/slo.rules.yml

Only python3 and PyYAML. Field conventions: slo/README.md.
"""
from __future__ import annotations

import argparse
import glob
import os
import re
import sys
from decimal import Decimal

import yaml

ANN = "stride.io/"
DURATION_RE = re.compile(r"^(\d+)(s|m|h|d|w)$")
SECONDS = {"s": 1, "m": 60, "h": 3600, "d": 86400, "w": 604800}
SEVERITIES = {"critical", "warning", "info"}
HEADER = """\
# GENERATED FILE — do not edit.
#
# Source: slo/*.yaml (OpenSLO v1). Regenerate with `make slo` or
#   python3 monitoring/scripts/generate_slo_rules.py
# CI (monitoring.yml, job `rules`) regenerates and fails on drift.
#
# Alerting follows the multi-window, multi-burn-rate recipe from the SRE
# workbook: a fast burn (14.4x budget, 1h and 5m windows both above) pages
# because the 30-day budget would be gone in ~2 days; a slow burn (6x over
# 6h and 30m) opens a ticket. The short window is a twelfth of the long one:
# it makes the alert stop as soon as the problem does, while the long window
# keeps a 5-minute blip from paging. Recording rules carry the error ratio per
# window; the alert divides it by the error budget (1 - target).
#
# Objectives (docs/slo.md):
"""


class SloError(Exception):
    pass


# ----------------------------------------------------------------- helpers
def get(doc: dict, path: str, where: str, required: bool = True):
    cur = doc
    for key in path.split("."):
        if isinstance(cur, list):
            try:
                cur = cur[int(key)]
            except (ValueError, IndexError):
                cur = None
        elif isinstance(cur, dict):
            cur = cur.get(key)
        else:
            cur = None
        if cur is None:
            if required:
                raise SloError(f"{where}: missing required field '{path}'")
            return None
    return cur


def annotation(obj: dict, name: str, where: str, required: bool = True):
    """metadata.annotations["stride.io/<name>"] (the key has a dot, so not via get())."""
    value = ((obj.get("metadata") or {}).get("annotations") or {}).get(ANN + name)
    if value is None and required:
        raise SloError(f"{where}: missing required annotation 'metadata.annotations.{ANN}{name}'")
    return value


def duration_seconds(value: str, where: str) -> int:
    m = DURATION_RE.match(str(value))
    if not m:
        raise SloError(f"{where}: '{value}' is not a Prometheus duration (e.g. 5m, 1h, 30d)")
    return int(m.group(1)) * SECONDS[m.group(2)]


def short_window(long: str, where: str) -> str:
    """The short window of a burn-rate pair is a twelfth of the long one (1h → 5m, 6h → 30m)."""
    secs = duration_seconds(long, where)
    if secs % 12:
        raise SloError(f"{where}: lookbackWindow {long} is not divisible by 12")
    short = secs // 12
    for unit in ("d", "h", "m", "s"):
        if short % SECONDS[unit] == 0:
            return f"{short // SECONDS[unit]}{unit}"
    raise AssertionError("unreachable")


def number(value, where: str) -> str:
    """Render a YAML number the way a human wrote it: 6 → '6', 14.4 → '14.4'."""
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise SloError(f"{where}: expected a number, got {value!r}")
    return str(value)


def budget_of(target, where: str) -> str:
    if not isinstance(target, (int, float)) or not 0 < target < 1:
        raise SloError(f"{where}: objectives[0].target must be a number in (0, 1), got {target!r}")
    return format(Decimal("1") - Decimal(str(target)), "f")


def target_percent(target) -> str:
    pct = Decimal(str(target)) * 100
    return format(pct.normalize(), "f")


def yaml_scalar(value: str) -> str:
    """Quote a label/annotation value for the generated YAML (always double quotes
    when it could be misread: templates, numbers, punctuation)."""
    s = str(value)
    if re.fullmatch(r"[A-Za-z][A-Za-z0-9_./:-]*", s) and s not in {"true", "false", "null", "yes", "no"}:
        return s
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


# ------------------------------------------------------------- validation
def load_slos(slo_dir: str) -> list[tuple[str, dict]]:
    files = sorted(glob.glob(os.path.join(slo_dir, "*.yaml")))
    if not files:
        raise SloError(f"no *.yaml files in {slo_dir}")
    out = []
    for path in files:
        with open(path, encoding="utf-8") as fh:
            doc = yaml.safe_load(fh)
        validate(doc, os.path.basename(path))
        out.append((os.path.basename(path), doc))
    names = [get(d, "metadata.name", f) for f, d in out]
    dupes = {n for n in names if names.count(n) > 1}
    if dupes:
        raise SloError(f"duplicate SLO names: {sorted(dupes)}")
    return out


def validate(doc: dict, where: str) -> None:
    if not isinstance(doc, dict):
        raise SloError(f"{where}: not a YAML mapping")
    if doc.get("apiVersion") != "openslo/v1":
        raise SloError(f"{where}: apiVersion must be 'openslo/v1'")
    if doc.get("kind") != "SLO":
        raise SloError(f"{where}: kind must be 'SLO'")
    name = get(doc, "metadata.name", where)
    if not re.fullmatch(r"[a-z0-9]([a-z0-9-]*[a-z0-9])?", str(name)):
        raise SloError(f"{where}: metadata.name '{name}' must be lowercase kebab-case")
    get(doc, "metadata.displayName", where)
    for label in ("team", "service", "tier"):
        get(doc, f"metadata.labels.{label}", where)
    if str(get(doc, "metadata.labels.tier", where)) not in {"1", "2"}:
        raise SloError(f"{where}: metadata.labels.tier must be \"1\" or \"2\"")
    for ann in ("recording-rule", "windows"):
        annotation(doc, ann, where)
    get(doc, "spec.description", where)
    get(doc, "spec.service", where)

    ratio = get(doc, "spec.indicator.spec.ratioMetric", where)
    if ("good" in ratio) == ("bad" in ratio):
        raise SloError(f"{where}: ratioMetric needs exactly one of 'good' or 'bad'")
    for part in ("good" if "good" in ratio else "bad", "total"):
        src = get(ratio, f"{part}.metricSource", where)
        if src.get("type") != "Prometheus":
            raise SloError(f"{where}: ratioMetric.{part}.metricSource.type must be 'Prometheus'")
        query = get(src, "spec.query", where)
        if "[" in query or "]" in query:
            raise SloError(f"{where}: ratioMetric.{part} query must be a selector without a range ([...]); the generator adds the window")

    windows = get(doc, "spec.timeWindow", where)
    if not isinstance(windows, list) or len(windows) != 1:
        raise SloError(f"{where}: spec.timeWindow must have exactly one entry")
    duration_seconds(get(windows[0], "duration", where), where)
    if windows[0].get("isRolling") is not True:
        raise SloError(f"{where}: spec.timeWindow[0].isRolling must be true (calendar windows are not supported)")
    get(doc, "spec.budgetingMethod", where)
    objectives = get(doc, "spec.objectives", where)
    if not isinstance(objectives, list) or len(objectives) != 1:
        raise SloError(f"{where}: spec.objectives must have exactly one entry")
    budget_of(objectives[0].get("target"), where)

    for w in str(annotation(doc, "windows", where)).split(","):
        duration_seconds(w.strip(), where)
    budget_rule = annotation(doc, "error-budget-rule", where, required=False)
    if budget_rule and not annotation(doc, "error-budget-slo-label", where, required=False):
        raise SloError(f"{where}: {ANN}error-budget-rule needs {ANN}error-budget-slo-label")

    policies = get(doc, "spec.alertPolicies", where)
    if not isinstance(policies, list) or not policies:
        raise SloError(f"{where}: spec.alertPolicies must be a non-empty list")
    seen = set()
    for i, policy in enumerate(policies):
        pw = f"{where}: alertPolicies[{i}]"
        if policy.get("kind") != "AlertPolicy" or policy.get("apiVersion") != "openslo/v1":
            raise SloError(f"{pw}: must be an inline openslo/v1 AlertPolicy")
        get(policy, "metadata.name", pw)
        for ann in ("alertname", "summary", "description", "runbook-url"):
            annotation(policy, ann, pw)
        alertname = policy["metadata"]["annotations"][ANN + "alertname"]
        if alertname in seen:
            raise SloError(f"{pw}: duplicate alert name {alertname}")
        seen.add(alertname)
        kind = policy["metadata"]["annotations"].get(ANN + "rule-kind", "burn-rate")
        if kind not in {"burn-rate", "budget-exhausted"}:
            raise SloError(f"{pw}: {ANN}rule-kind must be 'burn-rate' or 'budget-exhausted'")
        if kind == "budget-exhausted" and not budget_rule:
            raise SloError(f"{pw}: budget-exhausted alert needs {ANN}error-budget-rule on the SLO")
        conditions = get(policy, "spec.conditions", pw)
        if not isinstance(conditions, list) or len(conditions) != 1:
            raise SloError(f"{pw}: spec.conditions must have exactly one entry")
        cond = conditions[0]
        if cond.get("kind") != "AlertCondition":
            raise SloError(f"{pw}: condition kind must be 'AlertCondition'")
        if get(cond, "spec.severity", pw) not in SEVERITIES:
            raise SloError(f"{pw}: severity must be one of {sorted(SEVERITIES)}")
        c = get(cond, "spec.condition", pw)
        if c.get("kind") != "burnrate" or c.get("op") != "gt":
            raise SloError(f"{pw}: condition must be kind: burnrate, op: gt")
        number(c.get("threshold"), pw)
        duration_seconds(get(c, "lookbackWindow", pw), pw)
        duration_seconds(get(c, "alertAfter", pw), pw)
        if kind == "burn-rate":
            short_window(c["lookbackWindow"], pw)
            if c["lookbackWindow"] not in [w.strip() for w in doc["metadata"]["annotations"][ANN + "windows"].split(",")]:
                raise SloError(f"{pw}: lookbackWindow {c['lookbackWindow']} is not in {ANN}windows")
            if short_window(c["lookbackWindow"], pw) not in [w.strip() for w in doc["metadata"]["annotations"][ANN + "windows"].split(",")]:
                raise SloError(f"{pw}: short window {short_window(c['lookbackWindow'], pw)} is not in {ANN}windows")


# ------------------------------------------------------------- rendering
def render_ratio_rule(name: str, ratio: dict, fn: str, window: str, indent: str = "          ") -> str:
    total = ratio["total"]["metricSource"]["spec"]["query"]
    if "bad" in ratio:
        bad = ratio["bad"]["metricSource"]["spec"]["query"]
        return (
            f"{indent}sum by (app) ({fn}({bad}[{window}]))\n"
            f"{indent}  /\n"
            f"{indent}sum by (app) ({fn}({total}[{window}]))\n"
        )
    good = ratio["good"]["metricSource"]["spec"]["query"]
    return (
        f"{indent}1 - (\n"
        f"{indent}  sum by (app) ({fn}({good}[{window}]))\n"
        f"{indent}    /\n"
        f"{indent}  sum by (app) ({fn}({total}[{window}]))\n"
        f"{indent})\n"
    )


def render(slos: list[tuple[str, dict]]) -> str:
    out = [HEADER]
    for _, doc in slos:
        target = doc["spec"]["objectives"][0]["target"]
        out.append(
            f"#   {doc['metadata']['name']:<20}{target_percent(target)}% target, "
            f"budget {budget_of(target, '')}  ({doc['metadata']['labels']['team']})\n"
        )
    out.append("groups:\n\n  - name: stride-slo-recording\n    interval: 30s\n    rules:\n")

    for _, doc in slos:
        meta, spec = doc["metadata"], doc["spec"]
        ann = meta["annotations"]
        ratio = spec["indicator"]["spec"]["ratioMetric"]
        base = ann[ANN + "recording-rule"]
        windows = [w.strip() for w in ann[ANN + "windows"].split(",")]
        kind = "share of requests failing" if "bad" in ratio else "share of requests outside the good bucket"
        out.append(f"      # ---- {meta['displayName']}: {kind}, {len(windows)} window(s)\n")
        for w in windows:
            out.append(f"      - record: {base}{w}\n        expr: |\n")
            out.append(render_ratio_rule(base + w, ratio, "rate", w))
        budget_rule = ann.get(ANN + "error-budget-rule")
        if budget_rule:
            long_window = spec["timeWindow"][0]["duration"]
            budget = budget_of(spec["objectives"][0]["target"], "")
            if "bad" not in ratio:
                raise SloError(f"{meta['name']}: error-budget-rule is only implemented for 'bad'-based ratios")
            bad = ratio["bad"]["metricSource"]["spec"]["query"]
            total = ratio["total"]["metricSource"]["spec"]["query"]
            out.append(
                f"\n      # ---- {long_window} error budget remaining, 1 = untouched, 0 = spent, <0 = overspent\n"
                f"      - record: {budget_rule}\n"
                f"        labels:\n"
                f"          slo: {ann[ANN + 'error-budget-slo-label']}\n"
                f"        expr: |\n"
                f"          1 - (\n"
                f"            (\n"
                f"              sum by (app) (increase({bad}[{long_window}]))\n"
                f"                /\n"
                f"              sum by (app) (increase({total}[{long_window}]))\n"
                f"            ) / {budget}\n"
                f"          )\n"
            )
        out.append("\n")

    out.append("  - name: stride-slo-alerts\n    rules:\n")
    for _, doc in slos:
        meta, spec = doc["metadata"], doc["spec"]
        ann = meta["annotations"]
        base = ann[ANN + "recording-rule"]
        target = spec["objectives"][0]["target"]
        budget = budget_of(target, "")
        service = spec["service"]
        out.append(
            "\n      # ================================================================\n"
            f"      # {meta['displayName']} — {target_percent(target)} %, budget {budget}\n"
            f"      # {spec['description']}\n"
            "      # ================================================================\n"
        )
        for policy in spec["alertPolicies"]:
            pann = policy["metadata"]["annotations"]
            cond = policy["spec"]["conditions"][0]["spec"]
            c = cond["condition"]
            rule_kind = pann.get(ANN + "rule-kind", "burn-rate")
            labels = {
                "severity": cond["severity"],
                "team": meta["labels"]["team"],
                "tier": str(meta["labels"]["tier"]),
                "service": service,
                "slo": meta["name"],
            }
            if rule_kind == "budget-exhausted":
                labels["slo"] = ann[ANN + "error-budget-slo-label"]
            for key, value in pann.items():
                if key.startswith(ANN + "label."):
                    labels[key[len(ANN + "label."):]] = str(value)

            out.append(f"\n      - alert: {pann[ANN + 'alertname']}\n")
            if rule_kind == "burn-rate":
                long_w = c["lookbackWindow"]
                short_w = short_window(long_w, meta["name"])
                thr = number(c["threshold"], meta["name"])
                out.append(
                    "        expr: |\n"
                    f"          ({base}{long_w}{{app=\"{service}\"}} / {budget}) > {thr}\n"
                    "            and\n"
                    f"          ({base}{short_w}{{app=\"{service}\"}} / {budget}) > {thr}\n"
                )
            else:
                out.append(f"        expr: {ann[ANN + 'error-budget-rule']} < 0\n")
            out.append(f"        for: {c['alertAfter']}\n        labels:\n")
            for key in ("severity", "team", "tier", "service", "slo"):
                out.append(f"          {key}: {yaml_scalar(labels[key])}\n")
            for key in sorted(k for k in labels if k not in {"severity", "team", "tier", "service", "slo"}):
                out.append(f"          {key}: {yaml_scalar(labels[key])}\n")
            out.append("        annotations:\n")
            out.append(f"          summary: {yaml_scalar(pann[ANN + 'summary'])}\n")
            out.append(f"          description: {yaml_scalar(pann[ANN + 'description'])}\n")
            out.append(f"          runbook_url: {pann[ANN + 'runbook-url']}\n")
            if pann.get(ANN + "dashboard-url"):
                out.append(f"          dashboard_url: {pann[ANN + 'dashboard-url']}\n")
    return "".join(out)


# -------------------------------------------------------------------- main
def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--slo-dir", default="slo")
    ap.add_argument("--output", default="monitoring/alerts/slo.rules.yml")
    ap.add_argument("--check", action="store_true", help="validate and compare; do not write")
    args = ap.parse_args()

    try:
        slos = load_slos(args.slo_dir)
        text = render(slos)
        yaml.safe_load(text)  # the output must itself be valid YAML
    except SloError as exc:
        print(f"ERROR {exc}", file=sys.stderr)
        return 1

    if args.check:
        current = open(args.output, encoding="utf-8").read() if os.path.exists(args.output) else ""
        if current != text:
            print(f"ERROR {args.output} is stale; run monitoring/scripts/generate_slo_rules.py", file=sys.stderr)
            return 1
        print(f"OK {args.output} is up to date ({len(slos)} SLOs)")
        return 0

    with open(args.output, "w", encoding="utf-8") as fh:
        fh.write(text)
    print(f"wrote {args.output} from {len(slos)} SLO file(s)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
