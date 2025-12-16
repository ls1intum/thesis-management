--liquibase formatted sql
--changeset ramona:19_add_interview_invitation_email_template


INSERT INTO
    email_templates (email_template_id, template_case, subject, body_html, description)
VALUES
    (
        gen_random_uuid(),
        'INTERVIEW_INVITATION',
        'Interview Invitation',
        '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
  You have been invited to schedule an interview for the thesis <strong>[[${application.thesisTitle}]]</strong>.
</p>

<p th:inline="text">
  Please follow the link below to choose a suitable time:
</p>

<p th:inline="text">
  <a target="_blank" rel="noopener noreferrer" th:href="${inviteUrl}">Schedule your interview</a>
</p>

<p>Best regards,<br/>
The Thesis Coordination Team</p>',
        'Invitation email sent to interviewees with scheduling link, applicant name and thesis title'
    );