-- liquibase formatted sql

-- changeset thesis-management:46-schools-departments-study-programs
-- comment: Adds the organisational structure (schools/faculties, departments, study programs) that
--          research groups, students and theses refer to. The old free-text users.study_program column
--          is kept for one release so the change can be rolled back without losing data.
CREATE TABLE schools
(
    school_id         UUID                     NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name              VARCHAR(255)             NOT NULL,
    abbreviation      VARCHAR(50)              NOT NULL,
    website_url       VARCHAR(500),
    thesis_portal_url VARCHAR(500),
    active            BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_schools_name ON schools (lower(name));
CREATE UNIQUE INDEX ux_schools_abbreviation ON schools (lower(abbreviation));

CREATE TABLE departments
(
    department_id UUID                     NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    school_id     UUID                     NOT NULL REFERENCES schools (school_id),
    name          VARCHAR(255)             NOT NULL,
    abbreviation  VARCHAR(50),
    active        BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_departments_school_name ON departments (school_id, lower(name));

CREATE TABLE study_programs
(
    study_program_id UUID                     NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    key              VARCHAR(255)             NOT NULL,
    name             VARCHAR(255)             NOT NULL,
    school_id        UUID REFERENCES schools (school_id),
    active           BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_study_programs_key ON study_programs (lower(key));
CREATE INDEX ix_study_programs_school ON study_programs (school_id);

ALTER TABLE research_groups
    ADD COLUMN school_id UUID REFERENCES schools (school_id),
    ADD COLUMN department_id UUID REFERENCES departments (department_id);

ALTER TABLE users
    ADD COLUMN study_program_id UUID REFERENCES study_programs (study_program_id);

ALTER TABLE theses
    ADD COLUMN study_program_id UUID REFERENCES study_programs (study_program_id);

CREATE INDEX ix_research_groups_school ON research_groups (school_id);
CREATE INDEX ix_users_study_program ON users (study_program_id);
CREATE INDEX ix_theses_study_program ON theses (study_program_id);

-- Turn the study program keys already used by students into rows. Deployments customised the list through
-- an environment variable, so unknown keys get a generated name and no school; admins can complete them.
INSERT INTO study_programs (key, name)
SELECT DISTINCT ON (lower(btrim(study_program)))
       left(btrim(study_program), 255),
       CASE upper(btrim(study_program))
           WHEN 'COMPUTER_SCIENCE' THEN 'Computer Science'
           WHEN 'INFORMATION_SYSTEMS' THEN 'Information Systems'
           WHEN 'GAMES_ENGINEERING' THEN 'Games Engineering'
           WHEN 'MANAGEMENT_AND_TECHNOLOGY' THEN 'Management and Technology'
           WHEN 'OTHER' THEN 'Other'
           ELSE left(initcap(replace(lower(btrim(study_program)), '_', ' ')), 255)
       END
FROM users
WHERE study_program IS NOT NULL
  AND btrim(study_program) <> ''
-- COLLATE "C" makes the surviving spelling of case variants independent of the database collation (uppercase wins)
ORDER BY lower(btrim(study_program)), btrim(study_program) COLLATE "C";

UPDATE users u
SET study_program_id = sp.study_program_id
FROM study_programs sp
WHERE u.study_program IS NOT NULL
  AND lower(btrim(u.study_program)) = lower(sp.key);

-- Existing theses take the study program of their first student.
UPDATE theses t
SET study_program_id = (SELECT u.study_program_id
                        FROM thesis_roles r
                                 JOIN users u ON u.user_id = r.user_id
                        WHERE r.thesis_id = t.thesis_id
                          AND r.role = 'STUDENT'
                          AND u.study_program_id IS NOT NULL
                        ORDER BY r.position
                        LIMIT 1);
