package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.constants.ApplicationRejectReason;
import de.tum.cit.aet.thesis.constants.ThesisCommentType;
import de.tum.cit.aet.thesis.constants.ThesisFeedbackType;
import de.tum.cit.aet.thesis.constants.ThesisPresentationVisibility;
import de.tum.cit.aet.thesis.cron.model.ApplicationRejectObject;
import de.tum.cit.aet.thesis.entity.*;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.EmailTemplateRepository;
import de.tum.cit.aet.thesis.utility.DataFormatter;
import de.tum.cit.aet.thesis.utility.MailBuilder;
import de.tum.cit.aet.thesis.utility.MailConfig;
import jakarta.mail.util.ByteArrayDataSource;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class MailingService {
    private final JavaMailSender javaMailSender;
    private final UploadService uploadService;
    private final MailConfig config;
    private final EmailTemplateRepository emailTemplateRepository;

    private static final String NOTIFICATION_NAME_START = "thesis-";

    @Autowired
    public MailingService(
            JavaMailSender javaMailSender,
            UploadService uploadService,
            MailConfig config,
            EmailTemplateRepository emailTemplateRepository
    ) {
        this.javaMailSender = javaMailSender;
        this.uploadService = uploadService;
        this.config = config;
        this.emailTemplateRepository = emailTemplateRepository;
    }

    public void sendApplicationCreatedEmail(Application application) {
        EmailTemplate researchGroupEmailTemplate = loadTemplate(
                application.getResearchGroup().getId(),
                "APPLICATION_CREATED_CHAIR",
                "en");

        MailBuilder researchGroupMailBuilder = prepareApplicationCreatedMailBuilder(application, researchGroupEmailTemplate);
        researchGroupMailBuilder
            .sendToChairMembers(application.getResearchGroup().getId())
            .addNotificationName("new-applications")
            .filterChairMembersNewApplicationNotifications(application.getTopic(), "new-applications")
            .send(javaMailSender, uploadService);

        sendNotificationCopy(application.getResearchGroup(), prepareApplicationCreatedMailBuilder(application,researchGroupEmailTemplate));

        EmailTemplate studentEmailTemplate = loadTemplate(
                application.getResearchGroup().getId(),
                "APPLICATION_CREATED_STUDENT",
                "en");
        MailBuilder studentMailBuilder = new MailBuilder(config, studentEmailTemplate.getSubject(), studentEmailTemplate.getBodyHtml());
        studentMailBuilder
                .addPrimaryRecipient(application.getUser())
                .addStoredAttachment(application.getUser().getCvFilename(), getUserFilename(application.getUser(), "CV", application.getUser().getCvFilename()))
                .addStoredAttachment(application.getUser().getExaminationFilename(), getUserFilename(application.getUser(), "Examination Report", application.getUser().getExaminationFilename()))
                .addStoredAttachment(application.getUser().getDegreeFilename(), getUserFilename(application.getUser(), "Degree Report", application.getUser().getDegreeFilename()))
                .fillApplicationPlaceholders(application)
                .send(javaMailSender, uploadService);
    }

    private MailBuilder prepareApplicationCreatedMailBuilder(Application application, EmailTemplate template) {
        return new MailBuilder(config, template.getSubject(), template.getBodyHtml())
                .addStoredAttachment(application.getUser().getCvFilename(), getUserFilename(application.getUser(), "CV", application.getUser().getCvFilename()))
                .addStoredAttachment(application.getUser().getExaminationFilename(), getUserFilename(application.getUser(), "Examination Report", application.getUser().getExaminationFilename()))
                .addStoredAttachment(application.getUser().getDegreeFilename(), getUserFilename(application.getUser(), "Degree Report", application.getUser().getDegreeFilename()))
                .fillApplicationPlaceholders(application);
    }

    /**
     * Sends a copy of the application created notification to an additional email address
     * when specified in the research group's settings.
     *
     * @param researchGroup The ResearchGroup associated with the email.
     * @param mailBuilder The MailBuilder instance used to construct the email.
     */
    private void sendNotificationCopy(ResearchGroup researchGroup, MailBuilder mailBuilder) {
        if (researchGroup == null || researchGroup.getResearchGroupSettings() == null) {
            return;
        }

        String additionalEmail = researchGroup.getResearchGroupSettings().getApplicationNotificationEmail();

        if (additionalEmail == null || additionalEmail.isBlank()) {
            return;
        }

        mailBuilder
                .addPrimaryRecipient(buildNotificationRecipientForCopy(researchGroup, additionalEmail))
                .send(javaMailSender, uploadService);
    }

    private User buildNotificationRecipientForCopy(ResearchGroup researchGroup, String email) {
        User recipient = new User();
        recipient.setEmail(email);
        recipient.setResearchGroup(researchGroup);

        if (researchGroup != null) {
            recipient.setFirstName(researchGroup.getName());
            recipient.setLastName("");
        } else {
            recipient.setFirstName("Research Group");
            recipient.setLastName("");
        }

        return recipient;
    }

    public void sendApplicationAcceptanceEmail(Application application, Thesis thesis) {
        User advisor = thesis.getAdvisors().getFirst();
        User supervisor = thesis.getSupervisors().getFirst();

        String templateCase = advisor.getId().equals(supervisor.getId()) ? "APPLICATION_ACCEPTED_NO_ADVISOR" :
                "APPLICATION_ACCEPTED";

        EmailTemplate emailTemplate = loadTemplate(
                application.getResearchGroup().getId(),
                templateCase,
                "en");
        MailBuilder mailBuilder = new MailBuilder(config, emailTemplate.getSubject(), emailTemplate.getBodyHtml());
        mailBuilder
                .addPrimaryRecipient(application.getUser())
                .addSecondaryRecipient(advisor)
                .addDefaultBccRecipients(application.getResearchGroup().getHead().getEmail())
                .fillUserPlaceholders(advisor, "advisor")
                .fillApplicationPlaceholders(application)
                .fillThesisPlaceholders(thesis)
                .send(javaMailSender, uploadService);
    }

    public void sendApplicationRejectionEmail(Application application, ApplicationRejectReason reason) {
        EmailTemplate emailTemplate = loadTemplate(
                application.getResearchGroup().getId(),
                reason.getTemplateCase(),
                "en");
        MailBuilder mailBuilder = new MailBuilder(config, emailTemplate.getSubject(), emailTemplate.getBodyHtml());
        mailBuilder
                .addPrimaryRecipient(application.getUser())
                .addDefaultBccRecipients(application.getResearchGroup().getHead().getEmail())
                .fillApplicationPlaceholders(application)
                .send(javaMailSender, uploadService);
    }

    public void sendApplicationReminderEmail(User user, long unreviewedApplications) {
        EmailTemplate emailTemplate = loadTemplate(
                user.getResearchGroup().getId(),
                "APPLICATION_REMINDER",
                "en");
        MailBuilder mailBuilder = new MailBuilder(config, emailTemplate.getSubject(), emailTemplate.getBodyHtml());
        mailBuilder
                .addPrimaryRecipient(user)
                .addNotificationName("unreviewed-application-reminder")
                .fillPlaceholder("unreviewedApplications", String.valueOf(unreviewedApplications))
                .fillPlaceholder("reviewApplicationsLink", config.getClientHost() + "/applications")
                .send(javaMailSender, uploadService);
    }

    public void sendApplicationAutomaticRejectReminderEmail(User user, List<ApplicationRejectObject> applications) {
        EmailTemplate emailTemplate = loadTemplate(
                user.getResearchGroup().getId(),
                "APPLICATION_AUTOMATIC_REJECT_REMINDER",
                "en");

        Map<String, Object> model = new HashMap<>();
        model.put("applications", applications);
        model.put("clientHost", config.getClientHost());

        MailBuilder mailBuilder = new MailBuilder(config, emailTemplate.getSubject(), emailTemplate.getBodyHtml());
        mailBuilder
                .addPrimaryRecipient(user)
                .addNotificationName(emailTemplate.getSubject())
                .fillPlaceholders(model)
                .send(javaMailSender, uploadService);
    }

    public void sendInvitationEmail(Interviewee interviewee) {
        EmailTemplate emailTemplate = loadTemplate(
                interviewee.getApplication().getResearchGroup().getId(),
                "INTERVIEW_INVITATION",
                "en");

        MailBuilder mailBuilder = new MailBuilder(config, emailTemplate.getSubject(), emailTemplate.getBodyHtml());
        mailBuilder
                .addPrimaryRecipient(interviewee.getApplication().getUser())
                .addNotificationName(emailTemplate.getSubject())
                .fillApplicationPlaceholders(interviewee.getApplication())
                .fillIntervieweePlaceholders(interviewee)
                .send(javaMailSender, uploadService);
    }

    public void sendThesisCreatedEmail(User creatingUser, Thesis thesis) {
        EmailTemplate emailTemplate = loadTemplate(
                thesis.getResearchGroup().getId(),
                "THESIS_CREATED",
                "en");
        MailBuilder mailBuilder = new MailBuilder(config, emailTemplate.getSubject(), emailTemplate.getBodyHtml());
        mailBuilder
                .sendToThesisStudents(thesis)
                .addDefaultBccRecipients(thesis.getResearchGroup().getHead().getEmail())
                .addNotificationName(NOTIFICATION_NAME_START + thesis.getId())
                .fillThesisPlaceholders(thesis)
                .fillUserPlaceholders(creatingUser, "creatingUser")
                .send(javaMailSender, uploadService);
    }

    public void sendThesisClosedEmail(User deletingUser, Thesis thesis) {
        EmailTemplate emailTemplate = loadTemplate(
                thesis.getResearchGroup().getId(),
                "THESIS_CLOSED",
                "en");
        MailBuilder mailBuilder = new MailBuilder(config, emailTemplate.getSubject(), emailTemplate.getBodyHtml());
        mailBuilder
                .sendToThesisStudents(thesis)
                .addDefaultBccRecipients(thesis.getResearchGroup().getHead().getEmail())
                .addNotificationName(NOTIFICATION_NAME_START + thesis.getId())
                .fillThesisPlaceholders(thesis)
                .fillUserPlaceholders(deletingUser, "deletingUser")
                .send(javaMailSender, uploadService);
    }

    public void sendProposalUploadedEmail(ThesisProposal proposal) {
        EmailTemplate emailTemplate = loadTemplate(
            proposal.getResearchGroup().getId(),
            "THESIS_PROPOSAL_UPLOADED",
            "en");
        MailBuilder mailBuilder = new MailBuilder(config, emailTemplate.getSubject(), emailTemplate.getBodyHtml());
        mailBuilder
                .addPrimarySender(proposal.getCreatedBy())
                .sendToThesisAdvisors(proposal.getThesis())
                .addNotificationName(NOTIFICATION_NAME_START + proposal.getThesis().getId())
                .fillThesisProposalPlaceholders(proposal)
                .addStoredAttachment(proposal.getProposalFilename(), getThesisFilename(proposal.getThesis(), "Proposal", proposal.getProposalFilename()))
                .send(javaMailSender, uploadService);
    }

    public void sendProposalAcceptedEmail(ThesisProposal proposal) {
        EmailTemplate emailTemplate = loadTemplate(
                proposal.getResearchGroup().getId(),
                "THESIS_PROPOSAL_ACCEPTED",
                "en");
        MailBuilder mailBuilder = new MailBuilder(config, emailTemplate.getSubject(), emailTemplate.getBodyHtml());
        mailBuilder
                .addPrimarySender(proposal.getApprovedBy())
                .sendToThesisStudents(proposal.getThesis())
                .addNotificationName(NOTIFICATION_NAME_START + proposal.getThesis().getId())
                .fillThesisPlaceholders(proposal.getThesis())
                .fillThesisProposalPlaceholders(proposal)
                .send(javaMailSender, uploadService);
    }

    public void sendProposalChangeRequestEmail(User reviewingUser, Thesis thesis) {
        EmailTemplate emailTemplate = loadTemplate(
                thesis.getResearchGroup().getId(),
                "THESIS_PROPOSAL_REJECTED",
                "en");
        MailBuilder mailBuilder = new MailBuilder(config, emailTemplate.getSubject(), emailTemplate.getBodyHtml());
        mailBuilder
                .sendToThesisStudents(thesis)
                .addNotificationName(NOTIFICATION_NAME_START + thesis.getId())
                .fillUserPlaceholders(reviewingUser, "reviewingUser")
                .fillThesisPlaceholders(thesis)
                .fillThesisProposalPlaceholders(thesis.getProposals().getFirst())
                .fillPlaceholder(
                        "requestedChanges",
                        thesis.getFeedback().stream()
                                .filter(item -> item.getType() == ThesisFeedbackType.PROPOSAL && item.getCompletedAt() == null)
                                .map(ThesisFeedback::getFeedback)
                                .toList()
                )
                .send(javaMailSender, uploadService);
    }

    public void sendNewCommentEmail(ThesisComment comment) {
        EmailTemplate emailTemplate = loadTemplate(
                comment.getResearchGroup().getId(),
                "THESIS_COMMENT_POSTED",
                "en");
        MailBuilder mailBuilder = new MailBuilder(config, emailTemplate.getSubject(), emailTemplate.getBodyHtml());

        if (comment.getType() == ThesisCommentType.ADVISOR) {
            mailBuilder.sendToThesisAdvisors(comment.getThesis());
        } else {
            mailBuilder.sendToThesisStudents(comment.getThesis());
        }

        mailBuilder
                .addPrimarySender(comment.getCreatedBy())
                .addNotificationName("thesis-comments")
                .addNotificationName(NOTIFICATION_NAME_START + comment.getThesis().getId())
                .fillThesisCommentPlaceholders(comment)
                .addStoredAttachment(comment.getFilename(), getUserFilename(comment.getCreatedBy(), "Comment", comment.getUploadName()))
                .send(javaMailSender, uploadService);
    }

    public void sendScheduledPresentationEmail(String action, ThesisPresentation presentation, String icsFile) {
        if (presentation.getScheduledAt().isBefore(Instant.now())) {
            return;
        }

        String updatedString = "UPDATED";

        EmailTemplate privateEmailTemplate = loadTemplate(
                presentation.getResearchGroup().getId(),
                action.equals(updatedString) ? "THESIS_PRESENTATION_UPDATED" : "THESIS_PRESENTATION_SCHEDULED",
                "en");
        MailBuilder privateMailBuilder = new MailBuilder(config, privateEmailTemplate.getSubject(),
                privateEmailTemplate.getBodyHtml());
        privateMailBuilder
                .addPrimarySender(presentation.getCreatedBy())
                .sendToThesisStudents(presentation.getThesis())
                .addNotificationName(NOTIFICATION_NAME_START + presentation.getThesis().getId())
                .fillThesisPresentationPlaceholders(presentation)
                .send(javaMailSender, uploadService);

        if (presentation.getVisibility() == ThesisPresentationVisibility.PUBLIC) {
            EmailTemplate publicEmailTemplate = loadTemplate(
                    presentation.getResearchGroup().getId(),
                    action.equals(updatedString) ? "THESIS_PRESENTATION_INVITATION_UPDATED" : "THESIS_PRESENTATION_INVITATION",
                    "en");
            MailBuilder publicMailBuilder = new MailBuilder(config, publicEmailTemplate.getSubject(),
                    publicEmailTemplate.getBodyHtml());
            publicMailBuilder
                    .addPrimaryRecipient(presentation.getThesis().getStudents().getFirst())
                    .fillThesisPresentationPlaceholders(presentation);

            for (ThesisPresentationInvite invite : presentation.getInvites()) {
                publicMailBuilder.addBccRecipient(invite.getEmail());
            }

            if (icsFile != null && !icsFile.isBlank()) {
                publicMailBuilder.addRawAttatchment(
                        new ByteArrayDataSource(icsFile.getBytes(StandardCharsets.UTF_8), "application/octet-stream"),
                        "event.ics"
                );
            }

            publicMailBuilder.send(javaMailSender, uploadService);
        }
    }

    public void sendPresentationDeletedEmail(User deletingUser, ThesisPresentation presentation) {
        if (presentation.getScheduledAt().isBefore(Instant.now())) {
            return;
        }

        EmailTemplate emailTemplate = loadTemplate(
                presentation.getResearchGroup().getId(),
                "THESIS_PRESENTATION_DELETED",
                "en");
        MailBuilder mailBuilder = new MailBuilder(config, emailTemplate.getSubject(),
                emailTemplate.getBodyHtml());
        mailBuilder
                .sendToThesisStudents(presentation.getThesis())
                .addNotificationName(NOTIFICATION_NAME_START + presentation.getThesis().getId())
                .fillThesisPresentationPlaceholders(presentation)
                .fillUserPlaceholders(deletingUser, "deletingUser")
                .send(javaMailSender, uploadService);

        if (presentation.getVisibility() == ThesisPresentationVisibility.PUBLIC) {
            EmailTemplate publicEmailTemplate = loadTemplate(
                    presentation.getResearchGroup().getId(),
                    "THESIS_PRESENTATION_INVITATION_CANCELLED",
                    "en");
            MailBuilder publicMailBuilder = new MailBuilder(config, publicEmailTemplate.getSubject(),
                    publicEmailTemplate.getBodyHtml());
            publicMailBuilder
                    .addPrimaryRecipient(presentation.getThesis().getStudents().getFirst())
                    .fillThesisPresentationPlaceholders(presentation);

            for (ThesisPresentationInvite invite : presentation.getInvites()) {
                publicMailBuilder.addBccRecipient(invite.getEmail());
            }

            publicMailBuilder.send(javaMailSender, uploadService);
        }
    }

    public void sendFinalSubmissionEmail(Thesis thesis) {
        EmailTemplate emailTemplate = loadTemplate(
                thesis.getResearchGroup().getId(),
                "THESIS_FINAL_SUBMISSION",
                "en");
        MailBuilder mailBuilder = new MailBuilder(config, emailTemplate.getSubject(),
                emailTemplate.getBodyHtml());
        mailBuilder
                .sendToThesisAdvisors(thesis)
                .addNotificationName(NOTIFICATION_NAME_START + thesis.getId())
                .fillThesisPlaceholders(thesis)
                //.addStoredAttachment(thesis.getFinalThesisFilename(), getThesisFilename(thesis, "File", thesis.getFinalThesisFilename()))
                //.addStoredAttachment(thesis.getFinalPresentationFilename(), getThesisFilename(thesis, "Presentation", thesis.getFinalPresentationFilename()))
                .send(javaMailSender, uploadService);
    }

    public void sendAssessmentAddedEmail(ThesisAssessment assessment) {
        EmailTemplate emailTemplate = loadTemplate(
                assessment.getThesis().getResearchGroup().getId(),
                "THESIS_ASSESSMENT_ADDED",
                "en");
        MailBuilder mailBuilder = new MailBuilder(config, emailTemplate.getSubject(),
                emailTemplate.getBodyHtml());
        mailBuilder
                .addPrimarySender(assessment.getCreatedBy())
                .sendToThesisSupervisors(assessment.getThesis())
                .addNotificationName(NOTIFICATION_NAME_START + assessment.getThesis().getId())
                .fillThesisAssessmentPlaceholders(assessment)
                .send(javaMailSender, uploadService);
    }

    public void sendFinalGradeEmail(Thesis thesis) {
        EmailTemplate emailTemplate = loadTemplate(
                thesis.getResearchGroup().getId(),
                "THESIS_FINAL_GRADE",
                "en");
        MailBuilder mailBuilder = new MailBuilder(config, emailTemplate.getSubject(),
                emailTemplate.getBodyHtml());
        mailBuilder
                .sendToThesisStudents(thesis)
                .addNotificationName(NOTIFICATION_NAME_START + thesis.getId())
                .fillThesisPlaceholders(thesis)
                .send(javaMailSender, uploadService);
    }

    private EmailTemplate loadTemplate(UUID researchGroupId, String templateCase, String language) {
        return emailTemplateRepository
                .findTemplateWithFallback(researchGroupId, templateCase, language)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Email template not found (researchGroupId=%s, templateCase=%s, language=%s)",
                                researchGroupId, templateCase, language)
                ));
    }

    private String getUserFilename(User user, String name, String originalFilename) {
        StringBuilder builder = new StringBuilder();

        builder.append(name);

        if (user.getFirstName() != null) {
            builder.append(" ").append(user.getFirstName());
        }

        if (user.getLastName() != null) {
            builder.append(" ").append(user.getLastName());
        }

        if (originalFilename != null && !originalFilename.isBlank()) {
            builder.append(".").append(FilenameUtils.getExtension(originalFilename));
        } else {
            builder.append(".pdf");
        }

        return builder.toString();
    }

    private String getThesisFilename(Thesis thesis, String name, String originalFilename) {
        StringBuilder builder = new StringBuilder();

        builder.append(DataFormatter.formatConstantName(thesis.getType()));
        builder.append(" Thesis");

        if (name != null && !name.isBlank()) {
            builder.append(" ").append(name);
        }

        for (User student : thesis.getStudents()) {
            builder.append(" ").append(student.getFirstName());
            builder.append(" ").append(student.getLastName());
        }

        if (originalFilename != null && !originalFilename.isBlank()) {
            builder.append(".").append(FilenameUtils.getExtension(originalFilename));
        } else {
            builder.append(".pdf");
        }

        return builder.toString();
    }
}
