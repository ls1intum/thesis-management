#!/usr/bin/env bash
set -euo pipefail

# ============================================================================
# Documentation Screenshot Capture
#
# Boots the same stack as `execute-e2e-local.sh` (Docker + Spring server +
# client production bundle) and runs the Playwright screenshot pipeline in
# `client/screenshots/`. Every "scene" registered under
# `client/screenshots/scenes/` produces a PNG in
# `documentation/static/img/screenshots/`.
#
# Services are left running after the capture so re-runs (especially with
# --grep) don't pay the full startup cost every time. Use --stop to shut
# everything down.
#
# Usage:
#   ./capture-screenshots.sh                Capture every scene
#   ./capture-screenshots.sh --grep <regex> Only capture scenes matching regex
#   ./capture-screenshots.sh --ui           Open Playwright UI mode
#   ./capture-screenshots.sh --headed       Show the browser while capturing
#   ./capture-screenshots.sh --stop         Stop the services this script started
#
# Logs:
#   Server log: .capture-server.log
#   Client log: .capture-client.log
# ============================================================================

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
CLIENT_DIR="$ROOT_DIR/client"
SERVER_DIR="$ROOT_DIR/server"
PID_DIR="$ROOT_DIR/.capture-pids"
OUTPUT_DIR="$ROOT_DIR/documentation/static/img/screenshots"
CONFIG_REL="screenshots/playwright.config.ts"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log()  { echo -e "${CYAN}[capture]${NC} $*"; }
ok()   { echo -e "${GREEN}[capture]${NC} $*"; }
warn() { echo -e "${YELLOW}[capture]${NC} $*"; }
err()  { echo -e "${RED}[capture]${NC} $*"; }

if ! command -v pnpm >/dev/null 2>&1; then
  log "pnpm not on PATH — enabling Corepack..."
  corepack enable >/dev/null 2>&1 || {
    err "Failed to enable Corepack. Install pnpm manually: npm install -g pnpm"
    exit 1
  }
fi

# ---------------------------------------------------------------------------
# Helpers (kept in sync with execute-e2e-local.sh so the two scripts don't
# drift — if you change one, update the other).
# ---------------------------------------------------------------------------

is_port_open() {
  if command -v ss >/dev/null 2>&1; then
    ss -tlnp "sport = :$1" 2>/dev/null | grep -q LISTEN
  else
    lsof -iTCP:"$1" -sTCP:LISTEN -P -n -t >/dev/null 2>&1
  fi
}

wait_for_url() {
  local url="$1" label="$2" max_wait="${3:-120}"
  log "Waiting for $label ..."
  for i in $(seq 1 "$max_wait"); do
    if curl -sf "$url" >/dev/null 2>&1; then
      ok "$label is ready (${i}s)"
      return 0
    fi
    sleep 1
  done
  err "$label did not start within ${max_wait}s"
  return 1
}

save_pid() { mkdir -p "$PID_DIR"; echo "$2" > "$PID_DIR/$1.pid"; }

read_pid() {
  local f="$PID_DIR/$1.pid"
  [[ -f "$f" ]] && cat "$f" || echo ""
}

kill_pid() {
  local pid
  pid=$(read_pid "$1")
  if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
    pkill -P "$pid" 2>/dev/null || true
    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
    ok "Stopped $1 (PID $pid)"
  fi
  rm -f "$PID_DIR/$1.pid"
}

wait_for_port_release() {
  local port="$1"
  for i in $(seq 1 30); do
    is_port_open "$port" || return 0
    sleep 1
  done
  err "Port $port still in use after 30s — aborting"
  exit 1
}

# PIDs of the process(es) listening on $1, using whichever tool is available (mirrors
# is_port_open's detection). Empty when none can be determined.
listener_pids() {
  local port="$1"
  if command -v lsof >/dev/null 2>&1; then
    lsof -tiTCP:"$port" -sTCP:LISTEN -P -n 2>/dev/null
  elif command -v ss >/dev/null 2>&1; then
    ss -tlnpH "sport = :$port" 2>/dev/null | grep -oE 'pid=[0-9]+' | cut -d= -f2 | sort -u
  fi
}

# Stops a process holding $1 that we did NOT start via a pidfile (a stray `serve`, a manual
# `pnpm dev`, or a run whose pidfile was removed). Sends SIGTERM and waits briefly; returns 0
# once the port is free. Fails immediately with an actionable error if the owner can't be
# identified or won't release the port — never a blind SIGKILL on a process we don't own.
stop_port_listener() {
  local port="$1" pids
  pids=$(listener_pids "$port")
  if [[ -z "$pids" ]]; then
    err "Port $port is in use but its owner could not be identified (need lsof or ss). Free port $port and re-run."
    exit 1
  fi

  warn "Stopping untracked process(es) holding port $port: $(echo "$pids" | tr '\n' ' ')"
  # shellcheck disable=SC2086
  kill $pids 2>/dev/null || true

  for _ in $(seq 1 10); do
    is_port_open "$port" || return 0
    sleep 1
  done

  err "Could not free port $port (still held by PID(s): $(echo "$pids" | tr '\n' ' ')). Stop it manually and re-run."
  exit 1
}

# ---------------------------------------------------------------------------
# --stop: Shut down all services and exit
# ---------------------------------------------------------------------------

stop_all() {
  log "Stopping capture services..."
  kill_pid "client"
  kill_pid "server"
  (cd "$ROOT_DIR" && docker compose stop 2>/dev/null) || true
  rm -rf "$PID_DIR"
  ok "All services stopped."
  exit 0
}

# ---------------------------------------------------------------------------
# Parse command-line arguments
# ---------------------------------------------------------------------------

PLAYWRIGHT_ARGS=()
for arg in "$@"; do
  case "$arg" in
    --stop)   stop_all ;;
    --ui)     PLAYWRIGHT_ARGS+=(--ui) ;;
    --headed) PLAYWRIGHT_ARGS+=(--headed) ;;
    *)        PLAYWRIGHT_ARGS+=("$arg") ;;
  esac
done

# ---------------------------------------------------------------------------
# 1. Docker services (PostgreSQL + Keycloak)
# ---------------------------------------------------------------------------
# Reuse existing Docker services when they're already up — a full reset here
# is wasteful for a documentation task and would trash any state a human was
# curating on the local instance. Users who want a clean slate can run
# `docker compose down -v` explicitly beforehand.

if is_port_open 5444 && is_port_open 8181; then
  log "Docker services already running — reusing them."
else
  log "Starting Docker services..."
  (cd "$ROOT_DIR" && docker compose up -d) 2>&1 | while IFS= read -r line; do echo "     $line"; done
fi

wait_for_url "http://localhost:8181/realms/thesis-management" "Keycloak" 90

# ---------------------------------------------------------------------------
# 2. Server (Spring Boot with dev profile)
# ---------------------------------------------------------------------------
# Reuse an already-running server. If nothing is listening on 8180, start one.

if is_port_open 8180; then
  log "Server already running on port 8180 — reusing it."
else
  log "Starting server (dev profile)..."
  (cd "$SERVER_DIR" && exec ./gradlew bootRun --args='--spring.profiles.active=dev' \
    > "$ROOT_DIR/.capture-server.log" 2>&1) &
  save_pid "server" $!
fi

# ---------------------------------------------------------------------------
# 3. Client static bundle
# ---------------------------------------------------------------------------
# Same production-bundle strategy as e2e: it eliminates the dev overlay that
# occasionally intercepts pointer events, so screenshots stay clean.

need_client_start=1
if is_port_open 3100; then
  # A client is already listening. Only reuse it when it was built with AI features enabled —
  # staff-19-request-changes-ai-drafts needs the "Generate with AI" button, which the client
  # renders only when AI_FEATURES_ENABLED=true. The generated runtime-env.js carries that flag,
  # so probe it; if AI is not confirmed, restart the client with the required environment rather
  # than capturing against an incompatible build.
  if curl -sf "http://localhost:3100/runtime-env.js" 2>/dev/null | grep -q '"AI_FEATURES_ENABLED":"true"'; then
    log "Client already running on port 3100 with AI features enabled — reusing it."
    need_client_start=0
  else
    warn "Client on port 3100 was not built with AI_FEATURES_ENABLED=true — restarting it so AI screenshots render..."
    kill_pid "client"
    # kill_pid only stops a client THIS script started (tracked via .capture-pids/client.pid).
    # If a foreign process still holds 3100, stop it too — otherwise wait_for_port_release would
    # just hang for 30s before aborting.
    if is_port_open 3100; then
      stop_port_listener 3100
    fi
    wait_for_port_release 3100
  fi
fi

if [[ "$need_client_start" == "1" ]]; then
  # The AI feedback screenshot scenes mock the endpoints but still need the buttons rendered,
  # which the client only does when AI features are enabled (matching the dev server profile).
  export AI_FEATURES_ENABLED=true
  log "Building client (production)..."
  (cd "$CLIENT_DIR" && pnpm build > "$ROOT_DIR/.capture-client-build.log" 2>&1) || {
    err "Client build failed. See $ROOT_DIR/.capture-client-build.log"
    exit 1
  }
  log "Generating runtime-env.js..."
  (cd "$CLIENT_DIR/build" && node ../public/generate-runtime-env.js)
  log "Starting static client server (serve)..."
  (cd "$CLIENT_DIR" && exec pnpm dlx serve@14 -s build -l 3100 -c ../serve.e2e.json --no-clipboard --no-port-switching \
    > "$ROOT_DIR/.capture-client.log" 2>&1) &
  save_pid "client" $!
fi

# ---------------------------------------------------------------------------
# 4. Wait for server + client
# ---------------------------------------------------------------------------

wait_for_url "http://localhost:8180/api/actuator/health" "Server" 180
wait_for_url "http://localhost:3100" "Client" 60

# ---------------------------------------------------------------------------
# 5. Playwright browsers
# ---------------------------------------------------------------------------

log "Ensuring Playwright chromium browser is installed..."
(cd "$CLIENT_DIR" && pnpm exec playwright install chromium) || {
  err "Playwright browser install failed."
  exit 1
}

# ---------------------------------------------------------------------------
# 6. Capture screenshots
# ---------------------------------------------------------------------------

mkdir -p "$OUTPUT_DIR"

echo ""
log "Capturing screenshots into $OUTPUT_DIR ..."
echo ""

cd "$CLIENT_DIR"
EXIT_CODE=0
pnpm exec playwright test --config "$CONFIG_REL" "${PLAYWRIGHT_ARGS[@]+"${PLAYWRIGHT_ARGS[@]}"}" || EXIT_CODE=$?

echo ""
if [[ $EXIT_CODE -eq 0 ]]; then
  ok "All screenshots captured."
  ok "Output: $OUTPUT_DIR"
else
  err "Some scenes failed (exit code $EXIT_CODE)"
  warn "View report: cd client && pnpm exec playwright show-report screenshots/screenshot-report"
fi

warn "Services are still running. Use './capture-screenshots.sh --stop' to stop them."
exit $EXIT_CODE
