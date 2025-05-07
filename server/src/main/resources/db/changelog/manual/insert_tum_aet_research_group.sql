-- ============================================================================
-- Manual setup script to initialize the 'Applied Education Technologies (AET)'
-- research group for the Technical University of Munich (TUM).
--
-- This script:
--   - Inserts the AET research group into the database,
--   - Associates it with a specific head (Prof. Krusche),
--   - Assigns all existing topics, applications, and theses to the group.
--
-- IMPORTANT:
-- This script is NOT part of the standard Liquibase migration process,
-- as it only applies to TUM environments and must be executed manually
-- on the DEV and PROD instances.
--
-- Author: Marc Fett
-- Date: 2025/04/11
-- ============================================================================
DO
$$
    DECLARE
        ase_group_id UUID := gen_random_uuid();
        head_id      UUID;
    BEGIN
        SELECT user_id
        INTO head_id
        FROM users
        WHERE university_id = 'ne23kow' -- university_id and user_id (DEV and PROD) of Prof. Krusche
          AND user_id IN ('76638673-ca5e-4000-8f5c-3da377bf0eda', '69fa250e-f4eb-435f-b1fb-2cff8d051e32')
        LIMIT 1;

        IF head_id IS NOT NULL THEN
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
                    'Applied Education Technologies',
                    'AET',
                    'The research group develops innovative, AI-powered educational technologies and practical software solutions in collaboration with industry and academia. Using agile methods and project-based learning, the group prepares students for real-world challenges.',
                    'https://aet.cit.tum.de/',
                    'Munich - Garching',
                    NOW(),
                    head_id,
                    NOW(),
                    head_id,
                    FALSE);

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
            WHERE research_group_id IS NULL
              AND user_id IN (SELECT DISTINCT ug.user_id
                              FROM user_groups ug
                              WHERE ug.group IN ('advisor', 'supervisor'));
        END IF;
    END;
$$ LANGUAGE PLPGSQL;


ALTER TABLE applications
    ALTER COLUMN research_group_id
        SET NOT NULL;


ALTER TABLE theses
    ALTER COLUMN research_group_id
        SET NOT NULL;


ALTER TABLE topics
    ALTER COLUMN research_group_id
        SET NOT NULL;