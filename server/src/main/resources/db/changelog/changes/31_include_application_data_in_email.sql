--liquibase formatted sql

--changeset thesis:31_include_application_data_in_email

ALTER TABLE research_group_settings
    ADD COLUMN include_application_data_in_email BOOLEAN NOT NULL DEFAULT FALSE;
