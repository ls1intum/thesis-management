--liquibase formatted sql
--changeset pat:40_normalize_date_format_in_rejection_reminder

UPDATE email_templates
SET body_html = REPLACE(
  body_html,
  '#temporals.format(app.rejectionDate, ''dd.MM.yyyy'')',
  '#temporals.format(app.rejectionDate, ''yyyy-MM-dd'')'
)
WHERE body_html LIKE '%#temporals.format(app.rejectionDate, ''dd.MM.yyyy'')%';
