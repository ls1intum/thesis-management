--liquibase formatted sql

--changeset krusche:26-add-scientific-writing-guide-link-to-settings
ALTER TABLE research_group_settings ADD COLUMN IF NOT EXISTS scientific_writing_guide_link VARCHAR(500);
