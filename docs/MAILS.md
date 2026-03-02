# Mails

Mails can be customized in the email_templates table in the database or via API calls.
If no research group specific template is found, the default template will be used.

> **Note on Role Terminology:** The server uses `EXAMINER` and `SUPERVISOR` as thesis role names, matching the UI labels "Examiner" and "Supervisor". Keycloak groups remain `supervisor` and `advisor` for backward compatibility.

## Local Testing with Mailpit

For local development, all emails are captured by [Mailpit](https://github.com/axllent/mailpit) which is included in the dev Docker Compose setup. Start it with `docker compose up -d` and open **http://localhost:8025** to inspect emails including their content, recipients, and attachments. See [DEVELOPMENT.md](DEVELOPMENT.md#email-mailpit) for setup details.

## Templates

| Template Case                                                                                                                                  | TO                             | CC                    | BCC                   | Description                                                                    |
|------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------|-----------------------|-----------------------|--------------------------------------------------------------------------------|
| APPLICATION_ACCEPTED                                             | Application Student            | Examiner, Supervisor  | Research Group Head | Application was accepted with different supervisor and examiner                |
| APPLICATION_ACCEPTED_NO_SUPERVISOR                    | Application Student            | Examiner, Supervisor  | Research Group Head | Application was accepted with same supervisor and examiner                     |
| APPLICATION_CREATED_CHAIR                                   | Chair Members                  |                       |                       | All examiners and supervisors get a summary about a new application            |
| APPLICATION_CREATED_STUDENT                               | Application User               |                       |                       | Confirmation email to the applying student when application was submitted      |
| APPLICATION_REJECTED                                             | Application User               |                       | Research Group Head | Application was rejected                                                       |
| APPLICATION_REJECTED_TOPIC_REQUIREMENTS       | Application User               |                       | Research Group Head | Application was rejected because topic requirements were not met               |
| APPLICATION_REJECTED_STUDENT_REQUIREMENTS   | Application User               |                       | Research Group Head | Application was rejected because student does not fulfil chair's requirements  |
| APPLICATION_REJECTED_TITLE_NOT_INTERESTING | Application User               |                       | Research Group Head | Application was rejected because the suggested thesis title is not interesting |
| APPLICATION_REJECTED_TOPIC_FILLED                   | Application User               |                       | Research Group Head | Application was rejected because topic was closed                              |
| APPLICATION_REJECTED_TOPIC_OUTDATED               | Application User               |                       | Research Group Head | Application was rejected because topic is outdated                             |
| APPLICATION_REMINDER                                             | Chair Members                  |                       |                       | Weekly email if there are more than 10 unreviewed applications                 |
| THESIS_ASSESSMENT_ADDED                                       | Examiners                      |                       |                       | Assessment was added to a submitted thesis                                     |
| THESIS_CLOSED                                                           | Students                       | Examiners, Supervisors|                       | Thesis was closed before completion                                            |
| THESIS_COMMENT_POSTED                                           | Students / Supervisors         | Examiners, Supervisors|                       | New comment on a thesis. TO depends whether its a student or supervisor comment|
| THESIS_CREATED                                                         | Students                       | Examiners, Supervisors|                       | New thesis was created and assigned to a student                               |
| THESIS_FINAL_GRADE                                                 | Students                       | Examiners, Supervisors|                       | Final grade was added to a thesis                                              |
| THESIS_FINAL_SUBMISSION                                       | Supervisors                    | Examiners             |                       | Student submitted final thesis                                                 |
| THESIS_PRESENTATION_DELETED                               | Students                       | Examiners, Supervisors|                       | Scheduled presentation was deleted                                             |
| THESIS_PRESENTATION_SCHEDULED                           | Students                       | Examiners, Supervisors|                       | New presentation was scheduled                                                 |
| THESIS_PRESENTATION_UPDATED                               | Students                       | Examiners, Supervisors|                       | Presentation was updated                                                       |
| THESIS_PRESENTATION_INVITATION                         | Chair Members, Thesis Students |                       |                       | Public Presentation Invitation                                                 |
| THESIS_PRESENTATION_INVITATION_CANCELLED     | Chair Members, Thesis Students |                       |                       | Public Presentation was deleted                                                |
| THESIS_PRESENTATION_INVITATION_UPDATED         | Chair Members, Thesis Students |                       |                       | Public Presentation was updated                                                |
| THESIS_PROPOSAL_ACCEPTED                                     | Students                       | Examiners, Supervisors|                       | Proposal was accepted                                                          |
| THESIS_PROPOSAL_REJECTED                                     | Students                       | Student               |                       | Changes were requested for proposal                                            |
| THESIS_PROPOSAL_UPLOADED                                     | Supervisors                    | Examiners, Supervisors|                       | Student uploaded new proposal                                                  |
