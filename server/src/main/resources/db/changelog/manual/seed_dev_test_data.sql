-- ============================================================================
-- Dev seed data: realistic, interconnected test data matching Keycloak realm.
--
-- This script is idempotent (uses ON CONFLICT DO NOTHING / DO UPDATE).
-- It runs only when spring.profiles.active=dev via Liquibase context="dev".
--
-- Users are referenced by university_id (= Keycloak username) so the seed
-- works whether or not the user has already logged in via Keycloak.
-- ============================================================================

-- ============================================================================
-- 1. USERS (11 total: 4 existing + 7 new from Keycloak realm)
-- ============================================================================
INSERT INTO users (user_id, university_id, matriculation_number, email, first_name, last_name,
                   gender, nationality, study_degree, study_program, projects, interests,
                   special_skills, enrolled_at, updated_at, joined_at)
VALUES
    -- Existing Keycloak users
    (gen_random_uuid(), 'admin', NULL, 'admin@test.local', 'Admin', 'User',
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NOW(), NOW()),
    (gen_random_uuid(), 'supervisor', '03700001', 'supervisor@test.local', 'Supervisor', 'User',
     'MALE', 'DE', NULL, NULL, NULL, NULL, NULL, NULL, NOW(), NOW()),
    (gen_random_uuid(), 'advisor', '03700002', 'advisor@test.local', 'Advisor', 'User',
     'FEMALE', 'DE', NULL, NULL, NULL, NULL, NULL, NULL, NOW(), NOW()),
    (gen_random_uuid(), 'student', '03700003', 'student@test.local', 'Student', 'User',
     'MALE', 'DE', 'MASTER', 'COMPUTER_SCIENCE',
     'AI-based tutor recommendation system',
     'Machine learning, server development',
     'Java, Spring Boot, PostgreSQL',
     NOW(), NOW(), NOW()),
    -- New Keycloak users
    (gen_random_uuid(), 'supervisor2', '03700004', 'supervisor2@test.local', 'Supervisor2', 'User',
     'FEMALE', 'US', NULL, NULL, NULL, NULL, NULL, NULL, NOW(), NOW()),
    (gen_random_uuid(), 'advisor2', '03700005', 'advisor2@test.local', 'Advisor2', 'User',
     'MALE', 'UK', NULL, NULL, NULL, NULL, NULL, NULL, NOW(), NOW()),
    (gen_random_uuid(), 'student2', '03700006', 'student2@test.local', 'Student2', 'User',
     'FEMALE', 'US', 'BACHELOR', 'INFORMATION_SYSTEMS',
     NULL,
     'Process optimization, business analytics',
     'Python, R, Tableau',
     NOW(), NOW(), NOW()),
    (gen_random_uuid(), 'student3', '03700007', 'student3@test.local', 'Student3', 'User',
     'OTHER', 'CA', 'MASTER', 'GAMES_ENGINEERING',
     'Streaming pipeline for sensor data',
     'Real-time systems, cloud platforms',
     'Python, Kafka, AWS',
     NOW(), NOW(), NOW()),
    (gen_random_uuid(), 'student4', '03700008', 'student4@test.local', 'Student4', 'User',
     'FEMALE', 'FR', 'MASTER', 'OTHER',
     'Usability study for academic tools',
     'Interface design, eye-tracking',
     'Figma, UX research',
     NOW(), NOW(), NOW()),
    (gen_random_uuid(), 'student5', '03700009', 'student5@test.local', 'Student5', 'User',
     'MALE', 'DE', 'BACHELOR', 'MANAGEMENT_AND_TECHNOLOGY',
     NULL,
     'Web design, media production',
     'Adobe Suite, HTML/CSS',
     NOW(), NOW(), NOW()),
    (gen_random_uuid(), 'group-admin', '03700010', 'group-admin@test.local', 'GroupAdmin', 'User',
     NULL, 'DE', NULL, NULL, NULL, NULL, NULL, NULL, NOW(), NOW())
ON CONFLICT (university_id) DO UPDATE SET
    matriculation_number = COALESCE(users.matriculation_number, EXCLUDED.matriculation_number),
    email                = COALESCE(users.email, EXCLUDED.email),
    first_name           = COALESCE(users.first_name, EXCLUDED.first_name),
    last_name            = COALESCE(users.last_name, EXCLUDED.last_name),
    gender               = COALESCE(users.gender, EXCLUDED.gender),
    nationality          = COALESCE(users.nationality, EXCLUDED.nationality),
    study_degree         = COALESCE(users.study_degree, EXCLUDED.study_degree),
    study_program        = COALESCE(users.study_program, EXCLUDED.study_program),
    projects             = COALESCE(users.projects, EXCLUDED.projects),
    interests            = COALESCE(users.interests, EXCLUDED.interests),
    special_skills       = COALESCE(users.special_skills, EXCLUDED.special_skills),
    enrolled_at          = COALESCE(users.enrolled_at, EXCLUDED.enrolled_at);

-- ============================================================================
-- 2. USER GROUPS (role assignments)
-- ============================================================================
INSERT INTO user_groups (user_id, "group")
VALUES
    ((SELECT user_id FROM users WHERE university_id = 'admin'), 'admin'),
    ((SELECT user_id FROM users WHERE university_id = 'supervisor'), 'supervisor'),
    ((SELECT user_id FROM users WHERE university_id = 'advisor'), 'advisor'),
    ((SELECT user_id FROM users WHERE university_id = 'student'), 'student'),
    ((SELECT user_id FROM users WHERE university_id = 'supervisor2'), 'supervisor'),
    ((SELECT user_id FROM users WHERE university_id = 'advisor2'), 'advisor'),
    ((SELECT user_id FROM users WHERE university_id = 'student2'), 'student'),
    ((SELECT user_id FROM users WHERE university_id = 'student3'), 'student'),
    ((SELECT user_id FROM users WHERE university_id = 'student4'), 'student'),
    ((SELECT user_id FROM users WHERE university_id = 'student5'), 'student'),
    ((SELECT user_id FROM users WHERE university_id = 'group-admin'), 'group-admin')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 3. RESEARCH GROUPS
-- ============================================================================
INSERT INTO research_groups (research_group_id, head_user_id, name, abbreviation, description,
                             website_url, campus, created_at, created_by, updated_at, updated_by, archived)
VALUES
    ('00000000-0000-4000-a000-000000000001'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'supervisor'),
     'Applied Software Engineering', 'ASE',
     'The ASE group focuses on modern software engineering practices, agile development, and continuous integration. Our research bridges academia and industry.',
     'https://ase.cit.tum.de/', 'Munich - Main Campus',
     NOW(), (SELECT user_id FROM users WHERE university_id = 'supervisor'),
     NOW(), (SELECT user_id FROM users WHERE university_id = 'supervisor'), FALSE),
    ('00000000-0000-4000-a000-000000000002'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'supervisor2'),
     'Data Science and Analytics', 'DSA',
     'DSA researches scalable data processing, machine learning pipelines, and applied statistics for real-world decision support systems.',
     'https://dsa.cit.tum.de/', 'Munich - Garching',
     NOW(), (SELECT user_id FROM users WHERE university_id = 'supervisor2'),
     NOW(), (SELECT user_id FROM users WHERE university_id = 'supervisor2'), FALSE)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 4. RESEARCH GROUP SETTINGS
-- ============================================================================
INSERT INTO research_group_settings (research_group_id, automatic_reject_enabled, reject_duration,
                                     presentation_slot_duration, proposal_phase_active)
VALUES
    ('00000000-0000-4000-a000-000000000001'::UUID, TRUE, 8, 30, TRUE),
    ('00000000-0000-4000-a000-000000000002'::UUID, FALSE, 4, 45, TRUE)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 5. ASSIGN USERS TO RESEARCH GROUPS
-- ============================================================================
UPDATE users SET research_group_id = '00000000-0000-4000-a000-000000000001'::UUID
WHERE university_id IN ('supervisor', 'advisor', 'advisor2', 'group-admin') AND research_group_id IS NULL;

UPDATE users SET research_group_id = '00000000-0000-4000-a000-000000000002'::UUID
WHERE university_id = 'supervisor2' AND research_group_id IS NULL;

-- ============================================================================
-- 6. TOPICS (6 topics across both groups)
-- ============================================================================
INSERT INTO topics (topic_id, title, thesis_types, problem_statement, requirements, goals,
                    "references", published_at, closed_at, updated_at, created_at, created_by,
                    research_group_id)
VALUES
    -- OPEN topic 1 (ASE)
    ('00000000-0000-4000-b000-000000000001'::UUID,
     'Automated Code Review Using Large Language Models',
     ARRAY['MASTER'],
     'Manual code reviews are time-consuming and inconsistent. Current static analysis tools miss semantic issues.',
     'Strong programming skills in Java or Python. Familiarity with LLMs and prompt engineering.',
     'Develop and evaluate an LLM-based code review assistant that integrates with GitHub pull requests.',
     'Zhang et al., "LLM-Assisted Code Review: A Survey", ICSE 2024.',
     NOW() - INTERVAL '10 days', NULL,
     NOW(), NOW() - INTERVAL '10 days',
     (SELECT user_id FROM users WHERE university_id = 'supervisor'),
     '00000000-0000-4000-a000-000000000001'::UUID),

    -- OPEN topic 2 (ASE)
    ('00000000-0000-4000-b000-000000000002'::UUID,
     'Continuous Integration Pipeline Optimization',
     ARRAY['BACHELOR', 'MASTER'],
     'CI pipelines in large projects take too long, reducing developer productivity and feedback speed.',
     'Experience with CI/CD tools (Jenkins, GitHub Actions). Basic knowledge of build systems.',
     'Analyze and optimize CI pipeline execution times through intelligent test selection and caching.',
     'Memon et al., "Regression Test Selection for CI", FSE 2023.',
     NOW() - INTERVAL '5 days', NULL,
     NOW(), NOW() - INTERVAL '5 days',
     (SELECT user_id FROM users WHERE university_id = 'advisor'),
     '00000000-0000-4000-a000-000000000001'::UUID),

    -- OPEN topic 3 (DSA)
    ('00000000-0000-4000-b000-000000000003'::UUID,
     'Real-Time Anomaly Detection in Streaming Data',
     ARRAY['MASTER'],
     'Detecting anomalies in high-volume data streams requires low-latency algorithms that adapt to concept drift.',
     'Strong background in statistics and machine learning. Experience with Apache Kafka or Flink.',
     'Design and benchmark an online anomaly detection framework for IoT sensor data streams.',
     'Aggarwal, "Outlier Analysis", Springer 2022. Gama et al., "Concept Drift Adaptation", ACM CSUR 2014.',
     NOW() - INTERVAL '3 days', NULL,
     NOW(), NOW() - INTERVAL '3 days',
     (SELECT user_id FROM users WHERE university_id = 'supervisor2'),
     '00000000-0000-4000-a000-000000000002'::UUID),

    -- DRAFT topic 1 (ASE)
    ('00000000-0000-4000-b000-000000000004'::UUID,
     'Gamification Strategies for Programming Education',
     ARRAY['BACHELOR'],
     'Student engagement in programming courses drops after the initial weeks. Gamification may help sustain motivation.',
     'Interest in educational technology. Basic web development skills.',
     'Design and evaluate gamification elements for an online programming exercise platform.',
     'Deterding et al., "Gamification: Designing for Motivation", Interactions 2012.',
     NULL, NULL,
     NOW(), NOW() - INTERVAL '1 day',
     (SELECT user_id FROM users WHERE university_id = 'advisor'),
     '00000000-0000-4000-a000-000000000001'::UUID),

    -- DRAFT topic 2 (DSA)
    ('00000000-0000-4000-b000-000000000005'::UUID,
     'Federated Learning for Privacy-Preserving Healthcare Analytics',
     ARRAY['MASTER'],
     'Healthcare data is distributed across institutions and cannot be centralized due to privacy regulations.',
     'Strong ML background. Familiarity with PyTorch or TensorFlow. Understanding of differential privacy.',
     'Implement a federated learning framework for training models on distributed hospital datasets.',
     'McMahan et al., "Communication-Efficient Learning", AISTATS 2017.',
     NULL, NULL,
     NOW(), NOW() - INTERVAL '2 days',
     (SELECT user_id FROM users WHERE university_id = 'supervisor2'),
     '00000000-0000-4000-a000-000000000002'::UUID),

    -- CLOSED topic (ASE)
    ('00000000-0000-4000-b000-000000000006'::UUID,
     'Migration Strategies from Monolith to Microservices',
     ARRAY['BACHELOR', 'MASTER'],
     'Many organizations struggle with decomposing monolithic applications into microservices.',
     'Experience with Spring Boot, Docker, and Kubernetes. Understanding of distributed systems patterns.',
     'Develop a systematic migration approach with tooling support for dependency analysis.',
     'Newman, "Monolith to Microservices", O Reilly 2019.',
     NOW() - INTERVAL '60 days', NOW() - INTERVAL '10 days',
     NOW() - INTERVAL '10 days', NOW() - INTERVAL '60 days',
     (SELECT user_id FROM users WHERE university_id = 'supervisor'),
     '00000000-0000-4000-a000-000000000001'::UUID)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 7. TOPIC ROLES (advisor assignments per topic)
-- ============================================================================
INSERT INTO topic_roles (topic_id, user_id, role, position, assigned_at, assigned_by)
VALUES
    -- Topic 1: advisor
    ('00000000-0000-4000-b000-000000000001'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'advisor'), 'ADVISOR', 0,
     NOW(), (SELECT user_id FROM users WHERE university_id = 'supervisor')),
    -- Topic 2: advisor (created it)
    ('00000000-0000-4000-b000-000000000002'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'advisor'), 'ADVISOR', 0,
     NOW(), (SELECT user_id FROM users WHERE university_id = 'advisor')),
    -- Topic 3: advisor2
    ('00000000-0000-4000-b000-000000000003'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'advisor2'), 'ADVISOR', 0,
     NOW(), (SELECT user_id FROM users WHERE university_id = 'supervisor2')),
    -- Topic 4: advisor
    ('00000000-0000-4000-b000-000000000004'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'advisor'), 'ADVISOR', 0,
     NOW(), (SELECT user_id FROM users WHERE university_id = 'advisor')),
    -- Topic 5: advisor2
    ('00000000-0000-4000-b000-000000000005'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'advisor2'), 'ADVISOR', 0,
     NOW(), (SELECT user_id FROM users WHERE university_id = 'supervisor2')),
    -- Topic 6 (closed): advisor
    ('00000000-0000-4000-b000-000000000006'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'advisor'), 'ADVISOR', 0,
     NOW(), (SELECT user_id FROM users WHERE university_id = 'supervisor'))
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 8. APPLICATIONS (8 total across various states)
-- ============================================================================
INSERT INTO applications (application_id, user_id, topic_id, thesis_title, thesis_type, motivation,
                          state, reject_reason, desired_start_date, comment, created_at, reviewed_at,
                          research_group_id)
VALUES
    -- ACCEPTED #1: student on topic 1 (linked to thesis 1)
    ('00000000-0000-4000-c000-000000000001'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'student'),
     '00000000-0000-4000-b000-000000000001'::UUID,
     NULL, 'MASTER',
     'I am passionate about LLMs and have experience building GitHub integrations. This topic aligns perfectly with my skills.',
     'ACCEPTED', NULL,
     NOW() - INTERVAL '30 days', '',
     NOW() - INTERVAL '35 days', NOW() - INTERVAL '30 days',
     '00000000-0000-4000-a000-000000000001'::UUID),

    -- ACCEPTED #2: student2 on topic 2 (linked to thesis 2)
    ('00000000-0000-4000-c000-000000000002'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'student2'),
     '00000000-0000-4000-b000-000000000002'::UUID,
     NULL, 'BACHELOR',
     'I have worked extensively with GitHub Actions and want to contribute to improving CI pipelines.',
     'ACCEPTED', NULL,
     NOW() - INTERVAL '20 days', '',
     NOW() - INTERVAL '25 days', NOW() - INTERVAL '20 days',
     '00000000-0000-4000-a000-000000000001'::UUID),

    -- ACCEPTED #3: student3 on topic 3 (linked to thesis 3)
    ('00000000-0000-4000-c000-000000000003'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'student3'),
     '00000000-0000-4000-b000-000000000003'::UUID,
     NULL, 'MASTER',
     'My background in streaming data processing with Kafka makes me a great fit for this anomaly detection topic.',
     'ACCEPTED', NULL,
     NOW() - INTERVAL '15 days', '',
     NOW() - INTERVAL '20 days', NOW() - INTERVAL '15 days',
     '00000000-0000-4000-a000-000000000002'::UUID),

    -- NOT_ASSESSED #1: student4 on topic 1
    ('00000000-0000-4000-c000-000000000004'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'student4'),
     '00000000-0000-4000-b000-000000000001'::UUID,
     NULL, 'MASTER',
     'I would like to explore how LLMs can improve software quality. My UX research background could add a unique perspective.',
     'NOT_ASSESSED', NULL,
     NOW() + INTERVAL '30 days', '',
     NOW() - INTERVAL '2 days', NULL,
     '00000000-0000-4000-a000-000000000001'::UUID),

    -- NOT_ASSESSED #2: student5 on topic 2
    ('00000000-0000-4000-c000-000000000005'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'student5'),
     '00000000-0000-4000-b000-000000000002'::UUID,
     NULL, 'BACHELOR',
     'I am interested in DevOps and want to learn more about CI/CD optimization techniques.',
     'NOT_ASSESSED', NULL,
     NOW() + INTERVAL '45 days', '',
     NOW() - INTERVAL '1 day', NULL,
     '00000000-0000-4000-a000-000000000001'::UUID),

    -- REJECTED: student5 on topic 3
    ('00000000-0000-4000-c000-000000000006'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'student5'),
     '00000000-0000-4000-b000-000000000003'::UUID,
     NULL, 'MASTER',
     'I would like to work on anomaly detection even though my background is more in web development.',
     'REJECTED', 'FAILED_TOPIC_REQUIREMENTS',
     NOW() + INTERVAL '30 days', '',
     NOW() - INTERVAL '10 days', NOW() - INTERVAL '5 days',
     '00000000-0000-4000-a000-000000000002'::UUID),

    -- INTERVIEWING: student4 on topic 3
    ('00000000-0000-4000-c000-000000000007'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'student4'),
     '00000000-0000-4000-b000-000000000003'::UUID,
     NULL, 'MASTER',
     'My experience with eye-tracking data analysis translates well to time-series anomaly detection.',
     'INTERVIEWING', NULL,
     NOW() + INTERVAL '30 days', '',
     NOW() - INTERVAL '7 days', NULL,
     '00000000-0000-4000-a000-000000000002'::UUID),

    -- Free-form application (no topic): student4 on ASE
    ('00000000-0000-4000-c000-000000000008'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'student4'),
     NULL,
     'Accessibility Testing Automation for Web Applications',
     'MASTER',
     'I want to research automated accessibility testing tools and develop a framework that integrates with CI pipelines.',
     'NOT_ASSESSED', NULL,
     NOW() + INTERVAL '60 days', '',
     NOW() - INTERVAL '3 days', NULL,
     '00000000-0000-4000-a000-000000000001'::UUID),

    -- OLD REJECTED #1: student2 on topic 1, rejected 400 days ago (for data retention e2e test)
    ('00000000-0000-4000-c000-000000000009'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'student2'),
     '00000000-0000-4000-b000-000000000001'::UUID,
     NULL, 'MASTER',
     'I wanted to explore LLM-based code review but my application was not selected.',
     'REJECTED', 'FAILED_TOPIC_REQUIREMENTS',
     NOW() - INTERVAL '380 days', '',
     NOW() - INTERVAL '410 days', NOW() - INTERVAL '400 days',
     '00000000-0000-4000-a000-000000000001'::UUID),

    -- OLD REJECTED #2: student3 on topic 2, rejected 500 days ago (for data retention e2e test)
    ('00000000-0000-4000-c000-00000000000a'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'student3'),
     '00000000-0000-4000-b000-000000000002'::UUID,
     NULL, 'BACHELOR',
     'I was interested in CI pipeline optimization but there was no capacity at the time.',
     'REJECTED', 'NO_CAPACITY',
     NOW() - INTERVAL '480 days', '',
     NOW() - INTERVAL '510 days', NOW() - INTERVAL '500 days',
     '00000000-0000-4000-a000-000000000001'::UUID)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 9. APPLICATION REVIEWERS (advisor reviews on assessed applications)
-- ============================================================================
INSERT INTO application_reviewers (application_id, user_id, reason, reviewed_at)
VALUES
    -- Accepted app 1: advisor interested
    ('00000000-0000-4000-c000-000000000001'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'advisor'),
     'INTERESTED', NOW() - INTERVAL '32 days'),
    -- Accepted app 2: advisor interested
    ('00000000-0000-4000-c000-000000000002'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'advisor'),
     'INTERESTED', NOW() - INTERVAL '22 days'),
    -- Accepted app 3: advisor2 interested
    ('00000000-0000-4000-c000-000000000003'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'advisor2'),
     'INTERESTED', NOW() - INTERVAL '17 days'),
    -- Rejected app: advisor2 not interested
    ('00000000-0000-4000-c000-000000000006'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'advisor2'),
     'NOT_INTERESTED', NOW() - INTERVAL '6 days'),
    -- Old rejected app 1: advisor not interested
    ('00000000-0000-4000-c000-000000000009'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'advisor'),
     'NOT_INTERESTED', NOW() - INTERVAL '400 days'),
    -- Old rejected app 2: advisor not interested
    ('00000000-0000-4000-c000-00000000000a'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'advisor'),
     'NOT_INTERESTED', NOW() - INTERVAL '500 days')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 10. THESES (5 covering key lifecycle states)
-- ============================================================================
INSERT INTO theses (thesis_id, title, type, language, metadata, info, abstract, state,
                    visibility, keywords, application_id, start_date, end_date, created_at,
                    research_group_id)
VALUES
    -- Thesis 1: WRITING (student, ASE)
    ('00000000-0000-4000-d000-000000000001'::UUID,
     'Automated Code Review Using Large Language Models',
     'MASTER', 'ENGLISH',
     '{"titles":{},"credits":{}}',
     '', '',
     'WRITING', 'PRIVATE',
     ARRAY['LLM', 'code review', 'software engineering', 'automation'],
     '00000000-0000-4000-c000-000000000001'::UUID,
     NOW() - INTERVAL '25 days', NULL,
     NOW() - INTERVAL '30 days',
     '00000000-0000-4000-a000-000000000001'::UUID),

    -- Thesis 2: PROPOSAL (student2, ASE)
    ('00000000-0000-4000-d000-000000000002'::UUID,
     'CI Pipeline Optimization Through Intelligent Test Selection',
     'BACHELOR', 'ENGLISH',
     '{"titles":{},"credits":{}}',
     '', '',
     'PROPOSAL', 'PRIVATE',
     ARRAY['CI/CD', 'test selection', 'build optimization'],
     '00000000-0000-4000-c000-000000000002'::UUID,
     NULL, NULL,
     NOW() - INTERVAL '18 days',
     '00000000-0000-4000-a000-000000000001'::UUID),

    -- Thesis 3: SUBMITTED (student3, DSA)
    ('00000000-0000-4000-d000-000000000003'::UUID,
     'Online Anomaly Detection in IoT Sensor Streams',
     'MASTER', 'ENGLISH',
     '{"titles":{},"credits":{}}',
     '', 'This thesis presents a novel framework for real-time anomaly detection in IoT sensor data streams using adaptive statistical methods.',
     'SUBMITTED', 'INTERNAL',
     ARRAY['anomaly detection', 'IoT', 'streaming', 'machine learning'],
     '00000000-0000-4000-c000-000000000003'::UUID,
     NOW() - INTERVAL '180 days', NOW() - INTERVAL '2 days',
     NOW() - INTERVAL '180 days',
     '00000000-0000-4000-a000-000000000002'::UUID),

    -- Thesis 4: FINISHED (student on closed topic 6, ASE)
    ('00000000-0000-4000-d000-000000000004'::UUID,
     'Systematic Monolith to Microservices Migration',
     'MASTER', 'ENGLISH',
     '{"titles":{},"credits":{}}',
     '', 'A systematic approach to decomposing monolithic applications into microservices, including tooling for automated dependency analysis.',
     'FINISHED', 'PUBLIC',
     ARRAY['microservices', 'migration', 'software architecture'],
     NULL,
     NOW() - INTERVAL '365 days', NOW() - INTERVAL '60 days',
     NOW() - INTERVAL '365 days',
     '00000000-0000-4000-a000-000000000001'::UUID),

    -- Thesis 5: DROPPED_OUT (student5, DSA)
    ('00000000-0000-4000-d000-000000000005'::UUID,
     'Predictive Maintenance Using Federated Learning',
     'BACHELOR', 'ENGLISH',
     '{"titles":{},"credits":{}}',
     '', '',
     'DROPPED_OUT', 'PRIVATE',
     ARRAY['federated learning', 'predictive maintenance'],
     NULL,
     NOW() - INTERVAL '120 days', NOW() - INTERVAL '30 days',
     NOW() - INTERVAL '120 days',
     '00000000-0000-4000-a000-000000000002'::UUID)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 11. THESIS ROLES
-- ============================================================================
INSERT INTO thesis_roles (thesis_id, user_id, role, position, assigned_at, assigned_by)
VALUES
    -- Thesis 1 (WRITING): student + advisor + supervisor
    ('00000000-0000-4000-d000-000000000001'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'student'), 'STUDENT', 0,
     NOW() - INTERVAL '30 days', (SELECT user_id FROM users WHERE university_id = 'supervisor')),
    ('00000000-0000-4000-d000-000000000001'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'advisor'), 'ADVISOR', 0,
     NOW() - INTERVAL '30 days', (SELECT user_id FROM users WHERE university_id = 'supervisor')),
    ('00000000-0000-4000-d000-000000000001'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'supervisor'), 'SUPERVISOR', 0,
     NOW() - INTERVAL '30 days', (SELECT user_id FROM users WHERE university_id = 'supervisor')),

    -- Thesis 2 (PROPOSAL): student2 + advisor + supervisor
    ('00000000-0000-4000-d000-000000000002'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'student2'), 'STUDENT', 0,
     NOW() - INTERVAL '18 days', (SELECT user_id FROM users WHERE university_id = 'supervisor')),
    ('00000000-0000-4000-d000-000000000002'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'advisor'), 'ADVISOR', 0,
     NOW() - INTERVAL '18 days', (SELECT user_id FROM users WHERE university_id = 'supervisor')),
    ('00000000-0000-4000-d000-000000000002'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'supervisor'), 'SUPERVISOR', 0,
     NOW() - INTERVAL '18 days', (SELECT user_id FROM users WHERE university_id = 'supervisor')),

    -- Thesis 3 (SUBMITTED): student3 + advisor2 + supervisor2
    ('00000000-0000-4000-d000-000000000003'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'student3'), 'STUDENT', 0,
     NOW() - INTERVAL '180 days', (SELECT user_id FROM users WHERE university_id = 'supervisor2')),
    ('00000000-0000-4000-d000-000000000003'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'advisor2'), 'ADVISOR', 0,
     NOW() - INTERVAL '180 days', (SELECT user_id FROM users WHERE university_id = 'supervisor2')),
    ('00000000-0000-4000-d000-000000000003'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'supervisor2'), 'SUPERVISOR', 0,
     NOW() - INTERVAL '180 days', (SELECT user_id FROM users WHERE university_id = 'supervisor2')),

    -- Thesis 4 (FINISHED): student + advisor + supervisor (old thesis)
    ('00000000-0000-4000-d000-000000000004'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'student'), 'STUDENT', 0,
     NOW() - INTERVAL '365 days', (SELECT user_id FROM users WHERE university_id = 'supervisor')),
    ('00000000-0000-4000-d000-000000000004'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'advisor'), 'ADVISOR', 0,
     NOW() - INTERVAL '365 days', (SELECT user_id FROM users WHERE university_id = 'supervisor')),
    ('00000000-0000-4000-d000-000000000004'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'supervisor'), 'SUPERVISOR', 0,
     NOW() - INTERVAL '365 days', (SELECT user_id FROM users WHERE university_id = 'supervisor')),

    -- Thesis 5 (DROPPED_OUT): student5 + advisor2 + supervisor2
    ('00000000-0000-4000-d000-000000000005'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'student5'), 'STUDENT', 0,
     NOW() - INTERVAL '120 days', (SELECT user_id FROM users WHERE university_id = 'supervisor2')),
    ('00000000-0000-4000-d000-000000000005'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'advisor2'), 'ADVISOR', 0,
     NOW() - INTERVAL '120 days', (SELECT user_id FROM users WHERE university_id = 'supervisor2')),
    ('00000000-0000-4000-d000-000000000005'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'supervisor2'), 'SUPERVISOR', 0,
     NOW() - INTERVAL '120 days', (SELECT user_id FROM users WHERE university_id = 'supervisor2'))
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 12. THESIS STATE CHANGES (history for each thesis)
-- ============================================================================
INSERT INTO thesis_state_changes (thesis_id, state, changed_at)
VALUES
    -- Thesis 1: PROPOSAL -> WRITING
    ('00000000-0000-4000-d000-000000000001'::UUID, 'PROPOSAL', NOW() - INTERVAL '30 days'),
    ('00000000-0000-4000-d000-000000000001'::UUID, 'WRITING', NOW() - INTERVAL '25 days'),

    -- Thesis 2: PROPOSAL
    ('00000000-0000-4000-d000-000000000002'::UUID, 'PROPOSAL', NOW() - INTERVAL '18 days'),

    -- Thesis 3: PROPOSAL -> WRITING -> SUBMITTED
    ('00000000-0000-4000-d000-000000000003'::UUID, 'PROPOSAL', NOW() - INTERVAL '180 days'),
    ('00000000-0000-4000-d000-000000000003'::UUID, 'WRITING', NOW() - INTERVAL '170 days'),
    ('00000000-0000-4000-d000-000000000003'::UUID, 'SUBMITTED', NOW() - INTERVAL '2 days'),

    -- Thesis 4: PROPOSAL -> WRITING -> SUBMITTED -> ASSESSED -> GRADED -> FINISHED
    ('00000000-0000-4000-d000-000000000004'::UUID, 'PROPOSAL', NOW() - INTERVAL '365 days'),
    ('00000000-0000-4000-d000-000000000004'::UUID, 'WRITING', NOW() - INTERVAL '350 days'),
    ('00000000-0000-4000-d000-000000000004'::UUID, 'SUBMITTED', NOW() - INTERVAL '120 days'),
    ('00000000-0000-4000-d000-000000000004'::UUID, 'ASSESSED', NOW() - INTERVAL '90 days'),
    ('00000000-0000-4000-d000-000000000004'::UUID, 'GRADED', NOW() - INTERVAL '75 days'),
    ('00000000-0000-4000-d000-000000000004'::UUID, 'FINISHED', NOW() - INTERVAL '60 days'),

    -- Thesis 5: PROPOSAL -> WRITING -> DROPPED_OUT
    ('00000000-0000-4000-d000-000000000005'::UUID, 'PROPOSAL', NOW() - INTERVAL '120 days'),
    ('00000000-0000-4000-d000-000000000005'::UUID, 'WRITING', NOW() - INTERVAL '110 days'),
    ('00000000-0000-4000-d000-000000000005'::UUID, 'DROPPED_OUT', NOW() - INTERVAL '30 days')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 13. THESIS PROPOSALS
-- ============================================================================
INSERT INTO thesis_proposals (proposal_id, thesis_id, proposal_filename, approved_at, approved_by,
                              created_at, created_by)
VALUES
    -- Thesis 1 (WRITING): approved proposal
    ('00000000-0000-4000-e000-000000000001'::UUID,
     '00000000-0000-4000-d000-000000000001'::UUID,
     'proposal_llm_code_review_v1.pdf',
     NOW() - INTERVAL '26 days',
     (SELECT user_id FROM users WHERE university_id = 'advisor'),
     NOW() - INTERVAL '28 days',
     (SELECT user_id FROM users WHERE university_id = 'student')),

    -- Thesis 2 (PROPOSAL): pending proposal (not yet approved)
    ('00000000-0000-4000-e000-000000000002'::UUID,
     '00000000-0000-4000-d000-000000000002'::UUID,
     'proposal_ci_optimization_draft.pdf',
     NULL, NULL,
     NOW() - INTERVAL '10 days',
     (SELECT user_id FROM users WHERE university_id = 'student2')),

    -- Thesis 3 (SUBMITTED): approved proposal
    ('00000000-0000-4000-e000-000000000003'::UUID,
     '00000000-0000-4000-d000-000000000003'::UUID,
     'proposal_anomaly_detection_final.pdf',
     NOW() - INTERVAL '172 days',
     (SELECT user_id FROM users WHERE university_id = 'advisor2'),
     NOW() - INTERVAL '175 days',
     (SELECT user_id FROM users WHERE university_id = 'student3')),

    -- Thesis 4 (FINISHED): approved proposal
    ('00000000-0000-4000-e000-000000000004'::UUID,
     '00000000-0000-4000-d000-000000000004'::UUID,
     'proposal_microservices_migration.pdf',
     NOW() - INTERVAL '352 days',
     (SELECT user_id FROM users WHERE university_id = 'advisor'),
     NOW() - INTERVAL '358 days',
     (SELECT user_id FROM users WHERE university_id = 'student')),

    -- Thesis 5 (DROPPED_OUT): approved proposal before dropping out
    ('00000000-0000-4000-e000-000000000005'::UUID,
     '00000000-0000-4000-d000-000000000005'::UUID,
     'proposal_federated_learning.pdf',
     NOW() - INTERVAL '112 days',
     (SELECT user_id FROM users WHERE university_id = 'advisor2'),
     NOW() - INTERVAL '115 days',
     (SELECT user_id FROM users WHERE university_id = 'student5'))
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 14. THESIS ASSESSMENTS (for ASSESSED+ theses)
-- ============================================================================
INSERT INTO thesis_assessments (assessment_id, thesis_id, summary, positives, negatives,
                                grade_suggestion, created_at, created_by)
VALUES
    -- Thesis 4 (FINISHED): assessment by advisor
    ('00000000-0000-4000-e100-000000000001'::UUID,
     '00000000-0000-4000-d000-000000000004'::UUID,
     'The thesis presents a comprehensive migration framework from monolithic to microservice architectures. The approach is well-structured and the evaluation is thorough.',
     'Excellent literature review. The migration tool provides practical value. The case study with a real-world application demonstrates applicability.',
     'The performance evaluation could be more detailed. Some edge cases in the dependency analysis are not covered.',
     '1.3',
     NOW() - INTERVAL '95 days',
     (SELECT user_id FROM users WHERE university_id = 'advisor')),

    -- Thesis 4 (FINISHED): assessment by supervisor
    ('00000000-0000-4000-e100-000000000002'::UUID,
     '00000000-0000-4000-d000-000000000004'::UUID,
     'A solid contribution to the field of software architecture migration. The tooling aspect adds significant practical value beyond the academic contribution.',
     'Well-written and clearly structured. The automated dependency analysis tool is a notable contribution. Good balance of theory and practice.',
     'The scalability analysis for very large monoliths is limited. Could benefit from additional case studies.',
     '1.3',
     NOW() - INTERVAL '92 days',
     (SELECT user_id FROM users WHERE university_id = 'supervisor')),

    -- Thesis 3 (SUBMITTED): early assessment by advisor2
    ('00000000-0000-4000-e100-000000000003'::UUID,
     '00000000-0000-4000-d000-000000000003'::UUID,
     'The thesis addresses a relevant problem in IoT anomaly detection. The proposed framework shows promising results on the benchmark datasets.',
     'Novel combination of statistical and ML approaches. Good experimental design with multiple datasets. Clear presentation of results.',
     'The concept drift adaptation mechanism needs more rigorous evaluation. The latency benchmarks should include more baseline comparisons.',
     '1.7',
     NOW() - INTERVAL '1 day',
     (SELECT user_id FROM users WHERE university_id = 'advisor2'))
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 15. THESIS COMMENTS
-- ============================================================================
INSERT INTO thesis_comments (comment_id, thesis_id, type, message, filename, upload_name,
                             created_at, created_by)
VALUES
    -- Thesis 1 (WRITING): advisor feedback on progress
    ('00000000-0000-4000-e200-000000000001'::UUID,
     '00000000-0000-4000-d000-000000000001'::UUID,
     'ADVISOR',
     'Good progress on the literature review. Please make sure to include the recent work by Chen et al. on automated PR review.',
     NULL, NULL,
     NOW() - INTERVAL '20 days',
     (SELECT user_id FROM users WHERE university_id = 'advisor')),

    -- Thesis 1 (WRITING): student response
    ('00000000-0000-4000-e200-000000000002'::UUID,
     '00000000-0000-4000-d000-000000000001'::UUID,
     'THESIS',
     'Thanks for the feedback! I have added the reference and updated the related work section accordingly.',
     NULL, NULL,
     NOW() - INTERVAL '18 days',
     (SELECT user_id FROM users WHERE university_id = 'student')),

    -- Thesis 1 (WRITING): advisor with file attachment
    ('00000000-0000-4000-e200-000000000003'::UUID,
     '00000000-0000-4000-d000-000000000001'::UUID,
     'ADVISOR',
     'Here are my detailed comments on your Chapter 3 draft. Please address the points highlighted in the PDF.',
     'chapter3_review_notes.pdf', 'chapter3_review_notes.pdf',
     NOW() - INTERVAL '10 days',
     (SELECT user_id FROM users WHERE university_id = 'advisor')),

    -- Thesis 2 (PROPOSAL): advisor comment on proposal
    ('00000000-0000-4000-e200-000000000004'::UUID,
     '00000000-0000-4000-d000-000000000002'::UUID,
     'ADVISOR',
     'Your proposal looks promising. Please elaborate more on the test selection algorithm and add a timeline for the milestones.',
     NULL, NULL,
     NOW() - INTERVAL '8 days',
     (SELECT user_id FROM users WHERE university_id = 'advisor')),

    -- Thesis 3 (SUBMITTED): supervisor comment
    ('00000000-0000-4000-e200-000000000005'::UUID,
     '00000000-0000-4000-d000-000000000003'::UUID,
     'THESIS',
     'The thesis has been submitted. Please prepare for the final presentation and share the slides at least one week in advance.',
     NULL, NULL,
     NOW() - INTERVAL '1 day',
     (SELECT user_id FROM users WHERE university_id = 'supervisor2')),

    -- Thesis 4 (FINISHED): final comment
    ('00000000-0000-4000-e200-000000000006'::UUID,
     '00000000-0000-4000-d000-000000000004'::UUID,
     'THESIS',
     'Congratulations on completing your thesis! The final version has been archived.',
     NULL, NULL,
     NOW() - INTERVAL '60 days',
     (SELECT user_id FROM users WHERE university_id = 'supervisor'))
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 16. THESIS PRESENTATIONS
-- ============================================================================
INSERT INTO thesis_presentations (presentation_id, thesis_id, type, state, visibility, language,
                                  location, stream_url, scheduled_at, created_at, created_by)
VALUES
    -- Thesis 1 (WRITING): upcoming intermediate presentation
    ('00000000-0000-4000-e300-000000000001'::UUID,
     '00000000-0000-4000-d000-000000000001'::UUID,
     'INTERMEDIATE', 'SCHEDULED', 'PRIVATE', 'ENGLISH',
     'Room 01.07.023, Boltzmannstr. 3', NULL,
     NOW() + INTERVAL '7 days',
     NOW() - INTERVAL '5 days',
     (SELECT user_id FROM users WHERE university_id = 'advisor')),

    -- Thesis 3 (SUBMITTED): scheduled final presentation
    ('00000000-0000-4000-e300-000000000002'::UUID,
     '00000000-0000-4000-d000-000000000003'::UUID,
     'FINAL', 'SCHEDULED', 'PUBLIC', 'ENGLISH',
     'Room 00.08.038, Boltzmannstr. 3', 'https://tum-live.de/w/thesis-presentations',
     NOW() + INTERVAL '14 days',
     NOW() - INTERVAL '3 days',
     (SELECT user_id FROM users WHERE university_id = 'supervisor2')),

    -- Thesis 4 (FINISHED): past intermediate presentation
    ('00000000-0000-4000-e300-000000000003'::UUID,
     '00000000-0000-4000-d000-000000000004'::UUID,
     'INTERMEDIATE', 'SCHEDULED', 'PRIVATE', 'ENGLISH',
     'Room 01.07.023, Boltzmannstr. 3', NULL,
     NOW() - INTERVAL '200 days',
     NOW() - INTERVAL '210 days',
     (SELECT user_id FROM users WHERE university_id = 'advisor')),

    -- Thesis 4 (FINISHED): past final presentation
    ('00000000-0000-4000-e300-000000000004'::UUID,
     '00000000-0000-4000-d000-000000000004'::UUID,
     'FINAL', 'SCHEDULED', 'PUBLIC', 'ENGLISH',
     'Room 00.08.038, Boltzmannstr. 3', 'https://tum-live.de/w/thesis-presentations',
     NOW() - INTERVAL '70 days',
     NOW() - INTERVAL '80 days',
     (SELECT user_id FROM users WHERE university_id = 'supervisor')),

    -- Thesis 5 (DROPPED_OUT): drafted but never happened
    ('00000000-0000-4000-e300-000000000005'::UUID,
     '00000000-0000-4000-d000-000000000005'::UUID,
     'INTERMEDIATE', 'DRAFTED', 'PRIVATE', 'ENGLISH',
     NULL, NULL,
     NOW() - INTERVAL '40 days',
     NOW() - INTERVAL '50 days',
     (SELECT user_id FROM users WHERE university_id = 'advisor2'))
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 17. THESIS PRESENTATION INVITES
-- ============================================================================
INSERT INTO thesis_presentation_invites (presentation_id, email, invited_at)
VALUES
    -- Thesis 1 intermediate: invite advisor2 as guest
    ('00000000-0000-4000-e300-000000000001'::UUID, 'advisor2@test.local', NOW() - INTERVAL '4 days'),
    ('00000000-0000-4000-e300-000000000001'::UUID, 'supervisor@test.local', NOW() - INTERVAL '4 days'),

    -- Thesis 3 final: invite external reviewer + team
    ('00000000-0000-4000-e300-000000000002'::UUID, 'external.reviewer@university.edu', NOW() - INTERVAL '2 days'),
    ('00000000-0000-4000-e300-000000000002'::UUID, 'advisor@test.local', NOW() - INTERVAL '2 days'),
    ('00000000-0000-4000-e300-000000000002'::UUID, 'supervisor@test.local', NOW() - INTERVAL '2 days'),

    -- Thesis 4 final (past): invites for the completed presentation
    ('00000000-0000-4000-e300-000000000004'::UUID, 'advisor2@test.local', NOW() - INTERVAL '75 days'),
    ('00000000-0000-4000-e300-000000000004'::UUID, 'supervisor2@test.local', NOW() - INTERVAL '75 days')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 18. THESIS FILES
-- ============================================================================
INSERT INTO thesis_files (file_id, thesis_id, type, filename, upload_name, uploaded_at, uploaded_by)
VALUES
    -- Thesis 1 (WRITING): thesis draft
    ('00000000-0000-4000-e400-000000000001'::UUID,
     '00000000-0000-4000-d000-000000000001'::UUID,
     'THESIS', 'thesis_llm_code_review_draft_v2.pdf', 'thesis_llm_code_review_draft_v2.pdf',
     NOW() - INTERVAL '5 days',
     (SELECT user_id FROM users WHERE university_id = 'student')),

    -- Thesis 3 (SUBMITTED): final thesis
    ('00000000-0000-4000-e400-000000000002'::UUID,
     '00000000-0000-4000-d000-000000000003'::UUID,
     'THESIS', 'thesis_anomaly_detection_final.pdf', 'thesis_anomaly_detection_final.pdf',
     NOW() - INTERVAL '2 days',
     (SELECT user_id FROM users WHERE university_id = 'student3')),

    -- Thesis 3 (SUBMITTED): presentation slides
    ('00000000-0000-4000-e400-000000000003'::UUID,
     '00000000-0000-4000-d000-000000000003'::UUID,
     'PRESENTATION', 'slides_anomaly_detection.pdf', 'slides_anomaly_detection.pdf',
     NOW() - INTERVAL '1 day',
     (SELECT user_id FROM users WHERE university_id = 'student3')),

    -- Thesis 4 (FINISHED): final thesis
    ('00000000-0000-4000-e400-000000000004'::UUID,
     '00000000-0000-4000-d000-000000000004'::UUID,
     'THESIS', 'thesis_microservices_migration_final.pdf', 'thesis_microservices_migration_final.pdf',
     NOW() - INTERVAL '120 days',
     (SELECT user_id FROM users WHERE university_id = 'student')),

    -- Thesis 4 (FINISHED): presentation slides
    ('00000000-0000-4000-e400-000000000005'::UUID,
     '00000000-0000-4000-d000-000000000004'::UUID,
     'PRESENTATION', 'slides_microservices_final.pdf', 'slides_microservices_final.pdf',
     NOW() - INTERVAL '72 days',
     (SELECT user_id FROM users WHERE university_id = 'student'))
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 19. THESIS FEEDBACK
-- ============================================================================
INSERT INTO thesis_feedback (feedback_id, thesis_id, type, feedback, completed_at,
                             requested_at, requested_by)
VALUES
    -- Thesis 1 (WRITING): completed proposal feedback
    ('00000000-0000-4000-e500-000000000001'::UUID,
     '00000000-0000-4000-d000-000000000001'::UUID,
     'PROPOSAL',
     'The proposal is well-structured. Please clarify the evaluation metrics in Section 4 before proceeding.',
     NOW() - INTERVAL '26 days',
     NOW() - INTERVAL '28 days',
     (SELECT user_id FROM users WHERE university_id = 'advisor')),

    -- Thesis 2 (PROPOSAL): pending proposal feedback
    ('00000000-0000-4000-e500-000000000002'::UUID,
     '00000000-0000-4000-d000-000000000002'::UUID,
     'PROPOSAL',
     '',
     NULL,
     NOW() - INTERVAL '8 days',
     (SELECT user_id FROM users WHERE university_id = 'advisor')),

    -- Thesis 3 (SUBMITTED): completed thesis feedback
    ('00000000-0000-4000-e500-000000000003'::UUID,
     '00000000-0000-4000-d000-000000000003'::UUID,
     'THESIS',
     'The thesis is comprehensive and well-written. Minor revisions needed in the conclusion section regarding future work directions.',
     NOW() - INTERVAL '3 days',
     NOW() - INTERVAL '5 days',
     (SELECT user_id FROM users WHERE university_id = 'advisor2')),

    -- Thesis 3 (SUBMITTED): pending presentation feedback
    ('00000000-0000-4000-e500-000000000004'::UUID,
     '00000000-0000-4000-d000-000000000003'::UUID,
     'PRESENTATION',
     '',
     NULL,
     NOW() - INTERVAL '1 day',
     (SELECT user_id FROM users WHERE university_id = 'supervisor2')),

    -- Thesis 4 (FINISHED): completed thesis feedback
    ('00000000-0000-4000-e500-000000000005'::UUID,
     '00000000-0000-4000-d000-000000000004'::UUID,
     'THESIS',
     'Excellent work overall. The migration framework is practical and well-evaluated. The writing quality is very good.',
     NOW() - INTERVAL '100 days',
     NOW() - INTERVAL '115 days',
     (SELECT user_id FROM users WHERE university_id = 'advisor')),

    -- Thesis 4 (FINISHED): completed presentation feedback
    ('00000000-0000-4000-e500-000000000006'::UUID,
     '00000000-0000-4000-d000-000000000004'::UUID,
     'PRESENTATION',
     'Clear and engaging presentation. Good handling of questions. Slides were well-designed and the demo was impressive.',
     NOW() - INTERVAL '68 days',
     NOW() - INTERVAL '70 days',
     (SELECT user_id FROM users WHERE university_id = 'supervisor'))
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 20. NOTIFICATION SETTINGS
-- ============================================================================
INSERT INTO notification_settings (user_id, name, email, updated_at)
VALUES
    -- supervisor: all notifications enabled
    ((SELECT user_id FROM users WHERE university_id = 'supervisor'), 'new-applications', 'yes', NOW()),
    ((SELECT user_id FROM users WHERE university_id = 'supervisor'), 'thesis-comments', 'yes', NOW()),
    ((SELECT user_id FROM users WHERE university_id = 'supervisor'), 'unreviewed-application-reminder', 'yes', NOW()),

    -- advisor: selective notifications
    ((SELECT user_id FROM users WHERE university_id = 'advisor'), 'new-applications', 'yes', NOW()),
    ((SELECT user_id FROM users WHERE university_id = 'advisor'), 'thesis-comments', 'yes', NOW()),
    ((SELECT user_id FROM users WHERE university_id = 'advisor'), 'unreviewed-application-reminder', 'no', NOW()),

    -- supervisor2: all enabled
    ((SELECT user_id FROM users WHERE university_id = 'supervisor2'), 'new-applications', 'yes', NOW()),
    ((SELECT user_id FROM users WHERE university_id = 'supervisor2'), 'thesis-comments', 'yes', NOW()),
    ((SELECT user_id FROM users WHERE university_id = 'supervisor2'), 'unreviewed-application-reminder', 'yes', NOW()),

    -- advisor2: comments only
    ((SELECT user_id FROM users WHERE university_id = 'advisor2'), 'new-applications', 'no', NOW()),
    ((SELECT user_id FROM users WHERE university_id = 'advisor2'), 'thesis-comments', 'yes', NOW()),
    ((SELECT user_id FROM users WHERE university_id = 'advisor2'), 'unreviewed-application-reminder', 'no', NOW())
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 21. INTERVIEW PROCESSES (for topic 3 which has INTERVIEWING application)
-- ============================================================================
INSERT INTO interview_processes (interview_process_id, topic_id, completed)
VALUES
    -- Active interview process for topic 3 (anomaly detection)
    ('00000000-0000-4000-e600-000000000001'::UUID,
     '00000000-0000-4000-b000-000000000003'::UUID, FALSE),

    -- Completed interview process for topic 1 (LLM code review — already accepted a student)
    ('00000000-0000-4000-e600-000000000002'::UUID,
     '00000000-0000-4000-b000-000000000001'::UUID, TRUE),

    -- Active interview process for topic 2 (CI pipeline)
    ('00000000-0000-4000-e600-000000000003'::UUID,
     '00000000-0000-4000-b000-000000000002'::UUID, TRUE)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 22. INTERVIEWEES
-- ============================================================================
INSERT INTO interviewees (interviewee_id, interview_process_id, last_invited, score, application_id)
VALUES
    -- Topic 3 process: student4 (INTERVIEWING application)
    ('00000000-0000-4000-e700-000000000001'::UUID,
     '00000000-0000-4000-e600-000000000001'::UUID,
     NOW() - INTERVAL '5 days', NULL,
     '00000000-0000-4000-c000-000000000007'::UUID),

    -- Topic 3 process: student5 (REJECTED, was interviewed before rejection)
    ('00000000-0000-4000-e700-000000000002'::UUID,
     '00000000-0000-4000-e600-000000000001'::UUID,
     NOW() - INTERVAL '8 days', 45,
     '00000000-0000-4000-c000-000000000006'::UUID),

    -- Topic 1 process: student (accepted after interview)
    ('00000000-0000-4000-e700-000000000003'::UUID,
     '00000000-0000-4000-e600-000000000002'::UUID,
     NOW() - INTERVAL '33 days', 92,
     '00000000-0000-4000-c000-000000000001'::UUID),

    -- Topic 2 process: student2 (accepted after interview)
    ('00000000-0000-4000-e700-000000000004'::UUID,
     '00000000-0000-4000-e600-000000000003'::UUID,
     NOW() - INTERVAL '23 days', 85,
     '00000000-0000-4000-c000-000000000002'::UUID),

    -- Topic 3 process: student3 (accepted)
    ('00000000-0000-4000-e700-000000000005'::UUID,
     '00000000-0000-4000-e600-000000000001'::UUID,
     NOW() - INTERVAL '16 days', 88,
     '00000000-0000-4000-c000-000000000003'::UUID)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 23. INTERVIEW SLOTS
-- ============================================================================
INSERT INTO interview_slots (slot_id, interview_process_id, start_date, end_date,
                             interviewee_id, location, stream_link)
VALUES
    -- Topic 3: slot for student4 (upcoming)
    ('00000000-0000-4000-e800-000000000001'::UUID,
     '00000000-0000-4000-e600-000000000001'::UUID,
     NOW() + INTERVAL '3 days', NOW() + INTERVAL '3 days' + INTERVAL '45 minutes',
     '00000000-0000-4000-e700-000000000001'::UUID,
     'Room 01.07.023, Boltzmannstr. 3', 'https://tum-live.de/w/interview-3'),

    -- Topic 3: slot for student5 (past, scored)
    ('00000000-0000-4000-e800-000000000002'::UUID,
     '00000000-0000-4000-e600-000000000001'::UUID,
     NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days' + INTERVAL '45 minutes',
     '00000000-0000-4000-e700-000000000002'::UUID,
     'Room 01.07.023, Boltzmannstr. 3', NULL),

    -- Topic 3: empty slot (available)
    ('00000000-0000-4000-e800-000000000003'::UUID,
     '00000000-0000-4000-e600-000000000001'::UUID,
     NOW() + INTERVAL '3 days' + INTERVAL '1 hour', NOW() + INTERVAL '3 days' + INTERVAL '105 minutes',
     NULL,
     'Room 01.07.023, Boltzmannstr. 3', NULL),

    -- Topic 1: past slot for student (completed process)
    ('00000000-0000-4000-e800-000000000004'::UUID,
     '00000000-0000-4000-e600-000000000002'::UUID,
     NOW() - INTERVAL '32 days', NOW() - INTERVAL '32 days' + INTERVAL '30 minutes',
     '00000000-0000-4000-e700-000000000003'::UUID,
     'Room 00.08.038, Boltzmannstr. 3', NULL),

    -- Topic 2: past slot for student2
    ('00000000-0000-4000-e800-000000000005'::UUID,
     '00000000-0000-4000-e600-000000000003'::UUID,
     NOW() - INTERVAL '22 days', NOW() - INTERVAL '22 days' + INTERVAL '30 minutes',
     '00000000-0000-4000-e700-000000000004'::UUID,
     'Room 00.08.038, Boltzmannstr. 3', 'https://tum-live.de/w/interview-2'),

    -- Topic 3: past slot for student3
    ('00000000-0000-4000-e800-000000000006'::UUID,
     '00000000-0000-4000-e600-000000000001'::UUID,
     NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days' + INTERVAL '45 minutes',
     '00000000-0000-4000-e700-000000000005'::UUID,
     'Room 01.07.023, Boltzmannstr. 3', NULL)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 24. INTERVIEW ASSESSMENTS
-- ============================================================================
INSERT INTO interview_assessments (interview_assessment_id, interviewee_id, interview_note)
VALUES
    -- student5 on topic 3: assessed, low score
    ('00000000-0000-4000-e900-000000000001'::UUID,
     '00000000-0000-4000-e700-000000000002'::UUID,
     'The candidate showed enthusiasm but lacks the required technical background in streaming data processing and statistical methods for anomaly detection.'),

    -- student on topic 1: strong assessment
    ('00000000-0000-4000-e900-000000000002'::UUID,
     '00000000-0000-4000-e700-000000000003'::UUID,
     'Excellent technical knowledge of LLMs and prompt engineering. Demonstrated clear understanding of code review challenges. Strong communication skills.'),

    -- student2 on topic 2: good assessment
    ('00000000-0000-4000-e900-000000000003'::UUID,
     '00000000-0000-4000-e700-000000000004'::UUID,
     'Good understanding of CI/CD concepts. Has practical experience with GitHub Actions. Could elaborate more on test selection strategies but overall a strong candidate.'),

    -- student3 on topic 3: strong assessment
    ('00000000-0000-4000-e900-000000000004'::UUID,
     '00000000-0000-4000-e700-000000000005'::UUID,
     'Strong background in streaming data with hands-on Kafka experience. Demonstrated solid understanding of statistical anomaly detection methods. Very well prepared.')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 25. DATA EXPORT EMAIL TEMPLATE (override for dev)
-- ============================================================================
INSERT INTO email_templates (email_template_id, research_group_id, template_case, subject, body_html, language, description, created_at, updated_by, updated_at)
VALUES (gen_random_uuid(), NULL, 'DATA_EXPORT_READY', 'Your Data Export is Ready',
'<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
Your personal data export has been generated and is ready for download. You can download it from your data export page:
</p>

<p>
<a target="_blank" rel="noopener noreferrer nofollow" th:href="${downloadUrl}" th:text="${downloadUrl}"></a>
</p>

<p>
Please note that the download link will expire in 7 days. After that, you can request a new export.
</p>

<br/><br/>', 'en', 'Notification when data export is ready for download', NOW(), NULL, NOW())
ON CONFLICT (template_case, language) WHERE research_group_id IS NULL DO NOTHING;

-- ============================================================================
-- 26. ACCOUNT DELETION TEST USERS (3 users for testing deletion scenarios)
-- ============================================================================
INSERT INTO users (user_id, university_id, matriculation_number, email, first_name, last_name,
                   gender, nationality, study_degree, study_program, projects, interests,
                   special_skills, enrolled_at, updated_at, joined_at)
VALUES
    -- User with a FINISHED thesis from 7+ years ago (retention expired → full deletion)
    (gen_random_uuid(), 'delete_old_thesis', '03700011', 'delete_old_thesis@test.local',
     'OldThesis', 'Deletable', 'MALE', 'DE', 'MASTER', 'COMPUTER_SCIENCE',
     'Legacy project from years ago', 'Historical research', 'Java, C++',
     NOW() - INTERVAL '2800 days', NOW(), NOW() - INTERVAL '2800 days'),
    -- User with a FINISHED thesis from 2 years ago (under retention → soft deletion)
    (gen_random_uuid(), 'delete_recent_thesis', '03700012', 'delete_recent_thesis@test.local',
     'RecentThesis', 'Retainable', 'FEMALE', 'DE', 'MASTER', 'INFORMATION_SYSTEMS',
     'Recent data analytics project', 'Business intelligence', 'Python, SQL',
     NOW() - INTERVAL '800 days', NOW(), NOW() - INTERVAL '800 days'),
    -- User with only a rejected application (no thesis → full deletion)
    (gen_random_uuid(), 'delete_rejected_app', '03700013', 'delete_rejected_app@test.local',
     'RejectedApp', 'Deletable', 'OTHER', 'US', 'BACHELOR', 'MANAGEMENT_AND_TECHNOLOGY',
     NULL, 'Web development', 'HTML, CSS, JavaScript',
     NOW() - INTERVAL '100 days', NOW(), NOW() - INTERVAL '100 days')
ON CONFLICT (university_id) DO UPDATE SET
    matriculation_number = COALESCE(users.matriculation_number, EXCLUDED.matriculation_number),
    email                = COALESCE(users.email, EXCLUDED.email),
    first_name           = COALESCE(users.first_name, EXCLUDED.first_name),
    last_name            = COALESCE(users.last_name, EXCLUDED.last_name),
    gender               = COALESCE(users.gender, EXCLUDED.gender),
    nationality          = COALESCE(users.nationality, EXCLUDED.nationality),
    study_degree         = COALESCE(users.study_degree, EXCLUDED.study_degree),
    study_program        = COALESCE(users.study_program, EXCLUDED.study_program),
    projects             = COALESCE(users.projects, EXCLUDED.projects),
    interests            = COALESCE(users.interests, EXCLUDED.interests),
    special_skills       = COALESCE(users.special_skills, EXCLUDED.special_skills),
    enrolled_at          = COALESCE(users.enrolled_at, EXCLUDED.enrolled_at);

-- ============================================================================
-- 27. ACCOUNT DELETION TEST USER GROUPS
-- ============================================================================
INSERT INTO user_groups (user_id, "group")
VALUES
    ((SELECT user_id FROM users WHERE university_id = 'delete_old_thesis'), 'student'),
    ((SELECT user_id FROM users WHERE university_id = 'delete_recent_thesis'), 'student'),
    ((SELECT user_id FROM users WHERE university_id = 'delete_rejected_app'), 'student')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 28. ACCOUNT DELETION TEST THESES
-- ============================================================================
-- Thesis 6: FINISHED, created 7+ years ago (retention expired for delete_old_thesis)
INSERT INTO theses (thesis_id, title, type, language, metadata, info, abstract, state,
                    visibility, keywords, application_id, start_date, end_date, created_at,
                    research_group_id)
VALUES
    ('00000000-0000-4000-d000-000000000006'::UUID,
     'Legacy Software Migration Strategies',
     'MASTER', 'ENGLISH',
     '{"titles":{},"credits":{}}',
     '', 'A comprehensive study of legacy software migration strategies applied to enterprise systems.',
     'FINISHED', 'PUBLIC',
     ARRAY['legacy systems', 'migration', 'enterprise'],
     NULL,
     NOW() - INTERVAL '2800 days', NOW() - INTERVAL '2600 days',
     NOW() - INTERVAL '2800 days',
     '00000000-0000-4000-a000-000000000001'::UUID),
    -- Thesis 7: FINISHED, created 2 years ago (under retention for delete_recent_thesis)
    ('00000000-0000-4000-d000-000000000007'::UUID,
     'Business Intelligence Dashboard Design Patterns',
     'MASTER', 'ENGLISH',
     '{"titles":{},"credits":{}}',
     '', 'An analysis of effective dashboard design patterns for business intelligence applications.',
     'FINISHED', 'PUBLIC',
     ARRAY['business intelligence', 'dashboards', 'data visualization'],
     NULL,
     NOW() - INTERVAL '800 days', NOW() - INTERVAL '620 days',
     NOW() - INTERVAL '800 days',
     '00000000-0000-4000-a000-000000000001'::UUID)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 29. ACCOUNT DELETION TEST THESIS ROLES
-- ============================================================================
INSERT INTO thesis_roles (thesis_id, user_id, role, position, assigned_at, assigned_by)
VALUES
    -- Thesis 6 (old, retention expired): delete_old_thesis as STUDENT, supervisor + advisor
    ('00000000-0000-4000-d000-000000000006'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'delete_old_thesis'), 'STUDENT', 0,
     NOW() - INTERVAL '2800 days', (SELECT user_id FROM users WHERE university_id = 'supervisor')),
    ('00000000-0000-4000-d000-000000000006'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'advisor'), 'ADVISOR', 0,
     NOW() - INTERVAL '2800 days', (SELECT user_id FROM users WHERE university_id = 'supervisor')),
    ('00000000-0000-4000-d000-000000000006'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'supervisor'), 'SUPERVISOR', 0,
     NOW() - INTERVAL '2800 days', (SELECT user_id FROM users WHERE university_id = 'supervisor')),
    -- Thesis 7 (recent, under retention): delete_recent_thesis as STUDENT, supervisor + advisor
    ('00000000-0000-4000-d000-000000000007'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'delete_recent_thesis'), 'STUDENT', 0,
     NOW() - INTERVAL '800 days', (SELECT user_id FROM users WHERE university_id = 'supervisor')),
    ('00000000-0000-4000-d000-000000000007'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'advisor'), 'ADVISOR', 0,
     NOW() - INTERVAL '800 days', (SELECT user_id FROM users WHERE university_id = 'supervisor')),
    ('00000000-0000-4000-d000-000000000007'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'supervisor'), 'SUPERVISOR', 0,
     NOW() - INTERVAL '800 days', (SELECT user_id FROM users WHERE university_id = 'supervisor'))
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 30. ACCOUNT DELETION TEST THESIS STATE CHANGES
-- ============================================================================
INSERT INTO thesis_state_changes (thesis_id, state, changed_at)
VALUES
    ('00000000-0000-4000-d000-000000000006'::UUID, 'PROPOSAL', NOW() - INTERVAL '2800 days'),
    ('00000000-0000-4000-d000-000000000006'::UUID, 'WRITING', NOW() - INTERVAL '2780 days'),
    ('00000000-0000-4000-d000-000000000006'::UUID, 'SUBMITTED', NOW() - INTERVAL '2620 days'),
    ('00000000-0000-4000-d000-000000000006'::UUID, 'ASSESSED', NOW() - INTERVAL '2610 days'),
    ('00000000-0000-4000-d000-000000000006'::UUID, 'FINISHED', NOW() - INTERVAL '2600 days'),
    ('00000000-0000-4000-d000-000000000007'::UUID, 'PROPOSAL', NOW() - INTERVAL '800 days'),
    ('00000000-0000-4000-d000-000000000007'::UUID, 'WRITING', NOW() - INTERVAL '780 days'),
    ('00000000-0000-4000-d000-000000000007'::UUID, 'SUBMITTED', NOW() - INTERVAL '640 days'),
    ('00000000-0000-4000-d000-000000000007'::UUID, 'ASSESSED', NOW() - INTERVAL '630 days'),
    ('00000000-0000-4000-d000-000000000007'::UUID, 'FINISHED', NOW() - INTERVAL '620 days')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 31. ACCOUNT DELETION TEST APPLICATION (rejected, for delete_rejected_app)
-- ============================================================================
INSERT INTO applications (application_id, user_id, topic_id, thesis_title, thesis_type, motivation,
                          state, reject_reason, desired_start_date, comment, created_at, reviewed_at,
                          research_group_id)
VALUES
    ('00000000-0000-4000-c000-00000000000b'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'delete_rejected_app'),
     '00000000-0000-4000-b000-000000000001'::UUID,
     NULL, 'BACHELOR',
     'I am interested in LLM-based code review but my background did not match the requirements.',
     'REJECTED', 'FAILED_TOPIC_REQUIREMENTS',
     NOW() - INTERVAL '60 days', '',
     NOW() - INTERVAL '90 days', NOW() - INTERVAL '80 days',
     '00000000-0000-4000-a000-000000000001'::UUID)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 32. THESIS ANONYMIZATION TEST DATA
-- ============================================================================
-- Thesis 8: FINISHED, expired retention (6+ years ago) — will be anonymized
INSERT INTO theses (thesis_id, title, type, language, metadata, info, abstract, state,
                    visibility, keywords, application_id, start_date, end_date, created_at,
                    research_group_id)
VALUES
    ('00000000-0000-4000-d000-000000000008'::UUID,
     'Legacy Data Processing Pipeline Evaluation',
     'MASTER', 'ENGLISH',
     '{"titles":{},"credits":{}}',
     'Detailed analysis of data processing pipelines in legacy enterprise systems.',
     'This thesis evaluates legacy data processing pipelines and proposes modernization strategies.',
     'FINISHED', 'PRIVATE',
     ARRAY['data processing', 'legacy systems', 'pipeline evaluation'],
     NULL,
     NOW() - INTERVAL '2400 days', NOW() - INTERVAL '2200 days',
     NOW() - INTERVAL '2400 days',
     '00000000-0000-4000-a000-000000000001'::UUID)
ON CONFLICT DO NOTHING;

-- Thesis 8 roles
INSERT INTO thesis_roles (thesis_id, user_id, role, position, assigned_at, assigned_by)
VALUES
    ('00000000-0000-4000-d000-000000000008'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'student2'), 'STUDENT', 0,
     NOW() - INTERVAL '2400 days', (SELECT user_id FROM users WHERE university_id = 'supervisor')),
    ('00000000-0000-4000-d000-000000000008'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'advisor'), 'ADVISOR', 0,
     NOW() - INTERVAL '2400 days', (SELECT user_id FROM users WHERE university_id = 'supervisor')),
    ('00000000-0000-4000-d000-000000000008'::UUID,
     (SELECT user_id FROM users WHERE university_id = 'supervisor'), 'SUPERVISOR', 0,
     NOW() - INTERVAL '2400 days', (SELECT user_id FROM users WHERE university_id = 'supervisor'))
ON CONFLICT DO NOTHING;

-- Thesis 8 state changes
INSERT INTO thesis_state_changes (thesis_id, state, changed_at)
VALUES
    ('00000000-0000-4000-d000-000000000008'::UUID, 'PROPOSAL', NOW() - INTERVAL '2400 days'),
    ('00000000-0000-4000-d000-000000000008'::UUID, 'WRITING', NOW() - INTERVAL '2380 days'),
    ('00000000-0000-4000-d000-000000000008'::UUID, 'SUBMITTED', NOW() - INTERVAL '2220 days'),
    ('00000000-0000-4000-d000-000000000008'::UUID, 'ASSESSED', NOW() - INTERVAL '2210 days'),
    ('00000000-0000-4000-d000-000000000008'::UUID, 'GRADED', NOW() - INTERVAL '2205 days'),
    ('00000000-0000-4000-d000-000000000008'::UUID, 'FINISHED', NOW() - INTERVAL '2200 days')
ON CONFLICT DO NOTHING;

-- Thesis 8 comment (will be cleaned up during anonymization)
INSERT INTO thesis_comments (comment_id, thesis_id, type, message, filename, upload_name,
                             created_at, created_by)
VALUES
    ('00000000-0000-4000-e200-000000000008'::UUID,
     '00000000-0000-4000-d000-000000000008'::UUID,
     'THESIS',
     'Final review completed. Good work on the pipeline evaluation.',
     NULL, NULL,
     NOW() - INTERVAL '2200 days',
     (SELECT user_id FROM users WHERE university_id = 'advisor'))
ON CONFLICT DO NOTHING;

-- Thesis 8 proposal
INSERT INTO thesis_proposals (proposal_id, thesis_id, proposal_filename, approved_at, approved_by,
                              created_at, created_by)
VALUES
    ('00000000-0000-4000-e000-000000000008'::UUID,
     '00000000-0000-4000-d000-000000000008'::UUID,
     'proposal_legacy_pipeline.pdf',
     NOW() - INTERVAL '2385 days',
     (SELECT user_id FROM users WHERE university_id = 'advisor'),
     NOW() - INTERVAL '2390 days',
     (SELECT user_id FROM users WHERE university_id = 'student2'))
ON CONFLICT DO NOTHING;

-- Thesis 8 assessment
INSERT INTO thesis_assessments (assessment_id, thesis_id, summary, positives, negatives,
                                grade_suggestion, created_at, created_by)
VALUES
    ('00000000-0000-4000-e100-000000000008'::UUID,
     '00000000-0000-4000-d000-000000000008'::UUID,
     'Thorough evaluation of legacy pipeline architectures with practical recommendations.',
     'Comprehensive benchmark suite. Clear methodology. Practical migration guidelines.',
     'Limited coverage of streaming pipelines. Could include more industry case studies.',
     '1.7',
     NOW() - INTERVAL '2210 days',
     (SELECT user_id FROM users WHERE university_id = 'advisor'))
ON CONFLICT DO NOTHING;

-- Thesis 8 final grade
UPDATE theses SET final_grade = '1.7', final_feedback = 'Excellent work on legacy pipeline analysis.'
WHERE thesis_id = '00000000-0000-4000-d000-000000000008'::UUID AND final_grade IS NULL;

