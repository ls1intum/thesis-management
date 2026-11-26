--liquibase formatted sql
--changeset consistent-email-footer:49-add-email-signature-column
ALTER TABLE research_group_settings ADD COLUMN email_signature TEXT;

--changeset consistent-email-footer:49-replace-best-regards-with-signature-variable
-- Replace hardcoded "Best regards" sign-offs with the emailSignature template variable.
-- Matches the seeded form and equivalent formatting (<br/>, <br />, <br>, LF or CRLF line breaks).
UPDATE email_templates SET body_html = regexp_replace(body_html,
    '<p>\s*Best regards,\s*<br\s*/?>\s*The Thesis Coordination Team\s*</p>',
    '<div th:utext="${emailSignature}"></div>',
    'g')
WHERE body_html ~ '<p>\s*Best regards,\s*<br\s*/?>\s*The Thesis Coordination Team\s*</p>';

--changeset consistent-email-footer:49-replace-config-signature-with-email-signature
-- Replace AET ${config.signature} with ${emailSignature}
UPDATE email_templates SET body_html = REPLACE(body_html,
    '<div th:utext="${config.signature}"></div>',
    '<div th:utext="${emailSignature}"></div>')
WHERE body_html LIKE '%config.signature%';

--changeset consistent-email-footer:49-remove-notification-links-multiline
-- Remove multiline notification settings link blocks
UPDATE email_templates SET body_html = REPLACE(body_html,
    '<hr/>' || chr(10) || '<div style="text-align: center;font-size: 10px">' || chr(10) || '    Manage your notification settings <a th:href="${config.clientHost + ''/settings/notifications''}">here</a>' || chr(10) || '</div>' || chr(10) || '<br/><br/>',
    '')
WHERE body_html LIKE '%Manage your notification settings%';

--changeset consistent-email-footer:49-remove-notification-links-inline
-- Remove inline notification settings link blocks (presentation invitation templates)
UPDATE email_templates SET body_html = REPLACE(body_html,
    '<p><hr/>' || chr(10) || '</hr><div style="text-align: center;font-size: 10px"> Manage your notification settings <a th:href="${config.clientHost + ''/settings/notifications''}">here</a></div>',
    '')
WHERE body_html LIKE '%Manage your notification settings%';

--changeset consistent-email-footer:49-remove-standalone-notification-footer
-- Remove the standalone "Notification URL Footer" snippet that the template editor used to offer
-- (it starts with a stray closing hr tag, without the surrounding <p><hr/> of the seeded templates).
-- MailBuilder.send() now appends the notification link itself. Must run after the two steps above.
UPDATE email_templates SET body_html = REPLACE(body_html,
    '</hr><div style="text-align: center;font-size: 10px"> Manage your notification settings <a th:href="${config.clientHost + ''/settings/notifications''}">here</a></div>',
    '')
WHERE body_html LIKE '%Manage your notification settings%';

--changeset consistent-email-footer:49-remove-trailing-br
-- Remove trailing <br/><br/> from DATA_EXPORT_READY template
UPDATE email_templates SET body_html = REPLACE(body_html,
    '<br/><br/>',
    '')
WHERE template_case = 'DATA_EXPORT_READY' AND body_html LIKE '%<br/><br/>%';

--changeset consistent-email-footer:49-add-signature-to-templates-without
-- Add emailSignature variable to templates that don't have any sign-off
UPDATE email_templates SET body_html = body_html || chr(10) || '<div th:utext="${emailSignature}"></div>'
WHERE body_html NOT LIKE '%emailSignature%'
  AND body_html NOT LIKE '%Best regards%';
