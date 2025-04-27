-- ============================================================================
-- Manual setup script to insert email templates for the "Applied Education Technologies (AET)"
-- research group at the Technical University of Munich (TUM).
--
-- This script:
--   - Inserts all default email templates used by the AET group,
--   - Uses Thymeleaf file-based templates by referencing their path in the HTML,
--   - Should be executed manually on DEV and PROD environments.
--
-- IMPORTANT:
-- This script is NOT part of the standard Liquibase migration process.
--
-- Author: Marc Fett
-- Date: 2025/04/21
-- ============================================================================


WITH defaults
         AS (SELECT '4d5c6f1b-6e83-4e5e-9f6a-2657dc724aec'::uuid AS research_group_id, -- replace with correct uuid of the AET group
                    'en'::text                                   AS language,
                    NOW()                                        AS created_at,
                    NOW()                                        AS updated_at,
                    NULL ::uuid                                  AS updated_by),
     templates
         AS (SELECT gen_random_uuid() AS email_template_id,
                    d.research_group_id,
                    v.template_case,
                    v.subject,
                    v.body_html,
                    d.language,
                    v.description,
                    d.created_at,
                    d.updated_by,
                    d.updated_at
             FROM defaults d
                      CROSS JOIN (VALUES ('APPLICATION_ACCEPTED', 'Thesis Application Acceptance', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
  I am delighted to inform you that I would like to take the next steps in
  supervising your thesis. This includes writing a proposal and familiarizing yourself with the development environment
  independently. These steps are crucial to ensure that you are well-prepared to start the project.
</p>

<p th:inline="text">
  [[${advisor.firstName}]]&nbsp;[[${advisor.lastName}]] would be your advisor. Please coordinate the
  next steps with [[${advisor.firstName}]] [[${advisor.lastName}]] using the following link:
  <a target="_blank" rel="noopener noreferrer nofollow" th:href="${config.workspaceUrl}">[[${config.workspaceUrl}]]</a>
</p>

<p th:inline="text">
  I would like to emphasize that in undertaking this thesis, you assume the role of project manager for
  your thesis project. This role requires <b>proactive communication</b>, high dedication, and a strong commitment to the
  successful completion of the project. I have full confidence in your ability to rise to this challenge and produce
  exemplary work.
</p>

<p th:inline="text">
  I am excited about the opportunity to work with you and am eager to see the contributions you
  will make to our field. Please reach out if you have any questions or need further clarification on any
  aspect of the project.
</p>

<p th:inline="text">
  You can view your thesis details and tasks on: <a target="_blank" rel="noopener noreferrer nofollow" th:href="${thesisUrl}">[[${thesisUrl}]]</a>
</p>

<div th:utext="${config.signature}"></div>
', 'Application was accepted with different advisor and supervisor'),
                                         ('APPLICATION_ACCEPTED_NO_ADVISOR', 'Thesis Application Acceptance', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
I am delighted to inform you that I would like to take the next steps in
supervising your thesis. This includes writing a proposal and familiarizing yourself with the development environment
independently. These steps are crucial to ensure that you are well-prepared to start the project.
</p>
<p th:inline="text">
Please coordinate the next steps with me using the following link:
<a target="_blank" rel="noopener noreferrer nofollow" th:href="${config.workspaceUrl}">[[${config.workspaceUrl}]]</a>
</p>
<p th:inline="text">
I would like to emphasize that in undertaking this thesis, you assume the role of project manager for
your thesis project. This role requires <b>proactive communication</b>, high dedication, and a strong commitment to the
successful completion of the project. I have full confidence in your ability to rise to this challenge and produce
exemplary work.
</p>
<p th:inline="text">
I am excited about the opportunity to work with you and am eager to see the contributions you will make to our field.
Please reach out if you have any questions or need further clarification on any aspect of the
project.
</p>

<p th:inline="text">
You can view your thesis details and tasks on: <a target="_blank" rel="noopener noreferrer nofollow" th:href="${thesisUrl}">[[${thesisUrl}]]</a>
</p>

<div th:utext="${config.signature}"></div>', 'Application was accepted with same advisor and supervisor'),
                                         ('APPLICATION_CREATED_CHAIR', 'New Thesis Application', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">there is a new thesis application by <strong>[[${application.user.firstName}]]&nbsp;[[${application.user.lastName}]]</strong>.</p>
<p th:inline="text">We received the following thesis application details:</p>

<hr/>

<p th:inline="text">
<strong>Name:</strong><br/>
[[${application.user.firstName}]]&nbsp;[[${application.user.lastName}]]
</p>
<p th:inline="text">
<strong>Email:</strong><br/>
[[${application.user.email}]]
</p>
<p th:inline="text">
<strong>University ID:</strong><br/>
[[${application.user.universityId}]]
</p>
<p th:inline="text">
<strong>Matriculation Number:</strong><br/>
[[${application.user.matriculationNumber}]]
</p>
<p th:inline="text">
<strong>Study program:</strong><br/>
[[${DataFormatter.formatConstantName(application.user.studyProgram)}]]&nbsp;
[[${DataFormatter.formatConstantName(application.user.studyDegree)}]]&nbsp;
(Semester [[${DataFormatter.formatSemester(application.user.enrolledAt)}]])
</p>
<p th:inline="text">
<strong>Thesis Title:</strong><br/>
[[${application.thesisTitle}]]
</p>
<p th:inline="text">
<strong>Desired Thesis Start Date:</strong><br/>
[[${DataFormatter.formatDate(application.desiredStartDate)}]]
</p>
<p>
<strong>Motivation:</strong><br/>
<span th:utext="${application.motivation}"></span>
</p>
<br/>
<p>
<strong>Special Skills:</strong><br/>
<span th:utext="${application.user.specialSkills}"></span>
</p>
<br/>
<p>
<strong>Interests:</strong><br/>
<span th:utext="${application.user.interests}"></span>
</p>
<br/>
<p>
<strong>Projects:</strong><br/>
<span th:utext="${application.user.projects}"></span>
</p>
<br/>

<p th:inline="text">
Full Details: <a target="_blank" rel="noopener noreferrer nofollow" th:href="${applicationUrl}">[[${applicationUrl}]]</a>
</p>

<p th:inline="text"><strong>You can find the submitted files in the attachment part of this email.</strong></p>

<hr/>
<div style="text-align: center;font-size: 10px">
    Manage your notification settings <a th:href="${config.clientHost + ''/settings/notifications''}">here</a>
</div>
<br/><br/>',
                                          'All supervisors and advisors get a summary about a new application'),
                                         ('APPLICATION_CREATED_STUDENT', 'Thesis Application Confirmation', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">With this email, we confirm your thesis application.</p>
<p th:inline="text">We received the following details:</p>

<hr>

<p th:inline="text">
<strong>Name:</strong><br/>
[[${application.user.firstName}]]&nbsp;[[${application.user.lastName}]]
</p>
<p th:inline="text">
<strong>Email:</strong><br/>
[[${application.user.email}]]
</p>
<p th:inline="text">
<strong>University ID:</strong><br/>
[[${application.user.universityId}]]
</p>
<p th:inline="text">
<strong>Matriculation Number:</strong><br/>
[[${application.user.matriculationNumber}]]
</p>
<p th:inline="text">
<strong>Study program:</strong><br/>
[[${DataFormatter.formatConstantName(application.user.studyProgram)}]]&nbsp;
[[${DataFormatter.formatConstantName(application.user.studyDegree)}]]&nbsp;
(Semester [[${DataFormatter.formatSemester(application.user.enrolledAt)}]])
</p>
<p th:inline="text">
<strong>Thesis Title:</strong><br/>
[[${application.thesisTitle}]]
</p>
<p th:inline="text">
<strong>Desired Thesis Start Date:</strong><br/>
[[${DataFormatter.formatDate(application.desiredStartDate)}]]
</p>
<p>
<strong>Motivation:</strong><br/>
<span th:utext="${application.motivation}"></span>
</p>
<br/>
<p>
<strong>Special Skills:</strong><br/>
<span th:utext="${application.user.specialSkills}"></span>
</p>
<br/>
<p>
<strong>Interests:</strong><br/>
<span th:utext="${application.user.interests}"></span>
</p>
<br/>
<p>
<strong>Projects:</strong><br/>
<span th:utext="${application.user.projects}"></span>
</p>
<br/>

<p th:inline="text">
We are currently experiencing a high volume of thesis applications, and each one requires careful review.
While we aim to respond as quickly as possible, the combination of the application volume and the intensive teaching
and research commitments of our group may result in a response time of up to four weeks.
We appreciate your patience and understanding during this period.
</p>

<p th:inline="text"><strong>You can find the submitted files in the attachment part of this email.</strong></p>',
                                          'Confirmation email to the applying student when application was submitted'),
                                         ('APPLICATION_REJECTED', 'Thesis Application Rejection', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
Thank you for your interest in pursuing your thesis under my supervision.
I have carefully reviewed your application and supporting documents.
It is with regret that I inform you that I am unable to supervise your thesis.
The volume of applications received this year was exceptionally high, and I have limited capacity to ensure each student receives the appropriate level of support and guidance.
</p>
<p th:inline="text">
I recommend reaching out to other faculty members who may align more closely with your qualifications and area of interest.
</p>

<div th:utext="${config.signature}"></div>', 'Application was rejected'),
                                         ('APPLICATION_REJECTED_TOPIC_REQUIREMENTS', 'Thesis Application Rejection', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
Thank you for your interest in pursuing your thesis under my supervision.
After reviewing your application and supporting documents, I regret to inform you that you do not meet the necessary requirements for the chosen thesis topic.
</p>
<p th:inline="text">
I recommend considering other thesis topics or reaching out to faculty members whose research focus may better align with your qualifications.
</p>

<div th:utext="${config.signature}"></div>', 'Application was rejected because topic requirements were not met'),
                                         ('APPLICATION_REJECTED_STUDENT_REQUIREMENTS', 'Thesis Application Rejection', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
Thank you for your interest in pursuing your thesis under my supervision.
I have carefully reviewed your application and supporting documents.
Unfortunately, I must inform you that you do not currently meet the necessary requirements for thesis supervision under my guidance.
</p>
<p th:inline="text">
I recommend reaching out to other faculty members who may align more closely with your qualifications and area of interest.
</p>

<div th:utext="${config.signature}"></div>',
                                          'Application was rejected because student does not fulfil chair''s requirements'),
                                         ('APPLICATION_REJECTED_TITLE_NOT_INTERESTING', 'Thesis Application Rejection', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
Thank you for your interest in pursuing your thesis under my supervision.
I have carefully reviewed your proposed thesis.
However, the suggested topic does not align with the current research interests of my group, and I am unable to supervise your thesis on this basis
</p>
<p th:inline="text">
I encourage you to explore other faculty members whose research is more closely related to your proposed area of study.
</p>

<div th:utext="${config.signature}"></div>',
                                          'Application was rejected because the suggested thesis title is not interesting'),
                                         ('APPLICATION_REJECTED_TOPIC_FILLED', 'Thesis Application Rejection', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
Thank you for your interest in pursuing your thesis under my supervision.
</p>

<p th:inline="text">
We found a student for the specific topic you applied for.
</p>

<p th:inline="text">
You can explore other topics or suggest a topic yourself in your area of interest.
</p>

<div th:utext="${config.signature}"></div>', 'Application was rejected because topic was closed'),
                                         ('APPLICATION_REJECTED_TOPIC_OUTDATED', 'Thesis Application Rejection', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
Thank you for your interest in pursuing your thesis under my supervision.
</p>

<p th:inline="text">
I wanted to inform you that the topic you applied for is no longer available.
</p>

<p th:inline="text">
You can explore other topics or suggest a topic yourself in your area of interest.
</p>

<div th:utext="${config.signature}"></div>', 'Application was rejected because topic is outdated'),
                                         ('APPLICATION_REMINDER', 'Unreviewed Thesis Applications', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
There are currently <strong>[[${unreviewedApplications}]]</strong> unreviewed thesis applications.
</p>

<p th:inline="text">
Review Applications: <a target="_blank" rel="noopener noreferrer nofollow" th:href="${reviewApplicationsLink}">[[${reviewApplicationsLink}]]</a>
</p>

<hr/>
<div style="text-align: center;font-size: 10px">
    Manage your notification settings <a th:href="${config.clientHost + ''/settings/notifications''}">here</a>
</div>
<br/><br/>',
                                          'Weekly email if there are more than 10 unreviewed applications'),
                                         ('THESIS_ASSESSMENT_ADDED', 'Assessment added', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
[[${assessment.createdBy.firstName}]] [[${assessment.createdBy.lastName}]] added an assessment to thesis "[[${thesis.title}]]"
</p>

<p >
<strong>Summary</strong><br />
<span th:utext="${assessment.summary}"></span>
</p>

<p>
<strong>Positives</strong><br />
<span th:utext="${assessment.positives}"></span>
</p>

<p>
<strong>Negatives</strong><br />
<span th:utext="${assessment.negatives}"></span>
</p>

<p th:inline="text">
<strong>Grade Suggestion</strong>: [[${assessment.gradeSuggestion}]]
</p>

<p th:inline="text">
    Full Details: <a target="_blank" rel="noopener noreferrer nofollow" th:href="${thesisUrl}" th:text="${thesisUrl}"></a>
</p>

<hr/>
<div style="text-align: center;font-size: 10px">
    Manage your notification settings <a th:href="${config.clientHost + ''/settings/notifications''}">here</a>
</div>
<br/><br/>', 'Assessment was added to a submitted thesis'),
                                         ('THESIS_CLOSED', 'Thesis Closed', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
[[${deletingUser.firstName}]] [[${deletingUser.lastName}]] closed thesis "[[${thesis.title}]]".
Please contact your advisor or supervisor if you think that this was a mistake.
</p>

<p th:inline="text">
    Full Details: <a target="_blank" rel="noopener noreferrer nofollow" th:href="${thesisUrl}" th:text="${thesisUrl}"></a>
</p>

<hr/>
<div style="text-align: center;font-size: 10px">
    Manage your notification settings <a th:href="${config.clientHost + ''/settings/notifications''}">here</a>
</div>
<br/><br/>', 'Thesis was closed before completion'),
                                         ('THESIS_COMMENT_POSTED', 'A Comment was posted', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
[[${comment.createdBy.firstName}]] [[${comment.createdBy.lastName}]] posted a comment on thesis "[[${thesis.title}]]"
</p>

<p th:inline="text">
<strong>Message</strong><br />
[[${comment.message}]]
</p>

<p th:inline="text">
    Full Details: <a target="_blank" rel="noopener noreferrer nofollow" th:href="${thesisUrl}" th:text="${thesisUrl}"></a>
</p>

<hr/>
<div style="text-align: center;font-size: 10px">
    Manage your notification settings <a th:href="${config.clientHost + ''/settings/notifications''}">here</a>
</div>
<br/><br/>',
                                          'New comment on a thesis. TO depends whether its a student or advisor comment'),
                                         ('THESIS_CREATED', 'Thesis Created', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
[[${creatingUser.firstName}]] [[${creatingUser.lastName}]] created and assigned a thesis to you: <a target="_blank" rel="noopener noreferrer nofollow" th:href="${thesisUrl}">[[${thesisUrl}]]</a>
</p>

<p th:inline="text">
<strong>Title</strong>: [[${thesis.title}]]<br />
<strong>Supervisor</strong>: [[${DataFormatter.formatUsers(thesis.supervisors)}]]<br />
<strong>Advisor</strong>: [[${DataFormatter.formatUsers(thesis.advisors)}]]<br />
<strong>Student</strong>: [[${DataFormatter.formatUsers(thesis.students)}]]<br />
</p>

<p th:inline="text">
The next step is that you write a proposal and submit it on <a target="_blank" rel="noopener noreferrer nofollow" th:href="${thesisUrl}">[[${thesisUrl}]]</a>
</p>', 'New thesis was created and assigned to a student'),
                                         ('THESIS_FINAL_GRADE', 'Final Grade available for Thesis', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
[[${DataFormatter.formatUsers(thesis.supervisors)}]] added the final grade to your thesis "[[${thesis.title}]]"
</p>

<p th:inline="text">
<strong>Final Grade</strong>: [[${thesis.grade.finalGrade}]]
</p>

<p>
<strong>Feedback</strong><br />
<span th:utext="${thesis.grade.feedback}"></span>
</p>

<p th:inline="text">
    Full Details: <a target="_blank" rel="noopener noreferrer nofollow" th:href="${thesisUrl}" th:text="${thesisUrl}"></a>
</p>

<hr/>
<div style="text-align: center;font-size: 10px">
    Manage your notification settings <a th:href="${config.clientHost + ''/settings/notifications''}">here</a>
</div>
<br/><br/>', 'Final grade was added to a thesis'),
                                         ('THESIS_FINAL_SUBMISSION', 'Thesis Submitted', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
[[${DataFormatter.formatUsers(thesis.students)}]] submitted thesis "[[${thesis.title}]]".
</p>

<p th:inline="text">
The next step is to write an assessment about the thesis.
</p>

<p th:inline="text">
    Full Details: <a target="_blank" rel="noopener noreferrer nofollow" th:href="${thesisUrl}" th:text="${thesisUrl}"></a>
</p>

<hr/>
<div style="text-align: center;font-size: 10px">
    Manage your notification settings <a th:href="${config.clientHost + ''/settings/notifications''}">here</a>
</div>
<br/><br/>', 'Student submitted final thesis'),
                                         ('THESIS_PRESENTATION_DELETED', 'Presentation deleted', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
[[${deletingUser.firstName}]] [[${deletingUser.lastName}]] cancelled the presentation scheduled at [[${DataFormatter.formatDateTime(presentation.scheduledAt)}]] for thesis "[[${thesis.title}]]"
</p>

<p th:inline="text">
    Full Details: <a target="_blank" rel="noopener noreferrer nofollow" th:href="${thesisUrl}" th:text="${thesisUrl}"></a>
</p>

<hr/>
<div style="text-align: center;font-size: 10px">
    Manage your notification settings <a th:href="${config.clientHost + ''/settings/notifications''}">here</a>
</div>
<br/><br/>', 'Scheduled presentation was deleted'),
                                         ('THESIS_PRESENTATION_SCHEDULED', 'New Presentation scheduled', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
[[${presentation.createdBy.firstName}]] [[${presentation.createdBy.lastName}]] scheduled a presentation for thesis "[[${thesis.title}]]"
</p>

<p th:inline="text">
<strong>Type</strong><br />
[[${DataFormatter.formatEnum(presentation.type)}]]
</p>

<p th:inline="text">
<strong>Location</strong><br />
[[${DataFormatter.formatOptionalString(presentation.location)}]]
</p >

<p th:inline="text">
<strong>Stream URL</strong><br />
[[${DataFormatter.formatOptionalString(presentation.streamUrl)}]]
</p>

<p th:inline="text">
<strong>Language</strong><br />
[[${DataFormatter.formatConstantName(presentation.language)}]]
</p>

<p th:inline="text">
<strong>Scheduled At</strong><br />
[[${DataFormatter.formatDateTime(presentation.scheduledAt)}]]
</p>

<p th:inline="text">
    Full Details: <a target="_blank" rel="noopener noreferrer nofollow" th:href="${thesisUrl}" th:text="${thesisUrl}"></a>
</p>

<hr/>
<div style="text-align: center;font-size: 10px">
    Manage your notification settings <a th:href="${config.clientHost + ''/settings/notifications''}">here</a>
</div>
<br/><br/>', 'New presentation was scheduled'),
                                         ('THESIS_PRESENTATION_UPDATED', 'Presentation updated', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
[[${presentation.createdBy.firstName}]] [[${presentation.createdBy.lastName}]] updated a presentation for thesis "[[${thesis.title}]]"
</p>

<p th:inline="text">
<strong>Type</strong><br />
[[${DataFormatter.formatEnum(presentation.type)}]]
</p>

<p th:inline="text">
<strong>Location</strong><br />
[[${DataFormatter.formatOptionalString(presentation.location)}]]
</p >

<p th:inline="text">
<strong>Stream URL</strong><br />
[[${DataFormatter.formatOptionalString(presentation.streamUrl)}]]
</p>

<p th:inline="text">
<strong>Language</strong><br />
[[${DataFormatter.formatConstantName(presentation.language)}]]
</p>

<p th:inline="text">
<strong>Scheduled At</strong><br />
[[${DataFormatter.formatDateTime(presentation.scheduledAt)}]]
</p>

<p th:inline="text">
    Full Details: <a target="_blank" rel="noopener noreferrer nofollow" th:href="${thesisUrl}" th:text="${thesisUrl}"></a>
</p>

<hr/>
<div style="text-align: center;font-size: 10px">
    Manage your notification settings <a th:href="${config.clientHost + ''/settings/notifications''}">here</a>
</div>
<br/><br/>', 'Presentation was updated'),
                                         ('THESIS_PRESENTATION_INVITATION', 'Thesis Presentation Invitation', '<div style="text-align: center" th:inline="text">
<h2>INVITATION</h2>
<div>As part of their [[${DataFormatter.formatConstantName(thesis.type)}]]''s thesis</div>
<div><strong>[[${DataFormatter.formatUsers(thesis.students)}]]</strong></div>
<div>will give their [[${DataFormatter.formatEnum(presentation.type)}]] presentation on</div>
<div><strong>[[${DataFormatter.formatDateTime(presentation.scheduledAt)}]]</strong></div>
<div th:if="${presentation.streamUrl != null and !#strings.isEmpty(presentation.streamUrl)}" th:inline="text">
online at [[${presentation.streamUrl}]]
</div>
<div th:if="${presentation.location != null and !#strings.isEmpty(presentation.location)}">
<span th:if="${presentation.streamUrl != null and !#strings.isEmpty(presentation.streamUrl)}">and&nbsp;</span>onsite in <strong>[[${presentation.location}]]</strong>
</div>
<br/>
<div>Title:</div>
<div>[[${thesis.title}]]</div>
<br/>
<div>Supervisor: [[${DataFormatter.formatUsers(thesis.supervisors)}]]</div>
<div>Advisor(s): [[${DataFormatter.formatUsers(thesis.advisors)}]]</div>
<br/>
<div>
The presentation will be in [[${DataFormatter.formatConstantName(presentation.language)}]]. Everybody is cordially invited to attend.
</div>
</div>

<p>
<strong>Abstract</strong><br />
<span th:utext="${thesis.abstractText}"></span>
</p>

<p th:inline="text">
Full Details: <a target="_blank" rel="noopener noreferrer nofollow" th:href="${presentationUrl}">[[${presentationUrl}]]</a>
</p>

<hr/>
<div style="text-align: center;font-size: 10px">
    Manage your notification settings <a th:href="${config.clientHost + ''/settings/notifications''}">here</a>
</div>
<br/><br/>', 'Public Presentation Invitation'),
                                         ('THESIS_PRESENTATION_INVITATION_CANCELLED', 'Thesis Presentation Cancelled', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
The [[${DataFormatter.formatConstantName(thesis.type)}]] thesis presentation of
<strong>[[${DataFormatter.formatUsers(thesis.students)}]]</strong> scheduled at
[[${DataFormatter.formatDateTime(presentation.scheduledAt)}]] was cancelled.
</p>

<hr/>
<div style="text-align: center;font-size: 10px">
    Manage your notification settings <a th:href="${config.clientHost + ''/settings/notifications''}">here</a>
</div>
<br/><br/>', 'Public Presentation was deleted'),
                                         ('THESIS_PRESENTATION_INVITATION_UPDATED', 'Thesis Presentation Updated', '<div style="text-align: center" th:inline="text">
<h2>INVITATION</h2>
<div>As part of their [[${DataFormatter.formatConstantName(thesis.type)}]]''s thesis</div>
<div><strong>[[${DataFormatter.formatUsers(thesis.students)}]]</strong></div>
<div>will give their [[${DataFormatter.formatEnum(presentation.type)}]] presentation on</div>
<div><strong>[[${DataFormatter.formatDateTime(presentation.scheduledAt)}]]</strong></div>
<div th:if="${presentation.streamUrl != null and !#strings.isEmpty(presentation.streamUrl)}" th:inline="text">
online at [[${presentation.streamUrl}]]
</div>
<div th:if="${presentation.location != null and !#strings.isEmpty(presentation.location)}">
<span th:if="${presentation.streamUrl != null and !#strings.isEmpty(presentation.streamUrl)}">and&nbsp;</span>onsite in <strong>[[${presentation.location}]]</strong>
</div>
<br/>
<div>Title:</div>
<div>[[${thesis.title}]]</div>
<br/>
<div>Supervisor: [[${DataFormatter.formatUsers(thesis.supervisors)}]]</div>
<div>Advisor(s): [[${DataFormatter.formatUsers(thesis.advisors)}]]</div>
<br/>
<div>
The presentation will be in [[${DataFormatter.formatConstantName(presentation.language)}]]. Everybody is cordially invited to attend.
</div>
</div>

<p>
<strong>Abstract</strong><br />
<span th:utext="${thesis.abstractText}"></span>
</p>

<p th:inline="text">
Full Details: <a target="_blank" rel="noopener noreferrer nofollow" th:href="${presentationUrl}">[[${presentationUrl}]]</a>
</p>

<hr/>
<div style="text-align: center;font-size: 10px">
    Manage your notification settings <a th:href="${config.clientHost + ''/settings/notifications''}">here</a>
</div>
<br/><br/>', 'Public Presentation was updated'),
                                         ('THESIS_PROPOSAL_ACCEPTED', 'Thesis Proposal Accepted', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
[[${proposal.approvedBy.firstName}]] [[${proposal.approvedBy.lastName}]] approved the proposal of thesis "[[${thesis.title}]]".
The next step is to start with the project work and with writing the thesis.
You can see your submission deadline on <a target="_blank" rel="noopener noreferrer nofollow" th:href="${thesisUrl}">[[${thesisUrl}]]</a>.
</p>

<hr/>
<div style="text-align: center;font-size: 10px">
    Manage your notification settings <a th:href="${config.clientHost + ''/settings/notifications''}">here</a>
</div>
<br/><br/>', 'Proposal was accepted'),
                                         ('THESIS_PROPOSAL_REJECTED', 'Changes were requested for Proposal', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
[[${reviewingUser.firstName}]] [[${reviewingUser.lastName}]] reviewed your proposal for thesis "[[${thesis.title}]]".
</p>

<p th:inline="text">
The following changes were requested:<br />
<ul>
<li th:each="requestedChange : ${requestedChanges}" th:text="${requestedChange}"></li>
</ul>
</p>

<p th:inline="text">
    Full Details: <a target="_blank" rel="noopener noreferrer nofollow" th:href="${thesisUrl}" th:text="${thesisUrl}"></a>
</p>

<hr/>
<div style="text-align: center;font-size: 10px">
    Manage your notification settings <a th:href="${config.clientHost + ''/settings/notifications''}">here</a>
</div>
<br/><br/>', 'Changes were requested for proposal'),
                                         ('THESIS_PROPOSAL_UPLOADED', 'Thesis Proposal Added', '<p th:inline="text">Dear [[${recipient.firstName}]],</p>

<p th:inline="text">
[[${proposal.createdBy.firstName}]] [[${proposal.createdBy.lastName}]] uploaded a proposal to thesis "[[${thesis.title}]]".
You can find the submitted file in the attachment part of this email.
</p>

<p th:inline="text">
    Full Details: <a target="_blank" rel="noopener noreferrer nofollow" th:href="${thesisUrl}" th:text="${thesisUrl}"></a>
</p>

<hr/>
<div style="text-align: center;font-size: 10px">
    Manage your notification settings <a th:href="${config.clientHost + ''/settings/notifications''}">here</a>
</div>
<br/><br/>',
                                          'Student uploaded new proposal')) AS v(template_case, subject, body_html, description))
INSERT
INTO email_templates (email_template_id,
                      research_group_id,
                      template_case,
                      subject,
                      body_html,
                      language,
                      description,
                      created_at,
                      updated_by,
                      updated_at)
SELECT *
FROM templates;