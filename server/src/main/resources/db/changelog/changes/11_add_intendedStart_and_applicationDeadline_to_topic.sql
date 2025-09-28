--liquibase formatted sql
--changeset ramona:11-add-intendedStart-and-applicationDeadline-to-topic
ALTER TABLE topics
    ADD COLUMN intended_start TIMESTAMP NULL,
    ADD COLUMN application_deadline TIMESTAMP NULL;

CREATE TABLE research_group_settings (
    research_group_id UUID PRIMARY KEY,
    automatic_reject_enabled BOOLEAN NOT NULL,
    reject_duration INT NULL
);