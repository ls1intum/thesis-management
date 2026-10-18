#!/usr/bin/env python3
"""Builds a new sweep from an old one with one model's runs replaced by a rerun.

Copies BASE to OUT, drops every run of the rerun's models from runs.jsonl and the results tree,
and adds the rerun's runs in their place. The judge state is not copied, so judge OUT afresh.

Usage: python3 -I eval/replace-model.py BASE RERUN OUT
"""

import json
import shutil
import sys
from pathlib import Path


def main():
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    base, rerun, out = (Path(a) for a in sys.argv[1:])
    if out.exists():
        sys.exit(f"{out} already exists")

    rerun_manifest = json.loads((rerun / "manifest.json").read_text(encoding="utf-8"))
    models = rerun_manifest["models"]
    shutil.copytree(base, out, ignore=shutil.ignore_patterns("judge"))

    def slug(model):
        return "".join(c if c.isalnum() or c in "._-" else "_" for c in model.lower())

    for model in models:
        shutil.rmtree(out / slug(model), ignore_errors=True)
        shutil.copytree(rerun / slug(model), out / slug(model))

    kept = [line for line in (base / "runs.jsonl").read_text(encoding="utf-8").splitlines()
            if line.strip() and json.loads(line)["model"] not in models]
    added = [line for line in (rerun / "runs.jsonl").read_text(encoding="utf-8").splitlines() if line.strip()]
    (out / "runs.jsonl").write_text("\n".join(kept + added) + "\n", encoding="utf-8")

    manifest = json.loads((base / "manifest.json").read_text(encoding="utf-8"))
    manifest["replacedModels"] = {"models": models, "from": str(rerun), "rerunManifest": rerun_manifest}
    manifest["thinkingModels"] = sorted(set(manifest.get("thinkingModels") or [])
                                        | set(rerun_manifest.get("thinkingModels") or []))
    (out / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {out}: {len(kept)} runs kept, {len(added)} runs of {', '.join(models)} added")


if __name__ == "__main__":
    main()
