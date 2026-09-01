--liquibase formatted sql

--changeset thesis:45_ai_review_summary_document_version

-- Ties each AI review summary to the proposal or thesis-file revision it was produced from, so
-- the UI can hide an obsolete score once a newer document is uploaded instead of presenting it
-- as the current one. Nullable because rows written before this column exist and we cannot tell
-- retroactively which revision they described — those are treated as stale by the client.
-- No FK constraint: the target table depends on `type` (PROPOSAL → thesis_proposals,
-- THESIS → thesis_files), mirroring thesis_feedback.document_version_id.
ALTER TABLE ai_review_summary ADD COLUMN document_version_id UUID;
