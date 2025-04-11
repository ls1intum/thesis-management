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
    ase_group_id UUID := gen_random_uuid();
    head_id UUID;
BEGIN
    SELECT user_id INTO head_id
    FROM users
    WHERE university_id = ''ne23kow''
      AND user_id IN (''76638673-ca5e-4000-8f5c-3da377bf0eda'', ''69fa250e-f4eb-435f-b1fb-2cff8d051e32'')
    LIMIT 1;

    IF head_id IS NOT NULL THEN
        INSERT INTO research_groups (
            research_group_id,
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
        VALUES (
                   ase_group_id,
                   head_id,
                   ''Applied Education Technologies'',
                   ''AET'',
                   ''The research group develops innovative, AI-powered educational technologies and practical software solutions in collaboration with industry and academia. Using agile methods and project-based learning, the group prepares students for real-world challenges.'',
                   ''https://aet.cit.tum.de/'',
                   ''Munich - Garching'',
                   NOW(),
                   head_id,
                   NOW(),
                   head_id,
                   FALSE
               );

        UPDATE topics
        SET research_group_id = ase_group_id
        WHERE research_group_id IS NULL;

        UPDATE applications
        SET research_group_id = ase_group_id
        WHERE research_group_id IS NULL;

        UPDATE theses
        SET research_group_id = ase_group_id
        WHERE research_group_id IS NULL;

        UPDATE users
        SET research_group_id = ase_group_id
        WHERE research_group_id IS NULL;
    END IF;
END;'

--changeset marc:08-research-groups-7
ALTER TABLE applications
    ALTER COLUMN research_group_id SET NOT NULL;

ALTER TABLE theses
    ALTER COLUMN research_group_id SET NOT NULL;

ALTER TABLE topics
    ALTER COLUMN research_group_id SET NOT NULL;