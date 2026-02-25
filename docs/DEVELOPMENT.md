# Development Setup

## Application Architecture

![Architecture](files/subsystem-decomposition.svg)

## Keycloak

When the Keycloak container starts, it automatically imports the [`thesis-management` realm](../keycloak/thesis-management-realm.json), creates all test users with passwords, and configures their role mappings. No manual setup is needed. Start it from the project root:

```bash
docker compose up keycloak -d
```

The Keycloak admin console is available at http://localhost:8081 (`admin` / `admin`). See the [Test Users and Roles](#test-users-and-roles) table below for the pre-configured users (password = username).

## PostgreSQL Database

For local development start a database container by executing the following command from the project root:
```
docker compose up db -d
```

### Liquibase Migration

To apply the latest database schema changes using Liquibase, run the following command:

```bash
cd ../server
./gradlew bootRun
```

This will apply all migrations defined under `server/src/main/resources/db/changelog/changes`.

### Automatic Dev Seed Data

When running with the `dev` Spring profile, Liquibase automatically seeds the database with realistic test data. This is controlled by the `application-dev.yml` configuration which activates the `dev` Liquibase context.

The seed data script (`server/src/main/resources/db/changelog/manual/seed_dev_test_data.sql`) is **idempotent** — it uses `ON CONFLICT DO NOTHING / DO UPDATE`, so it is safe to run repeatedly.

To activate the dev profile, either:
- Set the environment variable `SPRING_PROFILES_ACTIVE=dev`
- Or pass `--spring.profiles.active=dev` when starting the server

> 💡 Tip: Default email templates are now inserted automatically by Liquibase migrations.

#### Test Users and Roles

> **Note on Role Terminology:** The server / Keycloak uses `supervisor` and `advisor` roles internally. In the UI, these are displayed as "Examiner" and "Supervisor" respectively to align with CIT terminology.

| Username    | First Name  | Last Name | Email                    | Role (Server) | UI Label   |
|-------------|-------------|-----------|--------------------------|---------------|------------|
| admin       | Admin       | User      | admin@test.local         | admin         | Admin      |
| supervisor  | Supervisor  | User      | supervisor@test.local    | supervisor    | Examiner   |
| supervisor2 | Supervisor2 | User      | supervisor2@test.local   | supervisor    | Examiner   |
| advisor     | Advisor     | User      | advisor@test.local       | advisor       | Supervisor |
| advisor2    | Advisor2    | User      | advisor2@test.local      | advisor       | Supervisor |
| student     | Student     | User      | student@test.local       | student       | Student    |
| student2    | Student2    | User      | student2@test.local      | student       | Student    |
| student3    | Student3    | User      | student3@test.local      | student       | Student    |
| student4    | Student4    | User      | student4@test.local      | student       | Student    |
| student5    | Student5    | User      | student5@test.local      | student       | Student    |

#### Research Groups

| Name                          | Abbreviation | Head        | Members                          |
|-------------------------------|--------------|-------------|----------------------------------|
| Applied Software Engineering  | ASE          | supervisor  | supervisor, advisor, advisor2    |
| Data Science and Analytics    | DSA          | supervisor2 | supervisor2                      |

#### Topics

The seed data includes 6 topics across both research groups:
- 3 **open** topics (published and accepting applications)
- 2 **draft** topics (not yet published)
- 1 **closed** topic

#### Applications

8 applications in various states: `ACCEPTED`, `NOT_ASSESSED`, `REJECTED`, `INTERVIEWING`, and one free-form application without a topic.

#### Theses

5 theses covering key lifecycle states: `PROPOSAL`, `WRITING`, `SUBMITTED`, `FINISHED`, and `DROPPED_OUT`. Each thesis includes associated roles, state history, proposals, comments, files, feedback, presentations, and assessments where applicable.

#### Interview Processes

3 interview processes (2 completed, 1 active) with interviewees, interview slots, and assessments.

## Postfix

Notice: local development does not support mailing functionality. The mails are printed in the console when no postfix instance is configured.

## Server

### Preconditions
* Database available at `jdbc:postgresql://db:5144/thesis-management`
* Keycloak realm `thesis-management` is available under http://localhost:8081 (See [Keycloak Setup](#keycloak-setup))

To start the sever application for local development, navigate to /server folder and execute the following command from the terminal:
```
./gradlew bootRun
```

Server is served at http://localhost:8080.

### Useful Gradle Commands

| Command | Description |
|---------|-------------|
| `./gradlew test` | Run all server tests |
| `./gradlew test jacocoTestReport` | Run tests with JaCoCo coverage report |
| `./gradlew build -x test` | Build the server without running tests |
| `./gradlew bootRun` | Start the server for local development |
| `./gradlew spotlessApply` | Auto-format code (imports, whitespace, tabs) |
| `./gradlew spotlessCheck` | Check code formatting without modifying files |
| `./gradlew checkstyleMain` | Run Checkstyle on main source code |
| `./gradlew checkstyleTest` | Run Checkstyle on test source code |
| `./gradlew dependencyUpdates -Drevision=release` | Find available dependency updates |

After running tests with coverage, the HTML report is available at `server/build/reports/jacoco/test/html/index.html`.

### Coding Conventions

#### Avoid `@Transactional` in Services

Do **not** annotate service methods with `@Transactional`. Long-running transactions hold database connections for the entire method duration, which degrades connection pool throughput under load. Large transaction scopes also increase lock contention and the risk of deadlocks.

Instead, rely on Spring Data's default per-repository-call transaction behavior: each `save()`, `delete()`, or `@Modifying` query runs in its own short-lived transaction. Design service operations to be **idempotent** so that partial completion can be safely retried.

The only acceptable uses of `@Transactional` are:
- On `@Modifying` repository methods (required by Spring Data JPA)
- On simple controller-level read operations that need a consistent snapshot (e.g., loading an entity and its lazy associations in one go)

```java
// Avoid — holds a connection for the entire multi-step operation
@Transactional
public void complexOperation(UUID id) {
    var entity = repo.findById(id).orElseThrow();
    // ... long processing ...
    repo.save(entity);
    otherRepo.deleteByParentId(id);
}

// Preferred — each repository call is its own short transaction
public void complexOperation(UUID id) {
    var entity = repo.findById(id).orElseThrow();
    // ... processing ...
    repo.save(entity);
    otherRepo.deleteByParentId(id);
}
```

#### DTOs

Use Java `record` types for all Data Transfer Objects (DTOs). Records are immutable, concise, and well-suited for API response objects.

Annotate every DTO with `@JsonInclude(JsonInclude.Include.NON_EMPTY)` to omit `null` values, empty strings, and empty collections from JSON responses. This reduces payload size and keeps API responses clean. On the client side, handle potentially missing fields with optional types (`?`) and fallback defaults (`?? ''`, `?? []`).

```java
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ExampleDto(UUID id, String name, List<String> items) {
    public static ExampleDto fromEntity(ExampleEntity entity) {
        return new ExampleDto(entity.getId(), entity.getName(), entity.getItems());
    }
}
```

#### JPA Entity Relationships

Prefer `FetchType.LAZY` over `FetchType.EAGER` for `@OneToMany` and `@ManyToMany` relationships. Eager loading causes additional SQL queries for every entity loaded, even when the related data is not needed (e.g. in list endpoints). Lazy loading defers these queries until the data is actually accessed.

```java
// Preferred
@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
private List<ChildEntity> children = new ArrayList<>();

// Avoid
@OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
private List<ChildEntity> children = new ArrayList<>();
```

## Client

### Coding Conventions

#### Handling API Responses

The server uses `@JsonInclude(JsonInclude.Include.NON_EMPTY)` on all DTOs, which means `null` values, empty strings (`""`), and empty collections (`[]`) are omitted from JSON responses. TypeScript interfaces must account for this:

- Mark fields that may be omitted as optional with `?`
- Always use fallback defaults when accessing optional fields: `?? ''` for strings, `?? []` for arrays
- Use optional chaining (`?.`) when accessing nested properties on optional fields
- Never assume an array field will be present — always fall back to `[]`

```typescript
// Correct: handle potentially missing fields
const types = topic.thesisTypes ?? []
const name = user.firstName ?? ''
const hasAccess = user.groups?.includes('admin') ?? false

// Incorrect: assumes field is always present
const types = topic.thesisTypes  // may be undefined
```

#### Preconditions
* Server running at http://localhost:8080
* Keycloak realm `thesis-management` is available under http://localhost:8081 (See [Keycloak Setup](#keycloak-setup))

To start the client application for local development, navigate to /client folder and execute the following command from the terminal:
```
npm install
npm run dev
```

Client is served at http://localhost:3000. 

## E2E Tests (Playwright)

The project includes end-to-end tests using [Playwright](https://playwright.dev/) that verify the client application works correctly across all user roles. Tests run against the full dev stack (PostgreSQL, Keycloak, server with seed data, client).

### Prerequisites

The E2E tests require all dev services to be running:

1. **PostgreSQL + Keycloak**: `docker compose up -d`
2. **Server** (dev profile with seed data): `cd server && ./gradlew bootRun --args='--spring.profiles.active=dev'`
3. **Client** (dev server): `cd client && npm run dev`
4. **Install Playwright browsers** (first time only): `cd client && npx playwright install chromium`

### Running E2E Tests

#### One-command local run (recommended)

The `execute-e2e-local.sh` script in the project root starts all required services automatically and runs the tests. It is idempotent — it detects already-running services and reuses them, so it can be executed repeatedly.

```bash
# Headless (default)
./execute-e2e-local.sh

# Interactive Playwright UI
./execute-e2e-local.sh --ui

# Headed browser (watch tests run)
./execute-e2e-local.sh --headed

# Stop all services started by the script
./execute-e2e-local.sh --stop
```

#### Manual run (when services are already running)

```bash
cd client

# Headless
npm run e2e

# Interactive Playwright UI
npm run e2e:ui

# Headed browser
npm run e2e:headed
```

#### Run a single test file

```bash
cd client
npx playwright test e2e/auth.spec.ts
```

### Test Structure

Tests are located in `client/e2e/` and authenticate via the Keycloak login form using the seeded test users (password = username). Auth state is cached in `e2e/.auth/` and reused across tests. Shared helpers (`helpers.ts`) provide utilities for navigation, Mantine component interaction (select, multi-select, rich text editor), and test data generation.

| File | Description |
|------|-------------|
| `auth.setup.ts` | Authenticates all test users (student, student2, student3, advisor, advisor2, supervisor, supervisor2, admin) via Keycloak and caches their session state |
| `auth.spec.ts` | Keycloak redirect for unauthenticated users, role-based navigation item visibility for all 5 access levels |
| `navigation.spec.ts` | Public page rendering (landing page, about, footer), sidebar navigation flow, route access per role |
| `dashboard.spec.ts` | Dashboard sections per role (My Theses, My Applications) |
| `topics.spec.ts` | Public topic browsing with search, filters, and list/grid toggle; supervisor management view; student apply button |
| `applications.spec.ts` | Student application stepper form, pre-selected topic flow, advisor and supervisor review page access |
| `theses.spec.ts` | Browse view per role, theses overview, thesis detail page sections, student viewing own thesis |
| `interviews.spec.ts` | Supervisor interview overview and process detail, advisor access, student access denied |
| `presentations.spec.ts` | Student and supervisor presentations page, public presentation detail access |
| `settings.spec.ts` | My Information and Notification Settings tabs for student and advisor |
| `research-groups.spec.ts` | Admin research group CRUD with search filtering, supervisor group access, student access denied |
| **Workflow Tests** | |
| `topic-workflow.spec.ts` | Supervisor creates a new topic end-to-end: fills title, thesis types, examiner, supervisor, problem statement |
| `thesis-workflow.spec.ts` | Supervisor creates a new thesis end-to-end: fills title, type, language, student, supervisor, examiner |
| `application-workflow.spec.ts` | Student submits an application through the full stepper: topic selection, student info, file uploads, motivation |
| `presentation-workflow.spec.ts` | Student creates a presentation draft for a submitted thesis: type, visibility, location, language, date/time |
| `proposal-feedback-workflow.spec.ts` | Advisor submits proposal feedback on a thesis in PROPOSAL state: opens feedback dialog, enters comment, submits |
| `application-review-workflow.spec.ts` | Advisor rejects and accepts NOT_ASSESSED applications: reject with reason, accept with pre-filled thesis details |
| `thesis-grading-workflow.spec.ts` | Sequential thesis grading: advisor submits assessment, supervisor submits final grade, supervisor marks thesis as finished |
| `interview-workflow.spec.ts` | Supervisor scores an interviewee with notes, opens add slot modal on interview process page |

### Tested Roles

Every major page is tested with appropriate roles to verify access control:

- **Unauthenticated** — public pages are accessible, protected routes redirect to Keycloak login
- **Student** — dashboard, submit application, browse theses, settings, presentations; cannot access management pages
- **Advisor** — dashboard, review applications, manage topics, theses overview, interviews, settings
- **Supervisor** — same as advisor (management view); additional thesis detail assertions
- **Admin** — all pages including research groups management

### Coverage

The E2E tests focus on page accessibility, content rendering, and role-based access control. The table below summarizes what is currently covered and what is not.

| Area | Covered | Not yet covered |
|------|---------|-----------------|
| **Authentication & RBAC** | Keycloak redirect, nav item visibility per role, access denied for unauthorized roles | Token refresh, session expiry, logout |
| **Topics** | Public browsing, search, filters, list/grid toggle, management view, student apply button, **creating a topic end-to-end** | Editing/closing topics, draft topics |
| **Applications** | Stepper form rendering, pre-selected topic, advisor/supervisor review page access, **submitting an application end-to-end**, **accepting and rejecting applications** | — |
| **Theses** | Browse per role, overview page, detail page sections, student own thesis, **creating a thesis end-to-end**, **submitting proposal feedback**, **assessment → final grade → mark as finished** | Comments |
| **Interviews** | Supervisor overview and process detail, advisor access, student denied, **scoring interviewees with notes**, **add slot modal** | Creating interview processes, booking slots |
| **Presentations** | Page access per role, public presentation detail, **creating a presentation draft** | Calendar integration |
| **Settings** | Tab rendering per role | Editing profile information, notification preferences |
| **Research Groups** | Admin CRUD page, search filtering, student denied | Creating/editing groups, member management |
| **Dashboard** | Section visibility per role (My Theses, My Applications) | Dashboard data accuracy, links to detail pages |
| **Navigation** | Public pages, sidebar flow, header logo, footer links, unknown routes | Mobile/responsive layout, deep linking |

**In summary:** The tests cover page rendering/access control across all roles and key end-to-end workflows including topic creation, thesis creation, application submission, presentation scheduling, proposal feedback, application accept/reject, thesis grading (assessment → grade → finish), and interview scoring.

### CI Integration

E2E tests run automatically in CI via the `e2e_tests.yml` reusable workflow, which is called from `dev.yml` on PRs and pushes to develop/main. The CI workflow spins up PostgreSQL and Keycloak as service containers, starts the server with the dev profile, builds and serves the client, and runs the Playwright tests.

Test artifacts (screenshots, traces, videos) are uploaded on failure and available in the GitHub Actions artifacts.

## Postman Collection

A ready-to-use Postman Collection is included: [`TUMApply API.postman_collection.json`](./Thesis%20Management%20API.postman_collection.json).

### Key Features

- ✅ **Pre-configured OAuth2 Authentication**  
  The collection handles the full OAuth2 flow using Keycloak. When sending a request, Postman 
  will automatically open a login window (otherwise go to the Collection > Authorization > Click 
  on "Get New Access Token" at the bottom) if the token is missing or expired. Token refresh is 
  also handled automatically.

- ✅ **Collection-Level Configuration**  
  Authentication and common headers are defined at the collection level, so you don't need to configure them for each individual request.

- ✅ **Collection Variables**  
  Key values like `{{baseUrl}}`, `{{accessToken}}`, `{{clientId}}`, etc. are pre-configured as variables. This makes the collection flexible and easy to adapt to different environments.

### How to Use

1. Open Postman and click **Import** on the top left.
2. Upload the provided [`TUMApply API.postman_collection.json`](./Thesis%20Management%20API.postman_collection.json).
   postman_collection.json).
3. The collection will appear in the sidebar.
4. Start sending requests — OAuth2 authentication will be handled automatically.

> 💡 No manual token handling is needed. Just sign in via Keycloak when prompted.
