# Development Setup

## Application Architecture

![Architecture](files/subsystem-decomposition.svg)

## Keycloak

For local development start a keycloak container by following the steps below:
1. From the project root execute:
```
docker compose up keycloak -d
```
2. Open http://localhost:8081 and sign in with admin credentials
    * Username: `admin`
    * Password: `admin`
3. Click on the menu in the top left and go to [Manage realm](http://localhost:8081/admin/master/console/#/master/realms)
4. Click on "Create realm", a popup appears
5. Import the [keycloak-realm-config-example-json](/keycloak-realm-config-example.json) using drag and drop (alternatively, you can also create a new realm `thesis-management` manually)
6. Select the newly created realm and create your user in [Users](http://localhost:8081/admin/master/console/#/thesis-management/users) (username, email, first name, last name)
7. Go to "Credentials" for the new user and set a password (disable "Temporary" to avoid being forced to change it on first login)
8. Go to "Role mapping" and assign the client roles `admin`, `supervisor`, `advisor` to the new user
   * Select "Filter by clients" and search for "thesis-management-app" to find the roles

## PostgreSQL Database

For local development start a database container by executing the following command from the project root:
```
docker compose up db -d
```

## Database Setup and Test Data

> 💡 Tip: Make sure the database container is running (`docker compose up db -d`) before executing the commands.

### Liquibase Migration

To apply the latest database schema changes using Liquibase, run the following command:

```bash
cd ../server
./gradlew bootRun
```

This will apply all migrations defined under `server/src/main/resources/db/changelog/changes`.

### Importing Test Data

To populate the database with test data, go to the manual migration folder:

```bash
cd ../server/src/main/resources/db/changelog/manual/
```

And execute all files that you need.
> 💡 Tip: Check the prerequisites !

> 💡 Tip: Default Templates are needed for many API calls.

#### Example Users and Roles

| Username       | First Name | Last Name | Email                    | Role       |
|----------------|------------|-----------|--------------------------|------------|
| sam_fischer    | Sam        | Fischer   | sam_fischer@gmail.com    | supervisor |
| jane_doe       | Jane       | Doe       | jane_doe@gmail.com       | supervisor |
| joey_read      | Joey       | Read      | joey_read@gmail.com      | advisor    |
| barney_young   | Barney     | Young     | barney_young@gmail.com   | advisor    |
| chloe_mitchell | Chloe      | Mitchell  | chloe_mitchell@gmail.com | student    |
| kelly_wilkins  | Kelly      | Wilkins   | kelly_wilkins@gmail.com  | student    |

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

## Client

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
