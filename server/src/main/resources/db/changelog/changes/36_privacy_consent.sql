-- liquibase formatted sql

-- changeset thesis-management:36-add-privacy-consent-timestamp
-- comment: Adds a server-side timestamp to record when the student accepted the privacy statement
--          during application submission, as required by GDPR Art. 7(1) for demonstrating consent.
--          The localStorage-based consent checkbox in the client is purely a UX convenience
--          (pre-filling the checkbox so users don't have to re-tick it every time). The actual
--          consent proof is this server-side timestamp, recorded at the moment of application creation.
ALTER TABLE applications ADD COLUMN consent_timestamp TIMESTAMP WITH TIME ZONE;
