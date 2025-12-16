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

INSERT INTO
    email_templates (email_template_id, template_case, subject, body_html, description)
VALUES
    (
        gen_random_uuid(),
        'INTERVIEW_INVITATION_REMINDER',
        'Interview Invitation Reminder',
        '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
  This is a reminder that you were invited to schedule an interview for the thesis <strong>[[${application.thesisTitle}]]</strong>.
</p>

<p th:inline="text">
  If you haven''t scheduled yet, please follow the link below to choose a suitable time:
</p>

<p th:inline="text">
  <a target="_blank" rel="noopener noreferrer" th:href="${inviteUrl}">Schedule your interview</a>
</p>

<p th:inline="text">
  If you already scheduled your interview, please ignore this reminder.
</p>

<p>Best regards,<br/>
The Thesis Coordination Team</p>',
        'Reminder email sent when resending the interview scheduling invitation'
    );

INSERT INTO
    email_templates (email_template_id, template_case, subject, body_html, description)
VALUES
    (
        gen_random_uuid(),
        'INTERVIEW_SLOT_BOOKED_CONFORMATION',
        'Interview Slot Booking Confirmation',
        '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
  Your interview slot for the interview process <strong>[[${application.thesisTitle}]]</strong> has been booked.
</p>

<p th:inline="text">
  <strong>Details</strong><br/>
  Date and time: <strong th:text="${DataFormatter.formatDateTime(slot.startDate)}">dd.MM.yyyy HH:mm</strong><br/>
  <span th:if="${slot.location != null and !#strings.isEmpty(inviteUrl)}">
  Location: <span th:text="${slot.location}">Location</span><br/>
</span>
<span th:unless="${slot.location != null and !#strings.isEmpty(slot.location)}">
  Location not available yet<br/>
</span>
</p>

<p th:inline="text">
  You can view or manage your booking using the link below:
</p>

<p th:inline="text">
  <a target="_blank" rel="noopener noreferrer" th:href="${slotBookingUrl}">View or change your booking</a>
</p>

<p>Best regards,<br/>
The Thesis Coordination Team</p>',
        'Confirmation email sent to interviewees when a slot was booked'
    );