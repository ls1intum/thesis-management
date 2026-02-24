#!/usr/bin/env bash
set -euo pipefail

# ============================================================================
# Local E2E Test Runner
#
# Starts all required services (Docker, server, client) and runs Playwright
# E2E tests against them. The server and client are always restarted to ensure
# the latest code changes are picked up.
#
# Services are left running after tests complete so you can re-run tests
# quickly with "cd client && npm run e2e". Use --stop to shut everything down.
#
# Usage:
#   ./execute-e2e-local.sh          Run E2E tests (starts/restarts services)
#   ./execute-e2e-local.sh --stop   Stop all services started by this script
#   ./execute-e2e-local.sh --ui     Run tests in interactive Playwright UI mode
#   ./execute-e2e-local.sh --headed Run tests in headed browser mode
#
# Logs:
#   Server log: .e2e-server.log
#   Client log: .e2e-client.log
# ============================================================================

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
CLIENT_DIR="$ROOT_DIR/client"
SERVER_DIR="$ROOT_DIR/server"
PID_DIR="$ROOT_DIR/.e2e-pids"          # stores PIDs of background processes

# Terminal colors for log output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log()  { echo -e "${CYAN}[e2e]${NC} $*"; }
ok()   { echo -e "${GREEN}[e2e]${NC} $*"; }
warn() { echo -e "${YELLOW}[e2e]${NC} $*"; }
err()  { echo -e "${RED}[e2e]${NC} $*"; }

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

# Check whether a given TCP port has a process listening on it.
# Uses ss on Linux (where it is standard) and falls back to lsof on macOS.
is_port_open() {
  if command -v ss >/dev/null 2>&1; then
    ss -tlnp "sport = :$1" 2>/dev/null | grep -q LISTEN
  else
    lsof -iTCP:"$1" -sTCP:LISTEN -P -n -t >/dev/null 2>&1
  fi
}

# Poll a URL until it responds successfully, with a configurable timeout.
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

# Persist a background process PID so we can stop it later (including across
# script invocations with --stop).
save_pid() {
  mkdir -p "$PID_DIR"
  echo "$2" > "$PID_DIR/$1.pid"
}

read_pid() {
  local f="$PID_DIR/$1.pid"
  [[ -f "$f" ]] && cat "$f" || echo ""
}

# Gracefully stop a previously saved background process and its child tree.
# pkill -P kills direct children (e.g. the JVM spawned by gradlew, or the
# node process spawned by npx) before terminating the wrapper process itself.
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

# Wait up to 30 seconds for a port to be released after stopping a process.
wait_for_port_release() {
  local port="$1"
  for i in $(seq 1 30); do
    is_port_open "$port" || return 0
    sleep 1
  done
  err "Port $port still in use after 30s — aborting"
  exit 1
}

# ---------------------------------------------------------------------------
# --stop: Shut down all services and exit
# ---------------------------------------------------------------------------

stop_all() {
  log "Stopping E2E services..."
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

# Separate our flags (--stop, --ui, --headed) from any extra args that should
# be forwarded to Playwright (e.g. test file filters, --grep, etc.).
PLAYWRIGHT_ARGS=()
for arg in "$@"; do
  case "$arg" in
    --stop)  stop_all ;;
    --ui)    PLAYWRIGHT_ARGS+=(--ui) ;;
    --headed) PLAYWRIGHT_ARGS+=(--headed) ;;
    *)       PLAYWRIGHT_ARGS+=("$arg") ;;
  esac
done

# ---------------------------------------------------------------------------
# 1. Docker services (PostgreSQL + Keycloak)
# ---------------------------------------------------------------------------
# Reset Docker services each run to ensure a fresh database. This removes
# anonymous volumes (PostgreSQL data), so Liquibase migrations and seed data
# recreate a clean state every time.

log "Resetting Docker services (fresh database)..."
(cd "$ROOT_DIR" && docker compose down -v 2>/dev/null) || true
log "Starting Docker services..."
(cd "$ROOT_DIR" && docker compose up -d) 2>&1 | while IFS= read -r line; do echo "     $line"; done

# Keycloak's /health endpoint is unavailable in dev mode, so we check the
# realm endpoint instead to confirm it is ready to accept auth requests.
wait_for_url "http://localhost:8081/realms/thesis-management" "Keycloak" 90

# ---------------------------------------------------------------------------
# 2. Server (Spring Boot with dev profile)
# ---------------------------------------------------------------------------
# Always restart the server to ensure the latest Java/Gradle changes are
# compiled and running. gradlew bootRun recompiles before starting.

if is_port_open 8080; then
  warn "Server already running on port 8080 — restarting to pick up latest changes..."
  kill_pid "server"
  wait_for_port_release 8080
fi
log "Starting server (dev profile)..."
(cd "$SERVER_DIR" && exec ./gradlew bootRun --args='--spring.profiles.active=dev' \
  > "$ROOT_DIR/.e2e-server.log" 2>&1) &
save_pid "server" $!

# ---------------------------------------------------------------------------
# 3. Client dev server (Webpack)
# ---------------------------------------------------------------------------
# Always restart the client to ensure a clean webpack build with the latest
# TypeScript/React changes. While webpack dev server supports hot reload,
# restarting guarantees a consistent state for E2E tests.

if is_port_open 3000; then
  warn "Client already running on port 3000 — restarting to pick up latest changes..."
  kill_pid "client"
  wait_for_port_release 3000
fi
log "Starting client dev server..."
(cd "$CLIENT_DIR" && exec npx webpack serve --env NODE_ENV=development \
  > "$ROOT_DIR/.e2e-client.log" 2>&1) &
save_pid "client" $!

# ---------------------------------------------------------------------------
# 4. Wait for server and client to be ready
# ---------------------------------------------------------------------------
# The server exposes an actuator health endpoint; the client just needs to
# serve its index page. We wait for both before running tests.

wait_for_url "http://localhost:8080/api/actuator/health" "Server" 180
wait_for_url "http://localhost:3000" "Client" 60

# ---------------------------------------------------------------------------
# 5. Run Playwright E2E tests
# ---------------------------------------------------------------------------

echo ""
log "Running Playwright E2E tests..."
echo ""

cd "$CLIENT_DIR"
npx playwright test "${PLAYWRIGHT_ARGS[@]+"${PLAYWRIGHT_ARGS[@]}"}"
EXIT_CODE=$?

echo ""
if [[ $EXIT_CODE -eq 0 ]]; then
  ok "All E2E tests passed!"
else
  err "Some E2E tests failed (exit code $EXIT_CODE)"
  warn "View report: cd client && npx playwright show-report"
fi

# Services are intentionally left running so you can quickly re-run tests
# with "cd client && npm run e2e" without waiting for services to start again.
warn "Services are still running. Use './execute-e2e-local.sh --stop' to stop them."
exit $EXIT_CODE
