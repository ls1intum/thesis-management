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

## 🔄 Major Version Upgrade (pg_dump/pg_restore)

PostgreSQL major versions (e.g. 17 → 18) use different internal data formats, so the new server
**cannot read data files** written by the old one. You must export the data with `pg_dump` and
re-import it with `pg_restore`.

### PGDATA Path Change in PG 18+

Starting with PostgreSQL 18, the official Docker image changed the default `PGDATA` from
`/var/lib/postgresql/data` to `/var/lib/postgresql/18/docker`
([docker-library/postgres#1259](https://github.com/docker-library/postgres/pull/1259)).
Our production compose file explicitly sets `PGDATA=/var/lib/postgresql/data` to keep existing
volume mounts working.

### Upgrade Procedure

> **Always test the upgrade on the dev environment first before applying to production.**

All commands must be run as the `thesistrack` user in `/home/thesistrack/` on the VM.
SSH in, then switch to the correct user:

```bash
sudo su
su thesistrack
cd ~
```

Then run the upgrade

```bash

# 1. Stop the application server (keep DB running)
docker compose -f docker-compose.prod.yml --env-file=.env.prod stop server client

# 2. Create a full database dump
docker exec thesis-management-db pg_dump -Fc -U "thesistrack" "thesistrack" > thesis_dump.dump

# 3. Stop and remove the old DB container
docker compose -f docker-compose.prod.yml --env-file=.env.prod down db

# 4. Back up the old data directory (do NOT delete it yet)
mv ./postgres_data ./postgres_data_backup

# 5. Update the postgres image tag in docker-compose.prod.yml
#    (or deploy the new version that already contains the change)
sed -i 's|postgres:17.*-alpine|postgres:18.2-alpine|' docker-compose.prod.yml

# 6. Start only the database service
docker compose -f docker-compose.prod.yml --env-file=.env.prod up -d db

# 7. Wait for the database to be ready
until docker exec thesis-management-db pg_isready -U "thesistrack"; do sleep 1; done

# 8. Copy the dump into the new container and restore
docker cp thesis_dump.dump thesis-management-db:/tmp/thesis_dump.dump
docker exec thesis-management-db pg_restore -U "thesistrack" -d "thesistrack" --no-owner --no-acl --exit-on-error /tmp/thesis_dump.dump

# 9. Verify data integrity
docker exec thesis-management-db psql -U "thesistrack" -d "thesistrack" -c "\dt+"

# 10. Start the full application stack
docker compose -f docker-compose.prod.yml --env-file=.env.prod up -d

# 11. Verify Hibernate validation and Liquibase pass
docker logs -f thesis-management-server  # check for errors

# 12. After 1-2 weeks of stable operation, remove the old data directory and dump
rm -rf ./postgres_data_backup thesis_dump.dump
```

### Rollback

If the upgrade fails, restore the previous version:

```bash
docker compose -f docker-compose.prod.yml --env-file=.env.prod down db
rm -rf ./postgres_data
mv ./postgres_data_backup ./postgres_data
# Revert docker-compose.prod.yml changes (PG image tag and PGDATA override)
docker compose -f docker-compose.prod.yml --env-file=.env.prod up -d
```

### References

- [PostgreSQL 18 Upgrading Guide](https://www.postgresql.org/docs/18/upgrading.html)
- [pg_dump Documentation](https://www.postgresql.org/docs/18/app-pgdump.html)
- [pg_restore Documentation](https://www.postgresql.org/docs/18/app-pg-restore.html)
- [PGDATA path change (docker-library/postgres#1259)](https://github.com/docker-library/postgres/pull/1259)

## 🗺️ Database Schema

![Database Schema](files/database-schema.svg)

_Always regenerate this diagram after structural changes to ensure documentation accuracy._