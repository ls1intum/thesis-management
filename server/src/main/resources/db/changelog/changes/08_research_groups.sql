--liquibase formatted sql
--changeset marc:08-research-groups-1
CREATE TABLE
    research_groups
(
    research_group_id UUID PRIMARY KEY,
    head_user_id      UUID      REFERENCES users (user_id) ON DELETE SET NULL,
    name              TEXT      NOT NULL UNIQUE,
    abbreviation      VARCHAR(50),
    description       TEXT,
    website_url       TEXT,
    campus            TEXT,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        UUID      REFERENCES users (user_id) ON DELETE SET NULL,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by        UUID      REFERENCES users (user_id) ON DELETE SET NULL,
    archived          BOOLEAN   NOT NULL DEFAULT FALSE
);

--changeset marc:08-research-groups-2
ALTER TABLE topics
    ADD COLUMN research_group_id UUID REFERENCES research_groups (research_group_id);

--changeset marc:08-research-groups-3
ALTER TABLE users
    ADD COLUMN research_group_id UUID REFERENCES research_groups (research_group_id);

--changeset marc:08-research-groups-4
ALTER TABLE applications
    ADD COLUMN research_group_id UUID REFERENCES research_groups (research_group_id);

--changeset marc:08-research-groups-5
ALTER TABLE theses
    ADD COLUMN research_group_id UUID REFERENCES research_groups (research_group_id);