#!/usr/bin/env bash
set -euo pipefail

# ============================================================================
# AI Review Judging
#
# Judges the findings of one evaluation sweep with a stronger model and turns
# the judgements into the evaluation chapter's tables.
#
#   1. prepare    pool every finding per document, hide its origin, shuffle,
#                 and write the judging jobs (eval/judge/judge.py)
#   2. judge      run every job through `claude -p`: three labelling passes
#                 per finding, one grouping job per document
#   3. aggregate  majority labels, pooled reference, metrics per run and per
#                 configuration -> <run>/judge/results.json
#   4. tables     results.json -> <run>/judge/tables.md and tables.typ
#
# Resumable: answered jobs are kept, so rerunning continues where it stopped.
# Blind: a judge sees findings under random ids, mixed across configurations,
# in a fresh session without tools, started outside the repository.
#
# Usage:
#   ./execute-review-judge.sh                       # latest run under eval/results
#   ./execute-review-judge.sh eval/results/<run>
#   ./execute-review-judge.sh <run> --parallel=8 --passes=3
#
# Options:
#   --passes=N        Labelling passes per finding (default: 3)
#   --batch=N         Findings per labelling call (default: 40)
#   --parallel=N      Judge calls running at once (default: 4)
#   --model=ID        Judging model (default: claude-opus-5-5[1m], 1M context)
#   --effort=LEVEL    Judging effort level (default: medium)
#   --retries=N       Extra attempts for a call whose answer is invalid (default: 2)
#   --fresh           Rebuild the pool and discard earlier judgements
#   --help            Show this help
#
# Needs: python3, and the `claude` CLI logged in.
# ============================================================================

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
JUDGE_SRC="$ROOT_DIR/eval/judge"

RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
NC='\033[0m'

log() { echo -e "${CYAN}[judge]${NC} $*"; }
ok()  { echo -e "${GREEN}[judge]${NC} $*"; }
err() { echo -e "${RED}[judge]${NC} $*" >&2; }

usage() { awk 'NR>=4 { if (!/^#/) exit; line=$0; sub(/^# ?/, "", line); print line }' "$0"; }

RUN_DIR=""
PASSES=3
BATCH=40
PARALLEL=4
MODEL="claude-opus-5-5[1m]"
EFFORT="medium"
RETRIES=2
FRESH=""
ONE_JOB=""

for arg in "$@"; do
  case "$arg" in
    --passes=*)   PASSES="${arg#*=}" ;;
    --batch=*)    BATCH="${arg#*=}" ;;
    --parallel=*) PARALLEL="${arg#*=}" ;;
    --model=*)    MODEL="${arg#*=}" ;;
    --effort=*)   EFFORT="${arg#*=}" ;;
    --retries=*)  RETRIES="${arg#*=}" ;;
    --fresh)      FRESH="--fresh" ;;
    --one-job=*)  ONE_JOB="${arg#*=}" ;;   # internal: one call, used by the parallel runner
    --help|-h)    usage; exit 0 ;;
    -*)           err "Unknown option: $arg"; echo; usage; exit 1 ;;
    *)            RUN_DIR="$arg" ;;
  esac
done

if [[ -z "$RUN_DIR" ]]; then
  RUN_DIR=$(find "$ROOT_DIR/eval/results" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | sort | tail -1)
fi
if [[ -z "$RUN_DIR" || ! -f "$RUN_DIR/runs.jsonl" ]]; then
  err "No sweep results at '${RUN_DIR:-eval/results/<run>}' (expected a runs.jsonl)"
  exit 1
fi
RUN_DIR="$(cd "$RUN_DIR" && pwd)"
JOBS_DIR="$RUN_DIR/judge/jobs"
ANSWERS_DIR="$RUN_DIR/judge/answers"
RAW_DIR="$RUN_DIR/judge/raw"

# One judge call, retried until its answer validates. Started outside the repository so no
# project instructions reach the judge, without tools so it judges only what the prompt shows,
# and without session persistence so no batch can see another.
run_one_job() {
  local job="$1"
  local neutral attempt
  neutral="$(mktemp -d)"
  mkdir -p "$RAW_DIR"
  for ((attempt = 0; attempt <= RETRIES; attempt++)); do
    if (cd "$neutral" && claude -p \
          --model "$MODEL" \
          --effort "$EFFORT" \
          --tools "" \
          --no-session-persistence \
          --output-format json \
          --system-prompt "$(cat "$JUDGE_SRC/system-prompt.md")" \
          --json-schema "$(cat "$JOBS_DIR/$job.schema.json")" \
          < "$JOBS_DIR/$job.prompt.md" > "$RAW_DIR/$job.json" 2> "$RAW_DIR/$job.stderr") \
       && python3 -I "$JUDGE_SRC/judge.py" check "$RUN_DIR" "$job" "$RAW_DIR/$job.json"; then
      rm -rf "$neutral"
      echo "  done: $job"
      return 0
    fi
    echo "  retry $((attempt + 1))/$RETRIES: $job" >&2
  done
  rm -rf "$neutral"
  echo "  FAILED: $job (raw output in $RAW_DIR/$job.json)" >&2
  return 1
}

if [[ -n "$ONE_JOB" ]]; then
  run_one_job "$ONE_JOB"
  exit $?
fi

if ! command -v claude >/dev/null 2>&1; then
  err "The claude CLI is not on PATH; install Claude Code and log in first."
  exit 1
fi
if [[ ! -f "$RUN_DIR/guidelines.json" || ! -d "$RUN_DIR/documents" ]]; then
  err "$RUN_DIR has no guidelines.json or documents/; it predates judging support. Rerun the sweep."
  exit 1
fi

log "Run:   $RUN_DIR"
log "Judge: $MODEL (effort $EFFORT), $PASSES pass(es), $PARALLEL in parallel"

python3 -I "$JUDGE_SRC/judge.py" prepare "$RUN_DIR" --passes="$PASSES" --batch="$BATCH" $FRESH
if [[ -n "$FRESH" ]]; then
  rm -rf "$ANSWERS_DIR" "$RAW_DIR"
fi

pending=()
for prompt in "$JOBS_DIR"/*.prompt.md; do
  job="$(basename "$prompt" .prompt.md)"
  [[ -f "$ANSWERS_DIR/$job.json" ]] || pending+=("$job")
done
log "${#pending[@]} job(s) to judge"

failed=0
if [[ ${#pending[@]} -gt 0 ]]; then
  printf '%s\n' "${pending[@]}" | xargs -P "$PARALLEL" -I{} "$0" "$RUN_DIR" \
    --one-job={} --model="$MODEL" --effort="$EFFORT" --retries="$RETRIES" || failed=1
fi
if [[ $failed -ne 0 ]]; then
  err "Some judge jobs failed. Rerun this script to retry only those; answered jobs are kept."
  exit 1
fi

python3 -I "$JUDGE_SRC/judge.py" aggregate "$RUN_DIR"
python3 -I "$JUDGE_SRC/tables.py" "$RUN_DIR"
ok "Tables: $RUN_DIR/judge/tables.md"
ok "Typst:  $RUN_DIR/judge/tables.typ"
