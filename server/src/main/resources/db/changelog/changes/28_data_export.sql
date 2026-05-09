--liquibase formatted sql

--changeset data-export:28-create-data-exports-table
CREATE TABLE data_exports (
    data_export_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id),
    state TEXT NOT NULL DEFAULT 'REQUESTED',
    file_path TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    creation_finished_at TIMESTAMP,
    downloaded_at TIMESTAMP
);

--changeset data-export:28-add-data-export-ready-email-template
INSERT INTO email_templates (email_template_id, research_group_id, template_case, subject, body_html, language, description, created_at, updated_by, updated_at)
VALUES (gen_random_uuid(), NULL, 'DATA_EXPORT_READY', 'Your Data Export is Ready',
'<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
Your personal data export has been generated and is ready for download. You can download it from your data export page:
</p>

<p>
<a target="_blank" rel="noopener noreferrer nofollow" th:href="${downloadUrl}" th:text="${downloadUrl}"></a>
</p>

<p>
Please note that the download link will expire in 7 days. After that, you can request a new export.
</p>

<br/><br/>', 'en', 'Notification when data export is ready for download', NOW(), NULL, NOW())
ON CONFLICT (template_case, language) WHERE research_group_id IS NULL DO NOTHING;
