--liquibase formatted sql
--changeset consistent-email-footer:36-add-email-signature-column
ALTER TABLE research_group_settings ADD COLUMN email_signature TEXT;

--changeset consistent-email-footer:36-replace-best-regards-with-signature-variable
-- Replace hardcoded "Best regards" sign-offs with the emailSignature template variable
UPDATE email_templates SET body_html = REPLACE(body_html,
    '<p>Best regards,<br/>' || chr(10) || 'The Thesis Coordination Team</p>',
    '<div th:utext="${emailSignature}"></div>')
WHERE body_html LIKE '%Best regards%Thesis Coordination Team%';

--changeset consistent-email-footer:36-replace-config-signature-with-email-signature
-- Replace AET ${config.signature} with ${emailSignature}
UPDATE email_templates SET body_html = REPLACE(body_html,
    '<div th:utext="${config.signature}"></div>',
    '<div th:utext="${emailSignature}"></div>')
WHERE body_html LIKE '%config.signature%';

--changeset consistent-email-footer:36-remove-notification-links-multiline
-- Remove multiline notification settings link blocks
UPDATE email_templates SET body_html = REPLACE(body_html,
    '<hr/>' || chr(10) || '<div style="text-align: center;font-size: 10px">' || chr(10) || '    Manage your notification settings <a th:href="${config.clientHost + ''/settings/notifications''}">here</a>' || chr(10) || '</div>' || chr(10) || '<br/><br/>',
    '')
WHERE body_html LIKE '%Manage your notification settings%';

--changeset consistent-email-footer:36-remove-notification-links-inline
-- Remove inline notification settings link blocks (presentation invitation templates)
UPDATE email_templates SET body_html = REPLACE(body_html,
    '<p><hr/>' || chr(10) || '</hr><div style="text-align: center;font-size: 10px"> Manage your notification settings <a th:href="${config.clientHost + ''/settings/notifications''}">here</a></div>',
    '')
WHERE body_html LIKE '%Manage your notification settings%';

--changeset consistent-email-footer:36-remove-trailing-br
-- Remove trailing <br/><br/> from DATA_EXPORT_READY template
UPDATE email_templates SET body_html = REPLACE(body_html,
    '<br/><br/>',
    '')
WHERE template_case = 'DATA_EXPORT_READY' AND body_html LIKE '%<br/><br/>%';

--changeset consistent-email-footer:36-add-signature-to-templates-without
-- Add emailSignature variable to templates that don't have any sign-off
UPDATE email_templates SET body_html = body_html || chr(10) || '<div th:utext="${emailSignature}"></div>'
WHERE body_html NOT LIKE '%emailSignature%'
  AND body_html NOT LIKE '%Best regards%';
