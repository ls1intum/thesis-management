#!/usr/bin/env bash
set -euo pipefail

# ============================================================================
# Local E2E test runner
#
# Usage:
#   ./execute-e2e-local.sh          Run E2E tests (starts services if needed)
#   ./execute-e2e-local.sh --stop   Stop all services started by this script
#   ./execute-e2e-local.sh --ui     Run tests in interactive Playwright UI mode
#   ./execute-e2e-local.sh --headed Run tests in headed browser mode
# ============================================================================

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
CLIENT_DIR="$ROOT_DIR/client"
SERVER_DIR="$ROOT_DIR/server"
PID_DIR="$ROOT_DIR/.e2e-pids"

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

is_port_open() {
  lsof -iTCP:"$1" -sTCP:LISTEN -t >/dev/null 2>&1
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

save_pid() {
  mkdir -p "$PID_DIR"
  echo "$2" > "$PID_DIR/$1.pid"
}

read_pid() {
  local f="$PID_DIR/$1.pid"
  [[ -f "$f" ]] && cat "$f" || echo ""
}

kill_pid() {
  local pid
  pid=$(read_pid "$1")
  if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
    ok "Stopped $1 (PID $pid)"
  fi
  rm -f "$PID_DIR/$1.pid"
}

# ---------------------------------------------------------------------------
# Stop command
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
# Parse args
# ---------------------------------------------------------------------------

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
# 1. Docker services (PostgreSQL + Keycloak + CalDAV)
# ---------------------------------------------------------------------------

log "Starting Docker services..."
(cd "$ROOT_DIR" && docker compose up -d) 2>&1 | while IFS= read -r line; do echo "     $line"; done

# Wait for Keycloak by checking the realm endpoint (health endpoint is not available in dev mode)
wait_for_url "http://localhost:8081/realms/thesis-management" "Keycloak" 90

# ---------------------------------------------------------------------------
# 2. Server (Spring Boot with dev profile)
# ---------------------------------------------------------------------------

if is_port_open 8080; then
  ok "Server already running on port 8080"
else
  log "Starting server (dev profile)..."
  (cd "$SERVER_DIR" && exec ./gradlew bootRun --args='--spring.profiles.active=dev' \
    > "$ROOT_DIR/.e2e-server.log" 2>&1) &
  save_pid "server" $!
fi

# ---------------------------------------------------------------------------
# 3. Client dev server
# ---------------------------------------------------------------------------

if is_port_open 3000; then
  ok "Client already running on port 3000"
else
  log "Starting client dev server..."
  (cd "$CLIENT_DIR" && exec npx webpack serve --env NODE_ENV=development \
    > "$ROOT_DIR/.e2e-client.log" 2>&1) &
  save_pid "client" $!
fi

# ---------------------------------------------------------------------------
# 4. Wait for everything to be ready
# ---------------------------------------------------------------------------

wait_for_url "http://localhost:8080/api/actuator/health" "Server" 180
wait_for_url "http://localhost:3000" "Client" 60

# ---------------------------------------------------------------------------
# 5. Run Playwright tests
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

warn "Services are still running. Use './execute-e2e-local.sh --stop' to stop them."
exit $EXIT_CODE
