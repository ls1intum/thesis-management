--liquibase formatted sql
--changeset codex:22_migrate_email_template_placeholder_variables

UPDATE email_templates SET body_html = REPLACE(body_html, 'DataFormatter.formatConstantName(application.user.studyProgram)', 'application.studyProgram');
UPDATE email_templates SET body_html = REPLACE(body_html, 'DataFormatter.formatConstantName(application.user.studyDegree)', 'application.studyDegree');
UPDATE email_templates SET body_html = REPLACE(body_html, 'DataFormatter.formatSemester(application.user.enrolledAt)', 'application.semester');
UPDATE email_templates SET body_html = REPLACE(body_html, 'DataFormatter.formatDate(application.desiredStartDate)', 'application.desiredStartDate');
UPDATE email_templates SET body_html = REPLACE(body_html, 'application.user.firstName', 'application.applicantFirstName');
UPDATE email_templates SET body_html = REPLACE(body_html, 'application.user.lastName', 'application.applicantLastName');
UPDATE email_templates SET body_html = REPLACE(body_html, 'application.user.email', 'application.applicantEmail');
UPDATE email_templates SET body_html = REPLACE(body_html, 'application.user.universityId', 'application.applicantUniversityId');
UPDATE email_templates SET body_html = REPLACE(body_html, 'application.user.matriculationNumber', 'application.applicantMatriculationNumber');
UPDATE email_templates SET body_html = REPLACE(body_html, 'application.user.specialSkills', 'application.specialSkills');
UPDATE email_templates SET body_html = REPLACE(body_html, 'application.user.interests', 'application.interests');
UPDATE email_templates SET body_html = REPLACE(body_html, 'application.user.projects', 'application.projects');

UPDATE email_templates SET body_html = REPLACE(body_html, 'DataFormatter.formatUsers(thesis.supervisors)', 'thesis.supervisors');
UPDATE email_templates SET body_html = REPLACE(body_html, 'DataFormatter.formatUsers(thesis.advisors)', 'thesis.advisors');
UPDATE email_templates SET body_html = REPLACE(body_html, 'DataFormatter.formatUsers(thesis.students)', 'thesis.students');
UPDATE email_templates SET body_html = REPLACE(body_html, 'DataFormatter.formatConstantName(thesis.type)', 'thesis.type');
UPDATE email_templates SET body_html = REPLACE(body_html, 'thesis.grade.finalGrade', 'thesis.finalGrade');
UPDATE email_templates SET body_html = REPLACE(body_html, 'thesis.grade.feedback', 'thesis.finalFeedback');

UPDATE email_templates SET body_html = REPLACE(body_html, 'DataFormatter.formatEnum(presentation.type)', 'presentation.type');
UPDATE email_templates SET body_html = REPLACE(body_html, 'DataFormatter.formatDateTime(presentation.scheduledAt)', 'presentation.scheduledAt');
UPDATE email_templates SET body_html = REPLACE(body_html, 'DataFormatter.formatOptionalString(presentation.location)', 'presentation.location');
UPDATE email_templates SET body_html = REPLACE(body_html, 'DataFormatter.formatOptionalString(presentation.streamUrl)', 'presentation.streamUrl');
UPDATE email_templates SET body_html = REPLACE(body_html, 'DataFormatter.formatConstantName(presentation.language)', 'presentation.language');
UPDATE email_templates SET body_html = REPLACE(body_html, 'presentation.createdBy.firstName', 'presentation.creatorFirstName');
UPDATE email_templates SET body_html = REPLACE(body_html, 'presentation.createdBy.lastName', 'presentation.creatorLastName');

UPDATE email_templates SET body_html = REPLACE(body_html, 'assessment.createdBy.firstName', 'assessment.creatorFirstName');
UPDATE email_templates SET body_html = REPLACE(body_html, 'assessment.createdBy.lastName', 'assessment.creatorLastName');

UPDATE email_templates SET body_html = REPLACE(body_html, 'proposal.createdBy.firstName', 'proposal.creatorFirstName');
UPDATE email_templates SET body_html = REPLACE(body_html, 'proposal.createdBy.lastName', 'proposal.creatorLastName');
UPDATE email_templates SET body_html = REPLACE(body_html, 'proposal.approvedBy.firstName', 'proposal.approverFirstName');
UPDATE email_templates SET body_html = REPLACE(body_html, 'proposal.approvedBy.lastName', 'proposal.approverLastName');

UPDATE email_templates SET body_html = REPLACE(body_html, 'comment.createdBy.firstName', 'comment.creatorFirstName');
UPDATE email_templates SET body_html = REPLACE(body_html, 'comment.createdBy.lastName', 'comment.creatorLastName');

UPDATE email_templates SET body_html = REPLACE(body_html, 'DataFormatter.formatDateTime(slot.startDate)', 'slot.startDate');
