--liquibase formatted sql
--changeset marc:08-research-groups-1
CREATE TABLE
    research_groups
(
    research_group_id UUID PRIMARY KEY,
    head_user_id      UUID      NOT NULL UNIQUE REFERENCES users (user_id),
    name              TEXT      NOT NULL UNIQUE,
    abbreviation      VARCHAR(50),
    description       TEXT,
    website_url       TEXT,
    campus            TEXT,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        UUID      NOT NULL REFERENCES users (user_id),
    archived          BOOLEAN   NOT NULL DEFAULT FALSE,
    archived_at       TIMESTAMP
);

--changeset marc:08-research-groups-2
ALTER TABLE topics
    ADD COLUMN research_group_id UUID REFERENCES research_groups (research_group_id);

--changeset marc:08-research-groups-3
ALTER TABLE users
    ADD COLUMN research_group_id UUID REFERENCES research_groups (research_group_id);