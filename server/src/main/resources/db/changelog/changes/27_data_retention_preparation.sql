--liquibase formatted sql

--changeset data-retention:27-add-cascade-to-application-reviewers
ALTER TABLE application_reviewers DROP CONSTRAINT IF EXISTS application_reviewers_application_id_fkey;
ALTER TABLE application_reviewers
    ADD CONSTRAINT application_reviewers_application_id_fkey
    FOREIGN KEY (application_id) REFERENCES applications (application_id) ON DELETE CASCADE;

--changeset data-retention:27-add-last-login-at-to-users
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP;
UPDATE users SET last_login_at = updated_at;
