-- liquibase formatted sql

-- changeset thesis-management:33-rename-thesis-roles
-- Rename backend roles to match UI labels:
--   old SUPERVISOR (UI "Examiner") → new EXAMINER
--   old ADVISOR    (UI "Supervisor") → new SUPERVISOR
--
-- Two-pass strategy because SUPERVISOR is both an old name and a new name.

-- Pass 1: free the SUPERVISOR name by renaming it to EXAMINER
UPDATE thesis_roles SET role = 'EXAMINER' WHERE role = 'SUPERVISOR';
UPDATE topic_roles SET role = 'EXAMINER' WHERE role = 'SUPERVISOR';

-- Pass 2: ADVISOR → SUPERVISOR (now safe)
UPDATE thesis_roles SET role = 'SUPERVISOR' WHERE role = 'ADVISOR';
UPDATE topic_roles SET role = 'SUPERVISOR' WHERE role = 'ADVISOR';

-- Comment type rename
UPDATE thesis_comments SET type = 'SUPERVISOR' WHERE type = 'ADVISOR';

-- Email template variables (order matters: supervisors first, then advisors)
UPDATE email_templates SET body_html = REPLACE(body_html, '${thesis.supervisors}', '${thesis.examiners}') WHERE body_html LIKE '%${thesis.supervisors}%';
UPDATE email_templates SET body_html = REPLACE(body_html, '${thesis.advisors}', '${thesis.supervisors}') WHERE body_html LIKE '%${thesis.advisors}%';
