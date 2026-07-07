package de.tum.cit.aet.thesis.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.core.user.repository.UserRepository;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.thesis.constants.ThesisVisibility;
import de.tum.cit.aet.thesis.thesis.controller.payload.CreateThesisPayload;
import de.tum.cit.aet.thesis.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.thesis.entity.ThesisStateChange;
import de.tum.cit.aet.thesis.thesis.entity.key.ThesisStateChangeId;
import de.tum.cit.aet.thesis.thesis.repository.ThesisRepository;
import de.tum.cit.aet.thesis.thesis.repository.ThesisStateChangeRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Testcontainers
class PublishedThesisControllerTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private ThesisRepository thesisRepository;

	@Autowired
	private ThesisStateChangeRepository thesisStateChangeRepository;

	@Autowired
	private UserRepository userRepository;

	private UUID createFinishedThesis(String title) throws Exception {
		UUID thesisId = createTestThesis(title);

		Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();

		ThesisStateChangeId stateChangeId = new ThesisStateChangeId();
		stateChangeId.setThesisId(thesis.getId());
		stateChangeId.setState(ThesisState.FINISHED);

		ThesisStateChange stateChange = new ThesisStateChange();
		stateChange.setId(stateChangeId);
		stateChange.setThesis(thesis);
		stateChange.setChangedAt(Instant.now());
		thesisStateChangeRepository.save(stateChange);

		thesis.setState(ThesisState.FINISHED);
		thesis.setVisibility(ThesisVisibility.PUBLIC);
		thesis.getStates().add(stateChange);
		thesisRepository.save(thesis);

		return thesisId;
	}

	@Nested
	class GetPublishedTheses {
		@Test
		void getPublishedTheses_EmptyList() throws Exception {
			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/published-theses")
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("totalElements").asInt()).isZero();
		}

		@Test
		void getPublishedTheses_WithFinishedThesis() throws Exception {
			createFinishedThesis("Finished Thesis");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/published-theses")
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(1)))
					.andExpect(jsonPath("$.content[0].title").value("Finished Thesis"));
		}

		@Test
		void getPublishedTheses_DoesNotIncludeNonFinished() throws Exception {
			createTestThesis("Active Thesis");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/published-theses")
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("totalElements").asInt()).isZero();
		}

		@Test
		void getPublishedTheses_WithSearch() throws Exception {
			createFinishedThesis("Unique Title XYZ");
			createFinishedThesis("Another Thesis");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/published-theses")
							.header("Authorization", createRandomAdminAuthentication())
							.param("search", "Unique Title XYZ"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isEqualTo(1);
		}

		@Test
		void getPublishedTheses_WithTypeFilter() throws Exception {
			createFinishedThesis("Master Thesis");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/published-theses")
							.header("Authorization", createRandomAdminAuthentication())
							.param("types", "MASTER"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(1)));

			String emptyResponse = mockMvc.perform(MockMvcRequestBuilders.get("/v2/published-theses")
							.header("Authorization", createRandomAdminAuthentication())
							.param("types", "BACHELOR"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			assertThat(objectMapper.readTree(emptyResponse).get("totalElements").asInt()).isZero();
		}

		@Test
		void getPublishedTheses_WithPagination() throws Exception {
			createFinishedThesis("Thesis A");
			createFinishedThesis("Thesis B");
			createFinishedThesis("Thesis C");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/published-theses")
							.header("Authorization", createRandomAdminAuthentication())
							.param("page", "0")
							.param("limit", "2"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(2)))
					.andExpect(jsonPath("$.totalElements").value(3));
		}
	}

	@Nested
	class GetThesisFile {
		@Test
		void getThesisFile_NotFound() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/published-theses/{id}/thesis", UUID.randomUUID())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNotFound());
		}

		@Test
		void getThesisFile_NonFinishedThesis_AccessDenied() throws Exception {
			UUID thesisId = createTestThesis("Private Thesis");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/published-theses/{id}/thesis", thesisId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class GetPublishedThesesSorting {
		@Test
		void getPublishedTheses_SortByAsc() throws Exception {
			createFinishedThesis("Sort Thesis A");
			createFinishedThesis("Sort Thesis B");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/published-theses")
							.header("Authorization", createRandomAdminAuthentication())
							.param("sortOrder", "asc"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(2)))
					.andExpect(jsonPath("$.content[0].thesisId").exists());
		}
	}

	@Nested
	class GetPublishedThesesSupervisorFilter {
		// Test users are created with given_name = family_name = universityId,
		// so a supervisor's full name reads as "<universityId> <universityId>".
		private String fullName(TestUser user) {
			return user.universityId() + " " + user.universityId();
		}

		private UUID createFinishedThesisWithRoles(
				String title,
				TestUser supervisor,
				TestUser examiner,
				TestUser student
		) throws Exception {
			UUID researchGroupId = createTestResearchGroup(
					"Published Filter Group " + UUID.randomUUID(),
					supervisor.universityId()
			);
			createTestEmailTemplate("THESIS_CREATED");

			CreateThesisPayload payload = new CreateThesisPayload(
					title,
					"MASTER",
					"ENGLISH",
					List.of(student.userId()),
					List.of(supervisor.userId()),
					List.of(examiner.userId()),
					researchGroupId
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			UUID thesisId = UUID.fromString(JsonPath.parse(response).read("$.thesisId", String.class));

			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			ThesisStateChangeId stateChangeId = new ThesisStateChangeId();
			stateChangeId.setThesisId(thesis.getId());
			stateChangeId.setState(ThesisState.FINISHED);
			ThesisStateChange stateChange = new ThesisStateChange();
			stateChange.setId(stateChangeId);
			stateChange.setThesis(thesis);
			stateChange.setChangedAt(Instant.now());
			thesisStateChangeRepository.save(stateChange);
			thesis.setState(ThesisState.FINISHED);
			thesis.setVisibility(ThesisVisibility.PUBLIC);
			thesis.getStates().add(stateChange);
			thesisRepository.save(thesis);

			return thesisId;
		}

		@Test
		void getPublishedTheses_FilterBySupervisorName_ReturnsOnlyMatchingTheses() throws Exception {
			TestUser supervisorA = createRandomTestUser(List.of("supervisor", "advisor"));
			TestUser supervisorB = createRandomTestUser(List.of("supervisor", "advisor"));
			TestUser student = createRandomTestUser(List.of("student"));

			UUID thesisA = createFinishedThesisWithRoles("Supervisor A Thesis", supervisorA, supervisorA, student);
			createFinishedThesisWithRoles("Supervisor B Thesis", supervisorB, supervisorB, student);

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/published-theses")
							.header("Authorization", createRandomAdminAuthentication())
							.param("supervisorName", fullName(supervisorA)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("totalElements").asInt()).isEqualTo(1);
			assertThat(json.get("content").get(0).get("thesisId").asString())
					.isEqualTo(thesisA.toString());
		}

		@Test
		void getPublishedTheses_FilterBySupervisorName_IgnoresExaminerOnlyMatches() throws Exception {
			TestUser supervisor = createRandomTestUser(List.of("supervisor", "advisor"));
			TestUser examinerOnly = createRandomTestUser(List.of("supervisor"));
			TestUser student = createRandomTestUser(List.of("student"));

			createFinishedThesisWithRoles("Examiner Only Thesis", supervisor, examinerOnly, student);

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/published-theses")
							.header("Authorization", createRandomAdminAuthentication())
							.param("supervisorName", fullName(examinerOnly)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.totalElements").value(0));
		}

		@Test
		void getPublishedTheses_FilterBySupervisorName_MatchesSupervisorWithMissingLastName() throws Exception {
			TestUser supervisor = createRandomTestUser(List.of("supervisor", "advisor"));
			TestUser student = createRandomTestUser(List.of("student"));

			User supervisorUser = userRepository.findById(supervisor.userId()).orElseThrow();
			supervisorUser.setLastName(null);
			userRepository.save(supervisorUser);

			UUID thesisId = createFinishedThesisWithRoles(
					"Single Name Supervisor Thesis", supervisor, supervisor, student);

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/published-theses")
							.header("Authorization", createRandomAdminAuthentication())
							.param("supervisorName", supervisor.universityId()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("totalElements").asInt()).isEqualTo(1);
			assertThat(json.get("content").get(0).get("thesisId").asString())
					.isEqualTo(thesisId.toString());
		}
	}
}
