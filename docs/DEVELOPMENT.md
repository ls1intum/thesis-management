# Development Setup

## Application Architecture

![Architecture](files/subsystem-decomposition.svg)

## Quick Start
To set up the development environment for the Thesis Management System, follow the steps below.

1. Start Support Services using Docker Compose

    ```bash
    docker compose up -d
    ```

2. Import Initial Test Data into the Database

    ```bash
    docker compose exec -T db psql -U thesis-management-postgres -d thesis-management < server/src/main/resources/db/changelog/manual/init.sql
    ```

3. Start the Server Application

    ```bash
    cd server
    ./gradlew bootRun
    ```

4. Start the Client Application

    ```bash
    cd ../client
    npm install
    npm run dev
    ```

5. Access the Application

    - Client: [http://localhost:3000](http://localhost:3000)
    - Server API: [http://localhost:8080](http://localhost:8080)
    - Keycloak Admin Console: [http://localhost:8081](http://localhost:8081)
    - **Use the test users provided in section [Keycloak](#keycloak).**

## Support Services

The easiest way to set up the development environment is to use Docker Compose. From the project root, run:

```bash
docker compose up -d
```

This command will start necessary support services:

- PostgreSQL Database (Port: 5144)
- Keycloak (Port: 8081)
- Radicale CalDav Server (Port: 5232)

## Keycloak

The Keycloak console is accessible at [http://localhost:8081](http://localhost:8081). \
Use the following admin credentials to log in:

- **Username:** admin
- **Password:** admin

On startup, the `thesis-management` realm is automatically imported from [thesis-management-realm.json](/keycloak/thesis-management-realm.json).
The realm includes test users with the following credentials:

| Username   | Email                   | Password   | Role       |
|------------|-------------------------|------------|------------|
| admin      | <admin@test.local>      | admin      | admin      |
| supervisor | <supervisor@test.local> | supervisor | supervisor |
| advisor    | <advisor@test.local>    | advisor    | advisor    |
| student    | <student@test.local>    | student    | student    |

Details can be found in the admin console in the realm [thesis-management](http://localhost:8081/admin/master/console/#/thesis-management).

## Database Setup and Test Data

> 💡 Tip: Make sure the database container is running (`docker compose up db -d`) before executing the commands.

### Importing Test Data

To use the Keycloak test users, you need to import initial test data into the database.
Run the following command from the project root:

```bash
docker compose exec -T db psql -U thesis-management-postgres -d thesis-management < server/src/main/resources/db/changelog/manual/init.sql
```

This will insert the four test users (admin, supervisor, advisor, student) into the `users` table and creates a sample research group which is led by the supervisor user.

### Liquibase Migration

Whenever there are changes to the database schema or initial data, these are managed via Liquibase migrations. To apply the latest migrations to your local database, navigate to the `server` folder and run:

```bash
cd ../server
./gradlew bootRun
```

This will apply all migrations defined under `server/src/main/resources/db/changelog/changes`.

### Additional Test Data

Optional test data such as more email templates, research groups, and users can be found in the `manual` folder. \
To apply these, navigate to:

```bash
cd ../server/src/main/resources/db/changelog/manual/
```

And execute all files that you need, e.g.,

```bash
docker compose exec -T db psql -U thesis-management-postgres -d thesis-management < insert_tum_aet_email_templates.sql
```

> 💡 Tip: Check the prerequisites !

> 💡 Tip: Default Templates are needed for many API calls.

## Postfix

Notice: local development does not support mailing functionality. The mails are printed in the console when no postfix instance is configured.

## Server

### Preconditions

- Database available at `jdbc:postgresql://db:5144/thesis-management`
- Keycloak realm `thesis-management` is available under <http://localhost:8081>

To start the sever application for local development, navigate to /server folder and execute the following command from the terminal:

```bash
./gradlew bootRun
```

Server is served at <http://localhost:8080>.

## Client

### Preconditions

- Server running at <http://localhost:8080>
- Keycloak realm `thesis-management` is available under <http://localhost:8081>

To start the client application for local development, navigate to /client folder and execute the following command from the terminal:

```bash
npm install
npm run dev
```

Client is served at <http://localhost:3000>.

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
3. The collection will appear in the sidebar.
4. Start sending requests — OAuth2 authentication will be handled automatically.

> 💡 No manual token handling is needed. Just sign in via Keycloak when prompted.
