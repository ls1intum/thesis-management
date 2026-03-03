-- liquibase formatted sql

-- changeset thesis-management:35-update-email-template-role-wording
-- Update email template body_html and description fields to use the new role
-- terminology (old "Advisor" → "Supervisor", old "Supervisor" → "Examiner").
-- This ensures existing deployments receive the updated wording. Fresh installs
-- already get correct text from the seed changeset (21).

-- APPLICATION_ACCEPTED: Update description and body wording
UPDATE email_templates
SET description = REPLACE(description, 'different advisor and supervisor', 'different supervisor and examiner'),
    body_html = REPLACE(body_html, 'would be your advisor', 'would be your supervisor')
WHERE template_case = 'APPLICATION_ACCEPTED'
  AND research_group_id IS NULL;

-- APPLICATION_ACCEPTED_NO_SUPERVISOR (formerly NO_ADVISOR): Update description
UPDATE email_templates
SET description = REPLACE(description, 'same advisor and supervisor', 'same supervisor and examiner')
WHERE template_case = 'APPLICATION_ACCEPTED_NO_SUPERVISOR'
  AND research_group_id IS NULL;

-- APPLICATION_CREATED_CHAIR: Update description
UPDATE email_templates
SET description = REPLACE(description, 'All supervisors and advisors get a summary', 'All examiners and supervisors get a summary')
WHERE template_case = 'APPLICATION_CREATED_CHAIR'
  AND research_group_id IS NULL;

-- APPLICATION_REJECTED (all rejection variants): Update body wording
UPDATE email_templates
SET body_html = REPLACE(body_html, 'contact your advisor or supervisor', 'contact your supervisor or examiner')
WHERE template_case LIKE 'APPLICATION_REJECTED%'
  AND research_group_id IS NULL;

-- THESIS_COMMENT_POSTED: Update description (also fixes "its" → "it's")
UPDATE email_templates
SET description = 'New comment on a thesis. TO depends on whether it''s a student or supervisor comment'
WHERE template_case = 'THESIS_COMMENT_POSTED'
  AND research_group_id IS NULL;

-- THESIS_CREATED: Update body — replace "Advisor" label with "Examiner" and variable
UPDATE email_templates
SET body_html = REPLACE(REPLACE(body_html, '<strong>Advisor</strong>: [[${thesis.advisors}]]', '<strong>Examiner</strong>: [[${thesis.examiners}]]'), '${thesis.advisors}', '${thesis.examiners}')
WHERE template_case = 'THESIS_CREATED'
  AND research_group_id IS NULL;

-- THESIS_FINAL_GRADE: Update body — replace supervisors variable with examiners
UPDATE email_templates
SET body_html = REPLACE(body_html, '[[${thesis.supervisors}]] added the final grade', '[[${thesis.examiners}]] added the final grade')
WHERE template_case = 'THESIS_FINAL_GRADE'
  AND research_group_id IS NULL;

-- THESIS_PRESENTATION_INVITATION: Update body — rename role labels
UPDATE email_templates
SET body_html = REPLACE(REPLACE(body_html, 'Supervisor: [[${thesis.supervisors}]]<br>Advisor(s): [[${thesis.advisors}]]', 'Examiner: [[${thesis.examiners}]]<br>Supervisor(s): [[${thesis.supervisors}]]'), '${thesis.advisors}', '${thesis.supervisors}')
WHERE template_case = 'THESIS_PRESENTATION_INVITATION'
  AND research_group_id IS NULL;

-- THESIS_PRESENTATION_INVITATION_CANCELLED: Update body — rename role labels
UPDATE email_templates
SET body_html = REPLACE(REPLACE(body_html, 'Supervisor: [[${thesis.supervisors}]]<br>Advisor(s): [[${thesis.advisors}]]', 'Examiner: [[${thesis.examiners}]]<br>Supervisor(s): [[${thesis.supervisors}]]'), '${thesis.advisors}', '${thesis.supervisors}')
WHERE template_case = 'THESIS_PRESENTATION_INVITATION_CANCELLED'
  AND research_group_id IS NULL;

-- THESIS_PRESENTATION_INVITATION_UPDATED: Update body — rename role labels
UPDATE email_templates
SET body_html = REPLACE(REPLACE(body_html, 'Supervisor: [[${thesis.supervisors}]]<br>Advisor(s): [[${thesis.advisors}]]', 'Examiner: [[${thesis.examiners}]]<br>Supervisor(s): [[${thesis.supervisors}]]'), '${thesis.advisors}', '${thesis.supervisors}')
WHERE template_case = 'THESIS_PRESENTATION_INVITATION_UPDATED'
  AND research_group_id IS NULL;
