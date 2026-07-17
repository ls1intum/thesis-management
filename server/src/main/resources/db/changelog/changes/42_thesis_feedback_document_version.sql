--liquibase formatted sql

--changeset thesis:42_thesis_feedback_document_version

-- Ties each feedback item to the specific proposal or thesis-file revision it was written
-- against, so the UI can label "v2" next to a comment even after the student uploads v3.
-- Nullable because presentation feedback isn't versioned and existing rows predate this column.
-- No FK constraint: the target table depends on the feedback `type` (PROPOSAL → thesis_proposals,
-- THESIS → thesis_files), and we don't want cross-table CHECK complexity.
ALTER TABLE thesis_feedback ADD COLUMN document_version_id UUID;
