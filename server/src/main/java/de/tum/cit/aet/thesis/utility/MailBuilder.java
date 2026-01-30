package de.tum.cit.aet.thesis.utility;

import de.tum.cit.aet.thesis.constants.ThesisRoleName;
import de.tum.cit.aet.thesis.dto.*;
import de.tum.cit.aet.thesis.entity.*;
import de.tum.cit.aet.thesis.service.UploadService;
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
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.stream.Collectors;

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

    public MailBuilder addStoredAttachment(String storedFile, String filename) {
        if (storedFile == null || storedFile.isBlank()) {
            return this;
        }

        fileAttachments.add(new StoredAttachment(filename, storedFile));

        return this;
    }

    public void addRawAttatchment(ByteArrayDataSource file, String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }

        rawAttachments.add(new RawAttachment(filename, file));

    }

    public MailBuilder addNotificationName(String name) {
        notificationNames.add(name);

        return this;
    }

    public MailBuilder addPrimarySender(User user) {
        this.primarySenders.add(user);

        return this;
    }

    public MailBuilder addDefaultBccRecipients(InternetAddress researchGroupHeadMail) {
        if (researchGroupHeadMail == null || researchGroupHeadMail.getAddress().isBlank()) {
            return this;
        }
        addBccRecipient(researchGroupHeadMail);
        return this;
    }

    public MailBuilder addPrimaryRecipient(User user) {
        if (primaryRecipients.contains(user)) {
            return this;
        }

        primaryRecipients.add(user);

        return this;
    }

    public MailBuilder addSecondaryRecipient(User user) {
        if (secondaryRecipients.contains(user)) {
            return this;
        }

        secondaryRecipients.add(user);

        return this;
    }

    public void addBccRecipient(InternetAddress address) {
        if (bccRecipients.contains(address)) {
            return;
        }

        bccRecipients.add(address);

    }

    public MailBuilder sendToChairMembers(UUID researchGroupId) {
        for (User user : config.getChairMembers(researchGroupId)) {
            addPrimaryRecipient(user);
        }

        return this;
    }

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

    public MailBuilder sendToThesisSupervisors(Thesis thesis) {
        for (ThesisRole role : thesis.getRoles()) {
            if (role.getId().getRole() == ThesisRoleName.SUPERVISOR) {
                addPrimaryRecipient(role.getUser());
            }
        }

        return this;
    }

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

    public MailBuilder fillPlaceholders(Map<String, Object> model) {
        this.variables.putAll(model);
        return this;
    }

    public MailBuilder fillPlaceholder(String placeholder, Object value) {
        this.variables.put(placeholder, value);

        return this;
    }

    public MailBuilder fillUserPlaceholders(User user, String placeholder) {
        fillPlaceholder(placeholder, UserDto.fromUserEntity(user));

        return this;
    }

    public MailBuilder fillApplicationPlaceholders(Application application) {
        fillPlaceholder("application", ApplicationDto.fromApplicationEntity(application, false));
        fillPlaceholder("applicationUrl", config.getClientHost() + "/applications/" + application.getId());

        return this;
    }

    public MailBuilder fillIntervieweePlaceholders(Interviewee interviewee) {
        fillPlaceholder("interviewee", IntervieweeDTO.fromIntervieweeEntity(interviewee));
        fillPlaceholder("inviteUrl", config.getClientHost() + "/interview_booking/" + interviewee.getInterviewProcess().getId());

        return this;
    }

    public MailBuilder fillInterviewSlotPlaceholders(InterviewSlot interviewSlot) {
        fillPlaceholder("slot", InterviewSlotDto.fromInterviewSlot(interviewSlot));

        return this;
    }

    public MailBuilder fillThesisPlaceholders(Thesis thesis) {
        fillPlaceholder("thesis", ThesisDto.fromThesisEntity(thesis, false, true));
        fillPlaceholder("thesisUrl", config.getClientHost() + "/theses/" + thesis.getId());

        return this;
    }

    public MailBuilder fillThesisCommentPlaceholders(ThesisComment comment) {
        fillThesisPlaceholders(comment.getThesis());

        fillPlaceholder("comment", ThesisCommentDto.fromCommentEntity(comment));

        return this;
    }

    public MailBuilder fillThesisPresentationPlaceholders(ThesisPresentation presentation) {
        fillThesisPlaceholders(presentation.getThesis());

        fillPlaceholder("presentation", ThesisDto.ThesisPresentationDto.fromPresentationEntity(presentation));
        fillPlaceholder("presentationUrl", config.getClientHost() + "/presentations/" + presentation.getId());

        return this;
    }

    public MailBuilder fillThesisProposalPlaceholders(ThesisProposal proposal) {
        fillThesisPlaceholders(proposal.getThesis());

        fillPlaceholder("proposal", ThesisDto.ThesisProposalDto.fromProposalEntity(proposal));

        return this;
    }

    public MailBuilder fillThesisAssessmentPlaceholders(ThesisAssessment assessment) {
        fillThesisPlaceholders(assessment.getThesis());

        fillPlaceholder("assessment", ThesisDto.ThesisAssessmentDto.fromAssessmentEntity(assessment));

        return this;
    }

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
