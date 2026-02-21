package de.tum.cit.aet.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.constants.ThesisVisibility;
import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.entity.ThesisStateChange;
import de.tum.cit.aet.thesis.entity.key.ThesisStateChangeId;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.repository.ThesisRepository;
import de.tum.cit.aet.thesis.repository.ThesisStateChangeRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
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
}
