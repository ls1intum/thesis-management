-- liquibase formatted sql

-- changeset frank:theses_keywords_not_null-1
UPDATE theses
    SET keywords = ARRAY[]::text[]
    WHERE keywords IS NULL;

-- changeset frank:theses_keywords_not_null-2
ALTER TABLE theses
    ALTER COLUMN keywords SET NOT NULL;
