--liquibase formatted sql
--changeset claude:24-make-reject-duration-not-null
UPDATE research_group_settings SET reject_duration = 0 WHERE reject_duration IS NULL;
ALTER TABLE research_group_settings ALTER COLUMN reject_duration SET NOT NULL;
ALTER TABLE research_group_settings ALTER COLUMN reject_duration SET DEFAULT 8;
