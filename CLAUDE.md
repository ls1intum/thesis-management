# CLAUDE.md

This file provides guidance for Claude Code when working with this repository.

## Build & Test Commands

### Server (Spring Boot + Gradle)
- **Run server**: `cd server && ./gradlew bootRun`
- **Run tests**: `cd server && ./gradlew test`
- **Run tests with coverage**: `cd server && ./gradlew test jacocoTestReport`
- **Format code**: `cd server && ./gradlew spotlessApply`
- **Check formatting**: `cd server && ./gradlew spotlessCheck`

### Client (React + Webpack)
- **Install dependencies**: `cd client && npm install`
- **Run dev server**: `cd client && npm run dev`
- **Build**: `cd client && npm run build`
- **Lint**: `cd client && npx eslint src/`
- **Type check**: `cd client && npx tsc --noEmit` (ignore mantine-datatable type errors — pre-existing)

### Client E2E Tests (Playwright)

E2E tests require the full dev stack running: PostgreSQL, Keycloak, server (dev profile), and client dev server.

**Prerequisites** (start these first):
1. `docker-compose up` (starts PostgreSQL + Keycloak + CalDAV)
2. `cd server && ./gradlew bootRun --args='--spring.profiles.active=dev'` (starts server with seed data)
3. `cd client && npm run dev` (starts client dev server on port 3000)

**One-command local run** (starts all services automatically):
- **Headless**: `./execute-e2e-local.sh`
- **Interactive UI**: `./execute-e2e-local.sh --ui`
- **Headed browser**: `./execute-e2e-local.sh --headed`
- **Stop services**: `./execute-e2e-local.sh --stop`

The script is idempotent — it reuses already-running services and can be executed repeatedly. Services stay running between invocations for fast re-runs.

**Run tests only** (when services are already running manually):
- **Headless**: `cd client && npm run e2e`
- **Interactive UI**: `cd client && npm run e2e:ui`
- **Headed browser**: `cd client && npm run e2e:headed`

**Install browsers** (first time only): `cd client && npx playwright install chromium`

Tests authenticate via the Keycloak login form using seeded test users (student/student, advisor/advisor, supervisor/supervisor, admin/admin). Auth state is cached in `e2e/.auth/` and reused across tests. Test files are in `client/e2e/`.

**Test coverage** (50 tests across 11 files):
- `auth.spec.ts` — Authentication & role-based nav visibility (5 roles: unauthenticated, student, advisor, supervisor, admin)
- `navigation.spec.ts` — Public pages, sidebar navigation, route access per role
- `dashboard.spec.ts` — Dashboard sections per role (My Theses, My Applications)
- `topics.spec.ts` — Public topic browsing (search, filters, list/grid), management view, student apply
- `applications.spec.ts` — Student stepper form, pre-selected topic, advisor/supervisor review access
- `theses.spec.ts` — Browse view per role, overview, detail page sections, student own thesis
- `interviews.spec.ts` — Supervisor overview/detail, advisor access, student denied
- `presentations.spec.ts` — Student/supervisor access, public presentation detail
- `settings.spec.ts` — My Information and Notification Settings tabs per role
- `research-groups.spec.ts` — Admin CRUD, search filtering, supervisor access, student denied

## Architecture

- **Server**: Spring Boot 3, Java 25, PostgreSQL, Keycloak for auth, Liquibase for migrations
- **Client**: React 19, TypeScript, Mantine UI, Webpack
- **Deployment**: Docker multi-platform images (amd64/arm64), deployed via GitHub Actions to VMs using docker-compose

## Key Conventions

### Server: DTO Serialization (`@JsonInclude(NON_EMPTY)`)

All DTOs use `@JsonInclude(JsonInclude.Include.NON_EMPTY)`. This is an intentional API contract:
- `null` values, empty strings (`""`), and empty collections (`[]`) are **omitted** from JSON responses
- This applies to **all** DTOs (detail and overview), not just list endpoints
- The client **must** handle missing fields gracefully — this is by design, not a bug

### Server: JPA Fetch Types

Prefer `FetchType.LAZY` for `@OneToMany` and `@ManyToMany` relationships. The application uses `spring.jpa.open-in-view=true`, so lazy loading works throughout the request lifecycle including in controllers.

### Client: Handling API Responses

Since the server omits empty/null fields from JSON:
- TypeScript interfaces mark omittable fields as optional (`?`)
- Always use fallback defaults: `?? ''` for strings, `?? []` for arrays
- Use optional chaining (`?.`) for nested access on optional fields
- Never assume an array or string field is present in the response

### Role Terminology

The backend/Keycloak uses `supervisor` and `advisor` roles. In the UI these are displayed as "Examiner" and "Supervisor" respectively.

## CI/CD

- `dev.yml`: Triggers on PRs to develop/main and pushes to develop/main. Has concurrency control per PR. Runs server tests, E2E tests, builds Docker images, and deploys.
- `prod.yml`: Triggers on pushes to main only. Has concurrency control (no cancellation).
- `build_docker.yml`: Separate jobs for server and client builds (not a matrix) to avoid output race conditions.
- `deploy_docker.yml`: Deploys to VMs via SSH. Uses environment protection rules requiring approval.
- `e2e_tests.yml`: Reusable workflow that spins up PostgreSQL + Keycloak, starts the server (dev profile with seed data) and client, then runs Playwright E2E tests.
