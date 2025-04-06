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
    created_by        UUID      NOT NULL REFERENCES users (user_id),
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by        UUID      NOT NULL REFERENCES users (user_id),
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

--changeset marc:08-research-groups-6
DO '
DECLARE
    ase_group_id UUID NOT NULL := gen_random_uuid();
    head_id UUID;
BEGIN
SELECT user_id
INTO head_id
FROM users
WHERE email ILIKE ''%krusche%''
    OR last_name ILIKE ''Krusche''
LIMIT 1;

INSERT INTO research_groups (research_group_id,
    head_user_id,
    name,
    abbreviation,
    description,
    website_url,
    campus,
    created_at,
    created_by,
    updated_at,
    updated_by,
    archived)
VALUES (ase_group_id,
    head_id,
    ''Applied Software Engineering'',
    ''ASE'',
    ''The Chair of Applied Software Engineering (ASE) at TUM investigates how software engineering principles can be taught and applied in educational and practical contexts. Our work focuses on modern teaching methods, large-scale practical courses, and empirical software engineering.'',
    ''https://ase.cit.tum.de'',
    ''Munich - Garching'',
    NOW(),
    head_id,
    NOW(),
    head_id,
    FALSE);

-- assign this research group to all existing topics
UPDATE topics SET research_group_id = ase_group_id WHERE research_group_id IS NULL;

-- assign research group to applications based on topic linkage
UPDATE applications a
SET research_group_id = t.research_group_id
FROM topics t
WHERE a.topic_id = t.topic_id
    AND a.research_group_id IS NULL;

-- assign research group to theses based on application linkage
UPDATE theses th
SET research_group_id = a.research_group_id
FROM applications a
WHERE th.application_id = a.application_id
    AND th.research_group_id IS NULL;

-- assign users to research group
UPDATE users SET research_group_id = ase_group_id WHERE research_group_id IS NULL;
END;'

--changeset marc:08-research-groups-7
ALTER TABLE applications
    ALTER COLUMN research_group_id SET NOT NULL;

ALTER TABLE theses
    ALTER COLUMN research_group_id SET NOT NULL;

ALTER TABLE topics
    ALTER COLUMN research_group_id SET NOT NULL;