package de.tum.cit.aet.thesis.mailVariables;

import de.tum.cit.aet.thesis.dto.MailVariableDto;

import java.util.ArrayList;
import java.util.List;

public class MailVariablesBuilder {
    public MailVariablesBuilder() {}

    enum MailVariableType {
        Application,
        Thesis,
        User,
        Proposal,
        Presentation,
        Assessment,
        ThesisComment,
        ApplicationReminder
    }

    public List<MailVariableDto> getMailVariables( String templateCase ) {
        return switch (templateCase) {
            case "APPLICATION_ACCEPTED",
                 "APPLICATION_ACCEPTED_NO_ADVISOR"
                    -> getVariablesFor(List.of(MailVariableType.Application, MailVariableType.Thesis, MailVariableType.User), templateCase);
            case "APPLICATION_REJECTED_TOPIC_REQUIREMENTS",
                 "APPLICATION_REJECTED_TOPIC_OUTDATED",
                 "APPLICATION_REJECTED_TITLE_NOT_INTERESTING",
                 "APPLICATION_REJECTED",
                 "APPLICATION_REJECTED_TOPIC_FILLED",
                 "APPLICATION_REJECTED_STUDENT_REQUIREMENTS",
                 "APPLICATION_CREATED_STUDENT",
                 "APPLICATION_CREATED_CHAIR"
                    -> getVariablesFor(List.of(MailVariableType.Application), templateCase);
            case "THESIS_CREATED",
                 "THESIS_CLOSED"
                    -> getVariablesFor(List.of(MailVariableType.Thesis, MailVariableType.User), templateCase);
            case "THESIS_PROPOSAL_UPLOADED",
                 "THESIS_PROPOSAL_REJECTED",
                 "THESIS_PROPOSAL_ACCEPTED"
                    -> getVariablesFor(List.of(MailVariableType.Proposal, MailVariableType.Thesis), templateCase);
            case "THESIS_PRESENTATION_UPDATED",
                 "THESIS_PRESENTATION_SCHEDULED",
                 "THESIS_PRESENTATION_INVITATION_CANCELLED",
                 "THESIS_PRESENTATION_INVITATION_UPDATED",
                 "THESIS_PRESENTATION_INVITATION"
                    -> getVariablesFor(List.of(MailVariableType.Presentation, MailVariableType.Thesis), templateCase);
            case "THESIS_PRESENTATION_DELETED"
                    -> getVariablesFor(List.of(MailVariableType.Presentation, MailVariableType.Thesis, MailVariableType.User), templateCase);
            case "THESIS_FINAL_SUBMISSION",
                 "THESIS_FINAL_GRADE"
                    -> getVariablesFor(List.of(MailVariableType.Thesis), templateCase);
            case "THESIS_ASSESSMENT_ADDED"
                    -> getVariablesFor(List.of(MailVariableType.Assessment), templateCase);
            case "THESIS_COMMENT_POSTED"
                    -> getVariablesFor(List.of(MailVariableType.ThesisComment), templateCase);
            case "APPLICATION_REMINDER"
                    -> getVariablesFor(List.of(MailVariableType.ApplicationReminder), templateCase);
            default -> getVariablesFor(new ArrayList<>(), templateCase);
        };
    }

    private List<MailVariableDto> getVariablesFor(List<MailVariableType> accessibleTypes, String templateCase) {
        // Implementation to return mail variables based on accessible types
        List<MailVariableDto> mailVariables = new ArrayList<>(getGeneralVariables());

        for (MailVariableType type : accessibleTypes) {
            switch (type) {
                case Application -> mailVariables.addAll(getApplicationVariables());
                case Thesis -> mailVariables.addAll(getThesisVariables());
                case User -> mailVariables.addAll(getUserVariables(templateCase));
                case Proposal -> mailVariables.addAll(getProposalVariables());
                case Presentation -> mailVariables.addAll(getPresentationVariables());
                case Assessment -> mailVariables.addAll(getAssessmentVariables());
                case ThesisComment -> mailVariables.addAll(getThesisCommentVariables());
                case ApplicationReminder -> mailVariables.addAll(getApplicationReminderVariables());
            }
        }
        return mailVariables;
    }

    private List<MailVariableDto> getGeneralVariables() {
        MailVariableDto recipientFirstNameVariable = new MailVariableDto(
                "Recipient First Name",
                "[[${recipient.firstName}]]",
                "Max",
                "General"
        );

        MailVariableDto  recipientLastNameVariable = new MailVariableDto(
                "Recipient Last Name",
                "[[${recipient.lastName}]]",
                "Mustermann",
                "General"
        );

        return List.of( recipientFirstNameVariable, recipientLastNameVariable);
    }

    private List<MailVariableDto> getApplicationVariables() {
        // TODO: Think about which ones make sense
        MailVariableDto thesisTitleVariable = new MailVariableDto(
                "Thesis Title",
                "[[${application.thesisTitle}]]",
                "Deep Learning for Natural Language Processing",
                "Application"
        );

        MailVariableDto applicantFirstNameVariable = new MailVariableDto(
                "Applicant First Name",
                "[[${application.user.firstName}]]",
                "Max",
                "Application"
        );

        MailVariableDto applicantLastNameVariable = new MailVariableDto(
                "Applicant Last Name",
                "[[${application.user.lastName}]]",
                "Mustermann",
                "Application"
        );

        MailVariableDto applicationEmailVariable = new MailVariableDto(
                "Applicant Email",
                "[[${application.user.email}]]",
                "max.musterman@test.de",
                "Application"
        );

        MailVariableDto applicationUniversityIdVariable = new MailVariableDto(
                "Applicant University ID",
                "[[${application.user.universityId}]]",
                "ge47zig",
                "Application"
        );

        MailVariableDto applicationUniversityMatriculationNrVariable = new MailVariableDto(
                "Applicant Matriculation Number",
                "[[${application.user.matriculationNumber}]]",
                "12345678",
                "Application"
        );

        MailVariableDto applicationStudyProgramVariable = new MailVariableDto(
                "Applicant Study Program",
                "[[${DataFormatter.formatConstantName(application.user.studyProgram)}]]",
                "Informatics",
                "Application"
        );

        MailVariableDto applicationStudyDegreeVariable = new MailVariableDto(
                "Applicant Study Degree",
                "[[${DataFormatter.formatConstantName(application.user.studyDegree)}]]",
                "Bachelor's",
                "Application"
        );

        MailVariableDto applicationSemesterVariable = new MailVariableDto(
                "Applicant Semester",
                "[[${DataFormatter.formatSemester(application.user.enrolledAt)}]]",
                "Winter Semester 2023/2024",
                "Application"
        );

        MailVariableDto applicationThesisTitle = new MailVariableDto(
                "Application Thesis Title",
                "[[${application.thesisTitle}]]",
                "Deep Learning for Natural Language Processing",
                "Application"
        );

        MailVariableDto desiredStartDateVariable = new MailVariableDto(
                "Desired Start Date",
                "[[${DataFormatter.formatDate(application.desiredStartDate)}]]",
                "01.10.2024",
                "Application"
        );

        MailVariableDto motivationVariable = new MailVariableDto(
                "Application Motivation",
                "[[${application.motivation}]]",
                "I am very interested in the topic and have relevant experience in deep learning.",
                "Application"
        );

        MailVariableDto specialSkillsVariable = new MailVariableDto(
                "Applicant Special Skills",
                "[[${application.user.specialSkills}]]",
                "Python, Java, Machine Learning",
                "Application"
        );

        MailVariableDto interestsVariable = new MailVariableDto(
                "Applicant Interests",
                "[[${application.user.interests}]]",
                "Artificial Intelligence, Natural Language Processing, Computer Vision",
                "Application"
        );

        MailVariableDto projectsVariable = new MailVariableDto(
                "Applicant Projects",
                "[[${application.user.projects}]]",
                "Chatbot Development, Image Recognition System",
                "Application"
        );

        MailVariableDto applicationUrlVariable = new MailVariableDto(
                "Application URL",
                "[[${applicationUrl}]]",
                "https://thesis-management.com/applications/123",
                "Application"
        );

        return List.of(thesisTitleVariable, applicantFirstNameVariable, applicantLastNameVariable, applicationEmailVariable, applicationUniversityIdVariable, applicationUniversityMatriculationNrVariable, applicationStudyProgramVariable, applicationStudyDegreeVariable, applicationSemesterVariable, applicationThesisTitle, desiredStartDateVariable, motivationVariable, specialSkillsVariable, interestsVariable, projectsVariable, applicationUrlVariable);
    }

    private List<MailVariableDto> getThesisVariables() {

        MailVariableDto thesisTitleVariable = new MailVariableDto(
                "Thesis Title",
                "[[${thesis.title}]]",
                "Deep Learning for Natural Language Processing",
                "Thesis"
        );

        MailVariableDto thesisUrlVariable = new MailVariableDto(
                "Thesis URL",
                "[[${thesisUrl}]]",
                "https://thesis-management.com/theses/123",
                "Thesis"
        );

        MailVariableDto thesisTypeVariable = new MailVariableDto(
                "Thesis Type",
                "[[${DataFormatter.formatConstantName(thesis.type)}]]",
                "Bachelor's",
                "Thesis"
        );

        MailVariableDto thesisStudentsVariable = new MailVariableDto(
                "Thesis Student(s)",
                "[[${DataFormatter.formatUsers(thesis.students)}]]",
                "Max Mustermann",
                "Thesis"
        );

        MailVariableDto thesisSupervisorsVariable = new MailVariableDto(
                "Thesis Supervisor(s)",
                "[[${DataFormatter.formatUsers(thesis.supervisors)}]]",
                "Maria Musterfrau",
                "Thesis"
        );

        MailVariableDto thesisAdvisorsVariable = new MailVariableDto(
                "Thesis Advisor(s)",
                "[[${DataFormatter.formatUsers(thesis.advisors)}]]",
                "Malena Musterfrau",
                "Thesis"
        );

        MailVariableDto thesisAbstractVariable = new MailVariableDto(
                "Thesis Abstract",
                "[[${thesis.abstractText}]]",
                "This thesis explores the application of deep learning techniques to natural language processing tasks, including text classification, sentiment analysis, and machine translation. The study evaluates various neural network architectures and training strategies to optimize performance on benchmark datasets.",
                "Thesis"
        );

        return List.of(thesisTitleVariable, thesisUrlVariable, thesisTypeVariable, thesisStudentsVariable, thesisSupervisorsVariable, thesisAdvisorsVariable, thesisAbstractVariable);
    }

    private List<MailVariableDto> getUserVariables(String templateCase) {
        //TODO
        return new ArrayList<>();
    }

    private List<MailVariableDto> getProposalVariables() {
        MailVariableDto proposalCreatorFirstNameVariable = new MailVariableDto(
                "Proposal Creator First Name",
                "[[${proposal.createdBy.firstName}]]",
                "Max",
                "Proposal"
        );

        MailVariableDto proposalCreatorLastNameVariable = new MailVariableDto(
                "Proposal Creator Last Name",
                "[[${proposal.createdBy.lastName}]]",
                "Mustermann",
                "Proposal"
        );

        MailVariableDto proposalApproverFirstName = new MailVariableDto(
                "Proposal Approver First Name",
                "[[${proposal.approvedBy.firstName}]]",
                "Maria",
                "Proposal"
        );

        MailVariableDto proposalApproverLastName = new MailVariableDto(
                "Proposal Approver Last Name",
                "[[${proposal.approvedBy.lastName}]]",
                "Musterfrau",
                "Proposal"
        );

        return List.of(proposalCreatorFirstNameVariable, proposalCreatorLastNameVariable, proposalApproverFirstName, proposalApproverLastName);
    }

    private List<MailVariableDto> getPresentationVariables() {
        MailVariableDto presentationCreatorFirstNameVariable = new MailVariableDto(
                "Presentation Creator First Name",
                "[[${presentation.createdBy.firstName}]]",
                "Max",
                "Presentation"
        );

        MailVariableDto presentationCreatorLastNameVariable = new MailVariableDto(
                "Presentation Creator Last Name",
                "[[${presentation.createdBy.lastName}]]",
                "Mustermann",
                "Presentation"
        );

        MailVariableDto presentationTypeVariable = new MailVariableDto(
                "Presentation Type",
                "[[${DataFormatter.formatEnum(presentation.type)}]]",
                "Thesis Proposal Presentation",
                "Presentation"
        );

        MailVariableDto presentationDateVariable = new MailVariableDto(
                "Presentation Date",
                "[[${DataFormatter.formatDateTime(presentation.scheduledAt)}]]",
                "01.10.2024, 14:00",
                "Presentation"
        );

        MailVariableDto presentationLocationVariable = new MailVariableDto(
                "Presentation Location",
                "[[${DataFormatter.formatOptionalString(presentation.location)}]]",
                "Room 101, TUM Campus",
                "Presentation"
        );

        MailVariableDto streamUrlVariable = new MailVariableDto(
                "Presentation Stream URL",
                "[[${DataFormatter.formatOptionalString(presentation.streamUrl)}]]",
                "https://videoconference.com/stream/123",
                "Presentation"
        );

        //TODO: Thesis Presentation Invitation Update does use it diffrently
        MailVariableDto presentationLanguageVariable = new MailVariableDto(
                "Presentation Language",
                "[[${DataFormatter.formatConstantName(presentation.language)}]]",
                "English",
                "Presentation"
        );

        MailVariableDto presentationUrlVariable = new MailVariableDto(
                "Presentation URL",
                "[[${presentationUrl}]]",
                "https://thesis-management.com/presentations/123",
                "Application"
        );

        return List.of(presentationCreatorFirstNameVariable, presentationCreatorLastNameVariable, presentationTypeVariable, presentationDateVariable, presentationLocationVariable, streamUrlVariable, presentationLanguageVariable, presentationUrlVariable);
    }

    private List<MailVariableDto> getAssessmentVariables() {
        MailVariableDto assessmentCreatorFirstNameVariable = new MailVariableDto(
                "Assessment Creator First Name",
                "[[${assessment.createdBy.firstName}]]",
                "Max",
                "Assessment"
        );

        MailVariableDto assessmentCreatorLastNameVariable = new MailVariableDto(
                "Assessment Creator Last Name",
                "[[${assessment.createdBy.lastName}]]",
                "Mustermann",
                "Assessment"
        );

        MailVariableDto assesmentSummaryVariable = new MailVariableDto(
                "Assessment Summary",
                "[[${assessment.summary}]]",
                "The thesis demonstrates a solid understanding of deep learning techniques and their application to natural language processing tasks. The implementation is well-structured and the results are promising.",
                "Assessment"
        );

        MailVariableDto assessmentGradeSuggestion = new MailVariableDto(
                "Assessment Grade Suggestion",
                "[[${assessment.gradeSuggestion}]]",
                "1.3",
                "Assessment"
        );

        MailVariableDto assessmentPositivesVariable = new MailVariableDto(
                "Assessment Positives",
                "[[${assessment.positives}]]",
                "The thesis provides a comprehensive evaluation of different neural network architectures and training strategies, demonstrating the student's ability to conduct independent research and critically analyze results.",
                "Assessment"
        );

        MailVariableDto assessmentNegativesVariable = new MailVariableDto(
                "Assessment Negatives",
                "[[${assessment.negatives}]]",
                "The thesis could benefit from a more detailed discussion of the limitations of the study and potential avenues for future research.",
                "Assessment"
        );

        return List.of(assessmentCreatorFirstNameVariable, assessmentCreatorLastNameVariable, assesmentSummaryVariable, assessmentGradeSuggestion, assessmentPositivesVariable, assessmentNegativesVariable);
    }

    private List<MailVariableDto> getThesisCommentVariables() {
        MailVariableDto commentCreatorFirstNameVariable = new MailVariableDto(
                "Comment Creator First Name",
                "[[${comment.createdBy.firstName}]]",
                "Max",
                "Thesis Comment"
        );

        MailVariableDto commentCreatorLastNameVariable = new MailVariableDto(
                "Comment Creator Last Name",
                "[[${comment.createdBy.lastName}]]",
                "Mustermann",
                "Thesis Comment"
        );

        MailVariableDto commentMessageVariable = new MailVariableDto(
                "Comment Message",
                "[[${comment.message}]]",
                "I have some concerns about the methodology used in the thesis.",
                "Thesis Comment"
        );

        return List.of(commentCreatorFirstNameVariable, commentCreatorLastNameVariable, commentMessageVariable);
    }

    private List<MailVariableDto> getApplicationReminderVariables() {
        MailVariableDto unreviewedApplicationsVariable = new MailVariableDto(
                "Unreviewed Applications",
                "[[${unreviewedApplications}]]",
                "- Max Mustermann: Deep Learning for Natural Language Processing\n- Maria Musterfrau: Computer Vision for Autonomous Vehicles",
                "Application Reminder"
        );

        MailVariableDto reviewApplicationsUrlVariable = new MailVariableDto(
                "Review Applications URL",
                "[[${${reviewApplicationsLink}}]]",
                "https://thesis-management.com/research-groups/123/applications",
                "Application Reminder"
        );

        return List.of(unreviewedApplicationsVariable, reviewApplicationsUrlVariable);
    }
}
