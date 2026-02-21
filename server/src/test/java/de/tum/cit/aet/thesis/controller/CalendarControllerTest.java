package de.tum.cit.aet.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.controller.payload.BookInterviewSlotPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateInterviewProcessPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateInterviewSlotsPayload;
import de.tum.cit.aet.thesis.controller.payload.InviteIntervieweesPayload;
import de.tum.cit.aet.thesis.controller.payload.ReplaceTopicPayload;
import de.tum.cit.aet.thesis.dto.InterviewSlotDto;
import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Testcontainers
class CalendarControllerTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private ResearchGroupRepository researchGroupRepository;

	@Nested
	class PresentationCalendar {
		@Test
		void getCalendar_Success() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Calendar Group", head.universityId());
			ResearchGroup group = researchGroupRepository.findById(groupId).orElseThrow();

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/calendar/presentations/{abbreviation}", group.getAbbreviation())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andExpect(content().contentType("text/calendar"))
					.andReturn().getResponse().getContentAsString();

			assertThat(response).contains("VCALENDAR");
		}

		@Test
		void getCalendar_UnknownAbbreviation_Returns404() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/calendar/presentations/{abbreviation}", "nonexistent-abbr")
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNotFound());
		}
	}

	@Nested
	class InterviewCalendar {
		@Test
		void getInterviewCalendar_Success() throws Exception {
			TestUser user = createRandomTestUser(List.of("student"));

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/calendar/interviews/user/{userId}", user.userId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andExpect(content().contentType("text/calendar"))
					.andReturn().getResponse().getContentAsString();

			assertThat(response).contains("VCALENDAR");
		}

		@Test
		void getInterviewCalendar_UnknownUser_ReturnsNotFound() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/calendar/interviews/user/{userId}", UUID.randomUUID())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNotFound());
		}

		@Test
		void getInterviewCalendar_WithBookedSlots_ContainsEvents() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");
			createTestEmailTemplate("INTERVIEW_INVITATION");
			createTestEmailTemplate("INTERVIEW_INVITATION_REMINDER");
			createTestEmailTemplate("INTERVIEW_SLOT_BOOKED_CONFORMATION");
			createTestEmailTemplate("INTERVIEW_SLOT_BOOKED_CANCELLATION");

			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Calendar Interview RG", advisor.universityId());

			// Create topic
			ReplaceTopicPayload topicPayload = new ReplaceTopicPayload(
					"Calendar Interview Topic", Set.of("MASTER"),
					"PS", "Req", "Goals", "Refs",
					List.of(advisor.userId()), List.of(advisor.userId()),
					researchGroupId, null, null, false
			);
			String topicResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/topics")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(topicPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID topicId = UUID.fromString(objectMapper.readTree(topicResponse).get("topicId").asString());

			// Create student application
			TestUser student = createRandomTestUser(List.of("student"));
			String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));
			CreateApplicationPayload appPayload = new CreateApplicationPayload(
					topicId, null, "MASTER", Instant.now(), "Calendar test", researchGroupId
			);
			String appResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(appPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID applicationId = UUID.fromString(objectMapper.readTree(appResponse).get("applicationId").asString());

			// Create interview process
			String advisorAuth = generateTestAuthenticationHeader(advisor.universityId(), List.of("supervisor", "advisor"));
			CreateInterviewProcessPayload processPayload = new CreateInterviewProcessPayload(topicId, List.of(applicationId));
			String processResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/interview-process")
							.header("Authorization", advisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(processPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID processId = UUID.fromString(objectMapper.readTree(processResponse).get("interviewProcessId").asString());

			// Get interviewee ID
			String intervieweesResponse = mockMvc.perform(MockMvcRequestBuilders.get("/v2/interview-process/{id}/interviewees", processId)
							.header("Authorization", advisorAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID intervieweeId = UUID.fromString(objectMapper.readTree(intervieweesResponse)
					.get("content").get(0).get("intervieweeId").asString());

			// Add slots (2 non-overlapping to avoid overlap bug)
			Instant now = Instant.now();
			InterviewSlotDto slot1 = new InterviewSlotDto(
					null, now.plus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
					null, "Room 101", "https://meet.example.com"
			);
			InterviewSlotDto slot2 = new InterviewSlotDto(
					null, now.plus(2, ChronoUnit.DAYS), now.plus(2, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
					null, "Room 102", null
			);
			CreateInterviewSlotsPayload slotsPayload = new CreateInterviewSlotsPayload(processId, List.of(slot1, slot2));
			String slotsResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/interview-process/interview-slots")
							.header("Authorization", advisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(slotsPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID slotId = UUID.fromString(objectMapper.readTree(slotsResponse).get(0).get("slotId").asString());

			// Invite interviewee
			InviteIntervieweesPayload invitePayload = new InviteIntervieweesPayload(List.of(intervieweeId));
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/interview-process/{id}/invite", processId)
							.header("Authorization", advisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(invitePayload)))
					.andExpect(status().isOk());

			// Book slot
			BookInterviewSlotPayload bookPayload = new BookInterviewSlotPayload(student.userId());
			mockMvc.perform(MockMvcRequestBuilders.put("/v2/interview-process/{id}/slot/{slotId}/book", processId, slotId)
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(bookPayload)))
					.andExpect(status().isOk());

			// Get the calendar for the advisor (who has interview slots)
			String calendarResponse = mockMvc.perform(MockMvcRequestBuilders.get("/v2/calendar/interviews/user/{userId}", advisor.userId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andExpect(content().contentType("text/calendar"))
					.andReturn().getResponse().getContentAsString();

			assertThat(calendarResponse).contains("VCALENDAR");
			assertThat(calendarResponse).contains("VEVENT");
			assertThat(calendarResponse).contains("Interview Slot");
		}
	}
}
