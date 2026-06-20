--liquibase formatted sql

--changeset thesis:40_abstract_extraction

-- Tracks where a thesis abstract came from so auto-fill never overwrites a human edit.
-- Existing rows default to MANUAL: a non-empty abstract is treated as human-entered
-- (only suggested on later uploads); an empty abstract is still auto-filled because the
-- fill rule keys off "blank" before checking the source.
ALTER TABLE theses ADD COLUMN abstract_source VARCHAR(20) NOT NULL DEFAULT 'MANUAL';

-- Holds a best-effort abstract extracted from an uploaded PDF that we were not confident
-- enough to store directly (or that would overwrite a human edit). NULL = no pending suggestion.
ALTER TABLE theses ADD COLUMN abstract_suggestion TEXT;
