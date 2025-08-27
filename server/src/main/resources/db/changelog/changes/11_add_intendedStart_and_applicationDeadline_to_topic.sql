--liquibase formatted sql
--changeset ramona:11-add-intendedStart-and-applicationDeadline-to-topic
ALTER TABLE topics
    ADD COLUMN intended_start TIMESTAMP NULL,
    ADD COLUMN application_deadline TIMESTAMP NULL;