--liquibase formatted sql

--changeset thesis:44_ai_review_summary

-- The AI review pipeline's latest read on a thesis's proposal or thesis document: a numeric
-- score, an overall assessment, and a short summary. At most one row per (thesis_id, type) —
-- every review run (student auto-review or supervisor preview) upserts this row, independent of
-- whether any resulting findings are actually saved.
CREATE TABLE ai_review_summary (
    ai_review_summary_id UUID PRIMARY KEY,
    thesis_id             UUID        NOT NULL REFERENCES theses (thesis_id) ON DELETE CASCADE,
    type                  VARCHAR(20) NOT NULL CHECK (type IN ('PROPOSAL', 'THESIS')),
    score                 INTEGER     CHECK (score IS NULL OR (score >= 0 AND score <= 100)),
    assessment            VARCHAR(20) CHECK (assessment IS NULL OR assessment IN ('GOOD', 'ACCEPTABLE', 'NEEDS_WORK')),
    summary               TEXT,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_ai_review_summary_thesis_type UNIQUE (thesis_id, type)
);
