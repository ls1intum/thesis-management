#!/usr/bin/env bash
set -euo pipefail

# ============================================================================
# AI Review Strategy Evaluation
#
# Runs the four AI review approaches — a single prompt, the category fan-out
# without and with verification, and the agent — over a fixed corpus of PDFs,
# once per model, and writes every review to disk so the outputs can be compared.
#
# Each combination of model, strategy and seed boots its own application
# context with exactly the properties the real server reads, so what is being
# measured is the configured pipeline rather than a stand-in. No database,
# Keycloak or web server is started; only a reachable LLM endpoint is needed.
#
# Usage:
#   ./execute-review-eval.sh --models=openai/gpt-oss-120b
#   ./execute-review-eval.sh --models=a/one,b/two --seeds=3
#   ./execute-review-eval.sh --approaches=agentic --models=a/one
#   ./execute-review-eval.sh --models=a/one --skip-judge
#
# Options:
#   --models=A,B        Chat models to evaluate (required)
#   --approaches=A,B    any of single-prompt, category-fan-out-unverified,
#                       category-fan-out-verified, agentic (default: all four)
#   --seeds=N           Repetitions per combination (default: 1)
#   --temperature=T     Sampling temperature, constant across the sweep (default: 0.4)
#   --corpus=DIR        Corpus directory (default: eval/corpus)
#   --out=DIR           Results directory (default: eval/results)
#   --thinking-models=A,B  Models that get chat_template_kwargs.enable_thinking=true,
#                       for Qwen3 behind vLLM, which otherwise answers without reasoning
#   --skip-judge        Write the reviews only; judge them later with
#                       ./execute-review-judge.sh <results dir>
#   --help              Show this help
#
# Input:   eval/corpus/proposal/*.pdf and eval/corpus/thesis/*.pdf
# Output:  eval/results/<timestamp>/, judged into <timestamp>/judge/tables.md
#
# After the sweep, execute-review-judge.sh labels every finding blind with a
# stronger model and writes the chapter's tables; see that script's header.
#
# Credentials and a standing model list live in an uncommitted wrapper; copy the
# template once and run that instead of this script:
#   cp execute-review-eval.local.sh.example execute-review-eval.local.sh
#   ./execute-review-eval.local.sh
#
# Otherwise set them in the environment, or in an uncommitted .env.local:
#   OPENAI_BASE_URL=https://gpu.aet.cit.tum.de/api
#   OPENAI_API_KEY=...
# ============================================================================

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
SERVER_DIR="$ROOT_DIR/server"
CORPUS_DIR="$ROOT_DIR/eval/corpus"
OUT_DIR="$ROOT_DIR/eval/results"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log()  { echo -e "${CYAN}[eval]${NC} $*"; }
ok()   { echo -e "${GREEN}[eval]${NC} $*"; }
warn() { echo -e "${YELLOW}[eval]${NC} $*"; }
err()  { echo -e "${RED}[eval]${NC} $*" >&2; }

# Prints the header block above, from line 4 to the first non-comment line, so editing the header
# can never leave the --help output truncated or trailing into the code.
usage() { awk 'NR>=4 { if (!/^#/) exit; line=$0; sub(/^# ?/, "", line); print line }' "$0"; }

MODELS=""
APPROACHES=""
SEEDS=""
TEMPERATURE=""
THINKING_MODELS=""
SKIP_JUDGE=""

for arg in "$@"; do
  case "$arg" in
    --models=*)      MODELS="${arg#*=}" ;;
    --approaches=*)  APPROACHES="${arg#*=}" ;;
    --seeds=*)       SEEDS="${arg#*=}" ;;
    --temperature=*) TEMPERATURE="${arg#*=}" ;;
    --thinking-models=*) THINKING_MODELS="${arg#*=}" ;;
    --corpus=*)      CORPUS_DIR="${arg#*=}" ;;
    --out=*)         OUT_DIR="${arg#*=}" ;;
    --skip-judge)    SKIP_JUDGE="true" ;;
    --help|-h)       usage; exit 0 ;;
    *)               err "Unknown option: $arg"; echo; usage; exit 1 ;;
  esac
done

# Credentials: whatever is already in the environment wins — that is how execute-review-eval.local.sh
# supplies them — and .env.local fills in only what is missing (it is gitignored; .env is committed
# and must stay secret-free). Never passed on the command line, so they stay out of the process list.
if [[ -f "$ROOT_DIR/.env.local" ]] && { [[ -z "${OPENAI_BASE_URL:-}" ]] || [[ -z "${OPENAI_API_KEY:-}" ]]; }; then
  log "Filling in missing credentials from .env.local"
  from_shell_base_url="${OPENAI_BASE_URL:-}"
  from_shell_api_key="${OPENAI_API_KEY:-}"
  set -a
  # shellcheck disable=SC1091
  source "$ROOT_DIR/.env.local"
  set +a
  [[ -n "$from_shell_base_url" ]] && export OPENAI_BASE_URL="$from_shell_base_url"
  [[ -n "$from_shell_api_key" ]] && export OPENAI_API_KEY="$from_shell_api_key"
fi

missing=0
for var in OPENAI_BASE_URL OPENAI_API_KEY; do
  if [[ -z "${!var:-}" ]]; then
    err "$var is not set"
    missing=1
  fi
done
if [[ $missing -eq 1 ]]; then
  err "Set them in your shell, or create an uncommitted .env.local:"
  err "    OPENAI_BASE_URL=https://gpu.aet.cit.tum.de/api"
  err "    OPENAI_API_KEY=..."
  exit 1
fi

if [[ -z "$MODELS" ]]; then
  err "--models is required, e.g. --models=openai/gpt-oss-120b,google/gemma-4-26B-A4B-it"
  err "Pass several to compare them; every model runs the whole corpus through every strategy."
  exit 1
fi

if [[ ! -d "$CORPUS_DIR" ]]; then
  err "Corpus directory does not exist: $CORPUS_DIR"
  exit 1
fi

pdf_count=$(find "$CORPUS_DIR" -name '*.pdf' -type f 2>/dev/null | wc -l | tr -d ' ')
if [[ "$pdf_count" -eq 0 ]]; then
  err "No PDF found under $CORPUS_DIR"
  err "Put proposals in $CORPUS_DIR/proposal/ and theses in $CORPUS_DIR/thesis/"
  err "The folder decides how a document is reviewed, so put each one in the right place."
  exit 1
fi

log "Corpus:    $CORPUS_DIR ($pdf_count PDF(s))"
log "Models:    $MODELS"
log "Approaches:${APPROACHES:-" all four"}"
log "Results:   $OUT_DIR"
echo

gradle_args=(
  "-Pmodels=$MODELS"
  "-Pcorpus=$CORPUS_DIR"
  "-Pout=$OUT_DIR"
)
[[ -n "$APPROACHES"  ]] && gradle_args+=("-Papproaches=$APPROACHES")
[[ -n "$SEEDS"       ]] && gradle_args+=("-Pseeds=$SEEDS")
[[ -n "$TEMPERATURE" ]] && gradle_args+=("-Ptemperature=$TEMPERATURE")
[[ -n "$THINKING_MODELS" ]] && gradle_args+=("-Pthinking-models=$THINKING_MODELS")

# A sweep is long and the useful output is the runner's own progress, not Gradle's.
cd "$SERVER_DIR"
if ! ./gradlew evalReview --quiet --console=plain "${gradle_args[@]}"; then
  err "The sweep did not finish. Partial results, if any, are under $OUT_DIR"
  exit 1
fi

latest=$(find "$OUT_DIR" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | sort | tail -1)
echo
ok "Results: ${latest:-$OUT_DIR}"
if [[ -n "$latest" ]]; then
  ok "  Per review:  <model>/<approach>/<document>/seed-<n>/feedback.md"
  ok "  For scripts: runs.jsonl"
  ok "  Compare:     diff -r ${latest}/*/category-fan-out-verified ${latest}/*/single-prompt"
fi

if [[ -n "$SKIP_JUDGE" || -z "$latest" ]]; then
  ok "Judge later: ./execute-review-judge.sh ${latest:-<results dir>}"
  exit 0
fi
echo
log "Judging the sweep"
"$ROOT_DIR/execute-review-judge.sh" "$latest"
