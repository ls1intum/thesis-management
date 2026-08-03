--liquibase formatted sql

--changeset thesis:41_research_group_guidelines

-- Custom AI review guidelines per research group. The lead uploads free-text guidelines
-- (raw_guidelines); the system preprocesses them into a structured, per-category representation
-- (structured_guidelines) that the AI review pipeline consumes. AI review features are gated on a
-- READY record existing for the group, so members of a group without guidelines cannot run reviews.
CREATE TABLE research_group_guidelines (
    research_group_id     UUID PRIMARY KEY REFERENCES research_groups (research_group_id) ON DELETE CASCADE,
    raw_guidelines        TEXT        NOT NULL,
    structured_guidelines JSONB,
    status                VARCHAR(20) NOT NULL,
    failure_reason        TEXT,
    processed_at          TIMESTAMP WITH TIME ZONE,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_by            UUID REFERENCES users (user_id) ON DELETE SET NULL,
    CONSTRAINT chk_research_group_guidelines_status CHECK (status IN ('READY', 'FAILED'))
);
