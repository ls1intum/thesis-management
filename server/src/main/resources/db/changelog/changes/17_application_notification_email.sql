--liquibase formatted sql
--changeset stephan:17_application_notification_email

ALTER TABLE research_group_settings
ADD COLUMN application_notification_email VARCHAR(200);
