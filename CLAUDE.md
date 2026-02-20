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

- `dev.yml`: Triggers on PRs to develop/main and pushes to develop/main. Has concurrency control per PR.
- `prod.yml`: Triggers on pushes to main only. Has concurrency control (no cancellation).
- `build_docker.yml`: Separate jobs for server and client builds (not a matrix) to avoid output race conditions.
- `deploy_docker.yml`: Deploys to VMs via SSH. Uses environment protection rules requiring approval.
