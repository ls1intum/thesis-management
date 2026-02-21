# Development Setup

## Application Architecture

![Architecture](files/subsystem-decomposition.svg)

## Keycloak

When the Keycloak container starts, it automatically imports the [`thesis-management` realm](../keycloak/thesis-management-realm.json), creates all test users with passwords, and configures their role mappings. No manual setup is needed. Start it from the project root:

```
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
