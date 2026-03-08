# CLAUDE.md

This file provides guidance for Claude Code when working with this repository.

## Build & Test Commands

### Server (Spring Boot + Gradle)
- **Run server**: `cd server && ./gradlew bootRun`
- **Run tests**: `cd server && ./gradlew test`
- **Format code**: `cd server && ./gradlew spotlessApply`

### Client (React + Webpack)
- **Install dependencies**: `cd client && npm install`
- **Run dev server**: `cd client && npm run dev`
- **Build**: `cd client && npm run build`
- **Lint**: `cd client && npx eslint src/`
- **Type check**: `cd client && npx tsc --noEmit` (ignore mantine-datatable type errors)

### E2E Tests (Playwright)
- **Run locally**: `./execute-e2e-local.sh` (starts all services automatically)
- **Run only tests**: `cd client && npm run e2e` (when services already running)
- **Interactive UI**: `./execute-e2e-local.sh --ui`

## Architecture

- **Server**: Spring Boot 3, Java 25, PostgreSQL, Keycloak for auth, Liquibase for migrations
- **Client**: React 19, TypeScript, Mantine UI, Webpack
- **Deployment**: Docker multi-platform images (amd64/arm64), deployed via GitHub Actions

## Local Dev Ports

Local development uses non-standard ports to avoid conflicts with other projects:

| Service | Port |
|---------|------|
| PostgreSQL | 5444 |
| Keycloak | 8181 |
| Spring Boot Server | 8180 |
| Webpack Dev Server | 3100 |
| Mailpit SMTP | 1125 |
| Mailpit Web UI | 8125 |

Production is unaffected — it uses Traefik on ports 80/443 with internal Docker networking.

Start Docker services with `docker compose up -d` and wait for Keycloak to be ready (`curl -sf http://localhost:8181/realms/thesis-management`) before starting the server. Mailpit catches all outgoing emails in dev — view them at http://localhost:8125.

## Key Conventions

### DTO Serialization (`@JsonInclude(NON_EMPTY)`)

All DTOs use `@JsonInclude(JsonInclude.Include.NON_EMPTY)`. `null`, empty strings, and empty collections are omitted from JSON. The client must handle missing fields with `?? ''`, `?? []`, and `?.`.

### Avoid `@Transactional` in Services

Do **not** use `@Transactional` on service methods. It causes performance issues (long-held DB connections) and concurrency problems (large transaction scopes leading to lock contention). Instead, rely on Spring Data's per-repository-call transactions and design operations to be idempotent. The only acceptable place for `@Transactional` is on `@Modifying` repository methods (where Spring Data requires it) and on simple controller-level read operations that need a consistent view.

### Role Terminology

The backend uses `EXAMINER` and `SUPERVISOR` as thesis role names (`ThesisRoleName` enum), matching the UI labels "Examiner" and "Supervisor". Keycloak groups remain `supervisor` and `advisor` for backward compatibility — do not rename them.

### Keycloak Configuration (Dev Only)

The Keycloak realm JSON (`keycloak/thesis-management-realm.json`) and the default secret in `application.yml` are **for local development only**. Production uses a separate, dedicated Keycloak server with its own configuration. Do not treat dev Keycloak settings (default secrets, disabled brute force protection, implicit flow, etc.) as security issues — they are intentional for developer convenience and do not affect production.
