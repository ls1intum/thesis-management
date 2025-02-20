package de.tum.cit.aet.thesis.service;

import jakarta.mail.util.ByteArrayDataSource;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import de.tum.cit.aet.thesis.constants.ThesisCommentType;
import de.tum.cit.aet.thesis.constants.ApplicationRejectReason;
import de.tum.cit.aet.thesis.constants.ThesisFeedbackType;
import de.tum.cit.aet.thesis.constants.ThesisPresentationVisibility;
import de.tum.cit.aet.thesis.entity.*;
import de.tum.cit.aet.thesis.utility.DataFormatter;
import de.tum.cit.aet.thesis.utility.MailBuilder;
import de.tum.cit.aet.thesis.utility.MailConfig;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
public class MailingService {
    private final JavaMailSender javaMailSender;
    private final UploadService uploadService;
    private final MailConfig config;

    @Autowired
    public MailingService(
            JavaMailSender javaMailSender,
            UploadService uploadService,
            MailConfig config
    ) {
        this.javaMailSender = javaMailSender;
        this.uploadService = uploadService;
        this.config = config;
    }

    public void sendApplicationCreatedEmail(Application application) {
        MailBuilder chairMailBuilder = new MailBuilder(
                config,
                "New Thesis Application",
                "application-created-chair"
        );
        chairMailBuilder
                .sendToChairMembers()
                .addNotificationName("new-applications")
                .addStoredAttachment(application.getUser().getCvFilename(), getUserFilename(application.getUser(), "CV", application.getUser().getCvFilename()))
                .addStoredAttachment(application.getUser().getExaminationFilename(), getUserFilename(application.getUser(), "Examination Report", application.getUser().getExaminationFilename()))
                .addStoredAttachment(application.getUser().getDegreeFilename(), getUserFilename(application.getUser(), "Degree Report", application.getUser().getDegreeFilename()))
                .fillApplicationPlaceholders(application)
                .send(javaMailSender, uploadService);

        MailBuilder studentMailBuilder = new MailBuilder(config, "Thesis Application Confirmation", "application-created-student");
        studentMailBuilder
                .addPrimaryRecipient(application.getUser())
                .addStoredAttachment(application.getUser().getCvFilename(), getUserFilename(application.getUser(), "CV", application.getUser().getCvFilename()))
                .addStoredAttachment(application.getUser().getExaminationFilename(), getUserFilename(application.getUser(), "Examination Report", application.getUser().getExaminationFilename()))
                .addStoredAttachment(application.getUser().getDegreeFilename(), getUserFilename(application.getUser(), "Degree Report", application.getUser().getDegreeFilename()))
                .fillApplicationPlaceholders(application)
                .send(javaMailSender, uploadService);
    }

    public void sendApplicationAcceptanceEmail(Application application, Thesis thesis) {
        User advisor = thesis.getAdvisors().getFirst();
        User supervisor = thesis.getSupervisors().getFirst();

        String template = advisor.getId().equals(supervisor.getId()) ? "application-accepted-no-advisor" : "application-accepted";

        MailBuilder builder = new MailBuilder(config, "Thesis Application Acceptance", template);
        builder
                .addPrimaryRecipient(application.getUser())
                .addSecondaryRecipient(advisor)
                .addDefaultBccRecipients()
                .fillUserPlaceholders(advisor, "advisor")
                .fillApplicationPlaceholders(application)
                .fillThesisPlaceholders(thesis)
                .send(javaMailSender, uploadService);
    }

    public void sendApplicationRejectionEmail(Application application, ApplicationRejectReason reason) {
        MailBuilder builder = new MailBuilder(config, "Thesis Application Rejection", reason.getMailTemplate());
        builder
                .addPrimaryRecipient(application.getUser())
                .addDefaultBccRecipients()
                .fillApplicationPlaceholders(application)
                .send(javaMailSender, uploadService);
    }

    public void sendApplicationReminderEmail(User user, long unreviewedApplications) {
        MailBuilder builder = new MailBuilder(config, "Unreviewed Thesis Applications", "application-reminder");
        builder
                .addPrimaryRecipient(user)
                .addNotificationName("unreviewed-application-reminder")
                .fillPlaceholder("unreviewedApplications", String.valueOf(unreviewedApplications))
                .fillPlaceholder("reviewApplicationsLink", config.getClientHost() + "/applications")
                .send(javaMailSender, uploadService);
    }

    public void sendThesisCreatedEmail(User creatingUser, Thesis thesis) {
        MailBuilder builder = new MailBuilder(config, "Thesis Created", "thesis-created");
        builder
                .sendToThesisStudents(thesis)
                .addDefaultBccRecipients()
                .addNotificationName("thesis-" + thesis.getId())
                .fillThesisPlaceholders(thesis)
                .fillUserPlaceholders(creatingUser, "creatingUser")
                .send(javaMailSender, uploadService);
    }

    public void sendThesisClosedEmail(User deletingUser, Thesis thesis) {
        MailBuilder builder = new MailBuilder(config, "Thesis Closed", "thesis-closed");
        builder
                .sendToThesisStudents(thesis)
                .addDefaultBccRecipients()
                .addNotificationName("thesis-" + thesis.getId())
                .fillThesisPlaceholders(thesis)
                .fillUserPlaceholders(deletingUser, "deletingUser")
                .send(javaMailSender, uploadService);
    }

    public void sendProposalUploadedEmail(ThesisProposal proposal) {
        MailBuilder builder = new MailBuilder(config, "Thesis Proposal Added", "thesis-proposal-uploaded");
        builder
                .addPrimarySender(proposal.getCreatedBy())
                .sendToThesisAdvisors(proposal.getThesis())
                .addNotificationName("thesis-" + proposal.getThesis().getId())
                .fillThesisProposalPlaceholders(proposal)
                .addStoredAttachment(proposal.getProposalFilename(), getThesisFilename(proposal.getThesis(), "Proposal", proposal.getProposalFilename()))
                .send(javaMailSender, uploadService);
    }

    public void sendProposalAcceptedEmail(ThesisProposal proposal) {
        MailBuilder builder = new MailBuilder(config, "Thesis Proposal Accepted", "thesis-proposal-accepted");
        builder
                .addPrimarySender(proposal.getApprovedBy())
                .sendToThesisStudents(proposal.getThesis())
                .addNotificationName("thesis-" + proposal.getThesis().getId())
                .fillThesisPlaceholders(proposal.getThesis())
                .fillThesisProposalPlaceholders(proposal)
                .send(javaMailSender, uploadService);
    }

    public void sendProposalChangeRequestEmail(User reviewingUser, Thesis thesis) {
        MailBuilder builder = new MailBuilder(
                config,
                "Changes were requested for Proposal",
                "thesis-proposal-rejected"
        );
        builder
                .sendToThesisStudents(thesis)
                .addNotificationName("thesis-" + thesis.getId())
                .fillUserPlaceholders(reviewingUser, "reviewingUser")
                .fillThesisPlaceholders(thesis)
                .fillThesisProposalPlaceholders(thesis.getProposals().getFirst())
                .fillPlaceholder(
                        "requestedChanges",
                        thesis.getFeedback().stream()
                                .filter((item) -> item.getType() == ThesisFeedbackType.PROPOSAL && item.getCompletedAt() == null)
                                .map(ThesisFeedback::getFeedback)
                                .toList()
                )
                .send(javaMailSender, uploadService);
    }

    public void sendNewCommentEmail(ThesisComment comment) {
        MailBuilder builder = new MailBuilder(
                config,
                "A Comment was posted",
                "thesis-comment-posted"
        );

        if (comment.getType() == ThesisCommentType.ADVISOR) {
            builder.sendToThesisAdvisors(comment.getThesis());
        } else {
            builder.sendToThesisStudents(comment.getThesis());
        }

        builder
                .addPrimarySender(comment.getCreatedBy())
                .addNotificationName("thesis-comments")
                .addNotificationName("thesis-" + comment.getThesis().getId())
                .fillThesisCommentPlaceholders(comment)
                .addStoredAttachment(comment.getFilename(), getUserFilename(comment.getCreatedBy(), "Comment", comment.getUploadName()))
                .send(javaMailSender, uploadService);
    }

    public void sendScheduledPresentationEmail(String action, ThesisPresentation presentation, String icsFile) {
        if (presentation.getScheduledAt().isBefore(Instant.now())) {
            return;
        }

        MailBuilder privateBuilder = new MailBuilder(
                config,
                action.equals("UPDATED") ? "Presentation updated" : "New Presentation scheduled",
                action.equals("UPDATED") ? "thesis-presentation-updated" : "thesis-presentation-scheduled"
        );
        privateBuilder
                .addPrimarySender(presentation.getCreatedBy())
                .sendToThesisStudents(presentation.getThesis())
                .addNotificationName("thesis-" + presentation.getThesis().getId())
                .fillThesisPresentationPlaceholders(presentation)
                .send(javaMailSender, uploadService);

        if (presentation.getVisibility() == ThesisPresentationVisibility.PUBLIC) {
            MailBuilder publicBuilder = new MailBuilder(
                    config,
                    action.equals("UPDATED") ? "Thesis Presentation Updated" : "Thesis Presentation Invitation",
                    action.equals("UPDATED") ? "thesis-presentation-invitation-updated" : "thesis-presentation-invitation"
            );
            publicBuilder
                    .addPrimaryRecipient(presentation.getThesis().getStudents().getFirst())
                    .fillThesisPresentationPlaceholders(presentation);

            for (ThesisPresentationInvite invite : presentation.getInvites()) {
                publicBuilder.addBccRecipient(invite.getEmail());
            }

            if (icsFile != null && !icsFile.isBlank()) {
                publicBuilder.addRawAttatchment(
                        new ByteArrayDataSource(icsFile.getBytes(StandardCharsets.UTF_8), "application/octet-stream"),
                        "event.ics"
                );
            }

            publicBuilder.send(javaMailSender, uploadService);
        }
    }

    public void sendPresentationDeletedEmail(User deletingUser, ThesisPresentation presentation) {
        if (presentation.getScheduledAt().isBefore(Instant.now())) {
            return;
        }

        MailBuilder builder = new MailBuilder(config, "Presentation deleted", "thesis-presentation-deleted");
        builder
                .sendToThesisStudents(presentation.getThesis())
                .addNotificationName("thesis-" + presentation.getThesis().getId())
                .fillThesisPresentationPlaceholders(presentation)
                .fillUserPlaceholders(deletingUser, "deletingUser")
                .send(javaMailSender, uploadService);

        if (presentation.getVisibility() == ThesisPresentationVisibility.PUBLIC) {
            MailBuilder publicBuilder = new MailBuilder(config, "Thesis Presentation Cancelled ", "thesis-presentation-invitation-cancelled");
            publicBuilder
                    .addPrimaryRecipient(presentation.getThesis().getStudents().getFirst())
                    .fillThesisPresentationPlaceholders(presentation);

            for (ThesisPresentationInvite invite : presentation.getInvites()) {
                publicBuilder.addBccRecipient(invite.getEmail());
            }

            publicBuilder.send(javaMailSender, uploadService);
        }
    }

    public void sendFinalSubmissionEmail(Thesis thesis) {
        MailBuilder builder = new MailBuilder(config, "Thesis Submitted", "thesis-final-submission");
        builder
                .sendToThesisAdvisors(thesis)
                .addNotificationName("thesis-" + thesis.getId())
                .fillThesisPlaceholders(thesis)
                //.addStoredAttachment(thesis.getFinalThesisFilename(), getThesisFilename(thesis, "File", thesis.getFinalThesisFilename()))
                //.addStoredAttachment(thesis.getFinalPresentationFilename(), getThesisFilename(thesis, "Presentation", thesis.getFinalPresentationFilename()))
                .send(javaMailSender, uploadService);
    }

    public void sendAssessmentAddedEmail(ThesisAssessment assessment) {
        MailBuilder builder = new MailBuilder(config, "Assessment added", "thesis-assessment-added");
        builder
                .addPrimarySender(assessment.getCreatedBy())
                .sendToThesisSupervisors(assessment.getThesis())
                .addNotificationName("thesis-" + assessment.getThesis().getId())
                .fillThesisAssessmentPlaceholders(assessment)
                .send(javaMailSender, uploadService);
    }

    public void sendFinalGradeEmail(Thesis thesis) {
        MailBuilder builder = new MailBuilder(config, "Final Grade available for Thesis", "thesis-final-grade");
        builder
                .sendToThesisStudents(thesis)
                .addNotificationName("thesis-" + thesis.getId())
                .fillThesisPlaceholders(thesis)
                .send(javaMailSender, uploadService);
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
