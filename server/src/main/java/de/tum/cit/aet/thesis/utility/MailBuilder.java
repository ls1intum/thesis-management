package de.tum.cit.aet.thesis.utility;

import de.tum.cit.aet.thesis.constants.ThesisRoleName;
import de.tum.cit.aet.thesis.entity.Application;
import de.tum.cit.aet.thesis.entity.InterviewSlot;
import de.tum.cit.aet.thesis.entity.Interviewee;
import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.entity.ThesisAssessment;
import de.tum.cit.aet.thesis.entity.ThesisComment;
import de.tum.cit.aet.thesis.entity.ThesisPresentation;
import de.tum.cit.aet.thesis.entity.ThesisProposal;
import de.tum.cit.aet.thesis.entity.ThesisRole;
import de.tum.cit.aet.thesis.entity.Topic;
import de.tum.cit.aet.thesis.entity.TopicRole;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.mailvariables.MailApplication;
import de.tum.cit.aet.thesis.mailvariables.MailInterviewSlot;
import de.tum.cit.aet.thesis.mailvariables.MailThesis;
import de.tum.cit.aet.thesis.mailvariables.MailThesisAssessment;
import de.tum.cit.aet.thesis.mailvariables.MailThesisComment;
import de.tum.cit.aet.thesis.mailvariables.MailThesisPresentation;
import de.tum.cit.aet.thesis.mailvariables.MailThesisProposal;
import de.tum.cit.aet.thesis.mailvariables.MailUser;
import de.tum.cit.aet.thesis.service.UploadService;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;

import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Builds and sends template-based emails with recipients, placeholders, and attachments.
 */
public class MailBuilder {
	private static final Logger log = LoggerFactory.getLogger(MailBuilder.class);
	private final MailConfig config;

	private final List<User> primarySenders;
	private final List<User> primaryRecipients;
	private final List<User> secondaryRecipients;
	private final List<InternetAddress> bccRecipients;

	@Getter
	private final String subject;

	@Getter
	private final String templateHtml;

	@Getter
	private final Map<String, Object> variables;

	@Getter
	private final Set<String> notificationNames;

	@Getter
	private final List<StoredAttachment> fileAttachments;

	@Getter
	private final List<RawAttachment> rawAttachments;

	private record RawAttachment(String filename, ByteArrayDataSource file) {}
	private record StoredAttachment(String filename, String file) {}

	/**
	 * Creates a new mail builder.
	 *
	 * @param config the mail configuration
	 * @param subject the email subject
	 * @param templateHtml the HTML template body
	 */
	public MailBuilder(MailConfig config, String subject, String templateHtml) {
		this.config = config;

		this.primarySenders = new ArrayList<>();
		this.primaryRecipients = new ArrayList<>();
		this.secondaryRecipients = new ArrayList<>();
		this.bccRecipients = new ArrayList<>();

		this.subject = subject;
		this.templateHtml = templateHtml;

		this.variables = new HashMap<>();
		this.variables.put("config", config.getConfigDto());

		this.fileAttachments = new ArrayList<>();
		this.rawAttachments = new ArrayList<>();
		this.notificationNames = new HashSet<>();
	}

	/**
	 * Adds an attachment that is stored in the upload storage.
	 *
	 * @param storedFile the stored file name
	 * @param filename the output attachment file name
	 * @return this builder
	 */
	public MailBuilder addStoredAttachment(String storedFile, String filename) {
		if (storedFile == null || storedFile.isBlank()) {
			return this;
		}

		fileAttachments.add(new StoredAttachment(filename, storedFile));

		return this;
	}

	/**
	 * Adds a raw in-memory attachment.
	 *
	 * @param file the file data
	 * @param filename the output attachment file name
	 */
	public void addRawAttatchment(ByteArrayDataSource file, String filename) {
		if (filename == null || filename.isBlank()) {
			return;
		}

		rawAttachments.add(new RawAttachment(filename, file));
	}

	/**
	 * Adds a notification key used for recipient preference filtering.
	 *
	 * @param name the notification name
	 * @return this builder
	 */
	public MailBuilder addNotificationName(String name) {
		notificationNames.add(name);

		return this;
	}

	/**
	 * Adds a primary sender.
	 *
	 * @param user the sender user
	 * @return this builder
	 */
	public MailBuilder addPrimarySender(User user) {
		this.primarySenders.add(user);

		return this;
	}

	/**
	 * Adds the research group head as BCC recipient.
	 *
	 * @param researchGroupHeadMail the head email address
	 * @return this builder
	 */
	public MailBuilder addDefaultBccRecipients(InternetAddress researchGroupHeadMail) {
		if (researchGroupHeadMail == null || researchGroupHeadMail.getAddress().isBlank()) {
			return this;
		}
		addBccRecipient(researchGroupHeadMail);
		return this;
	}

	/**
	 * Adds a primary recipient.
	 *
	 * @param user the recipient user
	 * @return this builder
	 */
	public MailBuilder addPrimaryRecipient(User user) {
		if (primaryRecipients.contains(user)) {
			return this;
		}

		primaryRecipients.add(user);

		return this;
	}

	/**
	 * Adds a secondary recipient.
	 *
	 * @param user the recipient user
	 * @return this builder
	 */
	public MailBuilder addSecondaryRecipient(User user) {
		if (secondaryRecipients.contains(user)) {
			return this;
		}

		secondaryRecipients.add(user);

		return this;
	}

	/**
	 * Adds a BCC recipient.
	 *
	 * @param address the email address
	 */
	public void addBccRecipient(InternetAddress address) {
		if (bccRecipients.contains(address)) {
			return;
		}

		bccRecipients.add(address);
	}

	/**
	 * Adds all chair members of a research group as primary recipients.
	 *
	 * @param researchGroupId the research group ID
	 * @return this builder
	 */
	public MailBuilder sendToChairMembers(UUID researchGroupId) {
		for (User user : config.getChairMembers(researchGroupId)) {
			addPrimaryRecipient(user);
		}

		return this;
	}

	/**
	 * Filters chair recipients for new-application notifications based on notification settings.
	 *
	 * @param topic the topic associated with the application
	 * @param notificationName the notification preference key
	 * @return this builder
	 */
	public MailBuilder filterChairMembersNewApplicationNotifications(Topic topic, String notificationName) {
		List<User> filteredMembers = new ArrayList<>();
		for (User user : primaryRecipients) {
			String notificationEmail = user.getNotificationEmail(notificationName);
			String notificationEmailSuggested = user.getNotificationEmail("include-suggested-topics");

			switch (notificationEmail) {
				case "all":
					if (notificationEmailSuggested.equals("none")) {
						if (topic != null) {
							filteredMembers.add(user);
						}
					} else {
						filteredMembers.add(user);
					}
					break;
				case "own":
					if (notificationEmailSuggested.equals("none")) {
						if (topic != null && topic.getRoles().stream().map(TopicRole::getUser).anyMatch(u -> u.getId().equals(user.getId()))) {
							filteredMembers.add(user);
						}
					} else {
						if (topic == null || topic.getRoles().stream().map(TopicRole::getUser).anyMatch(u -> u.getId().equals(user.getId()))) {
							filteredMembers.add(user);
						}
					}
					break;
				default:
					break;
			}
		}

		primaryRecipients.clear();
		primaryRecipients.addAll(filteredMembers);

		return this;
	}

	/**
	 * Adds all thesis supervisors as primary recipients.
	 *
	 * @param thesis the thesis
	 * @return this builder
	 */
	public MailBuilder sendToThesisSupervisors(Thesis thesis) {
		for (ThesisRole role : thesis.getRoles()) {
			if (role.getId().getRole() == ThesisRoleName.SUPERVISOR) {
				addPrimaryRecipient(role.getUser());
			}
		}

		return this;
	}

	/**
	 * Adds all thesis advisors and supervisors as primary recipients.
	 *
	 * @param thesis the thesis
	 * @return this builder
	 */
	public MailBuilder sendToThesisAdvisors(Thesis thesis) {
		for (ThesisRole role : thesis.getRoles()) {
			if (role.getId().getRole() == ThesisRoleName.ADVISOR) {
				addPrimaryRecipient(role.getUser());
			} else if (role.getId().getRole() == ThesisRoleName.SUPERVISOR) {
				addPrimaryRecipient(role.getUser());
			}
		}

		return this;
	}

	/**
	 * Adds thesis students as primary recipients and other roles as secondary recipients.
	 *
	 * @param thesis the thesis
	 * @return this builder
	 */
	public MailBuilder sendToThesisStudents(Thesis thesis) {
		for (ThesisRole role : thesis.getRoles()) {
			if (role.getId().getRole() == ThesisRoleName.STUDENT) {
				addPrimaryRecipient(role.getUser());
			} else {
				addSecondaryRecipient(role.getUser());
			}
		}

		return this;
	}

	/**
	 * Adds all placeholder values from a model map.
	 *
	 * @param model placeholder model values
	 * @return this builder
	 */
	public MailBuilder fillPlaceholders(Map<String, Object> model) {
		this.variables.putAll(model);
		return this;
	}

	/**
	 * Adds a single placeholder value.
	 *
	 * @param placeholder the placeholder key
	 * @param value the placeholder value
	 * @return this builder
	 */
	public MailBuilder fillPlaceholder(String placeholder, Object value) {
		this.variables.put(placeholder, value);

		return this;
	}

	/**
	 * Fills user placeholders under the given placeholder key.
	 *
	 * @param user the user entity
	 * @param placeholder the top-level placeholder key
	 * @return this builder
	 */
	public MailBuilder fillUserPlaceholders(User user, String placeholder) {
		fillPlaceholder(placeholder, MailUser.fromUser(user));

		return this;
	}

	/**
	 * Fills placeholders related to an application.
	 *
	 * @param application the application entity
	 * @return this builder
	 */
	public MailBuilder fillApplicationPlaceholders(Application application) {
		fillPlaceholder("application", MailApplication.fromApplication(application));
		fillPlaceholder("applicationUrl", config.getClientHost() + "/applications/" + application.getId());

		return this;
	}

	/**
	 * Fills placeholders related to interview invitations.
	 *
	 * @param interviewee the interviewee entity
	 * @return this builder
	 */
	public MailBuilder fillIntervieweePlaceholders(Interviewee interviewee) {
		if (interviewee == null || interviewee.getInterviewProcess() == null) {
			return this;
		}

		fillPlaceholder("inviteUrl", config.getClientHost() + "/interview_booking/" + interviewee.getInterviewProcess().getId());

		return this;
	}

	/**
	 * Fills placeholders related to interview slots.
	 *
	 * @param interviewSlot the interview slot entity
	 * @return this builder
	 */
	public MailBuilder fillInterviewSlotPlaceholders(InterviewSlot interviewSlot) {
		fillPlaceholder("slot", MailInterviewSlot.fromInterviewSlot(interviewSlot));

		return this;
	}

	/**
	 * Fills thesis-related placeholders.
	 *
	 * @param thesis the thesis entity
	 * @return this builder
	 */
	public MailBuilder fillThesisPlaceholders(Thesis thesis) {
		fillPlaceholder("thesis", MailThesis.fromThesis(thesis));
		fillPlaceholder("thesisUrl", config.getClientHost() + "/theses/" + thesis.getId());

		return this;
	}

	/**
	 * Fills thesis placeholders including grade fields.
	 *
	 * @param thesis the thesis entity
	 * @return this builder
	 */
	public MailBuilder fillThesisGradePlaceholders(Thesis thesis) {
		fillPlaceholder("thesis", MailThesis.fromThesisWithGrade(thesis));
		fillPlaceholder("thesisUrl", config.getClientHost() + "/theses/" + thesis.getId());

		return this;
	}

	/**
	 * Fills placeholders for thesis comment notifications.
	 *
	 * @param comment the thesis comment
	 * @return this builder
	 */
	public MailBuilder fillThesisCommentPlaceholders(ThesisComment comment) {
		fillThesisPlaceholders(comment.getThesis());
		fillPlaceholder("comment", MailThesisComment.fromComment(comment));

		return this;
	}

	/**
	 * Fills placeholders for thesis presentation notifications.
	 *
	 * @param presentation the thesis presentation
	 * @return this builder
	 */
	public MailBuilder fillThesisPresentationPlaceholders(ThesisPresentation presentation) {
		fillThesisPlaceholders(presentation.getThesis());
		fillPlaceholder("presentation", MailThesisPresentation.fromPresentation(presentation));
		fillPlaceholder("presentationUrl", config.getClientHost() + "/presentations/" + presentation.getId());

		return this;
	}

	/**
	 * Fills placeholders for thesis proposal notifications.
	 *
	 * @param proposal the thesis proposal
	 * @return this builder
	 */
	public MailBuilder fillThesisProposalPlaceholders(ThesisProposal proposal) {
		fillThesisPlaceholders(proposal.getThesis());
		fillPlaceholder("proposal", MailThesisProposal.fromProposal(proposal));

		return this;
	}

	/**
	 * Fills placeholders for thesis assessment notifications.
	 *
	 * @param assessment the thesis assessment
	 * @return this builder
	 */
	public MailBuilder fillThesisAssessmentPlaceholders(ThesisAssessment assessment) {
		fillThesisPlaceholders(assessment.getThesis());
		fillPlaceholder("assessment", MailThesisAssessment.fromAssessment(assessment));

		return this;
	}

	/**
	 * Renders and sends the email with all configured recipients, placeholders, and attachments.
	 *
	 * @param mailSender the mail sender
	 * @param uploadService upload service used for stored attachments
	 */
	public void send(JavaMailSender mailSender, UploadService uploadService) {
		List<User> toRecipients = new ArrayList<>();
		List<User> ccRecipients = new ArrayList<>();

		userLoop: for (User recipient : primaryRecipients) {
			for (String name : notificationNames) {
				if (!recipient.isNotificationEnabled(name)) {
					continue userLoop;
				}
			}

			if (primarySenders.contains(recipient) && secondaryRecipients.isEmpty()) {
				continue;
			}

			toRecipients.add(recipient);
		}

		userLoop: for (User recipient : secondaryRecipients) {
			for (String name : notificationNames) {
				if (!recipient.isNotificationEnabled(name)) {
					continue userLoop;
				}
			}

			ccRecipients.add(recipient);
		}

		if (toRecipients.isEmpty()) {
			toRecipients = ccRecipients;
			ccRecipients = new ArrayList<>();
		}

		for (User recipient : toRecipients) {
			try {
				MimeMessage message = mailSender.createMimeMessage();

				message.setFrom("ThesisManagement <" + config.getSender().getAddress() + ">");
				message.setSender(config.getSender());

				message.addRecipient(Message.RecipientType.TO, recipient.getEmail());

				for (User secondaryRecipient : ccRecipients) {
					message.addRecipient(Message.RecipientType.CC, secondaryRecipient.getEmail());
				}

				for (InternetAddress address : bccRecipients) {
					message.addRecipient(Message.RecipientType.BCC, address);
				}

				Context templateContext = new Context();
				templateContext.setVariables(this.variables);
				templateContext.setVariable("recipient", MailUser.fromUser(recipient));
				templateContext.setVariable("DataFormatter", DataFormatter.class);

				message.setSubject(subject);

				Multipart messageContent = new MimeMultipart();

				BodyPart messageBody = new MimeBodyPart();
				messageBody.setContent(config.getTemplateEngine().process(templateHtml, templateContext), "text/html; charset=utf-8");
				messageContent.addBodyPart(messageBody);

				for (StoredAttachment data : fileAttachments) {
					MimeBodyPart attachment = new MimeBodyPart();
					attachment.setDataHandler(new DataHandler(new FileDataSource(uploadService.load(data.file()).getFile())));
					attachment.setFileName(data.filename());
					messageContent.addBodyPart(attachment);
				}

				for (RawAttachment data : rawAttachments) {
					MimeBodyPart attachment = new MimeBodyPart();
					attachment.setDataHandler(new DataHandler(data.file()));
					attachment.setFileName(data.filename());
					messageContent.addBodyPart(attachment);
				}

				message.setContent(messageContent);

				if (config.isEnabled()) {
					mailSender.send(message);
				} else {
					log.debug("Sending Mail (postfix disabled)\n{}", MailLogger.getTextFromMimeMessage(message));
				}
			} catch (Exception exception) {
				log.warn("Failed to send email", exception);
			}
		}
	}
}
