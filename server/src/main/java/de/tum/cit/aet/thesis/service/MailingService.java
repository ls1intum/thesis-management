package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.constants.ApplicationRejectReason;
import de.tum.cit.aet.thesis.constants.ThesisCommentType;
import de.tum.cit.aet.thesis.constants.ThesisFeedbackType;
import de.tum.cit.aet.thesis.constants.ThesisPresentationVisibility;
import de.tum.cit.aet.thesis.cron.model.ApplicationRejectObject;
import de.tum.cit.aet.thesis.entity.Application;
import de.tum.cit.aet.thesis.entity.DataExport;
import de.tum.cit.aet.thesis.entity.EmailTemplate;
import de.tum.cit.aet.thesis.entity.InterviewSlot;
import de.tum.cit.aet.thesis.entity.Interviewee;
import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.entity.ThesisAssessment;
import de.tum.cit.aet.thesis.entity.ThesisComment;
import de.tum.cit.aet.thesis.entity.ThesisFeedback;
import de.tum.cit.aet.thesis.entity.ThesisPresentation;
import de.tum.cit.aet.thesis.entity.ThesisPresentationInvite;
import de.tum.cit.aet.thesis.entity.ThesisProposal;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.EmailTemplateRepository;
import de.tum.cit.aet.thesis.utility.DataFormatter;
import de.tum.cit.aet.thesis.utility.MailBuilder;
import de.tum.cit.aet.thesis.utility.MailConfig;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.mail.util.ByteArrayDataSource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Service for composing and sending email notifications for applications, theses, interviews, and presentations. */
@Service
public class MailingService {
	private final JavaMailSender javaMailSender;
	private final UploadService uploadService;
	private final MailConfig config;
	private final EmailTemplateRepository emailTemplateRepository;

	private static final String NOTIFICATION_NAME_START = "thesis-";

	/**
	 * Injects the mail sender, upload service, mail configuration, and email template repository.
	 *
	 * @param javaMailSender the mail sender for sending emails
	 * @param uploadService the service for handling file uploads
	 * @param config the mail configuration
	 * @param emailTemplateRepository the repository for email templates
	 */
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

	/**
	 * Sends application creation notification emails to the research group members and the student.
	 *
	 * @param application the newly created application
	 */
	public void sendApplicationCreatedEmail(Application application) {
		boolean includeData = application.getResearchGroup().getResearchGroupSettings() != null
				&& application.getResearchGroup().getResearchGroupSettings().isIncludeApplicationDataInEmail();

		MailBuilder researchGroupMailBuilder;
		if (includeData) {
			EmailTemplate researchGroupEmailTemplate = loadTemplate(
					application.getResearchGroup().getId(),
					"APPLICATION_CREATED_CHAIR",
					"en");
			researchGroupMailBuilder = prepareApplicationCreatedMailBuilder(application, researchGroupEmailTemplate);
		} else {
			researchGroupMailBuilder = prepareMinimalApplicationMailBuilder(application);
		}
		researchGroupMailBuilder
			.sendToChairMembers(application.getResearchGroup().getId())
			.addNotificationName("new-applications")
			.filterChairMembersNewApplicationNotifications(application.getTopic(), "new-applications")
			.send(javaMailSender, uploadService);

		if (includeData) {
			EmailTemplate researchGroupEmailTemplate = loadTemplate(
					application.getResearchGroup().getId(),
					"APPLICATION_CREATED_CHAIR",
					"en");
			sendNotificationCopy(application.getResearchGroup(),
					prepareApplicationCreatedMailBuilder(application, researchGroupEmailTemplate));
		} else {
			sendNotificationCopy(application.getResearchGroup(),
					prepareMinimalApplicationMailBuilder(application));
		}

		EmailTemplate studentEmailTemplate = loadTemplate(
				application.getResearchGroup().getId(),
				"APPLICATION_CREATED_STUDENT",
				"en");
		MailBuilder studentMailBuilder = new MailBuilder(config,
				studentEmailTemplate.getSubject(), studentEmailTemplate.getBodyHtml());
		studentMailBuilder
				.addPrimaryRecipient(application.getUser())
				.addStoredAttachment(application.getUser().getCvFilename(),
						getUserFilename(application.getUser(), "CV",
								application.getUser().getCvFilename()))
				.addStoredAttachment(application.getUser().getExaminationFilename(),
						getUserFilename(application.getUser(), "Examination Report",
								application.getUser().getExaminationFilename()))
				.addStoredAttachment(application.getUser().getDegreeFilename(),
						getUserFilename(application.getUser(), "Degree Report",
								application.getUser().getDegreeFilename()))
				.fillApplicationPlaceholders(application)
				.send(javaMailSender, uploadService);
	}

	private MailBuilder prepareApplicationCreatedMailBuilder(Application application, EmailTemplate template) {
		return new MailBuilder(config, template.getSubject(), template.getBodyHtml())
				.addStoredAttachment(application.getUser().getCvFilename(),
						getUserFilename(application.getUser(), "CV",
								application.getUser().getCvFilename()))
				.addStoredAttachment(application.getUser().getExaminationFilename(),
						getUserFilename(application.getUser(), "Examination Report",
								application.getUser().getExaminationFilename()))
				.addStoredAttachment(application.getUser().getDegreeFilename(),
						getUserFilename(application.getUser(), "Degree Report",
								application.getUser().getDegreeFilename()))
				.fillApplicationPlaceholders(application);
	}

	private MailBuilder prepareMinimalApplicationMailBuilder(Application application) {
		String applicantName = "";
		if (application.getUser() != null) {
			String firstName = application.getUser().getFirstName();
			String lastName = application.getUser().getLastName();
			applicantName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
		}

		String thesisTitle = "";
		if (application.getThesisTitle() != null && !application.getThesisTitle().isBlank()) {
			thesisTitle = application.getThesisTitle();
		} else if (application.getTopic() != null && application.getTopic().getTitle() != null
				&& !application.getTopic().getTitle().isBlank()) {
			thesisTitle = application.getTopic().getTitle();
		}

		String applicationUrl = config.getClientHost() + "/applications/" + application.getId();

		String subject = "New Thesis Application";
		String body = "<p>Dear colleague,</p>"
				+ "<p>A new thesis application has been submitted by <strong>" + applicantName + "</strong>"
				+ (thesisTitle.isEmpty() ? "." : " for the topic <strong>" + thesisTitle + "</strong>.")
				+ "</p>"
				+ "<p>You can view the full application details here: "
				+ "<a target=\"_blank\" rel=\"noopener noreferrer nofollow\" href=\"" + applicationUrl + "\">"
				+ applicationUrl + "</a></p>";

		return new MailBuilder(config, subject, body);
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

	/**
	 * Sends an application acceptance email to the student with advisor and thesis details.
	 *
	 * @param application the accepted application
	 * @param thesis the thesis created from the application
	 */
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

	/**
	 * Sends an application rejection email to the student with the specified rejection reason.
	 *
	 * @param application the rejected application
	 * @param reason the reason for rejection
	 */
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

	/**
	 * Sends a reminder email to a user about their unreviewed applications.
	 *
	 * @param user the user to send the reminder to
	 * @param unreviewedApplications the number of unreviewed applications
	 */
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

	/**
	 * Sends a reminder email warning about applications that will be automatically rejected.
	 *
	 * @param user the user to send the reminder to
	 * @param applications the list of applications pending automatic rejection
	 */
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

	/**
	 * Sends an interview invitation or reminder email to an interviewee.
	 *
	 * @param interviewee the interviewee to send the invitation to
	 * @param firstInvitation whether this is the first invitation or a reminder
	 */
	public void sendInterviewInvitationEmail(Interviewee interviewee, Boolean firstInvitation) {
		EmailTemplate emailTemplate = loadTemplate(
				interviewee.getApplication().getResearchGroup().getId(),
				firstInvitation ? "INTERVIEW_INVITATION" : "INTERVIEW_INVITATION_REMINDER",
				"en");

		MailBuilder mailBuilder = new MailBuilder(config, emailTemplate.getSubject(), emailTemplate.getBodyHtml());
		mailBuilder
				.addPrimaryRecipient(interviewee.getApplication().getUser())
				.addNotificationName(emailTemplate.getSubject())
				.fillApplicationPlaceholders(interviewee.getApplication())
				.fillIntervieweePlaceholders(interviewee)
				.send(javaMailSender, uploadService);
	}

	/**
	 * Sends an interview slot booking confirmation or cancellation email.
	 *
	 * @param slot the interview slot that was booked or cancelled
	 * @param type the email type, either "BOOK" or "CANCEL"
	 */
	public void sendInterviewSlotConfirmationEmail(InterviewSlot slot, String type) {
		String templateCase = switch (type) {
			case "BOOK" -> "INTERVIEW_SLOT_BOOKED_CONFORMATION";
			case "CANCEL" -> "INTERVIEW_SLOT_BOOKED_CANCELLATION";
			default -> throw new IllegalArgumentException("Invalid interview slot email type: " + type);
		};
		EmailTemplate emailTemplate = loadTemplate(
				slot.getInterviewee().getApplication().getResearchGroup().getId(),
				templateCase,
				"en");

		User advisor = slot.getInterviewProcess().getTopic().getAdvisors().getFirst();

		MailBuilder mailBuilder = new MailBuilder(config, emailTemplate.getSubject(), emailTemplate.getBodyHtml());
		mailBuilder
				.addPrimaryRecipient(slot.getInterviewee().getApplication().getUser())
				.addSecondaryRecipient(advisor)
				.addNotificationName(emailTemplate.getSubject())
				.fillApplicationPlaceholders(slot.getInterviewee().getApplication())
				.fillIntervieweePlaceholders(slot.getInterviewee())
				.fillInterviewSlotPlaceholders(slot)
				.send(javaMailSender, uploadService);
	}

	/**
	 * Sends a thesis creation notification email to the thesis students.
	 *
	 * @param creatingUser the user who created the thesis
	 * @param thesis the newly created thesis
	 */
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

	/**
	 * Sends a thesis closure notification email to the thesis students.
	 *
	 * @param deletingUser the user who closed the thesis
	 * @param thesis the closed thesis
	 */
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

	/**
	 * Sends a proposal upload notification email to thesis advisors with the proposal as an attachment.
	 *
	 * @param proposal the uploaded thesis proposal
	 */
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

	/**
	 * Sends a proposal acceptance notification email to the thesis students.
	 *
	 * @param proposal the accepted thesis proposal
	 */
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

	/**
	 * Sends a proposal change request email to thesis students with the requested changes.
	 *
	 * @param reviewingUser the user who requested changes
	 * @param thesis the thesis whose proposal needs changes
	 */
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

	/**
	 * Sends a new comment notification email to advisors or students depending on the comment type.
	 *
	 * @param comment the newly posted thesis comment
	 */
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

	/**
	 * Sends scheduled or updated presentation notification emails to students and optionally to public invitees.
	 *
	 * @param action the action type, e.g. "UPDATED" or "SCHEDULED"
	 * @param presentation the thesis presentation
	 * @param icsFile the ICS calendar file content, or null if not applicable
	 */
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

	/**
	 * Sends presentation deletion notification emails to students and optionally to public invitees.
	 *
	 * @param deletingUser the user who deleted the presentation
	 * @param presentation the deleted thesis presentation
	 */
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

	/**
	 * Sends a final thesis submission notification email to the thesis advisors.
	 *
	 * @param thesis the submitted thesis
	 */
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

	/**
	 * Sends an assessment added notification email to thesis supervisors.
	 *
	 * @param assessment the newly added thesis assessment
	 */
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

	/**
	 * Sends the final grade notification email to the thesis students.
	 *
	 * @param thesis the graded thesis
	 */
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
				.fillThesisGradePlaceholders(thesis)
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

	/**
	 * Sends an email notifying the user that their data export is ready for download.
	 *
	 * @param user the user who requested the export
	 * @param export the completed data export
	 */
	public void sendDataExportReadyEmail(User user, DataExport export) {
		EmailTemplate template = loadTemplate(null, "DATA_EXPORT_READY", "en");

		String downloadUrl = config.getClientHost() + "/data-export";

		new MailBuilder(config, template.getSubject(), template.getBodyHtml())
				.addPrimaryRecipient(user)
				.fillUserPlaceholders(user, "user")
				.fillPlaceholder("downloadUrl", downloadUrl)
				.send(javaMailSender, uploadService);
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
