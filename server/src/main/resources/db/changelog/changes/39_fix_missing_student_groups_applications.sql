--liquibase formatted sql

--changeset thesis-management:39-fix-missing-student-groups-applications
INSERT INTO user_groups (user_id, "group")
SELECT DISTINCT a.user_id, 'student'
FROM applications a
WHERE a.state IN ('NOT_ASSESSED', 'ACCEPTED', 'INTERVIEWING')
  AND NOT EXISTS (
    SELECT 1 FROM user_groups ug
    WHERE ug.user_id = a.user_id AND ug."group" = 'student'
  );
