#!/usr/bin/env python3
"""Renders judge/results.json as the evaluation chapter's tables. No model involved.

Writes, next to results.json:
  tables.md    every table and every value the chapter's prose quotes, for reading
  tables.typ   the same as Typst arrays, for the thesis:

      #import "eval-tables.typ": *
      table(columns: 6, ..., table.header(...), ..eval-main-rows)

Cells hold "mean ± sd" over the repetitions. Labels for models, approaches and documents come
from eval/judge/corpus.json.

Usage: python3 -I eval/judge/tables.py RUN_DIR [--model MODEL] [--config corpus.json]
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
DASH = "--"


def load_json(path):
    return json.loads(Path(path).read_text(encoding="utf-8"))


def pct(stat, signed=False):
    if not stat or stat.get("mean") is None:
        return DASH
    text = f"{stat['mean'] * 100:{'+' if signed else ''}.1f}"
    return text + (f" ± {stat['sd'] * 100:.1f}" if stat.get("n", 0) > 1 else "")


def num(stat, digits=1, scale=1.0, suffix=""):
    if not stat or stat.get("mean") is None:
        return DASH
    text = f"{stat['mean'] / scale:.{digits}f}{suffix}"
    return text + (f" ± {stat['sd'] / scale:.{digits}f}{suffix}" if stat.get("n", 0) > 1 else "")


def share(value):
    return DASH if value is None else f"{value * 100:.1f}"


def typst_cell(text):
    escaped = "".join("\\" + c if c in "[]#*_@<>$\\`" else c for c in str(text))
    return f"[{escaped}]"


def typst_array(name, rows):
    cells = ",\n".join("  " + ", ".join(typst_cell(c) for c in row) for row in rows)
    return f"#let {name} = (\n{cells},\n)\n" if rows else f"#let {name} = ()\n"


def markdown_table(header, rows):
    lines = ["| " + " | ".join(header) + " |", "|" + "---|" * len(header)]
    lines += ["| " + " | ".join(str(c) for c in row) + " |" for row in rows]
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("run_dir")
    parser.add_argument("--model", help="model for the single-model tables (default: cloudModel in corpus.json)")
    parser.add_argument("--config", help="corpus configuration (default eval/judge/corpus.json)")
    args = parser.parse_args()

    run_dir = Path(args.run_dir)
    results = load_json(run_dir / "judge" / "results.json")
    config = load_json(args.config or HERE / "corpus.json")
    guidelines = load_json(run_dir / "guidelines.json")
    focus = args.model or config.get("cloudModel")

    model_labels = config.get("models", {})
    approach_labels = config.get("approaches", {})
    doc_labels = {doc: entry.get("label", doc) for doc, entry in config.get("documents", {}).items()}
    configs = {(c["approach"], c["model"]): c for c in results["configurations"]}

    def ordered(keys, preferred):
        return [k for k in preferred if k in keys] + sorted(k for k in keys if k not in preferred)

    approaches = ordered({a for a, _ in configs}, list(approach_labels))
    models = ordered({m for _, m in configs}, list(model_labels))

    def name(approach, model):
        return f"{approach_labels.get(approach, approach)}, {model_labels.get(model, model)}"

    # Table: main comparison.
    main_rows = []
    for approach in approaches:
        for model in models:
            c = configs.get((approach, model))
            if c:
                main_rows.append([name(approach, model), pct(c["validity"]), pct(c["coverage"]),
                                  num(c["timeS"], 0), num(c["depth"], 1), num(c["tokens"], 1, 1000, "k")])

    # Table: per-document differences on the focus model.
    documents = sorted({doc for c in configs.values() for doc in c["perDocument"]},
                       key=lambda d: (doc_labels.get(d, d), d))
    comparisons = [
        ("Per category -- single prompt, validity", "category-fan-out-unverified", "single-prompt", "validity"),
        ("Per category -- single prompt, coverage", "category-fan-out-unverified", "single-prompt", "coverage"),
        ("Verification -- none, validity", "category-fan-out-verified", "category-fan-out-unverified", "validity"),
        ("Verification -- none, coverage", "category-fan-out-verified", "category-fan-out-unverified", "coverage"),
        ("Agentic -- per category + verif., coverage", "agentic", "category-fan-out-verified", "coverage"),
    ]
    paired_rows = []
    for label, first, second, metric in comparisons:
        row = [label]
        for doc in documents:
            a = configs.get((first, focus), {}).get("perDocument", {}).get(doc, {}).get(metric, {})
            b = configs.get((second, focus), {}).get("perDocument", {}).get(doc, {}).get(metric, {})
            if a.get("mean") is None or b.get("mean") is None:
                row.append(DASH)
            else:
                row.append(f"{(a['mean'] - b['mean']) * 100:+.1f}")
        paired_rows.append(row)

    # Table: per category, verified fan-out on the focus model.
    verified = configs.get(("category-fan-out-verified", focus))
    rule_count = {}
    category_name = {}
    for rule in guidelines["rules"]:
        rule_count[rule["category"]] = rule_count.get(rule["category"], 0) + 1
        category_name[rule["category"]] = rule["categoryName"]
    category_rows = []
    for category, count in rule_count.items():
        stats = (verified or {}).get("byCategory", {}).get(category, {})
        category_rows.append([category_name[category], str(count),
                              share(stats.get("validity")), share(stats.get("coverage"))])

    # Table: what verification dropped, verified fan-out on the focus model.
    filter_rows = []
    if verified and verified.get("verification"):
        v = verified["verification"]
        total = v["candidates"]
        rejected = v["rejected"]
        filter_rows = [
            ["Quote not in the document", str(v["ungrounded"]), share(v["ungrounded"] / total if total else None)],
            ["Rule not in the category, judged invalid", str(rejected.get("invalid", 0)),
             share(rejected.get("invalid", 0) / total if total else None)],
            ["Rule not in the category, judged valid", str(rejected.get("valid", 0)),
             share(rejected.get("valid", 0) / total if total else None)],
            ["Rule not in the category, judged out of scope", str(rejected.get("out_of_scope", 0)),
             share(rejected.get("out_of_scope", 0) / total if total else None)],
        ]
        if rejected.get("disputed"):
            filter_rows.append(["Rule not in the category, label disputed", str(rejected["disputed"]),
                                share(rejected["disputed"] / total if total else None)])

    # Values the prose quotes.
    def value(approach, key, fmt=pct, model=focus):
        c = configs.get((approach, model))
        return fmt(c[key]) if c else DASH

    agentic_runs = sum(configs[(a, m)]["runs"] for a, m in configs if a == "agentic")
    problems = {"failedRuns": {}, "failedCategories": {}}
    for c in configs.values():
        for kind in problems:
            for cause, n in c["executionProblems"][kind].items():
                problems[kind][cause] = problems[kind].get(cause, 0) + n
    judge = results["judge"]
    values = [
        ("Duplication, per category (%)", value("category-fan-out-unverified", "duplication")),
        ("Duplication, per category + verif. (%)", value("category-fan-out-verified", "duplication")),
        ("Findings on the conforming proposal, per category + verif.",
         value("category-fan-out-verified", "overReporting", lambda s: num(s, 1))),
        ("Findings on the conforming proposal, single prompt",
         value("single-prompt", "overReporting", lambda s: num(s, 1))),
        ("Recall on the defective proposal, per category + verif. (%)", value("category-fan-out-verified", "recall")),
        ("Agentic runs at the turn budget",
         f"{sum(c['turnCapHits'] for (a, _), c in configs.items() if a == 'agentic')} of {agentic_runs}"),
        ("Runs in total", str(sum(c["runs"] for c in configs.values()))),
        ("Category passes rejected by the structured output format",
         str(problems["failedCategories"].get("structured-output", 0))),
        ("Runs or passes that timed out",
         str(problems["failedRuns"].get("timeout", 0) + problems["failedCategories"].get("timeout", 0))),
        ("Runs rejected by the structured output format", str(problems["failedRuns"].get("structured-output", 0))),
        ("Runs failed for another reason", str(problems["failedRuns"].get("other", 0))),
        ("Category passes failed for another reason", str(problems["failedCategories"].get("other", 0))),
        ("Judge: findings labelled", str(judge["findingsLabelled"])),
        ("Judge: unanimous across passes (%)", share(judge["unanimous"])),
        ("Judge: pairwise agreement (%)", share(judge["pairwise"])),
        ("Judge: disputed labels", str(judge["disputed"])),
        ("Judge: cost (USD)", f"{judge['costUsd']:.2f}"),
    ]
    values += [(f"Pooled reference size, {doc_labels.get(doc, doc)}", str(n)) for doc, n in results["reference"].items()]

    focus_label = model_labels.get(focus, focus)
    md = [
        f"# Evaluation Tables\n\nGenerated from `{run_dir / 'judge' / 'results.json'}`. "
        f"Cells: mean ± standard deviation over the repetitions; single-model tables use {focus_label} (`{focus}`).",
    ]
    if results.get("missingJobs"):
        md.append(f"> **Incomplete:** {len(results['missingJobs'])} judge job(s) had no answer.")
    md += [
        "## Main Comparison (%, s, stages, tokens)",
        markdown_table(["Configuration", "Validity", "Coverage", "Time (s)", "Depth", "Tokens"], main_rows),
        f"## Per-Document Differences, {focus_label} (percentage points)",
        markdown_table(["Comparison", *[doc_labels.get(d, d) for d in documents]], paired_rows),
        f"## Per Category, Per Category + Verif., {focus_label} (%)",
        markdown_table(["Category", "Rules", "Validity", "Coverage"], category_rows),
        f"## Dropped by Verification, {focus_label}",
        markdown_table(["Tier and label of the dropped finding", "Count", "Share of all candidates (%)"], filter_rows),
        "## Values Quoted in the Text",
        markdown_table(["Value", ""], values),
    ]
    (run_dir / "judge" / "tables.md").write_text("\n\n".join(md) + "\n", encoding="utf-8")

    typ = [
        f"// Generated by eval/judge/tables.py from {run_dir.name}/judge/results.json. Do not edit by hand.\n"
        f"// Single-model tables use {focus}. Cells hold mean ± sd over the repetitions.\n",
        typst_array("eval-main-rows", main_rows),
        typst_array("eval-paired-header", [[doc_labels.get(d, d) for d in documents]]),
        typst_array("eval-paired-rows", paired_rows),
        typst_array("eval-category-rows", category_rows),
        typst_array("eval-filter-rows", filter_rows),
        "#let eval-values = (\n" + "".join(f"  {json.dumps(k)}: {typst_cell(v)},\n" for k, v in values) + ")\n",
    ]
    (run_dir / "judge" / "tables.typ").write_text("\n".join(typ), encoding="utf-8")
    print(f"Wrote {run_dir / 'judge' / 'tables.md'} and {run_dir / 'judge' / 'tables.typ'}")


if __name__ == "__main__":
    main()
