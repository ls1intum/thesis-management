--liquibase formatted sql

--changeset thesis-management:38-fix-missing-student-groups
-- Backfill student group for users with active theses
INSERT INTO user_groups (user_id, "group")
SELECT DISTINCT tr.user_id, 'student'
FROM thesis_roles tr
JOIN theses t ON tr.thesis_id = t.thesis_id
WHERE tr.role = 'STUDENT'
  AND t.state IN ('PROPOSAL', 'WRITING', 'SUBMITTED', 'ASSESSED', 'GRADED')
  AND NOT EXISTS (
    SELECT 1 FROM user_groups ug
    WHERE ug.user_id = tr.user_id AND ug."group" = 'student'
  );

-- Backfill student group for users with active applications (not yet rejected)
INSERT INTO user_groups (user_id, "group")
SELECT DISTINCT a.user_id, 'student'
FROM applications a
WHERE a.state IN ('NOT_ASSESSED', 'ACCEPTED', 'INTERVIEWING')
  AND NOT EXISTS (
    SELECT 1 FROM user_groups ug
    WHERE ug.user_id = a.user_id AND ug."group" = 'student'
  );
