package de.tum.cit.aet.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.constants.ThesisPresentationState;
import de.tum.cit.aet.thesis.constants.ThesisPresentationType;
import de.tum.cit.aet.thesis.constants.ThesisPresentationVisibility;
import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.entity.ThesisPresentation;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.repository.ThesisPresentationRepository;
import de.tum.cit.aet.thesis.repository.ThesisRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Testcontainers
class PublishedPresentationControllerTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private ThesisRepository thesisRepository;

	@Autowired
	private ThesisPresentationRepository thesisPresentationRepository;

	@Autowired
	private UserRepository userRepository;

	private UUID createPublicScheduledPresentation() throws Exception {
		UUID thesisId = createTestThesis("Presentation Thesis");
		Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
		User creator = userRepository.findAll().getFirst();

		ThesisPresentation presentation = new ThesisPresentation();
		presentation.setThesis(thesis);
		presentation.setState(ThesisPresentationState.SCHEDULED);
		presentation.setType(ThesisPresentationType.FINAL);
		presentation.setVisibility(ThesisPresentationVisibility.PUBLIC);
		presentation.setLocation("Room 101");
		presentation.setLanguage("ENGLISH");
		presentation.setScheduledAt(Instant.now().plus(7, ChronoUnit.DAYS));
		presentation.setCreatedAt(Instant.now());
		presentation.setCreatedBy(creator);

		presentation = thesisPresentationRepository.save(presentation);
		return presentation.getId();
	}

	@Nested
	class GetPublishedPresentations {
		@Test
		void getPresentations_EmptyList() throws Exception {
			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/published-presentations")
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("totalElements").asInt()).isZero();
		}

		@Test
		void getPresentations_WithPublicPresentation() throws Exception {
			createPublicScheduledPresentation();

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/published-presentations")
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(1)));
		}

		@Test
		void getPresentations_IncludeDrafts() throws Exception {
			UUID thesisId = createTestThesis("Draft Thesis");
			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			User creator = userRepository.findAll().getFirst();

			ThesisPresentation draft = new ThesisPresentation();
			draft.setThesis(thesis);
			draft.setState(ThesisPresentationState.DRAFTED);
			draft.setType(ThesisPresentationType.FINAL);
			draft.setVisibility(ThesisPresentationVisibility.PUBLIC);
			draft.setLocation("Room 202");
			draft.setLanguage("ENGLISH");
			draft.setScheduledAt(Instant.now().plus(14, ChronoUnit.DAYS));
			draft.setCreatedAt(Instant.now());
			draft.setCreatedBy(creator);
			thesisPresentationRepository.save(draft);

			String responseNoDrafts = mockMvc.perform(MockMvcRequestBuilders.get("/v2/published-presentations")
							.header("Authorization", createRandomAdminAuthentication())
							.param("includeDrafts", "false"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode noDraftsJson = objectMapper.readTree(responseNoDrafts);
			assertThat(noDraftsJson.get("totalElements").asInt()).isZero();

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/published-presentations")
							.header("Authorization", createRandomAdminAuthentication())
							.param("includeDrafts", "true"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(1)));
		}
	}

	@Nested
	class GetSinglePresentation {
		@Test
		void getPresentation_Success() throws Exception {
			UUID presentationId = createPublicScheduledPresentation();

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/published-presentations/{id}", presentationId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.presentationId").value(presentationId.toString()));
		}

		@Test
		void getPresentation_NotFound() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/published-presentations/{id}", UUID.randomUUID())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNotFound());
		}

		@Test
		void getPresentation_PrivatePresentation_ReturnsForbidden() throws Exception {
			UUID thesisId = createTestThesis("Private Presentation Thesis");
			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			User creator = userRepository.findAll().getFirst();

			ThesisPresentation privatePresentation = new ThesisPresentation();
			privatePresentation.setThesis(thesis);
			privatePresentation.setState(ThesisPresentationState.SCHEDULED);
			privatePresentation.setType(ThesisPresentationType.FINAL);
			privatePresentation.setVisibility(ThesisPresentationVisibility.PRIVATE);
			privatePresentation.setLocation("Room 303");
			privatePresentation.setLanguage("ENGLISH");
			privatePresentation.setScheduledAt(Instant.now().plus(7, ChronoUnit.DAYS));
			privatePresentation.setCreatedAt(Instant.now());
			privatePresentation.setCreatedBy(creator);
			privatePresentation = thesisPresentationRepository.save(privatePresentation);

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/published-presentations/{id}", privatePresentation.getId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isForbidden());
		}
	}
}
