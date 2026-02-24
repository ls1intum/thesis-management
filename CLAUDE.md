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

## Key Conventions

### DTO Serialization (`@JsonInclude(NON_EMPTY)`)

All DTOs use `@JsonInclude(JsonInclude.Include.NON_EMPTY)`. `null`, empty strings, and empty collections are omitted from JSON. The client must handle missing fields with `?? ''`, `?? []`, and `?.`.

### Avoid `@Transactional` in Services

Do **not** use `@Transactional` on service methods. It causes performance issues (long-held DB connections) and concurrency problems (large transaction scopes leading to lock contention). Instead, rely on Spring Data's per-repository-call transactions and design operations to be idempotent. The only acceptable place for `@Transactional` is on `@Modifying` repository methods (where Spring Data requires it) and on simple controller-level read operations that need a consistent view.

### Role Terminology

The backend/Keycloak uses `supervisor` and `advisor` roles. In the UI these are displayed as "Examiner" and "Supervisor" respectively.
