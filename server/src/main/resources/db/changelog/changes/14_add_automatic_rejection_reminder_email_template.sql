--liquibase formatted sql
--changeset ramona:14_add_automatic_rejection_reminder_email_template

INSERT INTO
    email_templates (email_template_id, template_case, subject, body_html, description)
VALUES
    (
        gen_random_uuid (),
        'APPLICATION_AUTOMATIC_REJECT_REMINDER',
        'Automatic Rejection Reminder',
        '<p th:inline="text">Dear [[${recipient.firstName}]],</p>
<p th:inline="text">
This is a reminder that Automatic Application Rejection is enabled for your group.
The following application(s) require your attention:
</p>
<ul th:if="${!#lists.isEmpty(applications)}">
 <li th:each="app : ${applications}">
   <strong th:text="${app.name}">Applicant Name</strong> (applying for: <span th:text="${app.topicTitle}">Topic</span>) – will be rejected on <span th:text="${#temporals.format(app.rejectionDate, ''dd.MM.yyyy'')}">dd.MM.yyyy</span> – <a th:href="@{|${clientHost}/applications/${app.applicationId}|}">Application Link</a>
 </li>
</ul>
<p th:inline="text">
Please review the applications, if you don''t want them to be rejected automatically.
</p>
<p>Best regards,<br/>
The Thesis Coordination Team</p>',
        'This will be send out when you have soon to be automatically rejected applications.'
    );