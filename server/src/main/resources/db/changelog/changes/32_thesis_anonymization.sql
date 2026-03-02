-- ============================================================================
-- 32: Add thesis anonymization columns and email template
-- ============================================================================

-- Add anonymization tracking columns to theses table
ALTER TABLE theses ADD COLUMN anonymized_at TIMESTAMP;
ALTER TABLE theses ADD COLUMN anonymization_notified_at TIMESTAMP;
CREATE INDEX idx_theses_anonymized_at ON theses (anonymized_at);

-- Seed the anonymization reminder email template (global, no research group)
INSERT INTO email_templates (email_template_id, research_group_id, template_case, description, subject, body_html, language, created_at, updated_at, updated_by)
VALUES (
    gen_random_uuid(),
    NULL,
    'THESIS_ANONYMIZATION_REMINDER',
    'Sent to research group heads when theses are approaching the end of the retention period and will be anonymized.',
    'Upcoming Thesis Anonymization Notice',
    '<p>Dear {{recipient}},</p>
<p>The following theses in the <strong>{{researchGroupName}}</strong> research group will be anonymized on <strong>{{anonymizationDate}}</strong> as part of the data retention policy (5-year retention period):</p>
<ul>
{{#each theses}}
<li>{{this}}</li>
{{/each}}
</ul>
<p>After anonymization, personal data (files, comments, assessments, feedback, and role assignments) will be permanently removed. The thesis record (title, type, grade, and dates) will be preserved for statistical purposes.</p>
<p>If you have any concerns, please contact the system administrator before the anonymization date.</p>',
    'en',
    NOW(),
    NOW(),
    NULL
) ON CONFLICT DO NOTHING;
