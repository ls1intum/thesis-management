DO
$$
    DECLARE
        iecs_research_group_id UUID := gen_random_uuid();
        iecs_head_id UUID := gen_random_uuid();
BEGIN
-- Add Sample Research Group (without head_user_id initially)
INSERT INTO
    research_groups (
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
        archived
    )
VALUES
    (
        iecs_research_group_id,
        NULL,
        'Institute for Excellence in Computer Science',
        'IECS',
        'The Institute for Excellence in Computer Science (IECS) is dedicated to research and education in the most excellent areas of computer science.',
        'https://www.iecs.example.edu',
        'Munich - Garching',
        NOW (),
        NULL,
        NOW (),
        NULL,
        FALSE
    );

-- Add Sample Users
INSERT INTO
    users (
        user_id,
        university_id,
        matriculation_number,
        email,
        first_name,
        last_name,
        gender,
        nationality,
        cv_filename,
        degree_filename,
        examination_filename,
        study_degree,
        study_program,
        projects,
        interests,
        special_skills,
        enrolled_at,
        updated_at,
        joined_at,
        custom_data,
        avatar,
        research_group_id
    )
VALUES
    (
        gen_random_uuid (),
        'admin',
        10000000,
        'admin@test.local',
        'Admin',
        'User',
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NOW (),
        NOW (),
        NOW (),
        NULL,
        NULL,
        NULL
    ),
    (
        iecs_head_id,
        'supervisor',
        10000001,
        'supervisor@test.local',
        'Supervisor',
        'User',
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NOW (),
        NOW (),
        NOW (),
        NULL,
        NULL,
        iecs_research_group_id
    ),
    (
        gen_random_uuid (),
        'advisor',
        10000002,
        'advisor@test.local',
        'Advisor',
        'User',
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NOW (),
        NOW (),
        NOW (),
        NULL,
        NULL,
        iecs_research_group_id
    ),
    (
        gen_random_uuid (),
        'student',
        20000001,
        'student@test.local',
        'Student',
        'One',
        'MALE',
        'DE',
        NULL,
        NULL,
        NULL,
        'MASTER',
        'COMPUTER_SCIENCE',
        'AI-based tutor recommendation system',
        'Educational technology, backend development',
        'Java, Spring Boot, PostgreSQL',
        NOW (),
        NOW (),
        NOW (),
        NULL,
        NULL,
        NULL
    )
;

-- Update Research Group with head_user_id
UPDATE research_groups
SET head_user_id = iecs_head_id,
    created_by = iecs_head_id,
    updated_by = iecs_head_id
WHERE research_group_id = iecs_research_group_id;

END;

$$ LANGUAGE PLPGSQL;