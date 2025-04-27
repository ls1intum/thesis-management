# Mails

Mails can be customized in the email_templates table in the database or via API calls.
If no research group specific template is found, the default template will be used.

## Templates

| Template Case                                                                                                                                  | TO                             | CC                    | BCC                   | Description                                                                    |
|------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------|-----------------------|-----------------------|--------------------------------------------------------------------------------|
| APPLICATION_ACCEPTED                                             | Application Student            | Supervisor, Advisor   | `MAIL_BCC_RECIPIENTS` | Application was accepted with different advisor and supervisor                 |
| APPLICATION_ACCEPTED_NO_ADVISOR                       | Application Student            | Supervisor, Advisor   | `MAIL_BCC_RECIPIENTS` | Application was accepted with same advisor and supervisor                      |
| APPLICATION_CREATED_CHAIR                                   | Chair Members                  |                       |                       | All supervisors and advisors get a summary about a new application             |
| APPLICATION_CREATED_STUDENT                               | Application User               |                       |                       | Confirmation email to the applying student when application was submitted      |
| APPLICATION_REJECTED                                             | Application User               |                       | `MAIL_BCC_RECIPIENTS` | Application was rejected                                                       |
| APPLICATION_REJECTED_TOPIC_REQUIREMENTS       | Application User               |                       | `MAIL_BCC_RECIPIENTS` | Application was rejected because topic requirements were not met               |
| APPLICATION_REJECTED_STUDENT_REQUIREMENTS   | Application User               |                       | `MAIL_BCC_RECIPIENTS` | Application was rejected because student does not fulfil chair's requirements  |
| APPLICATION_REJECTED_TITLE_NOT_INTERESTING | Application User               |                       | `MAIL_BCC_RECIPIENTS` | Application was rejected because the suggested thesis title is not interesting |
| APPLICATION_REJECTED_TOPIC_FILLED                   | Application User               |                       | `MAIL_BCC_RECIPIENTS` | Application was rejected because topic was closed                              |
| APPLICATION_REJECTED_TOPIC_OUTDATED               | Application User               |                       | `MAIL_BCC_RECIPIENTS` | Application was rejected because topic is outdated                             |
| APPLICATION_REMINDER                                             | Chair Members                  |                       |                       | Weekly email if there are more than 10 unreviewed applications                 |
| THESIS_ASSESSMENT_ADDED                                       | Supervisors                    |                       |                       | Assessment was added to a submitted thesis                                     |
| THESIS_CLOSED                                                           | Students                       | Supervisors, Advisors |                       | Thesis was closed before completion                                            |
| THESIS_COMMENT_POSTED                                           | Students / Advisors            | Supervisors, Advisors |                       | New comment on a thesis. TO depends whether its a student or advisor comment   |
| THESIS_CREATED                                                         | Students                       | Supervisors, Advisors |                       | New thesis was created and assigned to a student                               |
| THESIS_FINAL_GRADE                                                 | Students                       | Supervisors, Advisors |                       | Final grade was added to a thesis                                              |
| THESIS_FINAL_SUBMISSION                                       | Advisors                       | Supervisors           |                       | Student submitted final thesis                                                 |
| THESIS_PRESENTATION_DELETED                               | Students                       | Supervisors, Advisors |                       | Scheduled presentation was deleted                                             |
| THESIS_PRESENTATION_SCHEDULED                           | Students                       | Supervisors, Advisors |                       | New presentation was scheduled                                                 |
| THESIS_PRESENTATION_UPDATED                               | Students                       | Supervisors, Advisors |                       | Presentation was updated                                                       |
| THESIS_PRESENTATION_INVITATION                         | Chair Members, Thesis Students |                       |                       | Public Presentation Invitation                                                 |
| THESIS_PRESENTATION_INVITATION_CANCELLED     | Chair Members, Thesis Students |                       |                       | Public Presentation was deleted                                                |
| THESIS_PRESENTATION_INVITATION_UPDATED         | Chair Members, Thesis Students |                       |                       | Public Presentation was updated                                                |
| THESIS_PROPOSAL_ACCEPTED                                     | Students                       | Supervisors, Advisors |                       | Proposal was accepted                                                          |
| THESIS_PROPOSAL_REJECTED                                     | Students                       | Student               |                       | Changes were requested for proposal                                            |
| THESIS_PROPOSAL_UPLOADED                                     | Advisors                       | Supervisors, Advisors |                       | Student uploaded new proposal                                                  |