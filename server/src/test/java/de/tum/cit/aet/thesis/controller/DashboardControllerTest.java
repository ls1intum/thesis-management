package de.tum.cit.aet.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.constants.ThesisPresentationType;
import de.tum.cit.aet.thesis.constants.ThesisPresentationVisibility;
import de.tum.cit.aet.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.controller.payload.CreateApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateThesisPayload;
import de.tum.cit.aet.thesis.controller.payload.ReplacePresentationPayload;
import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;
import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.entity.ThesisStateChange;
import de.tum.cit.aet.thesis.entity.key.ThesisStateChangeId;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.repository.ResearchGroupSettingsRepository;
import de.tum.cit.aet.thesis.repository.ThesisRepository;
import de.tum.cit.aet.thesis.repository.ThesisStateChangeRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Testcontainers
class DashboardControllerTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private ThesisRepository thesisRepository;

	@Autowired
	private ThesisStateChangeRepository thesisStateChangeRepository;

	@Autowired
	private ResearchGroupSettingsRepository researchGroupSettingsRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ResearchGroupRepository researchGroupRepository;

	private UUID createThesisWithState(String title, ThesisState targetState,
			List<UUID> students, List<UUID> supervisors, List<UUID> examiners, UUID researchGroupId) throws Exception {
		CreateThesisPayload payload = new CreateThesisPayload(
				title, "MASTER", "ENGLISH", students, supervisors, examiners, researchGroupId
		);

		String thesisResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		UUID thesisId = UUID.fromString(objectMapper.readTree(thesisResponse).get("thesisId").asString());

		if (targetState != null && targetState != ThesisState.PROPOSAL) {
			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			ThesisStateChangeId stateChangeId = new ThesisStateChangeId();
			stateChangeId.setThesisId(thesis.getId());
			stateChangeId.setState(targetState);
			ThesisStateChange stateChange = new ThesisStateChange();
			stateChange.setId(stateChangeId);
			stateChange.setThesis(thesis);
			stateChange.setChangedAt(Instant.now());
			thesisStateChangeRepository.save(stateChange);
			thesis.setState(targetState);
			thesis.getStates().add(stateChange);
			thesisRepository.save(thesis);
		}

		return thesisId;
	}

	private boolean hasTaskContaining(JsonNode tasks, String text) {
		for (JsonNode task : tasks) {
			if (task.get("message").asString().contains(text)) {
				return true;
			}
		}
		return false;
	}

	@Nested
	class StudentTasks {
		@Test
		void getTasks_AsStudent_ReturnsScientificWritingGuideTask() throws Exception {
			TestUser student = createRandomTestUser(List.of("student"));
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID researchGroupId = createTestResearchGroup("Writing Guide Group", head.universityId());

			// Set research group directly on the student (the assignment endpoint would
			// change their role to advisor, which is correct but this test needs a student
			// in a research group to verify the writing guide task)
			var user = userRepository.findById(student.userId()).orElseThrow();
			user.setResearchGroup(researchGroupRepository.findById(researchGroupId).orElseThrow());
			userRepository.save(user);

			ResearchGroupSettings settings = new ResearchGroupSettings();
			settings.setResearchGroupId(researchGroupId);
			settings.setScientificWritingGuideLink("https://example.com/writing-guide");
			researchGroupSettingsRepository.save(settings);

			String auth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/dashboard/tasks")
							.header("Authorization", auth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.size()).isGreaterThanOrEqualTo(1);
			assertThat(hasTaskContaining(json, "scientific writing")).isTrue();
		}

		@Test
		void getTasks_StudentWithThesis_ReturnsAbstractTask() throws Exception {
			createTestEmailTemplate("THESIS_CREATED");
			TestUser student = createRandomTestUser(List.of("student"));
			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Dashboard Test Group", advisor.universityId());

			createThesisWithState("Dashboard Test Thesis", null,
					List.of(student.userId()), List.of(advisor.userId()), List.of(advisor.userId()), researchGroupId);

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/dashboard/tasks")
							.header("Authorization", generateTestAuthenticationHeader(student.universityId(), List.of("student"))))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(hasTaskContaining(json, "abstract")).isTrue();
			assertThat(hasTaskContaining(json, "proposal")).isTrue();
		}

		@Test
		void getTasks_StudentWithWritingThesis_ReturnsSubmissionTask() throws Exception {
			createTestEmailTemplate("THESIS_CREATED");
			TestUser student = createRandomTestUser(List.of("student"));
			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Writing Thesis Group", advisor.universityId());

			createThesisWithState("Writing Thesis", ThesisState.WRITING,
					List.of(student.userId()), List.of(advisor.userId()), List.of(advisor.userId()), researchGroupId);

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/dashboard/tasks")
							.header("Authorization", generateTestAuthenticationHeader(student.universityId(), List.of("student"))))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(hasTaskContaining(json, "Submit your final thesis")).isTrue();
		}
	}

	@Nested
	class AdvisorTasks {
		@Test
		void getTasks_AdvisorWithSubmittedThesis_ReturnsAssessmentTask() throws Exception {
			createTestEmailTemplate("THESIS_CREATED");
			TestUser student = createRandomTestUser(List.of("student"));
			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Advisor Task Group", advisor.universityId());

			createThesisWithState("Submitted Thesis", ThesisState.SUBMITTED,
					List.of(student.userId()), List.of(advisor.userId()), List.of(advisor.userId()), researchGroupId);

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/dashboard/tasks")
							.header("Authorization", generateTestAuthenticationHeader(advisor.universityId(), List.of("supervisor", "advisor"))))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(hasTaskContaining(json, "assessment")).isTrue();
		}

		@Test
		void getTasks_AdvisorWithMissingDates_ReturnsDateTask() throws Exception {
			createTestEmailTemplate("THESIS_CREATED");
			TestUser student = createRandomTestUser(List.of("student"));
			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Dates Task Group", advisor.universityId());

			createThesisWithState("No Dates Thesis", ThesisState.WRITING,
					List.of(student.userId()), List.of(advisor.userId()), List.of(advisor.userId()), researchGroupId);

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/dashboard/tasks")
							.header("Authorization", generateTestAuthenticationHeader(advisor.universityId(), List.of("supervisor", "advisor"))))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(hasTaskContaining(json, "start and end date")).isTrue();
		}
	}

	@Nested
	class SupervisorTasks {
		@Test
		void getTasks_SupervisorWithAssessedThesis_ReturnsGradeTask() throws Exception {
			createTestEmailTemplate("THESIS_CREATED");
			TestUser student = createRandomTestUser(List.of("student"));
			TestUser supervisor = createRandomTestUser(List.of("supervisor"));
			TestUser advisor = createRandomTestUser(List.of("advisor"));
			UUID researchGroupId = createTestResearchGroup("Supervisor Task Group", supervisor.universityId());

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/assign/{username}", researchGroupId, advisor.universityId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			createThesisWithState("Assessed Thesis", ThesisState.ASSESSED,
					List.of(student.userId()), List.of(advisor.userId()), List.of(supervisor.userId()), researchGroupId);

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/dashboard/tasks")
							.header("Authorization", generateTestAuthenticationHeader(supervisor.universityId(), List.of("supervisor"))))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(hasTaskContaining(json, "final grade")).isTrue();
		}
	}

	@Nested
	class SupervisorCloseTasks {
		@Test
		void getTasks_SupervisorWithGradedThesis_ReturnsCloseTask() throws Exception {
			createTestEmailTemplate("THESIS_CREATED");
			TestUser student = createRandomTestUser(List.of("student"));
			TestUser supervisor = createRandomTestUser(List.of("supervisor"));
			TestUser advisor = createRandomTestUser(List.of("advisor"));
			UUID researchGroupId = createTestResearchGroup("Close Task Group", supervisor.universityId());

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/assign/{username}", researchGroupId, advisor.universityId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			createThesisWithState("Graded Thesis", ThesisState.GRADED,
					List.of(student.userId()), List.of(advisor.userId()), List.of(supervisor.userId()), researchGroupId);

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/dashboard/tasks")
							.header("Authorization", generateTestAuthenticationHeader(supervisor.universityId(), List.of("supervisor"))))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(hasTaskContaining(json, "graded but not completed")).isTrue();
		}
	}

	@Nested
	class PresentationDraftTasks {
		@Test
		void getTasks_AdvisorWithDraftPresentation_ReturnsReviewTask() throws Exception {
			createTestEmailTemplate("THESIS_CREATED");
			TestUser student = createRandomTestUser(List.of("student"));
			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Presentation Draft Group", advisor.universityId());

			UUID thesisId = createThesisWithState("Draft Presentation Thesis", ThesisState.WRITING,
					List.of(student.userId()), List.of(advisor.userId()), List.of(advisor.userId()), researchGroupId);

			String advisorAuth = generateTestAuthenticationHeader(advisor.universityId(), List.of("supervisor", "advisor"));
			ReplacePresentationPayload presPayload = new ReplacePresentationPayload(
					ThesisPresentationType.INTERMEDIATE,
					ThesisPresentationVisibility.PUBLIC,
					"Room 101", "http://stream.url", "English",
					Instant.now().plusSeconds(86400)
			);
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/presentations", thesisId)
							.header("Authorization", advisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(presPayload)))
					.andExpect(status().isOk());

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/dashboard/tasks")
							.header("Authorization", advisorAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(hasTaskContaining(json, "presentation draft")).isTrue();
		}
	}

	@Nested
	class ProposalReviewTasks {
		@Test
		void getTasks_AdvisorWithSubmittedProposal_ReturnsReviewTask() throws Exception {
			createTestEmailTemplate("THESIS_CREATED");
			createTestEmailTemplate("THESIS_PROPOSAL_UPLOADED");
			TestUser student = createRandomTestUser(List.of("student"));
			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Proposal Review Group", advisor.universityId());

			UUID thesisId = createThesisWithState("Proposal Review Thesis", null,
					List.of(student.userId()), List.of(advisor.userId()), List.of(advisor.userId()), researchGroupId);

			String advisorAuth = generateTestAuthenticationHeader(advisor.universityId(), List.of("supervisor", "advisor"));
			MockMultipartFile proposalFile = new MockMultipartFile(
					"proposal", "proposal.pdf", "application/pdf", "proposal content".getBytes()
			);
			mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/proposal", thesisId)
							.file(proposalFile)
							.header("Authorization", advisorAuth))
					.andExpect(status().isOk());

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/dashboard/tasks")
							.header("Authorization", advisorAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(hasTaskContaining(json, "proposal was submitted")).isTrue();
		}
	}

	@Nested
	class AdminTasks {
		@Test
		void getTasks_AdminWithUnreviewedApplications_ReturnsApplicationTask() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID researchGroupId = createTestResearchGroup("Admin Task Group", head.universityId());

			String studentAuth = createRandomAuthentication("student");
			CreateApplicationPayload appPayload = new CreateApplicationPayload(
					null, "Test App Title", "BACHELOR", Instant.now(), "Motivation", researchGroupId,
			true);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(appPayload)))
					.andExpect(status().isOk());

			String headAuth = generateTestAuthenticationHeader(head.universityId(), List.of("supervisor"));
			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/dashboard/tasks")
							.header("Authorization", headAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(hasTaskContaining(json, "unreviewed applications")).isTrue();
		}

		@Test
		void getTasks_SortedByPriorityDescending() throws Exception {
			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/dashboard/tasks")
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.size()).as("Admin should have at least one dashboard task").isGreaterThan(0);
			int prevPriority = Integer.MAX_VALUE;
			for (JsonNode task : json) {
				int priority = task.get("priority").asInt();
				assertThat(priority).isLessThanOrEqualTo(prevPriority);
				prevPriority = priority;
			}
		}
	}
}
