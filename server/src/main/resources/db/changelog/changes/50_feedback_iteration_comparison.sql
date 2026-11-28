--liquibase formatted sql

--changeset thesis:50_feedback_iteration_comparison

-- Comparison across iterations: a feedback entry can follow up an earlier, completed entry whose
-- defect recurred in a newer document version. The link lets the reviewer count how often a defect
-- has recurred (to defer it to the supervisor instead of restating it again) and lets the UI point
-- from a follow-up to the entry it follows up. ON DELETE SET NULL: deleting the original must not
-- take the follow-up with it, which still describes a real defect of the newer version.
ALTER TABLE thesis_feedback
    ADD COLUMN follows_up_id UUID REFERENCES thesis_feedback (feedback_id) ON DELETE SET NULL;

-- Marks a follow-up that hands a repeatedly recurring defect over to the supervisor rather than
-- explaining it once more; the UI labels it "discuss with your supervisor".
ALTER TABLE thesis_feedback
    ADD COLUMN deferred_to_supervisor BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_thesis_feedback_follows_up_id ON thesis_feedback (follows_up_id);

-- The completed entries the latest auto review found resolved, so the UI can acknowledge them next
-- to the score. JSON array of thesis_feedback ids; NULL for rows written before this column.
ALTER TABLE ai_review_summary
    ADD COLUMN resolved_feedback_ids JSONB;
