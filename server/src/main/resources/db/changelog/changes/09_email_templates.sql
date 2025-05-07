--liquibase formatted sql
--changeset marc:09-email-templates-1
CREATE TABLE email_templates
(
    email_template_id UUID PRIMARY KEY,
    research_group_id UUID REFERENCES research_groups (research_group_id) ON DELETE CASCADE,
    template_case     VARCHAR(255) NOT NULL,
    description       TEXT,
    subject           TEXT         NOT NULL,
    body_html         TEXT         NOT NULL,
    language          VARCHAR(5)   NOT NULL DEFAULT 'en',
    created_at        TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_by        UUID REFERENCES users (user_id) ON DELETE SET NULL,
    updated_at        TIMESTAMP             DEFAULT CURRENT_TIMESTAMP
);

--changeset marc:09-email-templates-2
CREATE UNIQUE INDEX uq_template_per_group
    ON email_templates (research_group_id, template_case, language)
    WHERE research_group_id IS NOT NULL;

CREATE UNIQUE INDEX uq_template_default
    ON email_templates (template_case, language)
    WHERE research_group_id IS NULL;