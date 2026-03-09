# Development Setup

## Quick Start

Run these commands from the project root to get the full app running locally:

```bash
# 1. Start all Docker services (PostgreSQL, Keycloak, Mailpit)
docker compose up -d

# 2. Wait for Keycloak to be ready (takes 5-10 seconds)
#    Run this until it returns JSON:
curl -sf http://localhost:8181/realms/thesis-management

# 3. Start the server (in a separate terminal)
cd server
./gradlew bootRun --args='--spring.profiles.active=dev'

# 4. Start the client (in a separate terminal)
cd client
npm install
npm run dev
```

The application is now available at http://localhost:3100. Log in with any [test user](#test-users-and-roles) (password = username).

### Local Dev Ports

Local development ports deviate from typical ports (typically +100) to avoid conflicts with other apps.

| Service | Port |
|---------|------|
| PostgreSQL | 5444 |
| Keycloak | 8181 |
| Spring Boot Server | 8180 |
| Webpack Dev Server | 3100 |
| Mailpit SMTP | 1125 |
| Mailpit Web UI | 8125 |

These ports are also used during e2e testing. The production system, however, uses the default ports (e.g. 8080 for the server and 3000 for the client).

## Application Architecture

![Architecture](files/subsystem-decomposition.svg)

## Keycloak

When the Keycloak container starts, it automatically imports the [`thesis-management` realm](../keycloak/thesis-management-realm.json), creates all test users with passwords, and configures their role mappings. No manual setup is needed. Start it from the project root:

```bash
docker compose up keycloak -d
```

The Keycloak admin console is available at http://localhost:8181 (`admin` / `admin`). See the [Test Users and Roles](#test-users-and-roles) table below for the pre-configured users (password = username).

## PostgreSQL Database

For local development start a database container by executing the following command from the project root:
```
docker compose up db -d
```

## Email (Mailpit)

Local development uses [Mailpit](https://github.com/axllent/mailpit) to capture all outgoing emails. Mailpit is included in `docker-compose.yml` and starts automatically with the other services:

```bash
docker compose up -d
```

When the server runs with the `dev` profile, it is pre-configured to send emails to Mailpit (SMTP on port 1125) with mail sending enabled. No additional configuration is needed.

> **Important:** The `dev` profile is required for emails to work. Without it, `mail.enabled` defaults to `false` and no emails are sent (you also won't see any mail-related log output).

Open the Mailpit web UI to browse captured emails:

**http://localhost:8125**

All emails (including attachments) sent by the application are available there for inspection. This replaces the previous console-only logging approach and makes it easy to verify email content, formatting, and recipients during development and testing.

## Database Migrations (Liquibase)

Liquibase migrations run automatically when the server starts. All migrations are defined under `server/src/main/resources/db/changelog/changes`.

To apply the latest schema changes, simply start the server with the `dev` profile:

```bash
cd server
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Automatic Dev Seed Data

When running with the `dev` Spring profile, Liquibase automatically seeds the database with realistic test data. This is controlled by the `application-dev.yml` configuration which activates the `dev` Liquibase context.

The seed data script (`server/src/main/resources/db/changelog/manual/seed_dev_test_data.sql`) is **idempotent** — it uses `ON CONFLICT DO NOTHING / DO UPDATE`, so it is safe to run repeatedly.

To activate the dev profile, either:
- Set the environment variable `SPRING_PROFILES_ACTIVE=dev`
- Or pass `--spring.profiles.active=dev` when starting the server

#### Test Users and Roles

> **Note on Role Terminology:** The server uses `EXAMINER` and `SUPERVISOR` as thesis role names, matching the UI labels "Examiner" and "Supervisor". Keycloak groups remain `supervisor` and `advisor` for backward compatibility — do not rename them. Usernames match the UI role labels for clarity.

| Username    | First Name  | Last Name | Email                    | Keycloak Role | UI Label   |
|-------------|-------------|-----------|--------------------------|---------------|------------|
| admin       | Admin       | User      | admin@test.local         | admin         | Admin      |
| examiner    | Examiner    | User      | examiner@test.local      | supervisor    | Examiner   |
| examiner2   | Examiner2   | User      | examiner2@test.local     | supervisor    | Examiner   |
| supervisor  | Supervisor  | User      | supervisor@test.local    | advisor       | Supervisor |
| supervisor2 | Supervisor2 | User      | supervisor2@test.local   | advisor       | Supervisor |
| student     | Student     | User      | student@test.local       | student       | Student    |
| student2    | Student2    | User      | student2@test.local      | student       | Student    |
| student3    | Student3    | User      | student3@test.local      | student       | Student    |
| student4    | Student4    | User      | student4@test.local      | student       | Student    |
| student5    | Student5    | User      | student5@test.local      | student       | Student    |

#### Research Groups

| Name                          | Abbreviation | Head        | Members                              |
|-------------------------------|--------------|-------------|--------------------------------------|
| Applied Software Engineering  | ASE          | examiner    | examiner, supervisor, supervisor2    |
| Data Science and Analytics    | DSA          | examiner2   | examiner2                            |

#### Topics

The seed data includes 10 topics across both research groups:
- 4 **open** topics (published and accepting applications)
- 3 **draft** topics (not yet published)
- 1 **closed** topic
- 1 **open** topic dedicated to interview process E2E tests

#### Applications

11 applications in various states: `ACCEPTED`, `NOT_ASSESSED`, `REJECTED`, `INTERVIEWING`, and one free-form application without a topic. Includes 2 old rejected applications for data retention E2E tests and 1 additional INTERVIEWING application for interview slot booking E2E tests.

#### Theses

11 theses covering key lifecycle states: `PROPOSAL`, `WRITING`, `SUBMITTED`, `FINISHED`, and `DROPPED_OUT`. Includes 6 additional theses (IDs 13-18) dedicated to E2E coverage gap tests: proposal acceptance, final submission, close thesis, content editing, comments, and presentation management. Each thesis includes associated roles, state history, proposals, comments, files, feedback, presentations, and assessments where applicable.

#### Interview Processes

4 interview processes (2 completed, 1 active, 1 for E2E tests) with interviewees, interview slots, and assessments.

## Server

### Preconditions
* Docker services running: `docker compose up -d`
* Wait for Keycloak to be ready: `curl -sf http://localhost:8181/realms/thesis-management` (returns JSON when ready)

> **Important:** Keycloak takes 30-60 seconds to start. If you start the server before Keycloak is ready, you will get `Connection reset` or `HTTPS required` errors and the server will fail to boot.

To start the server for local development, navigate to the `server/` folder and execute the following command from the terminal:
```
./gradlew bootRun --args='--spring.profiles.active=dev'
```

The `dev` profile is required for seed data, Mailpit email delivery, and dev Liquibase contexts. Without it, emails are disabled, no test data is loaded, and the server starts on port 8080 instead of 8180.

Server is served at http://localhost:8180.

### IntelliJ IDEA

A shared run configuration is included at `server/.run/Thesis Management Server (Dev).run.xml`. It starts the server with the `dev` profile and all required settings. Use **Run > Thesis Management Server (Dev)** from the toolbar.

If the run configuration is not detected automatically, go to **File > Settings > Build, Execution, Deployment > Build Tools > Gradle** and ensure the Gradle JVM is set to Java 25.

### Useful Gradle Commands

| Command | Description |
|---------|-------------|
| `./gradlew bootRun --args='--spring.profiles.active=dev'` | Start the server for local development |
| `./gradlew test` | Run all server tests |
| `./gradlew test jacocoTestReport` | Run tests with JaCoCo coverage report |
| `./gradlew build -x test` | Build the server without running tests |
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
* Server running at http://localhost:8180
* Keycloak realm `thesis-management` is available under http://localhost:8181 (See [Keycloak](#keycloak))

To start the client application for local development, navigate to the `client/` folder and execute the following commands from the terminal:
```
npm install
npm run dev
```

Client is served at http://localhost:3100.

## E2E Tests (Playwright)

The project includes end-to-end tests using [Playwright](https://playwright.dev/) that verify the client application works correctly across all user roles. Tests run against the full dev stack (PostgreSQL, Keycloak, server with seed data, client).

### Prerequisites

The E2E tests require all dev services to be running:

1. **PostgreSQL + Keycloak + Mailpit**: `docker compose up -d`
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
| `auth.setup.ts` | Authenticates all test users (student, student2, student3, student4, student5, supervisor, supervisor2, examiner, examiner2, admin, delete_old_thesis, delete_recent_thesis, delete_rejected_app) via Keycloak and caches their session state |
| `auth.spec.ts` | Keycloak redirect for unauthenticated users, role-based navigation item visibility for all 5 access levels |
| `navigation.spec.ts` | Public page rendering (landing page, about, footer, privacy, imprint), sidebar navigation flow, route access per role |
| `dashboard.spec.ts` | Dashboard sections per role (My Theses, My Applications), seed data verification |
| `topics.spec.ts` | Public topic browsing with search filtering, list/grid toggle, tab switching with aria-selected validation; examiner management view with seed data; student apply button |
| `applications.spec.ts` | Student application stepper form, pre-selected topic flow, supervisor and examiner review page access, NOT_ASSESSED application detail with state assertion |
| `theses.spec.ts` | Browse view per role with seed data assertions, theses overview, thesis detail page sections, student viewing own thesis, examiner2 thesis views |
| `interviews.spec.ts` | Examiner interview overview and process detail with seed data, supervisor access, examiner2 views, student access denied |
| `presentations.spec.ts` | Student, examiner, and supervisor presentations page with seed data, public presentation detail access, private presentation access denied |
| `settings.spec.ts` | My Information tab with seed data verification for student/supervisor/examiner, Notification Settings with email notification assertions |
| `research-groups.spec.ts` | Admin research group CRUD with search filtering, DSA group settings, examiner group access, student access denied |
| `public-api.spec.ts` | Published-theses API endpoint structure, avatar access control for publicly visible users, avatar denied for non-public users, pagination endpoint |
| **Workflow Tests** | |
| `topic-workflow.spec.ts` | Examiner creates a new topic end-to-end: fills title, thesis types, examiner, supervisor, problem statement; examiner2 verifies DSA group pre-fill |
| `thesis-workflow.spec.ts` | Examiner creates a new thesis end-to-end: fills title, type, language, student, supervisor, examiner |
| `application-workflow.spec.ts` | Student submits an application through the full stepper: topic selection, student info, file uploads, motivation |
| `presentation-workflow.spec.ts` | Student creates a presentation draft for a submitted thesis: type, visibility, location, language, date/time |
| `proposal-feedback-workflow.spec.ts` | Supervisor submits proposal feedback on a thesis in PROPOSAL state: opens feedback dialog, enters comment, submits |
| `application-review-workflow.spec.ts` | Supervisor rejects and accepts NOT_ASSESSED applications: reject with reason, accept with pre-filled thesis details |
| `thesis-grading-workflow.spec.ts` | Sequential thesis grading: examiner2 submits assessment, examiner2 submits final grade, examiner2 marks thesis as finished |
| `interview-workflow.spec.ts` | Examiner scores an interviewee with notes, opens add slot modal on interview process page, verifies both seeded interviewees, filter tabs (All/Uncontacted/Invited/Scheduled/Completed) |
| `thesis-lifecycle-workflow.spec.ts` | Thesis lifecycle transitions: supervisor2 accepts proposal (PROPOSAL → WRITING with email verification), student submits thesis (WRITING → SUBMITTED), supervisor closes thesis (DROPPED_OUT) |
| `thesis-content-editing.spec.ts` | Supervisor edits thesis configuration (visibility, title persistence after reload), student edits thesis info (abstract via rich text editor), student cannot modify config fields (disabled inputs, hidden buttons) |
| `thesis-comments.spec.ts` | Supervisor views existing comments with "Not visible to student" badges, adds text comment with post/disable state verification, adds comment with PDF file attachment, student cannot see supervisor-only comments |
| `topic-editing-lifecycle.spec.ts` | Examiner edits open topic (title change with modal verification), examiner closes open topic (reason select, notify students checkbox), examiner closes draft topic (different dialog title, no reason/notify options), closed topics have no action buttons |
| `interview-process-workflow.spec.ts` | Examiner creates new interview process: topic search, applicant selection, process creation with success notification; student views interview booking page with scheduled slot |
| `presentation-management.spec.ts` | Supervisor edits presentation (location change with cleanup), supervisor deletes presentation with confirmation dialog, student adds presentation note via rich text editor |
| `research-group-management.spec.ts` | Admin creates research group (name, abbreviation, group head autocomplete), admin views group settings (General/Members/Email Settings tabs, settings cards), admin views group members (member table, search, add member button) |
| `email-template-editing.spec.ts` | Admin navigates to template editor (search, edit/preview buttons), admin edits and saves template subject with reset to default |
| `user-profile-settings.spec.ts` | Student verifies readonly fields (first name, last name, email, matriculation number), student updates profile fields (gender, semester, privacy consent), student uploads documents (examination report, CV) |
| **Data Management Tests** | |
| `thesis-anonymization.spec.ts` | Admin triggers anonymization from admin page, idempotent second run, anonymized thesis banner, recent thesis unaffected, student cannot access admin page |
| `thesis-delete.spec.ts` | Admin anonymizes old/recent/active theses with appropriate warnings, examiner anonymizes own thesis, student cannot see anonymize button |
| `data-retention.spec.ts` | Admin deletes individual application with confirmation modal, batch cleanup from admin page, recent rejected application survives cleanup, supervisor cannot see delete button or admin page |
| `account-deletion.spec.ts` | Self-service account deletion for 3 user types (full deletion, soft deletion with retention, expired retention), research group head blocked, confirmation dialog safety (cancel resets state), admin user search and deletion preview (retention-blocked, active thesis, research group head), route protection |
| `data-export.spec.ts` | Data export page rendering, requesting an export and verifying processing status, privacy page link for authenticated/unauthenticated users, route protection |
| `thesis-config-user-search.spec.ts` | Thesis configuration user search filters by role (student selector shows students only, excludes supervisors/examiners), lazy user fetching verification (no /v2/users requests until dropdown opened) |
| **Coverage Gap Tests** | |
| `topic-publish.spec.ts` | Examiner publishes a draft topic via edit modal (Save & Create Topic), verifies transition from Draft to Open tab |
| `research-group-settings.spec.ts` | Admin views research group settings (Group Information with seed data, Application/Proposal/Presentation settings cards), Members tab, Email Settings tab |
| `notification-settings.spec.ts` | Examiner toggles notification preferences (Presentation Invitations switch toggle + restore), per-thesis notification toggles, student sees limited options (no management-only settings) |
| `interview-booking.spec.ts` | Student with existing booking sees Interview Scheduled page (topic info, slot details, cancel option), student without booking sees slot selection UI, completed process shows completion message |
| `thesis-file-management.spec.ts` | Supervisor views file section for WRITING thesis (upload controls, file types, upload history, submission button), student views same with upload controls, PROPOSAL thesis lacks file section |
| `thesis-file-upload.spec.ts` | Student uploads thesis PDF and verifies file history update, student uploads presentation file via file types table, supervisor uploads thesis file and verifies download button and file preview |
| `interview-slot-booking.spec.ts` | Student books an available interview slot (slot selection, reserve, verification of scheduled state with topic info), student cancels booked slot (confirmation modal, return to slot selection) |
| `research-group-settings-editing.spec.ts` | Admin toggles application auto-reject (initial state, toggle off with warning alert, toggle back on), admin changes presentation slot duration (persistence after reload), admin toggles proposal phase setting |

### Tested Roles

Every major page is tested with appropriate roles to verify access control:

- **Unauthenticated** — public pages are accessible, protected routes redirect to Keycloak login, data export page inaccessible
- **Student** — dashboard, submit application, browse theses, settings, presentations, data export, self-service account deletion; cannot access management pages
- **Supervisor** — dashboard, review applications, manage topics, theses overview, interviews, settings; cannot see delete buttons on applications or access admin page
- **Examiner** — same as supervisor (management view); additional thesis detail assertions; research group head deletion blocked
- **Admin** — all pages including research groups management, thesis anonymization, data retention cleanup, user account deletion
- **Dedicated deletion users** — `delete_rejected_app`, `delete_recent_thesis`, `delete_old_thesis` test the full account deletion flow for different retention scenarios

### Coverage

The E2E tests cover page accessibility, content rendering, role-based access control, and key end-to-end workflows. The table below summarizes what is covered and what is not.

| Area | Covered | Not yet covered |
|------|---------|-----------------|
| **Authentication & RBAC** | Keycloak redirect, nav item visibility per role (student, supervisor, examiner, admin), access denied for unauthorized roles, user menu display | Logout flow, token refresh, session expiry |
| **Topics** | Public browsing with search filtering, list/grid toggle, tab switching (Open/Published), management view with seed data, student apply button, **creating a topic end-to-end** (examiner + examiner2 with group pre-fill), **editing existing topics** (title change), **closing open topics** (reason, notify students), **closing draft topics** (different dialog, no reason/notify), **publishing draft topics** (draft → open via edit modal) | — |
| **Applications** | Stepper form with topic selection and filters, pre-selected topic flow, file uploads with privacy consent, supervisor/examiner review page with search, NOT_ASSESSED application detail, **submitting an application end-to-end** (with email verification), **accepting and rejecting applications** (with email verification) | Editing submitted applications, application filtering by state/topic |
| **Theses** | Browse per role with seed data, overview page, detail page sections (Configuration, Involved Persons, Proposal, Presentation, Comments), student own thesis, examiner2 ASSESSED thesis, **creating a thesis end-to-end** (with THESIS_CREATED email), **proposal upload** (with email), **proposal change request** (with email), **assessment → final grade → mark as finished**, user search filters by role, lazy user fetching, **proposal acceptance** (PROPOSAL → WRITING with email), **thesis submission** (WRITING → SUBMITTED), **close thesis** (DROPPED_OUT), **editing thesis configuration** (visibility, title persistence), **editing thesis info** (abstract via rich text editor), **student config restrictions** (disabled inputs, hidden buttons), **supervisor/student comments** (text + file attachment, visibility restrictions), **file management** (upload controls, file types table, upload history, submission button, PROPOSAL state lacks file section), **file upload/download** (student uploads thesis PDF + presentation file, supervisor uploads thesis + verifies download button and file preview) | Feedback requests on thesis content |
| **Thesis Anonymization** | Admin batch anonymization, idempotent second run, anonymized thesis banner with structural data preserved, pre-anonymized thesis banner, recent thesis unaffected, non-admin restrictions (student + supervisor) | — |
| **Thesis Delete** | Admin anonymizes old/recent/active theses with state + retention warnings, modal cancel/close interactions, non-admin restrictions (examiner + student) | — |
| **Interviews** | Examiner overview with interview topics + upcoming interviews, process detail with interviewees + filter tabs (All/Uncontacted/Invited/Scheduled/Completed), supervisor access, examiner2 views, student denied, **scoring interviewees** (SegmentedControl + notes), **add slot modal** (length + date), interview slots section, **creating new interview process** (topic search, applicant selection), **student interview booking page** (booked slot with topic info + cancel, slot selection UI, completed process message), **slot booking/cancellation** (student books available slot, verifies scheduled state, cancels booking with confirmation modal, returns to slot selection) | Adding/inviting interviewees, accepting applicants from interviews |
| **Presentations** | Page access per role with seed data, public presentation detail, private presentation access denied, non-existent presentation error, **creating a presentation draft**, **examiner accepts/schedules presentation** (with THESIS_PRESENTATION_SCHEDULED email), **editing presentations** (location change with cleanup), **deleting presentations** (confirmation dialog), **presentation notes** (student adds note via rich text editor) | — |
| **Settings** | My Information tab with profile data verification (student/supervisor/examiner), Notification Settings tab with email notification assertions, Account tab with deletion UI, **editing profile fields** (gender, semester, privacy consent), **uploading user documents** (examination report, CV), **readonly field verification** (first name, last name, email, matriculation number), **notification preference changes** (toggle switches + restore, per-thesis toggles, role-appropriate options) | — |
| **Research Groups** | Admin page with search filtering, group settings page (General, Members, Email Settings tabs), DSA group settings, examiner group access, Email Settings tab with application email content toggle + email template enable/disable, student access denied, **creating new research groups** (name, abbreviation, group head), **viewing group members** (member table, search, add member button), **email template editing** (navigate to editor, edit subject, save, reset to default), **settings page** (Group Information with seed data, Application/Proposal/Presentation settings cards, Members tab, Email Settings tab), **settings editing** (auto-reject toggle with warning alert, presentation slot duration change with persistence, proposal phase toggle) | Adding/removing members |
| **Public API** | Published-theses endpoint structure + content + pagination, avatar access control (allowed for publicly visible users, denied for non-public/non-existent), unauthenticated application creation rejected | — |
| **Account Deletion** | Self-service: full deletion, soft deletion with retention, full deletion after retention expiry; research group head blocked; confirmation dialog safety (cancel resets state); admin: user search + deletion preview (retention-blocked, active thesis, research group head); route protection | — |
| **Data Retention** | Admin individual application deletion with confirmation, batch cleanup of expired rejected applications, recent rejected application survives, non-admin restrictions (supervisor cannot see delete button/admin page/direct URL) | — |
| **Data Export** | Page rendering with request button, requesting export + processing status, privacy page link (authenticated sees it, unauthenticated does not), route protection (authenticated allowed, unauthenticated redirected) | Downloading completed export |
| **Email Notifications** | THESIS_CREATED, APPLICATION_CREATED_STUDENT, APPLICATION_CREATED_CHAIR, APPLICATION_REJECTED_TOPIC_REQUIREMENTS, APPLICATION_ACCEPTED, THESIS_PROPOSAL_UPLOADED, THESIS_PROPOSAL_REJECTED, THESIS_PROPOSAL_ACCEPTED, THESIS_PRESENTATION_SCHEDULED, THESIS_FINAL_GRADE — all verified via Mailpit integration | — |
| **Dashboard** | Section visibility per role (My Theses, My Applications), seed data verification (thesis states, accepted applications) | Task list accuracy, links to detail pages |
| **Navigation** | Public pages (landing, about, privacy, imprint), sidebar flow between pages, header logo navigation, footer links, unknown routes → landing page | Mobile/responsive layout, deep linking |
| **Landing Page** | Topic search and filtering UI, view toggle | Research group-specific landing pages (`/:abbreviation`), published theses section |

#### Notable gaps

All three previously identified major coverage gaps have been addressed:

1. **Actual file upload/download** — Covered by `thesis-file-upload.spec.ts`: student uploads thesis PDF and presentation file, supervisor uploads and verifies download button and file preview.
2. **Actual interview slot booking/cancellation** — Covered by `interview-slot-booking.spec.ts`: student books an available slot (serial flow), then cancels the booking via confirmation modal.
3. **Editing research group settings** — Covered by `research-group-settings-editing.spec.ts`: admin toggles auto-reject with warning alert, changes presentation slot duration with persistence after reload, toggles proposal phase.

No major functional gaps remain. Minor uncovered areas include: feedback requests on thesis content, adding/inviting interviewees, and adding/removing group members.

### CI Integration

E2E tests run automatically in CI via the `e2e_tests.yml` reusable workflow, which is called from `dev.yml` on PRs and pushes to develop/main. Test artifacts (Playwright report, screenshots, traces, videos) are uploaded as GitHub Actions artifacts and retained for 14 days.

#### CI Architecture

The CI environment mirrors the local setup: only infrastructure runs in containers, while the server and client run as native processes on the GitHub Actions runner.

| Component | How it runs in CI |
|-----------|-------------------|
| PostgreSQL | GitHub Actions service container |
| Mailpit | GitHub Actions service container |
| Keycloak | `docker run` (service containers don't support custom entrypoint commands like `start-dev`), realm imported via REST API |
| Server | `./gradlew bootRun` as a background process on the runner |
| Client | `npx webpack serve` as a background process on the runner |

CI-specific Playwright settings (controlled by `CI=1`): 2 workers (vs 8 locally), 2 retries (vs 1), no automatic `webServer` startup, and the `github` reporter is added alongside the HTML report.

#### Why Server and Client Run Natively (Not in Docker)

An alternative approach is to run the server and client in Docker containers during E2E tests, using the same (or similar) images as production. This is closer to the production environment but comes with significant trade-offs. The following analysis explains why we chose to run them natively.

**How the E2E environment differs from production:**

| Aspect | E2E (current) | Production |
|--------|---------------|------------|
| Server | `gradlew bootRun` (dev profile, full JDK) | Packaged JAR in `zulu-openjdk:25-jre` |
| Client | Webpack dev server (HMR, unminified JS) | Static build served by nginx |
| Reverse proxy | None | Traefik (TLS, rate limiting, compression) |
| Ports | 8180 / 3100 | 8080 / 80 behind Traefik on 443 |

**Trade-off analysis:**

| Factor | Native processes (current) | Docker containers |
|--------|:-:|:-:|
| **CI speed** | ++ | -- |
| **Production fidelity** | - | ++ |
| **Catches infra bugs** (nginx config, Traefik routing) | -- | + |
| **Debugging ease** | ++ | - |
| **Maintenance effort** | + | - |
| **Catches application-level bugs** | = | = |

- **Speed (major advantage of native):** Docker image builds add significant time. The server Dockerfile runs a full Gradle build inside a multi-stage image (JDK build stage → JRE runtime stage), and the client Dockerfile runs `npm install` + `npm run build` and copies the output into an nginx image. Without layer caching, this adds 5-10 minutes. Additionally, the Docker build workflow (`build_docker.yml`) currently runs in **parallel** with E2E tests. If E2E tests depended on those images, they would become **sequential** — E2E couldn't start until both images were built and loaded. With native processes, `gradlew bootRun` compiles and starts in one step, and the Webpack dev server starts without needing a production build at all.

- **Production fidelity (main advantage of Docker):** The Webpack dev server handles client-side routing natively, but in production nginx needs an explicit `try_files` configuration — a broken `nginx.conf` would not be caught by native E2E tests. Similarly, the runtime environment injection (`generate-runtime-env.js`) only runs inside the Docker client image. The server also differs: `bootRun` uses the full JDK with dev tooling, while production runs a packaged JAR on a minimal JRE.

- **Why the fidelity gap is acceptable here:** The nginx config is a simple SPA setup that rarely changes. Spring Boot JAR vs `bootRun` differences are negligible for functional testing. E2E tests exercise application logic (UI flows, API interactions, authentication), not infrastructure routing. Any Docker-specific issues (broken Dockerfile, bad nginx config) are still caught by the parallel `build_docker.yml` job — just not by the E2E tests themselves.

- **Debugging:** Native processes write logs directly to files (`.e2e-server.log`, `.e2e-client.log`) that are easy to inspect. With Docker, logs require `docker logs` and may be lost when containers are removed. Stack traces from `gradlew bootRun` are straightforward; in a container, you may need additional log driver configuration.

**Conclusion:** For this project, the speed advantage of native processes outweighs the marginal fidelity improvement of Docker. The application is a straightforward Spring Boot + React SPA with a simple infrastructure layer. Projects with more complex infrastructure (multiple backend services, custom proxy logic, SSR) would benefit more from the Docker-based approach.

## Postman Collection

A ready-to-use Postman Collection is included: [`Thesis Management API.postman_collection.json`](./Thesis%20Management%20API.postman_collection.json).

### Key Features

- **Pre-configured OAuth2 Authentication**
  The collection handles the full OAuth2 flow using Keycloak. When sending a request, Postman
  will automatically open a login window (otherwise go to the Collection > Authorization > Click
  on "Get New Access Token" at the bottom) if the token is missing or expired. Token refresh is
  also handled automatically.

- **Collection-Level Configuration**
  Authentication and common headers are defined at the collection level, so you don't need to configure them for each individual request.

- **Collection Variables**
  Key values like `{{baseUrl}}`, `{{accessToken}}`, `{{clientId}}`, etc. are pre-configured as variables. This makes the collection flexible and easy to adapt to different environments.

### How to Use

1. Open Postman and click **Import** on the top left.
2. Upload the provided [`Thesis Management API.postman_collection.json`](./Thesis%20Management%20API.postman_collection.json).
3. The collection will appear in the sidebar.
4. Start sending requests — OAuth2 authentication will be handled automatically.

> No manual token handling is needed. Just sign in via Keycloak when prompted.
