#!/usr/bin/env python3
"""Blind LLM judging of an evaluation sweep: everything around the model calls, nothing inside them.

The model calls themselves are made by execute-review-judge.sh. This script does the parts that
must be deterministic and auditable:

  prepare    Pools every finding of a sweep per document, hides where each one came from, and
             writes the judging jobs: several labelling passes over shuffled batches, and one
             grouping job per document that builds the pooled reference.
  check      Validates one judge answer against its job and stores it, or fails so the runner
             retries the call.
  aggregate  Turns the stored answers into metrics per run, per configuration, and per document
             and writes judge/results.json, which tables.py renders.

Blind and randomized, concretely:
  - A judge sees a finding's title, description, and locations under a random id. It never sees
    the model, the approach, the seed, the category pass, or whether the finding is a final
    result or a candidate the verifier removed. The mapping lives only in judge/pool.json.
  - Identical findings from different runs are judged once, so a judge cannot notice duplicates
    and infer that they share an origin.
  - Every labelling pass shuffles the pool with its own seed and cuts it into batches, so findings
    of all configurations are interleaved, and each pass sees different neighbours.
  - Each job is a fresh, tool-less model session, so no judgement carries over to another batch.
  - Three passes label every finding; the majority label carries.

Standard library only. Run with python3 -I.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import random
import re
import secrets
import statistics
import sys
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent
LABELS = ("valid", "invalid", "out_of_scope")
FAN_OUT = ("category-fan-out-unverified", "category-fan-out-verified")


# ---------------------------------------------------------------------------------------------
# Shared helpers
# ---------------------------------------------------------------------------------------------

def load_json(path):
    return json.loads(Path(path).read_text(encoding="utf-8"))


def dump_json(path, data):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def slug(model):
    """Same as EvalPlan.slug: the directory a model's results live under."""
    return re.sub(r"[^a-z0-9._-]+", "_", model.lower())


def category_slug(name):
    """ReviewCategory is serialized by name (WRITING_STYLE); guidelines.json uses slugs."""
    return name.lower().replace("_", "-")


def load_runs(run_dir):
    rows = []
    for line in (run_dir / "runs.jsonl").read_text(encoding="utf-8").splitlines():
        if line.strip():
            rows.append(json.loads(line))
    return rows


def run_key(row):
    return f"{row['model']}|{row['approach']}|{row['document']}|{row['seed']}"


def run_path(run_dir, row):
    return run_dir / slug(row["model"]) / row["approach"] / row["document"] / f"seed-{row['seed']}"


def load_trace(run_dir, row):
    path = run_path(run_dir, row) / "trace.json"
    return load_json(path) if path.exists() else {"categories": [], "categoryFailures": {}}


def load_config(path):
    return load_json(path) if path else load_json(HERE / "corpus.json")


def visible(finding):
    """What a judge sees of a finding: its claim and where it points, nothing about its origin.

    Severity and the model's category token are left out: neither decides validity, and both
    differ in style between reviewers, which would make a finding's origin guessable."""
    locations = []
    for location in finding.get("locations") or []:
        entry = {k: location[k] for k in ("page", "section", "quote") if location.get(k) not in (None, "")}
        if entry:
            locations.append(entry)
    shown = {"title": (finding.get("title") or "").strip(),
             "description": (finding.get("description") or "").strip()}
    if locations:
        shown["locations"] = locations
    return shown


# ---------------------------------------------------------------------------------------------
# prepare
# ---------------------------------------------------------------------------------------------

def render_rules(guidelines):
    lines = [guidelines.get("overview", "").strip(), ""]
    for rule in guidelines["rules"]:
        lines.append(f"- **{rule['id']}** ({rule['categoryName']}): {rule['text']}")
    return "\n".join(lines).strip()


def render_items(items, ids):
    return "<findings>\n" + json.dumps([{"id": i, **items[i]["visible"]} for i in ids],
                                       indent=1, ensure_ascii=False) + "\n</findings>"


def render_known_defects(defects):
    if not defects:
        return ("## Known Defects\n\nNo list of known defects exists for this document. Set "
                "`known_defect` to `null` for every defect.")
    shown = [{k: d.get(k) for k in ("id", "rule", "section", "page", "quote", "violation")} for d in defects]
    return ("## Known Defects\n\nThe document was constructed with the defects listed below. When a defect "
            "you build is the same breach at the same place as a known defect, set `known_defect` to that "
            "known defect's id. A known defect that no finding reports gets no entry of its own: build "
            "defects from the findings only.\n\n<known-defects>\n"
            + json.dumps(shown, indent=1, ensure_ascii=False) + "\n</known-defects>")


def schema(kind, rule_ids):
    rule = {"type": ["string", "null"], "enum": [*rule_ids, None]}
    if kind == "label":
        entry = {"type": "object", "additionalProperties": False,
                 "required": ["id", "rule", "reason", "label"],
                 "properties": {"id": {"type": "string"}, "rule": rule, "reason": {"type": "string"},
                                "label": {"type": "string", "enum": list(LABELS)}}}
        return {"type": "object", "additionalProperties": False, "required": ["judgements"],
                "properties": {"judgements": {"type": "array", "items": entry}}}
    entry = {"type": "object", "additionalProperties": False,
             "required": ["id", "rule", "description", "known_defect", "members"],
             "properties": {"id": {"type": "string"}, "rule": rule, "description": {"type": "string"},
                            "known_defect": {"type": ["string", "null"]},
                            "members": {"type": "array", "items": {"type": "string"}, "minItems": 1}}}
    return {"type": "object", "additionalProperties": False, "required": ["defects"],
            "properties": {"defects": {"type": "array", "items": entry}}}


def known_defects_for(config, doc):
    entry = config.get("documents", {}).get(doc, {})
    if not entry.get("defects"):
        return None
    path = REPO / entry["defects"]
    if not path.exists():
        print(f"warning: {doc} lists known defects at {path}, which does not exist", file=sys.stderr)
        return None
    return load_json(path)


def cmd_prepare(args):
    run_dir = Path(args.run_dir)
    judge_dir = run_dir / "judge"
    pool_file = judge_dir / "pool.json"
    if pool_file.exists() and not args.fresh:
        # Reusing the pool keeps ids and shuffles stable, so an interrupted run resumes instead of
        # starting over with different batches.
        print(f"Reusing {pool_file} (pass --fresh to rebuild it and discard earlier judgements)")
        return

    config = load_config(args.config)
    guidelines = load_json(run_dir / "guidelines.json")
    rule_ids = [rule["id"] for rule in guidelines["rules"]]
    rules_text = render_rules(guidelines)
    templates = {kind: (HERE / f"{kind}-prompt.md").read_text(encoding="utf-8") for kind in ("label", "cluster")}

    salt = secrets.token_hex(16)
    items = {}
    by_content = {}

    def add(doc, finding, occurrence):
        shown = visible(finding)
        key = json.dumps(shown, sort_keys=True, ensure_ascii=False)
        ident = by_content.get((doc, key))
        if ident is None:
            ident = "F" + hashlib.sha256(f"{salt}|{doc}|{key}".encode()).hexdigest()[:10]
            by_content[(doc, key)] = ident
            items[ident] = {"doc": doc, "visible": shown, "occurrences": []}
        items[ident]["occurrences"].append(occurrence)

    for row in load_runs(run_dir):
        if row.get("status") != "ok":
            continue
        doc, key = row["document"], run_key(row)
        for finding in row.get("findings") or []:
            add(doc, finding, {"run": key, "role": "final"})
        for category in load_trace(run_dir, row).get("categories") or []:
            name = category_slug(category["category"])
            for finding in category.get("kept") or []:
                add(doc, finding, {"run": key, "role": "kept", "category": name})
            for rejected in category.get("rejected") or []:
                add(doc, rejected["finding"], {"run": key, "role": "rejected", "category": name,
                                               "reason": rejected.get("reason")})

    jobs_dir = judge_dir / "jobs"
    jobs = []

    def write_job(job_id, kind, doc, ids, extra):
        prompt = (templates[kind]
                  .replace("{{RULES}}", rules_text)
                  .replace("{{DOCUMENT}}", "<document>\n" + document_text[doc].rstrip() + "\n</document>")
                  .replace("{{ITEMS}}", render_items(items, ids))
                  .replace("{{KNOWN_DEFECTS}}", extra.get("known", "")))
        (jobs_dir / f"{job_id}.prompt.md").parent.mkdir(parents=True, exist_ok=True)
        (jobs_dir / f"{job_id}.prompt.md").write_text(prompt, encoding="utf-8")
        dump_json(jobs_dir / f"{job_id}.schema.json", schema(kind, rule_ids))
        jobs.append({"id": job_id, "kind": kind, "doc": doc, "pass": extra.get("pass"), "items": ids})

    docs = sorted({item["doc"] for item in items.values()})
    document_text = {doc: (run_dir / "documents" / f"{doc}.txt").read_text(encoding="utf-8") for doc in docs}
    for doc in docs:
        to_label = sorted(i for i, it in items.items()
                          if it["doc"] == doc and any(o["role"] in ("final", "rejected") for o in it["occurrences"]))
        for number in range(1, args.passes + 1):
            order = to_label[:]
            random.Random(f"{salt}|{doc}|label|{number}").shuffle(order)
            for batch, start in enumerate(range(0, len(order), args.batch), 1):
                write_job(f"label-{doc}-pass{number}-{batch:02d}", "label", doc,
                          order[start:start + args.batch], {"pass": number})

        to_group = sorted(i for i, it in items.items()
                          if it["doc"] == doc and any(o["role"] in ("final", "kept") for o in it["occurrences"]))
        random.Random(f"{salt}|{doc}|cluster").shuffle(to_group)
        if to_group:
            write_job(f"cluster-{doc}", "cluster", doc, to_group,
                      {"known": render_known_defects(known_defects_for(config, doc))})

    dump_json(pool_file, {
        "createdAt": datetime.now(timezone.utc).isoformat(),
        "salt": salt,
        "passes": args.passes,
        "batch": args.batch,
        "items": items,
        "jobs": jobs,
    })
    labels = sum(1 for job in jobs if job["kind"] == "label")
    print(f"Prepared {len(items)} unique findings over {len(docs)} document(s): "
          f"{labels} labelling job(s) in {args.passes} pass(es), {len(jobs) - labels} grouping job(s)")


# ---------------------------------------------------------------------------------------------
# check
# ---------------------------------------------------------------------------------------------

def extract_answer(raw_text):
    """The structured answer inside `claude -p --output-format json` output, wherever it is."""
    data = json.loads(raw_text)
    if isinstance(data, dict) and data.get("is_error"):
        raise ValueError(f"the judge call reported an error: {data.get('result')}")
    if isinstance(data, dict) and isinstance(data.get("structured_output"), dict):
        return data["structured_output"], data
    if isinstance(data, dict) and ("judgements" in data or "defects" in data):
        return data, {}
    result = data.get("result") if isinstance(data, dict) else None
    if isinstance(result, str):
        text = re.sub(r"^```(?:json)?\s*|\s*```$", "", result.strip())
        start, end = text.find("{"), text.rfind("}")
        if start >= 0 and end > start:
            return json.loads(text[start:end + 1]), data
    raise ValueError("no structured answer found in the judge output")


def validate(job, answer, rule_ids):
    expected = set(job["items"])
    rules = set(rule_ids)
    problems = []
    if job["kind"] == "label":
        seen = Counter()
        for entry in answer.get("judgements") or []:
            seen[entry.get("id")] += 1
            if entry.get("label") not in LABELS:
                problems.append(f"{entry.get('id')}: unknown label {entry.get('label')!r}")
            if entry.get("rule") is not None and entry["rule"] not in rules:
                problems.append(f"{entry.get('id')}: unknown rule {entry.get('rule')!r}")
            if entry.get("label") == "out_of_scope" and entry.get("rule") is not None:
                entry["rule"] = None  # out of scope means no rule states it; normalize rather than retry
        missing = expected - set(seen)
        extra = set(seen) - expected
        doubled = [i for i, n in seen.items() if n > 1]
    else:
        assigned = Counter()
        for entry in answer.get("defects") or []:
            if entry.get("rule") is not None and entry["rule"] not in rules:
                problems.append(f"{entry.get('id')}: unknown rule {entry.get('rule')!r}")
            for member in entry.get("members") or []:
                assigned[member] += 1
        missing = expected - set(assigned)
        extra = set(assigned) - expected
        doubled = []  # a finding may report several defects
    if missing:
        problems.append(f"{len(missing)} finding(s) without an answer, e.g. {sorted(missing)[:5]}")
    if extra:
        problems.append(f"{len(extra)} unknown id(s), e.g. {sorted(extra)[:5]}")
    if doubled:
        problems.append(f"{len(doubled)} finding(s) answered more than once, e.g. {doubled[:5]}")
    return problems


def cmd_check(args):
    run_dir = Path(args.run_dir)
    pool = load_json(run_dir / "judge" / "pool.json")
    job = next((j for j in pool["jobs"] if j["id"] == args.job), None)
    if job is None:
        sys.exit(f"unknown job {args.job}")
    rule_ids = [rule["id"] for rule in load_json(run_dir / "guidelines.json")["rules"]]
    try:
        answer, envelope = extract_answer(Path(args.raw).read_text(encoding="utf-8"))
    except (ValueError, json.JSONDecodeError) as e:
        sys.exit(f"{args.job}: {e}")
    problems = validate(job, answer, rule_ids)
    if problems:
        sys.exit(f"{args.job}: " + "; ".join(problems[:6]))
    dump_json(run_dir / "judge" / "answers" / f"{args.job}.json", {
        "job": args.job,
        "answer": answer,
        "costUsd": envelope.get("total_cost_usd"),
        "durationMs": envelope.get("duration_ms"),
        "model": envelope.get("model") or (list((envelope.get("modelUsage") or {}).keys()) or [None])[0],
    })


# ---------------------------------------------------------------------------------------------
# aggregate
# ---------------------------------------------------------------------------------------------

def majority(votes):
    """Label and rule a finding carries: the label more than half the passes gave, else
    'disputed'; the rule most often given together with that label, first pass breaking ties."""
    labels = Counter(label for label, _ in votes)
    label, count = labels.most_common(1)[0]
    if count * 2 <= len(votes):
        label = "disputed"
    rules = [rule for l, rule in votes if (l == label or label == "disputed") and rule]
    rule = max(rules, key=lambda r: (rules.count(r), -rules.index(r))) if rules else None
    return label, rule


def mean_sd(values):
    values = [v for v in values if v is not None]
    if not values:
        return {"mean": None, "sd": None, "n": 0}
    return {"mean": statistics.fmean(values),
            "sd": statistics.stdev(values) if len(values) > 1 else 0.0,
            "n": len(values)}


def ratio(part, whole):
    return part / whole if whole else None


def failure_kind(cause):
    text = (cause or "").lower()
    if any(word in text for word in ("timeout", "timed out")):
        return "timeout"
    if any(word in text for word in ("json", "parse", "deserializ", "mismatchedinput", "streamread", "conver", "cannot construct")):
        return "structured-output"
    return "other"


def cmd_aggregate(args):
    run_dir = Path(args.run_dir)
    judge_dir = run_dir / "judge"
    pool = load_json(judge_dir / "pool.json")
    config = load_config(args.config)
    guidelines = load_json(run_dir / "guidelines.json")
    manifest = load_json(run_dir / "manifest.json") if (run_dir / "manifest.json").exists() else {}
    turn_cap = manifest.get("agentMaxTurns", 40)
    rule_category = {rule["id"]: rule["category"] for rule in guidelines["rules"]}
    categories = list(dict.fromkeys(rule["category"] for rule in guidelines["rules"]))
    items = pool["items"]

    answers, missing = {}, []
    for job in pool["jobs"]:
        path = judge_dir / "answers" / f"{job['id']}.json"
        if path.exists():
            answers[job["id"]] = load_json(path)
        else:
            missing.append(job["id"])
    if missing and not args.allow_missing:
        sys.exit(f"{len(missing)} judge job(s) have no answer yet, e.g. {missing[:5]}; "
                 "rerun the judge or pass --allow-missing")

    # Labels: majority over the passes.
    votes = defaultdict(list)
    for job in pool["jobs"]:
        if job["kind"] == "label" and job["id"] in answers:
            for entry in answers[job["id"]]["answer"]["judgements"]:
                votes[entry["id"]].append((entry["label"], entry.get("rule")))
    verdict = {item: majority(v) for item, v in votes.items()}
    multi = [v for v in votes.values() if len(v) > 1]
    agreement = {
        "findingsLabelled": len(votes),
        "unanimous": ratio(sum(1 for v in multi if len({l for l, _ in v}) == 1), len(multi)),
        "pairwise": ratio(sum(sum(1 for a in range(len(v)) for b in range(a + 1, len(v)) if v[a][0] == v[b][0])
                              for v in multi),
                          sum(len(v) * (len(v) - 1) // 2 for v in multi)),
        "disputed": sum(1 for label, _ in verdict.values() if label == "disputed"),
        "labels": dict(Counter(label for label, _ in verdict.values())),
    }

    def label_of(item):
        return verdict.get(item, ("unjudged", None))[0]

    def rule_of(item):
        return verdict.get(item, (None, None))[1]

    # Defects and the pooled reference, per document.
    defects = {}
    item_defects = defaultdict(set)
    for job in pool["jobs"]:
        if job["kind"] == "cluster" and job["id"] in answers:
            for entry in answers[job["id"]]["answer"]["defects"]:
                key = f"{job['doc']}#{entry['id']}"
                defects[key] = {"doc": job["doc"], **entry}
                for member in entry["members"]:
                    item_defects[member].add(key)
    final_items = {i for i, it in items.items() if any(o["role"] == "final" for o in it["occurrences"])}
    reference = defaultdict(set)
    for key, defect in defects.items():
        if any(label_of(m) == "valid" and m in final_items for m in defect["members"]):
            reference[defect["doc"]].add(key)

    occurrences = defaultdict(list)
    for ident, item in items.items():
        for occurrence in item["occurrences"]:
            occurrences[occurrence["run"]].append((ident, occurrence))

    documents = config.get("documents", {})
    role = {doc: entry.get("role") for doc, entry in documents.items()}
    known = {doc: known_defects_for(config, doc) for doc in documents}

    runs = []
    for row in load_runs(run_dir):
        key = run_key(row)
        trace = load_trace(run_dir, row)
        record = {"model": row["model"], "approach": row["approach"], "document": row["document"],
                  "seed": row["seed"], "status": row.get("status"),
                  "rootCause": row.get("rootCause"),
                  "categoryFailures": trace.get("categoryFailures") or {}}
        metrics = row.get("metrics") or {}
        if row.get("status") == "ok":
            finals = [i for i, o in occurrences[key] if o["role"] == "final"]
            valid = [i for i in finals if label_of(i) == "valid"]
            ref = reference[row["document"]]
            covered = {d for i in valid for d in item_defects[i] if d in ref}
            record.update({
                "findings": len(finals),
                "validity": ratio(len(valid), len(finals)),
                "coverage": ratio(len(covered), len(ref)),
                "timeS": metrics["wallMillis"] / 1000 if "wallMillis" in metrics else None,
                "depth": metrics.get("callDepth"),
                "llmCalls": metrics.get("llmCalls"),
                "tokens": (metrics.get("promptTokens", 0) + metrics.get("completionTokens", 0)) if metrics else None,
                "turnCapHit": row["approach"] == "agentic" and (metrics.get("llmCalls") or 0) >= turn_cap,
                "labels": dict(Counter(label_of(i) for i in finals)),
                "byCategory": {c: {
                    "findings": sum(1 for i in finals if rule_category.get(rule_of(i)) == c),
                    "valid": sum(1 for i in valid if rule_category.get(rule_of(i)) == c),
                    "reference": sum(1 for d in ref if rule_category.get(defects[d]["rule"]) == c),
                    "covered": sum(1 for d in covered if rule_category.get(defects[d]["rule"]) == c),
                } for c in categories},
            })
            if known.get(row["document"]):
                hit = {defects[d]["known_defect"] for i in valid for d in item_defects[i]
                       if defects[d].get("known_defect")}
                record["recall"] = ratio(len(hit & {k["id"] for k in known[row["document"]]}),
                                         len(known[row["document"]]))
            if row["approach"] in FAN_OUT:
                kept = [(i, o["category"]) for i, o in occurrences[key] if o["role"] == "kept"]
                duplicated = sum(1 for i, c in kept
                                 if any(c2 != c and item_defects[i] & item_defects[i2] for i2, c2 in kept))
                rejected = [i for i, o in occurrences[key] if o["role"] == "rejected"]
                record.update({
                    "duplication": ratio(duplicated, len(kept)),
                    "candidates": sum(len(c.get("generated") or []) for c in trace["categories"]),
                    "keptCandidates": len(kept),
                    "ungrounded": sum(len(c.get("ungrounded") or []) for c in trace["categories"]),
                    "rejected": dict(Counter(label_of(i) for i in rejected)),
                    "ruleCheckFailed": sum(1 for c in trace["categories"] if c.get("ruleCheckFailed")),
                })
        runs.append(record)

    results = {
        "runDir": str(run_dir),
        "createdAt": datetime.now(timezone.utc).isoformat(),
        "turnCap": turn_cap,
        "missingJobs": missing,
        "judge": {
            **agreement,
            "passes": pool["passes"],
            "costUsd": sum(a.get("costUsd") or 0 for a in answers.values()),
            "models": sorted({a["model"] for a in answers.values() if a.get("model")}),
        },
        "reference": {doc: len(keys) for doc, keys in sorted(reference.items())},
        "defects": [{"key": k, **{f: d[f] for f in ("doc", "rule", "description", "known_defect")},
                     "inReference": k in reference[d["doc"]], "members": len(d["members"])}
                    for k, d in sorted(defects.items())],
        "configurations": summarize(runs, role, categories),
        "runs": runs,
    }
    dump_json(judge_dir / "results.json", results)
    print(f"Wrote {judge_dir / 'results.json'} ({len(runs)} runs, {len(defects)} defects, "
          f"{agreement['disputed']} disputed label(s))")


def summarize(runs, role, categories):
    """Per configuration: every metric as the mean over the repetitions with its standard
    deviation, where one repetition's value is the mean over the documents it reviewed."""
    by_config = defaultdict(list)
    for run in runs:
        by_config[(run["approach"], run["model"])].append(run)

    summary = []
    for (approach, model), config_runs in sorted(by_config.items()):
        ok = [r for r in config_runs if r["status"] == "ok"]
        seeds = sorted({r["seed"] for r in ok})

        def over_seeds(metric, documents=None):
            per_seed = []
            for seed in seeds:
                values = [r.get(metric) for r in ok if r["seed"] == seed
                          and (documents is None or r["document"] in documents)]
                values = [v for v in values if v is not None]
                per_seed.append(statistics.fmean(values) if values else None)
            return mean_sd(per_seed)

        conforming = {d for d, r in role.items() if r == "conforming"}
        defective = {d for d, r in role.items() if r == "defective"}
        entry = {
            "approach": approach,
            "model": model,
            "runs": len(config_runs),
            "failedRuns": sum(1 for r in config_runs if r["status"] != "ok"),
            **{metric: over_seeds(metric) for metric in
               ("validity", "coverage", "timeS", "depth", "tokens", "llmCalls", "findings", "duplication")},
            "overReporting": over_seeds("findings", conforming),
            "recall": over_seeds("recall", defective),
            "turnCapHits": sum(1 for r in ok if r.get("turnCapHit")),
            "perDocument": {doc: {metric: mean_sd([r.get(metric) for r in ok if r["document"] == doc])
                                  for metric in ("validity", "coverage", "findings", "recall")}
                            for doc in sorted({r["document"] for r in ok})},
            "byCategory": {c: {
                "validity": ratio(sum(r["byCategory"][c]["valid"] for r in ok),
                                  sum(r["byCategory"][c]["findings"] for r in ok)),
                "coverage": ratio(sum(r["byCategory"][c]["covered"] for r in ok),
                                  sum(r["byCategory"][c]["reference"] for r in ok)),
            } for c in categories},
            "executionProblems": {
                "failedRuns": dict(Counter(failure_kind(r.get("rootCause")) for r in config_runs
                                           if r["status"] != "ok")),
                "failedCategories": dict(Counter(failure_kind(cause) for r in config_runs
                                                 for cause in r["categoryFailures"].values())),
            },
        }
        if approach in FAN_OUT:
            entry["verification"] = {
                "candidates": sum(r.get("candidates", 0) for r in ok),
                "ungrounded": sum(r.get("ungrounded", 0) for r in ok),
                "rejected": dict(sum((Counter(r.get("rejected") or {}) for r in ok), Counter())),
                "ruleCheckFailed": sum(r.get("ruleCheckFailed", 0) for r in ok),
            }
        summary.append(entry)
    return summary


# ---------------------------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)

    prepare = sub.add_parser("prepare", help="pool the findings and write the judging jobs")
    prepare.add_argument("run_dir")
    prepare.add_argument("--passes", type=int, default=3, help="labelling passes per finding (default 3)")
    prepare.add_argument("--batch", type=int, default=40, help="findings per labelling call (default 40)")
    prepare.add_argument("--config", help="corpus configuration (default eval/judge/corpus.json)")
    prepare.add_argument("--fresh", action="store_true", help="rebuild the pool even if one exists")
    prepare.set_defaults(func=cmd_prepare)

    check = sub.add_parser("check", help="validate and store one judge answer")
    check.add_argument("run_dir")
    check.add_argument("job")
    check.add_argument("raw", help="the raw `claude -p --output-format json` output")
    check.set_defaults(func=cmd_check)

    aggregate = sub.add_parser("aggregate", help="compute the metrics from the stored answers")
    aggregate.add_argument("run_dir")
    aggregate.add_argument("--config", help="corpus configuration (default eval/judge/corpus.json)")
    aggregate.add_argument("--allow-missing", action="store_true", help="aggregate despite unanswered jobs")
    aggregate.set_defaults(func=cmd_aggregate)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
