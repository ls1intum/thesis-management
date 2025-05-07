# 🗄️ Database Changes

ThesisManagement uses **Liquibase** for managing database schema changes. All schema modifications
must be tracked and versioned properly to ensure consistency across development, testing, and
production environments.

## 📋 How to Apply a Schema Change

1. Create a new changeset by adding a SQL script in the [
   `/server/src/main/resources/db/changelog/changes`](../server/src/main/resources/db/changelog/changes)
   directory.
2. Use descriptive and unique filenames (e.g. `09_new_feature_name.sql`).
3. Include the new file in the [
   `db.changelog-master.xml`](../server/src/main/resources/db/changelog/db.changelog-master.xml) to
   ensure it is executed.
4. Stick to the [Liquibase formatted SQL](https://docs.liquibase.com/concepts/basic/sql-format.html)
   format:
    ```sql
    --liquibase formatted sql
    --changeset yourname:unique-id
    ```

## 🏷️ Naming Conventions

- Prefix filenames with a two-digit incremental number to preserve execution order.
- Use lowercase and underscores (e.g. `10_add_user_flags.sql`).
- Use your name or GitHub handle in the changeset author (e.g.
  `--changeset marc:10-add-user-flags`).

## 🧪 Local Development Notes

- Liquibase will auto-run on application start.
- Always test schema changes with a local PostgreSQL instance before pushing.

## 🧑‍💻 Manual SQL Scripts

Some SQL scripts are **not part of the Liquibase process**. These are intended for manual execution
in specific environments (e.g. TUM-specific data).

📄 Example:

- [
  `insert_tum_aet_research_group.sql`](../server/src/main/resources/db/changelog/manual/insert_tum_aet_research_group.sql)

This file manually adds the "Applied Education Technologies (AET)" research group, only applicable
for TUM environments (DEV & PROD). **Do not include in Liquibase.**

## 🗺️ Database Schema

![Database Schema](files/database-schema.svg)

_Always regenerate this diagram after structural changes to ensure documentation accuracy._