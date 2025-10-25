--liquibase formatted sql
--changeset ramona:13_check_max_limit_research_group_description
UPDATE research_groups
SET description = SUBSTRING(description FROM 1 FOR 500)
WHERE LENGTH(description) > 500;

ALTER TABLE research_groups
ALTER COLUMN description TYPE VARCHAR(500);
