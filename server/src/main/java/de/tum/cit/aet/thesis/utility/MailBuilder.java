package de.tum.cit.aet.thesis.utility;

import de.tum.cit.aet.thesis.constants.ThesisRoleName;
import de.tum.cit.aet.thesis.dto.ApplicationDto;
import de.tum.cit.aet.thesis.dto.InterviewSlotDto;
import de.tum.cit.aet.thesis.dto.IntervieweeDTO;
import de.tum.cit.aet.thesis.dto.ThesisCommentDto;
import de.tum.cit.aet.thesis.dto.ThesisDto;
import de.tum.cit.aet.thesis.dto.UserDto;
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
 * Fluent builder for composing and sending email messages with recipients, template placeholders, and attachments.
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
	 * Initializes a new mail builder with the given mail configuration, subject, and template.
	 *
	 * @param config the mail configuration
	 * @param subject the email subject line
	 * @param templateHtml the HTML template for the email body
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
	 * Adds a stored file attachment referenced by its storage path.
	 *
	 * @param storedFile the storage path of the file
	 * @param filename the display filename for the attachment
	 * @return the updated mail builder instance
	 */
	public MailBuilder addStoredAttachment(String storedFile, String filename) {
		if (storedFile == null || storedFile.isBlank()) {
			return this;
		}

		fileAttachments.add(new StoredAttachment(filename, storedFile));

		return this;
	}

	/**
	 * Adds a raw byte array attachment to the email.
	 *
	 * @param file the byte array data source for the attachment
	 * @param filename the display filename for the attachment
	 */
	public void addRawAttatchment(ByteArrayDataSource file, String filename) {
		if (filename == null || filename.isBlank()) {
			return;
		}

		rawAttachments.add(new RawAttachment(filename, file));

	}

	/**
	 * Registers a notification name used to filter recipients based on their notification preferences.
	 *
	 * @param name the notification name to register
	 * @return the updated mail builder instance
	 */
	public MailBuilder addNotificationName(String name) {
		notificationNames.add(name);

		return this;
	}

	/**
	 * Adds a user as a primary sender, which is used to exclude them from the recipient list when appropriate.
	 *
	 * @param user the user to add as primary sender
	 * @return the updated mail builder instance
	 */
	public MailBuilder addPrimarySender(User user) {
		this.primarySenders.add(user);

		return this;
	}

	/**
	 * Adds the research group head email address as a BCC recipient if it is non-null and non-blank.
	 *
	 * @param researchGroupHeadMail the email address of the research group head
	 * @return the updated mail builder instance
	 */
	public MailBuilder addDefaultBccRecipients(InternetAddress researchGroupHeadMail) {
		if (researchGroupHeadMail == null || researchGroupHeadMail.getAddress().isBlank()) {
			return this;
		}
		addBccRecipient(researchGroupHeadMail);
		return this;
	}

	/**
	 * Adds a user as a primary (TO) recipient, preventing duplicates.
	 *
	 * @param user the user to add as a primary recipient
	 * @return the updated mail builder instance
	 */
	public MailBuilder addPrimaryRecipient(User user) {
		if (primaryRecipients.contains(user)) {
			return this;
		}

		primaryRecipients.add(user);

		return this;
	}

	/**
	 * Adds a user as a secondary (CC) recipient, preventing duplicates.
	 *
	 * @param user the user to add as a secondary recipient
	 * @return the updated mail builder instance
	 */
	public MailBuilder addSecondaryRecipient(User user) {
		if (secondaryRecipients.contains(user)) {
			return this;
		}

		secondaryRecipients.add(user);

		return this;
	}

	/**
	 * Adds an email address as a BCC recipient, preventing duplicates.
	 *
	 * @param address the email address to add as a BCC recipient
	 */
	public void addBccRecipient(InternetAddress address) {
		if (bccRecipients.contains(address)) {
			return;
		}

		bccRecipients.add(address);

	}

	/**
	 * Adds all chair members of the given research group as primary recipients.
	 *
	 * @param researchGroupId the ID of the research group
	 * @return the updated mail builder instance
	 */
	public MailBuilder sendToChairMembers(UUID researchGroupId) {
		for (User user : config.getChairMembers(researchGroupId)) {
			addPrimaryRecipient(user);
		}

		return this;
	}

	/**
	 * Filters primary recipients to only include chair members whose notification preferences match the given topic.
	 *
	 * @param topic the topic to filter notifications against
	 * @param notificationName the notification preference name to check
	 * @return the updated mail builder instance
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
	 * Adds all supervisors of the given thesis as primary recipients.
	 *
	 * @param thesis the thesis whose supervisors will be added
	 * @return the updated mail builder instance
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
	 * Adds all advisors and supervisors of the given thesis as primary recipients.
	 *
	 * @param thesis the thesis whose advisors and supervisors will be added
	 * @return the updated mail builder instance
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
	 * Adds students of the given thesis as primary recipients and all other roles as secondary recipients.
	 *
	 * @param thesis the thesis whose students and other roles will be added
	 * @return the updated mail builder instance
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
	 * Adds all entries from the given map as template placeholder variables.
	 *
	 * @param model the map of placeholder names to values
	 * @return the updated mail builder instance
	 */
	public MailBuilder fillPlaceholders(Map<String, Object> model) {
		this.variables.putAll(model);
		return this;
	}

	/**
	 * Sets a single template placeholder variable to the given value.
	 *
	 * @param placeholder the placeholder name
	 * @param value the value to set for the placeholder
	 * @return the updated mail builder instance
	 */
	public MailBuilder fillPlaceholder(String placeholder, Object value) {
		this.variables.put(placeholder, value);

		return this;
	}

	/**
	 * Sets a user DTO as a template placeholder variable under the given name.
	 *
	 * @param user the user to convert to a DTO and set as a placeholder
	 * @param placeholder the placeholder name for the user DTO
	 * @return the updated mail builder instance
	 */
	public MailBuilder fillUserPlaceholders(User user, String placeholder) {
		fillPlaceholder(placeholder, UserDto.fromUserEntity(user));

		return this;
	}

	/**
	 * Sets the application DTO and application URL as template placeholder variables.
	 *
	 * @param application the application to populate placeholders from
	 * @return the updated mail builder instance
	 */
	public MailBuilder fillApplicationPlaceholders(Application application) {
		fillPlaceholder("application", ApplicationDto.fromApplicationEntity(application, false));
		fillPlaceholder("applicationUrl", config.getClientHost() + "/applications/" + application.getId());

		return this;
	}

	/**
	 * Sets the interviewee DTO and interview booking URL as template placeholder variables.
	 *
	 * @param interviewee the interviewee to populate placeholders from
	 * @return the updated mail builder instance
	 */
	public MailBuilder fillIntervieweePlaceholders(Interviewee interviewee) {
		fillPlaceholder("interviewee", IntervieweeDTO.fromIntervieweeEntity(interviewee));
		fillPlaceholder("inviteUrl", config.getClientHost() + "/interview_booking/" + interviewee.getInterviewProcess().getId());

		return this;
	}

	/**
	 * Sets the interview slot DTO as a template placeholder variable.
	 *
	 * @param interviewSlot the interview slot to populate placeholders from
	 * @return the updated mail builder instance
	 */
	public MailBuilder fillInterviewSlotPlaceholders(InterviewSlot interviewSlot) {
		fillPlaceholder("slot", InterviewSlotDto.fromInterviewSlot(interviewSlot));

		return this;
	}

	/**
	 * Sets the thesis DTO and thesis URL as template placeholder variables.
	 *
	 * @param thesis the thesis to populate placeholders from
	 * @return the updated mail builder instance
	 */
	public MailBuilder fillThesisPlaceholders(Thesis thesis) {
		fillPlaceholder("thesis", ThesisDto.fromThesisEntity(thesis, false, true));
		fillPlaceholder("thesisUrl", config.getClientHost() + "/theses/" + thesis.getId());

		return this;
	}

	/**
	 * Sets the thesis comment DTO and associated thesis placeholders as template variables.
	 *
	 * @param comment the thesis comment to populate placeholders from
	 * @return the updated mail builder instance
	 */
	public MailBuilder fillThesisCommentPlaceholders(ThesisComment comment) {
		fillThesisPlaceholders(comment.getThesis());

		fillPlaceholder("comment", ThesisCommentDto.fromCommentEntity(comment));

		return this;
	}

	/**
	 * Sets the thesis presentation DTO, presentation URL, and associated thesis placeholders as template variables.
	 *
	 * @param presentation the thesis presentation to populate placeholders from
	 * @return the updated mail builder instance
	 */
	public MailBuilder fillThesisPresentationPlaceholders(ThesisPresentation presentation) {
		fillThesisPlaceholders(presentation.getThesis());

		fillPlaceholder("presentation", ThesisDto.ThesisPresentationDto.fromPresentationEntity(presentation));
		fillPlaceholder("presentationUrl", config.getClientHost() + "/presentations/" + presentation.getId());

		return this;
	}

	/**
	 * Sets the thesis proposal DTO and associated thesis placeholders as template variables.
	 *
	 * @param proposal the thesis proposal to populate placeholders from
	 * @return the updated mail builder instance
	 */
	public MailBuilder fillThesisProposalPlaceholders(ThesisProposal proposal) {
		fillThesisPlaceholders(proposal.getThesis());

		fillPlaceholder("proposal", ThesisDto.ThesisProposalDto.fromProposalEntity(proposal));

		return this;
	}

	/**
	 * Sets the thesis assessment DTO and associated thesis placeholders as template variables.
	 *
	 * @param assessment the thesis assessment to populate placeholders from
	 * @return the updated mail builder instance
	 */
	public MailBuilder fillThesisAssessmentPlaceholders(ThesisAssessment assessment) {
		fillThesisPlaceholders(assessment.getThesis());

		fillPlaceholder("assessment", ThesisDto.ThesisAssessmentDto.fromAssessmentEntity(assessment));

		return this;
	}

	/**
	 * Composes and sends the email to all filtered recipients using the configured mail sender and upload service.
	 *
	 * @param mailSender the mail sender to use for sending
	 * @param uploadService the upload service for resolving file attachments
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
				templateContext.setVariable("recipient", UserDto.fromUserEntity(recipient));
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
					log.info("Sending Mail (postfix disabled)\n{}", MailLogger.getTextFromMimeMessage(message));
				}
			} catch (Exception exception) {
				log.warn("Failed to send email", exception);
			}
		}
	}
}
