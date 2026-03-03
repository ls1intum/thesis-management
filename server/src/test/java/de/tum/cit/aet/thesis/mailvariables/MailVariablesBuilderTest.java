package de.tum.cit.aet.thesis.mailvariables;

import static org.assertj.core.api.Assertions.assertThat;

import de.tum.cit.aet.thesis.dto.MailVariableDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

class MailVariablesBuilderTest {

	private final MailVariablesBuilder builder = new MailVariablesBuilder();

	@Nested
	class ApplicationAccepted {
		@ParameterizedTest
		@ValueSource(strings = {"APPLICATION_ACCEPTED", "APPLICATION_ACCEPTED_NO_SUPERVISOR"})
		void returnsApplicationAndThesisAndUserVariables(String templateCase) {
			List<MailVariableDto> variables = builder.getMailVariables(templateCase);

			assertThat(variables).isNotEmpty();
			assertThat(variables).anyMatch(v -> v.group().equals("Application"));
			assertThat(variables).anyMatch(v -> v.group().equals("Thesis"));
			assertThat(variables).anyMatch(v -> v.group().equals("User"));
			assertThat(variables).anyMatch(v -> v.group().equals("General"));
		}
	}

	@Nested
	class ApplicationRejected {
		@ParameterizedTest
		@ValueSource(strings = {
				"APPLICATION_REJECTED_TOPIC_REQUIREMENTS",
				"APPLICATION_REJECTED_TOPIC_OUTDATED",
				"APPLICATION_REJECTED_TITLE_NOT_INTERESTING",
				"APPLICATION_REJECTED",
				"APPLICATION_REJECTED_TOPIC_FILLED",
				"APPLICATION_REJECTED_STUDENT_REQUIREMENTS",
				"APPLICATION_CREATED_STUDENT",
				"APPLICATION_CREATED_CHAIR"
		})
		void returnsApplicationVariables(String templateCase) {
			List<MailVariableDto> variables = builder.getMailVariables(templateCase);

			assertThat(variables).isNotEmpty();
			assertThat(variables).anyMatch(v -> v.group().equals("Application"));
			assertThat(variables).anyMatch(v -> v.group().equals("General"));
		}
	}

	@Nested
	class ThesisCreatedAndClosed {
		@ParameterizedTest
		@ValueSource(strings = {"THESIS_CREATED", "THESIS_CLOSED"})
		void returnsThesisAndUserVariables(String templateCase) {
			List<MailVariableDto> variables = builder.getMailVariables(templateCase);

			assertThat(variables).isNotEmpty();
			assertThat(variables).anyMatch(v -> v.group().equals("Thesis"));
			assertThat(variables).anyMatch(v -> v.group().equals("User"));
		}

		@Test
		void thesisCreated_HasCreatingUserVariables() {
			List<MailVariableDto> variables = builder.getMailVariables("THESIS_CREATED");

			assertThat(variables).anyMatch(v -> v.templateVariable().contains("creatingUser"));
		}

		@Test
		void thesisClosed_HasDeletingUserVariables() {
			List<MailVariableDto> variables = builder.getMailVariables("THESIS_CLOSED");

			assertThat(variables).anyMatch(v -> v.templateVariable().contains("deletingUser"));
		}
	}

	@Nested
	class ThesisProposals {
		@ParameterizedTest
		@ValueSource(strings = {"THESIS_PROPOSAL_UPLOADED", "THESIS_PROPOSAL_REJECTED", "THESIS_PROPOSAL_ACCEPTED"})
		void returnsProposalAndThesisAndUserVariables(String templateCase) {
			List<MailVariableDto> variables = builder.getMailVariables(templateCase);

			assertThat(variables).isNotEmpty();
			assertThat(variables).anyMatch(v -> v.group().equals("Proposal"));
			assertThat(variables).anyMatch(v -> v.group().equals("Thesis"));
		}

		@Test
		void proposalRejected_HasRequestedChangesVariable() {
			List<MailVariableDto> variables = builder.getMailVariables("THESIS_PROPOSAL_REJECTED");

			assertThat(variables).anyMatch(v -> v.templateVariable().contains("requestedChanges"));
			assertThat(variables).anyMatch(v -> v.templateVariable().contains("reviewingUser"));
		}
	}

	@Nested
	class ThesisPresentation {
		@ParameterizedTest
		@ValueSource(strings = {
				"THESIS_PRESENTATION_UPDATED",
				"THESIS_PRESENTATION_SCHEDULED",
				"THESIS_PRESENTATION_INVITATION_CANCELLED",
				"THESIS_PRESENTATION_INVITATION_UPDATED",
				"THESIS_PRESENTATION_INVITATION"
		})
		void returnsPresentationAndThesisVariables(String templateCase) {
			List<MailVariableDto> variables = builder.getMailVariables(templateCase);

			assertThat(variables).isNotEmpty();
			assertThat(variables).anyMatch(v -> v.group().equals("Presentation"));
			assertThat(variables).anyMatch(v -> v.group().equals("Thesis"));
		}

		@Test
		void presentationDeleted_HasDeletingUserVariables() {
			List<MailVariableDto> variables = builder.getMailVariables("THESIS_PRESENTATION_DELETED");

			assertThat(variables).anyMatch(v -> v.group().equals("Presentation"));
			assertThat(variables).anyMatch(v -> v.group().equals("Thesis"));
			assertThat(variables).anyMatch(v -> v.templateVariable().contains("deletingUser"));
		}
	}

	@Nested
	class ThesisFinalSubmission {
		@Test
		void returnsThesisVariables() {
			List<MailVariableDto> variables = builder.getMailVariables("THESIS_FINAL_SUBMISSION");

			assertThat(variables).isNotEmpty();
			assertThat(variables).anyMatch(v -> v.group().equals("Thesis"));
		}
	}

	@Nested
	class ThesisFinalGrade {
		@Test
		void returnsThesisAndGradeVariables() {
			List<MailVariableDto> variables = builder.getMailVariables("THESIS_FINAL_GRADE");

			assertThat(variables).isNotEmpty();
			assertThat(variables).anyMatch(v -> v.group().equals("Thesis"));
			assertThat(variables).anyMatch(v -> v.templateVariable().contains("finalGrade"));
		}
	}

	@Nested
	class ThesisAssessment {
		@Test
		void returnsAssessmentAndThesisVariables() {
			List<MailVariableDto> variables = builder.getMailVariables("THESIS_ASSESSMENT_ADDED");

			assertThat(variables).isNotEmpty();
			assertThat(variables).anyMatch(v -> v.group().equals("Assessment"));
			assertThat(variables).anyMatch(v -> v.group().equals("Thesis"));
		}
	}

	@Nested
	class ThesisComment {
		@Test
		void returnsCommentAndThesisVariables() {
			List<MailVariableDto> variables = builder.getMailVariables("THESIS_COMMENT_POSTED");

			assertThat(variables).isNotEmpty();
			assertThat(variables).anyMatch(v -> v.group().equals("Thesis Comment"));
			assertThat(variables).anyMatch(v -> v.group().equals("Thesis"));
		}
	}

	@Nested
	class ApplicationReminder {
		@Test
		void returnsReminderVariables() {
			List<MailVariableDto> variables = builder.getMailVariables("APPLICATION_REMINDER");

			assertThat(variables).isNotEmpty();
			assertThat(variables).anyMatch(v -> v.group().equals("Application Reminder"));
		}
	}

	@Nested
	class AutomaticRejectReminder {
		@Test
		void returnsAutomaticRejectReminderVariables() {
			List<MailVariableDto> variables = builder.getMailVariables("APPLICATION_AUTOMATIC_REJECT_REMINDER");

			assertThat(variables).isNotEmpty();
			assertThat(variables).anyMatch(v -> v.group().equals("Application Reminder"));
			assertThat(variables).anyMatch(v -> v.templateVariable().contains("applications"));
			assertThat(variables).anyMatch(v -> v.templateVariable().contains("clientHost"));
		}
	}

	@Nested
	class InterviewCases {
		@ParameterizedTest
		@ValueSource(strings = {"INTERVIEW_INVITATION", "INTERVIEW_INVITATION_REMINDER"})
		void returnsApplicationAndInterviewVariables(String templateCase) {
			List<MailVariableDto> variables = builder.getMailVariables(templateCase);

			assertThat(variables).isNotEmpty();
			assertThat(variables).anyMatch(v -> v.group().equals("Application"));
			assertThat(variables).anyMatch(v -> v.group().equals("Interview"));
		}

		@ParameterizedTest
		@ValueSource(strings = {"INTERVIEW_SLOT_BOOKED_CONFORMATION", "INTERVIEW_SLOT_BOOKED_CANCELLATION"})
		void returnsApplicationAndInterviewAndSlotVariables(String templateCase) {
			List<MailVariableDto> variables = builder.getMailVariables(templateCase);

			assertThat(variables).isNotEmpty();
			assertThat(variables).anyMatch(v -> v.group().equals("Application"));
			assertThat(variables).anyMatch(v -> v.group().equals("Interview"));
			assertThat(variables).anyMatch(v -> v.group().equals("Interview Slot"));
		}
	}

	@Nested
	class DefaultCase {
		@Test
		void unknownTemplateCase_ReturnsOnlyGeneralVariables() {
			List<MailVariableDto> variables = builder.getMailVariables("UNKNOWN_TEMPLATE_CASE");

			assertThat(variables).isNotEmpty();
			assertThat(variables).allMatch(v -> v.group().equals("General"));
		}
	}

	@Nested
	class GeneralVariables {
		@Test
		void allCases_IncludeRecipientVariables() {
			List<MailVariableDto> variables = builder.getMailVariables("APPLICATION_ACCEPTED");

			assertThat(variables).anyMatch(v -> v.templateVariable().contains("recipient"));
		}

		@Test
		void allCases_IncludeNotificationUrlFooter() {
			List<MailVariableDto> variables = builder.getMailVariables("APPLICATION_ACCEPTED");

			assertThat(variables).anyMatch(v -> v.label().equals("Notification URL Footer"));
		}
	}
}
