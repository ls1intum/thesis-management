--liquibase formatted sql

--changeset thesis:41_thesis_feedback_ai_columns

-- Adds classification columns to thesis_feedback so AI-generated findings (and manually entered
-- items) can carry a severity, a category, and a source marker. Existing rows are treated as
-- human-authored feedback with no classification — the UI renders them as uncategorized.

ALTER TABLE thesis_feedback ADD COLUMN category VARCHAR(50);
ALTER TABLE thesis_feedback ADD CONSTRAINT chk_thesis_feedback_category
    CHECK (category IS NULL OR category IN (
        'FORMATTING', 'STRUCTURE', 'CITATION', 'METHODOLOGY',
        'WRITING', 'FIGURES', 'LOGIC', 'COMPLETENESS', 'OTHER'
    ));

ALTER TABLE thesis_feedback ADD COLUMN severity VARCHAR(20);
ALTER TABLE thesis_feedback ADD CONSTRAINT chk_thesis_feedback_severity
    CHECK (severity IS NULL OR severity IN (
        'CRITICAL', 'MAJOR', 'MINOR', 'SUGGESTION'
    ));

ALTER TABLE thesis_feedback ADD COLUMN generation_source VARCHAR(30) NOT NULL DEFAULT 'HUMAN';
ALTER TABLE thesis_feedback ADD CONSTRAINT chk_thesis_feedback_generation_source
    CHECK (generation_source IN ('AI', 'HUMAN', 'AI_REVIEWED_BY_HUMAN'));
