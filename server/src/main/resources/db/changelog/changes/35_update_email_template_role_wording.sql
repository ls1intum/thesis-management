-- liquibase formatted sql

-- changeset thesis-management:35-update-email-template-role-wording
-- Update email template body_html and description fields to use the new role
-- terminology (old "Advisor" → "Supervisor", old "Supervisor" → "Examiner").
-- This ensures existing deployments receive the updated wording. Fresh installs
-- already get correct text from the seed changeset (21).
--
-- Note: Migration 33 already renamed Thymeleaf variables
-- (${thesis.supervisors} → ${thesis.examiners}, ${thesis.advisors} → ${thesis.supervisors})
-- so the REPLACE patterns here match the post-migration-33 state of body_html.

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

-- THESIS_COMMENT_POSTED: Update description (also fixes "its" → "it's" and adds "on")
UPDATE email_templates
SET description = 'New comment on a thesis. TO depends on whether it''s a student or supervisor comment'
WHERE template_case = 'THESIS_COMMENT_POSTED'
  AND research_group_id IS NULL;

-- THESIS_CREATED: Rename role labels (variables already updated by migration 33)
-- Post-migration 33 state: <strong>Supervisor</strong>: [[${thesis.examiners}]] / <strong>Advisor</strong>: [[${thesis.supervisors}]]
-- Two-pass: rename "Supervisor" label to "Examiner" first, then "Advisor" label to "Supervisor"
UPDATE email_templates
SET body_html = REPLACE(
    REPLACE(body_html,
        '<strong>Supervisor</strong>: [[${thesis.examiners}]]',
        '<strong>Examiner</strong>: [[${thesis.examiners}]]'),
    '<strong>Advisor</strong>: [[${thesis.supervisors}]]',
    '<strong>Supervisor</strong>: [[${thesis.supervisors}]]')
WHERE template_case = 'THESIS_CREATED'
  AND research_group_id IS NULL;

-- THESIS_PRESENTATION_INVITATION: Rename role labels in body
-- Post-migration 33 state: Supervisor: [[${thesis.examiners}]]<br>Advisor(s): [[${thesis.supervisors}]]
UPDATE email_templates
SET body_html = REPLACE(body_html,
    'Supervisor: [[${thesis.examiners}]]<br>Advisor(s): [[${thesis.supervisors}]]',
    'Examiner: [[${thesis.examiners}]]<br>Supervisor(s): [[${thesis.supervisors}]]')
WHERE template_case = 'THESIS_PRESENTATION_INVITATION'
  AND research_group_id IS NULL;

-- THESIS_PRESENTATION_INVITATION_CANCELLED: Rename role labels in body
UPDATE email_templates
SET body_html = REPLACE(body_html,
    'Supervisor: [[${thesis.examiners}]]<br>Advisor(s): [[${thesis.supervisors}]]',
    'Examiner: [[${thesis.examiners}]]<br>Supervisor(s): [[${thesis.supervisors}]]')
WHERE template_case = 'THESIS_PRESENTATION_INVITATION_CANCELLED'
  AND research_group_id IS NULL;

-- THESIS_PRESENTATION_INVITATION_UPDATED: Rename role labels in body
UPDATE email_templates
SET body_html = REPLACE(body_html,
    'Supervisor: [[${thesis.examiners}]]<br>Advisor(s): [[${thesis.supervisors}]]',
    'Examiner: [[${thesis.examiners}]]<br>Supervisor(s): [[${thesis.supervisors}]]')
WHERE template_case = 'THESIS_PRESENTATION_INVITATION_UPDATED'
  AND research_group_id IS NULL;
