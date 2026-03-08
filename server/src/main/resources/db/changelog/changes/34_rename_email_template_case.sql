-- liquibase formatted sql

-- changeset thesis-management:34-rename-email-template-case
-- Rename APPLICATION_ACCEPTED_NO_ADVISOR → APPLICATION_ACCEPTED_NO_SUPERVISOR
-- to match the updated role terminology (old "Advisor" is now "Supervisor").

UPDATE email_templates
SET template_case = 'APPLICATION_ACCEPTED_NO_SUPERVISOR'
WHERE template_case = 'APPLICATION_ACCEPTED_NO_ADVISOR';
