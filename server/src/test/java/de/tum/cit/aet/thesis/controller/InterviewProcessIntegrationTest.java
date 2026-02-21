package de.tum.cit.aet.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.controller.payload.AddIntervieweesPayload;
import de.tum.cit.aet.thesis.controller.payload.BookInterviewSlotPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateInterviewProcessPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateInterviewSlotsPayload;
import de.tum.cit.aet.thesis.controller.payload.InviteIntervieweesPayload;
import de.tum.cit.aet.thesis.controller.payload.ReplaceTopicPayload;
import de.tum.cit.aet.thesis.controller.payload.UpdateIntervieweeAssessmentPayload;
import de.tum.cit.aet.thesis.dto.InterviewSlotDto;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Testcontainers
class InterviewProcessIntegrationTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	private record InterviewSetup(UUID processId, UUID topicId, UUID intervieweeId, TestUser advisor, String advisorAuth, TestUser student) {}

	private InterviewSetup createInterviewProcess() throws Exception {
		createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
		createTestEmailTemplate("APPLICATION_CREATED_STUDENT");
		createTestEmailTemplate("INTERVIEW_INVITATION");
		createTestEmailTemplate("INTERVIEW_INVITATION_REMINDER");
		createTestEmailTemplate("INTERVIEW_SLOT_BOOKED_CONFORMATION");
		createTestEmailTemplate("INTERVIEW_SLOT_BOOKED_CANCELLATION");

		TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
		UUID researchGroupId = createTestResearchGroup("Interview RG", advisor.universityId());

		UUID topicId = createTestTopicForGroup("Interview Topic", advisor, researchGroupId);

		// Create a student application for the topic
		TestUser student = createRandomTestUser(List.of("student"));
		String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));
		CreateApplicationPayload appPayload = new CreateApplicationPayload(
				topicId, null, "MASTER", Instant.now(), "Interview motivation", researchGroupId
		);
		String appResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
						.header("Authorization", studentAuth)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(appPayload)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		UUID applicationId = UUID.fromString(objectMapper.readTree(appResponse).get("applicationId").asString());

		// Create the interview process
		String advisorAuth = generateTestAuthenticationHeader(advisor.universityId(), List.of("supervisor", "advisor"));
		CreateInterviewProcessPayload processPayload = new CreateInterviewProcessPayload(
				topicId, List.of(applicationId)
		);
		String processResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/interview-process")
						.header("Authorization", advisorAuth)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(processPayload)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		JsonNode processJson = objectMapper.readTree(processResponse);
		UUID processId = UUID.fromString(processJson.get("interviewProcessId").asString());

		// Get interviewee ID from interviewees endpoint (InterviewProcessDto doesn't include interviewees list)
		String intervieweesResponse = mockMvc.perform(MockMvcRequestBuilders.get("/v2/interview-process/{id}/interviewees", processId)
						.header("Authorization", advisorAuth))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		UUID intervieweeId = UUID.fromString(objectMapper.readTree(intervieweesResponse)
				.get("content").get(0).get("intervieweeId").asString());

		return new InterviewSetup(processId, topicId, intervieweeId, advisor, advisorAuth, student);
	}

	private UUID createTestTopicForGroup(String title, TestUser advisor, UUID researchGroupId) throws Exception {
		ReplaceTopicPayload payload = new ReplaceTopicPayload(
				title,
				Set.of("MASTER", "BACHELOR"),
				"Problem Statement", "Requirements", "Goals", "References",
				List.of(advisor.userId()), List.of(advisor.userId()),
				researchGroupId, null, null, false
		);

		String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/topics")
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		return UUID.fromString(objectMapper.readTree(response).get("topicId").asString());
	}

	@Nested
	class ProcessCreation {
		@Test
		void createInterviewProcess_Success() throws Exception {
			InterviewSetup setup = createInterviewProcess();
			assertThat(setup.processId).isNotNull();
			assertThat(setup.intervieweeId).isNotNull();
		}

		@Test
		void getInterviewProcess_Success() throws Exception {
			InterviewSetup setup = createInterviewProcess();

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/interview-process/{id}", setup.processId)
							.header("Authorization", setup.advisorAuth))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.interviewProcessId").value(setup.processId.toString()));
		}

		@Test
		void getMyInterviewProcesses_Success() throws Exception {
			InterviewSetup setup = createInterviewProcess();

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/interview-process")
							.header("Authorization", setup.advisorAuth))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
		}

		@Test
		void getInterviewProcessTopic_Success() throws Exception {
			InterviewSetup setup = createInterviewProcess();

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/interview-process/{id}/topic", setup.processId)
							.header("Authorization", setup.advisorAuth))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.topicId").value(setup.topicId.toString()));
		}

		@Test
		void isInterviewProcessCompleted_ReturnsFalse() throws Exception {
			InterviewSetup setup = createInterviewProcess();

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/interview-process/{id}/completed", setup.processId)
							.header("Authorization", setup.advisorAuth))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$").value(false));
		}

		@Test
		void getInterviewApplications_Success() throws Exception {
			InterviewSetup setup = createInterviewProcess();

			// All applications moved to INTERVIEWING, so no NOT_ASSESSED applications available
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/interview-process/{id}/interview-applications", setup.processId)
							.header("Authorization", setup.advisorAuth))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.totalElements").value(0));
		}
	}

	@Nested
	class SlotManagement {
		@Test
		void addSlots_Success() throws Exception {
			InterviewSetup setup = createInterviewProcess();

			Instant now = Instant.now();
			InterviewSlotDto slot1 = new InterviewSlotDto(
					null, now.plus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
					null, "Room 101", "https://meet.example.com"
			);
			InterviewSlotDto slot2 = new InterviewSlotDto(
					null, now.plus(2, ChronoUnit.DAYS), now.plus(2, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
					null, "Room 102", null
			);

			CreateInterviewSlotsPayload slotsPayload = new CreateInterviewSlotsPayload(
					setup.processId, List.of(slot1, slot2)
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/interview-process/interview-slots")
							.header("Authorization", setup.advisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(slotsPayload)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(2)));
		}

		@Test
		void getSlots_Success() throws Exception {
			InterviewSetup setup = createInterviewProcess();
			addSlotsToProcess(setup);

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/interview-process/{id}/interview-slots", setup.processId)
							.header("Authorization", setup.advisorAuth))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
		}

		@Test
		void getSlots_ExcludeBooked() throws Exception {
			InterviewSetup setup = createInterviewProcess();
			addSlotsToProcess(setup);

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/interview-process/{id}/interview-slots", setup.processId)
							.header("Authorization", setup.advisorAuth)
							.param("excludeBooked", "true"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
		}
	}

	@Nested
	class IntervieweeManagement {
		@Test
		void getInterviewees_Success() throws Exception {
			InterviewSetup setup = createInterviewProcess();

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/interview-process/{id}/interviewees", setup.processId)
							.header("Authorization", setup.advisorAuth))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(1)));
		}

		@Test
		void getInterviewee_Success() throws Exception {
			InterviewSetup setup = createInterviewProcess();

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/interview-process/{id}/interviewee/{iId}",
							setup.processId, setup.intervieweeId)
							.header("Authorization", setup.advisorAuth))
					.andExpect(status().isOk());
		}

		@Test
		void updateIntervieweeAssessment_Success() throws Exception {
			InterviewSetup setup = createInterviewProcess();

			UpdateIntervieweeAssessmentPayload payload = new UpdateIntervieweeAssessmentPayload(
					"Good technical skills", 8
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/interview-process/{id}/interviewee/{iId}",
							setup.processId, setup.intervieweeId)
							.header("Authorization", setup.advisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());
		}

		@Test
		void updateIntervieweeAssessment_NegativeScore_ClearsScore() throws Exception {
			InterviewSetup setup = createInterviewProcess();

			UpdateIntervieweeAssessmentPayload payload = new UpdateIntervieweeAssessmentPayload(
					"Reset score", -1
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/interview-process/{id}/interviewee/{iId}",
							setup.processId, setup.intervieweeId)
							.header("Authorization", setup.advisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());
		}

		@Test
		void inviteInterviewees_Success() throws Exception {
			InterviewSetup setup = createInterviewProcess();
			addSlotsToProcess(setup);

			InviteIntervieweesPayload payload = new InviteIntervieweesPayload(
					List.of(setup.intervieweeId)
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/interview-process/{id}/invite", setup.processId)
							.header("Authorization", setup.advisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(1)));
		}

		@Test
		void addMoreInterviewees_Success() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			InterviewSetup setup = createInterviewProcess();

			// Create another student and application
			TestUser student2 = createRandomTestUser(List.of("student"));
			String student2Auth = generateTestAuthenticationHeader(student2.universityId(), List.of("student"));
			CreateApplicationPayload appPayload = new CreateApplicationPayload(
					setup.topicId, null, "BACHELOR", Instant.now(), "Another motivation", null
			);
			String appResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", student2Auth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(appPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID app2Id = UUID.fromString(objectMapper.readTree(appResponse).get("applicationId").asString());

			// Add the new interviewee to the existing process
			AddIntervieweesPayload addPayload = new AddIntervieweesPayload(List.of(app2Id));

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/interview-process/{id}/interviewees", setup.processId)
							.header("Authorization", setup.advisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(addPayload)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.totalInterviewees").value(2));
		}
	}

	@Nested
	class SlotBookingAndCancellation {
		@Test
		void bookAndCancelSlot_FullFlow() throws Exception {
			InterviewSetup setup = createInterviewProcess();
			UUID slotId = addSlotsToProcess(setup);

			// Invite the interviewee first
			InviteIntervieweesPayload invitePayload = new InviteIntervieweesPayload(
					List.of(setup.intervieweeId)
			);
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/interview-process/{id}/invite", setup.processId)
							.header("Authorization", setup.advisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(invitePayload)))
					.andExpect(status().isOk());

			// Book the slot (as the student interviewee)
			String studentAuth = generateTestAuthenticationHeader(setup.student.universityId(), List.of("student"));
			BookInterviewSlotPayload bookPayload = new BookInterviewSlotPayload(setup.student.userId());

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/interview-process/{id}/slot/{slotId}/book",
							setup.processId, slotId)
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(bookPayload)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.bookedBy").isNotEmpty());

			// Cancel the slot (as advisor)
			mockMvc.perform(MockMvcRequestBuilders.put("/v2/interview-process/{id}/slot/{slotId}/cancel",
							setup.processId, slotId)
							.header("Authorization", setup.advisorAuth))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.bookedBy").isEmpty());
		}
	}

	@Nested
	class UpcomingInterviews {
		@Test
		void getUpcomingInterviews_ReturnsEmpty() throws Exception {
			InterviewSetup setup = createInterviewProcess();

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/interview-process/upcoming-interviews")
							.header("Authorization", setup.advisorAuth))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(0)));
		}
	}

	private UUID addSlotsToProcess(InterviewSetup setup) throws Exception {
		Instant now = Instant.now();
		// Must include at least 2 non-overlapping slots (service overlap check returns true for single slot)
		InterviewSlotDto slot = new InterviewSlotDto(
				null, now.plus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
				null, "Room 101", "https://meet.example.com"
		);
		InterviewSlotDto slot2 = new InterviewSlotDto(
				null, now.plus(2, ChronoUnit.DAYS), now.plus(2, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
				null, "Room 102", null
		);

		CreateInterviewSlotsPayload slotsPayload = new CreateInterviewSlotsPayload(
				setup.processId, List.of(slot, slot2)
		);

		String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/interview-process/interview-slots")
						.header("Authorization", setup.advisorAuth)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(slotsPayload)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		JsonNode slotsJson = objectMapper.readTree(response);
		return UUID.fromString(slotsJson.get(0).get("slotId").asString());
	}
}
