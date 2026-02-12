package de.tum.cit.aet.thesis.mailVariables;

import de.tum.cit.aet.thesis.dto.MailVariableDto;

import java.util.ArrayList;
import java.util.List;

public class MailVariablesBuilder {

    public List<MailVariableDto> getMailVariables(String templateCase) {
        List<MailVariableDto> mailVariables = new ArrayList<>(MailUser.templateVariables("recipient", "Recipient", "General"));

        switch (templateCase) {
            case "APPLICATION_ACCEPTED", "APPLICATION_ACCEPTED_NO_ADVISOR" -> {
                mailVariables.addAll(MailApplication.templateVariables());
                mailVariables.addAll(MailThesis.templateVariables());
                mailVariables.addAll(userTemplateVariables(templateCase));
            }
            case "APPLICATION_REJECTED_TOPIC_REQUIREMENTS", "APPLICATION_REJECTED_TOPIC_OUTDATED", "APPLICATION_REJECTED_TITLE_NOT_INTERESTING", "APPLICATION_REJECTED", "APPLICATION_REJECTED_TOPIC_FILLED", "APPLICATION_REJECTED_STUDENT_REQUIREMENTS", "APPLICATION_CREATED_STUDENT", "APPLICATION_CREATED_CHAIR" ->
                    mailVariables.addAll(MailApplication.templateVariables());
            case "THESIS_CREATED", "THESIS_CLOSED" -> {
                mailVariables.addAll(MailThesis.templateVariables());
                mailVariables.addAll(userTemplateVariables(templateCase));
            }
            case "THESIS_PROPOSAL_UPLOADED", "THESIS_PROPOSAL_REJECTED", "THESIS_PROPOSAL_ACCEPTED" -> {
                mailVariables.addAll(MailThesisProposal.templateVariables());
                mailVariables.addAll(MailThesis.templateVariables());
                mailVariables.addAll(userTemplateVariables(templateCase));
                if ("THESIS_PROPOSAL_REJECTED".equals(templateCase)) {
                    mailVariables.addAll(proposalRejectedTemplateVariables());
                }
            }
            case "THESIS_PRESENTATION_UPDATED", "THESIS_PRESENTATION_SCHEDULED", "THESIS_PRESENTATION_INVITATION_CANCELLED", "THESIS_PRESENTATION_INVITATION_UPDATED", "THESIS_PRESENTATION_INVITATION" -> {
                mailVariables.addAll(MailThesisPresentation.templateVariables());
                mailVariables.addAll(MailThesis.templateVariables());
            }
            case "THESIS_PRESENTATION_DELETED" -> {
                mailVariables.addAll(MailThesisPresentation.templateVariables());
                mailVariables.addAll(MailThesis.templateVariables());
                mailVariables.addAll(userTemplateVariables(templateCase));
            }
            case "THESIS_FINAL_SUBMISSION", "THESIS_FINAL_GRADE" -> mailVariables.addAll(MailThesis.templateVariables());
            case "THESIS_ASSESSMENT_ADDED" -> {
                mailVariables.addAll(MailThesisAssessment.templateVariables());
                mailVariables.addAll(MailThesis.templateVariables());
            }
            case "THESIS_COMMENT_POSTED" -> {
                mailVariables.addAll(MailThesisComment.templateVariables());
                mailVariables.addAll(MailThesis.templateVariables());
            }
            case "APPLICATION_REMINDER" -> mailVariables.addAll(MailApplicationReminder.templateVariables());
            case "APPLICATION_AUTOMATIC_REJECT_REMINDER" -> mailVariables.addAll(automaticRejectReminderTemplateVariables());
            case "INTERVIEW_INVITATION", "INTERVIEW_INVITATION_REMINDER" -> {
                mailVariables.addAll(MailApplication.templateVariables());
                mailVariables.addAll(MailInterview.templateVariables());
            }
            case "INTERVIEW_SLOT_BOOKED_CONFORMATION", "INTERVIEW_SLOT_BOOKED_CANCELLATION" -> {
                mailVariables.addAll(MailApplication.templateVariables());
                mailVariables.addAll(MailInterview.templateVariables());
                mailVariables.addAll(MailInterviewSlot.templateVariables());
            }
            default -> {
            }
        }

        return mailVariables;
    }

    private List<MailVariableDto> userTemplateVariables(String templateCase) {
        return switch (templateCase) {
            case "APPLICATION_ACCEPTED", "APPLICATION_ACCEPTED_NO_ADVISOR" ->
                    MailUser.templateVariables("advisor", "Advisor", "User");
            case "THESIS_CREATED" -> MailUser.templateVariables("creatingUser", "Creating User", "User");
            case "THESIS_CLOSED", "THESIS_PRESENTATION_DELETED" ->
                    MailUser.templateVariables("deletingUser", "Deleting User", "User");
            case "THESIS_PROPOSAL_REJECTED" ->
                    MailUser.templateVariables("reviewingUser", "Reviewing User", "User");
            default -> List.of();
        };
    }

    private List<MailVariableDto> proposalRejectedTemplateVariables() {
        return List.of(
                new MailVariableDto(
                        "Requested Changes",
                        "[[${requestedChanges}]]",
                        "Clarify evaluation setup",
                        "Proposal"
                )
        );
    }

    private List<MailVariableDto> automaticRejectReminderTemplateVariables() {
        return List.of(
                new MailVariableDto(
                        "Applications",
                        "[[${applications}]]",
                        "List of expiring applications",
                        "Application Reminder"
                ),
                new MailVariableDto(
                        "Client Host",
                        "[[${clientHost}]]",
                        "https://thesis-management.com",
                        "Application Reminder"
                )
        );
    }
}
