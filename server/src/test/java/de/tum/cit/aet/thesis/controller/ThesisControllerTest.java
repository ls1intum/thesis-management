package de.tum.cit.aet.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.constants.ThesisCommentType;
import de.tum.cit.aet.thesis.constants.ThesisFeedbackType;
import de.tum.cit.aet.thesis.constants.ThesisPresentationType;
import de.tum.cit.aet.thesis.constants.ThesisPresentationVisibility;
import de.tum.cit.aet.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.constants.ThesisVisibility;
import de.tum.cit.aet.thesis.controller.payload.AddThesisGradePayload;
import de.tum.cit.aet.thesis.controller.payload.CreateAssessmentPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateThesisPayload;
import de.tum.cit.aet.thesis.controller.payload.PostThesisCommentPayload;
import de.tum.cit.aet.thesis.controller.payload.ReplacePresentationPayload;
import de.tum.cit.aet.thesis.controller.payload.RequestChangesPayload;
import de.tum.cit.aet.thesis.controller.payload.UpdateThesisCreditsPayload;
import de.tum.cit.aet.thesis.controller.payload.UpdateThesisInfoPayload;
import de.tum.cit.aet.thesis.controller.payload.UpdateThesisPayload;
import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.repository.ThesisAssessmentRepository;
import de.tum.cit.aet.thesis.repository.ThesisCommentRepository;
import de.tum.cit.aet.thesis.repository.ThesisFeedbackRepository;
import de.tum.cit.aet.thesis.repository.ThesisFileRepository;
import de.tum.cit.aet.thesis.repository.ThesisPresentationRepository;
import de.tum.cit.aet.thesis.repository.ThesisProposalRepository;
import de.tum.cit.aet.thesis.repository.ThesisRepository;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Testcontainers
class ThesisControllerTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private ThesisRepository thesisRepository;

	@Autowired
	private ThesisFeedbackRepository thesisFeedbackRepository;

	@Autowired
	private ThesisProposalRepository thesisProposalRepository;

	@Autowired
	private ThesisFileRepository thesisFileRepository;

	@Autowired
	private ThesisCommentRepository thesisCommentRepository;

	@Autowired
	private ThesisPresentationRepository thesisPresentationRepository;

	@Autowired
	private ThesisAssessmentRepository thesisAssessmentRepository;

	@Nested
	class ThesisBasicOperations {
		@Test
		void getTheses_Success() throws Exception {
			createTestThesis("Test Thesis");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.param("fetchAll", "true"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").isArray()).isTrue();
			assertThat(json.get("content").size()).isEqualTo(1);
			assertThat(json.get("totalElements").isNumber()).isTrue();
		}

		@Test
		void getThesis_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses/{thesisId}", thesisId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("thesisId").asString()).isEqualTo(thesisId.toString());
			assertThat(json.get("title").asString()).isEqualTo("Test Thesis");
			assertThat(json.get("type").asString()).isEqualTo("MASTER");
			assertThat(json.get("language").asString()).isEqualTo("ENGLISH");
			assertThat(json.get("state").asString()).isEqualTo("PROPOSAL");
			assertThat(json.get("visibility").asString()).isEqualTo("INTERNAL");
		}

		@Test
		void getThesis_NotFound() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses/{thesisId}", UUID.randomUUID())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNotFound());
		}

		@Test
		void getThesis_AccessDenied_AsNonMemberStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String studentAuth = createRandomAuthentication("student");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses/{thesisId}", thesisId)
							.header("Authorization", studentAuth))
					.andExpect(status().isForbidden());
		}

		@Test
		void getTheses_FilterByState() throws Exception {
			createTestThesis("Test Thesis");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.param("fetchAll", "true")
							.param("state", "PROPOSAL"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isEqualTo(1);

			String responseEmpty = mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.param("fetchAll", "true")
							.param("state", "WRITING"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode jsonEmpty = objectMapper.readTree(responseEmpty);
			assertThat(jsonEmpty.get("content").size()).isEqualTo(0);
		}

		@Test
		void createThesis_Success() throws Exception {
			TestUser advisor = createTestUser("supervisor", List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Test Research Group", advisor.universityId());
			createTestEmailTemplate("THESIS_CREATED");

			CreateThesisPayload payload = new CreateThesisPayload(
					"Test Thesis",
					"MASTER",
					"ENGLISH",
					List.of(advisor.userId()),
					List.of(advisor.userId()),
					List.of(advisor.userId()),
					researchGroupId
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("title").asString()).isEqualTo("Test Thesis");
			assertThat(json.get("type").asString()).isEqualTo("MASTER");
		}

		@Test
		void createThesis_AsStudent_Forbidden() throws Exception {
			String studentAuth = createRandomAuthentication("student");

			CreateThesisPayload payload = new CreateThesisPayload(
					"Test Thesis", "MASTER", "ENGLISH",
					List.of(UUID.randomUUID()), List.of(UUID.randomUUID()),
					List.of(UUID.randomUUID()), UUID.randomUUID()
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isForbidden());
		}

		@Test
		void updateThesis_Success() throws Exception {
			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Test Research Group", advisor.universityId());
			UUID thesisId = createTestThesis("Test Thesis");

			UpdateThesisPayload payload = new UpdateThesisPayload(
					"Updated Thesis",
					"MASTER",
					"ENGLISH",
					ThesisVisibility.PUBLIC,
					Set.of("keyword1", "keyword2"),
					Instant.now(),
					Instant.now().plusSeconds(86400),
					List.of(advisor.userId()),
					List.of(advisor.userId()),
					List.of(advisor.userId()),
					List.of(),
					researchGroupId
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}", thesisId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("title").asString()).isEqualTo("Updated Thesis");
			assertThat(json.get("visibility").asString()).isEqualTo("PUBLIC");
		}

		@Test
		void updateThesis_AccessDenied_AsStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String studentAuth = createRandomAuthentication("student");

			UpdateThesisPayload payload = new UpdateThesisPayload(
					"Updated", "MASTER", "ENGLISH", ThesisVisibility.PUBLIC,
					Set.of(), Instant.now(), Instant.now().plusSeconds(86400),
					List.of(), List.of(), List.of(), List.of(), UUID.randomUUID()
			);

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}", thesisId)
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isForbidden());

			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getTitle()).isEqualTo("Test Thesis");
		}

		@Test
		void closeThesis_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			createTestEmailTemplate("THESIS_CLOSED");

			mockMvc.perform(MockMvcRequestBuilders.delete("/v2/theses/{thesisId}", thesisId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.DROPPED_OUT);
		}

		@Test
		void closeThesis_AccessDenied_AsStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String studentAuth = createRandomAuthentication("student");

			mockMvc.perform(MockMvcRequestBuilders.delete("/v2/theses/{thesisId}", thesisId)
							.header("Authorization", studentAuth))
					.andExpect(status().isForbidden());

			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.PROPOSAL);
		}
	}

	@Nested
	class ThesisInfoAndCredits {
		@Test
		void updateThesisInfo_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");

			UpdateThesisInfoPayload payload = new UpdateThesisInfoPayload(
					"Test abstract text",
					"Test info text",
					"Updated Primary Title",
					Map.of("de", "German Title")
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}/info", thesisId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("abstractText").asString()).isEqualTo("Test abstract text");
			assertThat(json.get("infoText").asString()).isEqualTo("Test info text");
			assertThat(json.get("title").asString()).isEqualTo("Updated Primary Title");

			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getAbstractField()).isEqualTo("Test abstract text");
			assertThat(thesis.getInfo()).isEqualTo("Test info text");
			assertThat(thesis.getTitle()).isEqualTo("Updated Primary Title");
		}

		@Test
		void updateThesisInfo_AccessDenied_AsStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String studentAuth = createRandomAuthentication("student");

			UpdateThesisInfoPayload payload = new UpdateThesisInfoPayload(
					"abstract", "info", "title", Map.of()
			);

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}/info", thesisId)
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isForbidden());

			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getTitle()).isEqualTo("Test Thesis");
		}

		@Test
		void updateThesisCredits_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");

			Thesis thesisBefore = thesisRepository.findById(thesisId).orElseThrow();
			UUID studentId = thesisBefore.getRoles().getFirst().getUser().getId();

			UpdateThesisCreditsPayload payload = new UpdateThesisCreditsPayload(
					Map.of(studentId, 30)
			);

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}/credits", thesisId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getMetadata().credits()).containsEntry(studentId, 30);
		}

		@Test
		void updateThesisCredits_AccessDenied_AsStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String studentAuth = createRandomAuthentication("student");

			UpdateThesisCreditsPayload payload = new UpdateThesisCreditsPayload(Map.of());

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}/credits", thesisId)
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isForbidden());

			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getMetadata().credits()).isEmpty();
		}
	}

	@Nested
	class ThesisFeedbackOperations {
		@Test
		void requestChanges_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");

			RequestChangesPayload payload = new RequestChangesPayload(
					ThesisFeedbackType.THESIS,
					List.of(
							new RequestChangesPayload.RequestedChange("Please fix section 1", false),
							new RequestChangesPayload.RequestedChange("Update references", false)
					)
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/feedback", thesisId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("feedback").size()).isEqualTo(2);

			assertThat(thesisFeedbackRepository.count()).isEqualTo(2);
		}

		@Test
		void completeFeedback_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String adminAuth = createRandomAdminAuthentication();

			RequestChangesPayload payload = new RequestChangesPayload(
					ThesisFeedbackType.THESIS,
					List.of(new RequestChangesPayload.RequestedChange("Fix this", false))
			);

			String feedbackResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/feedback", thesisId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			UUID feedbackId = UUID.fromString(objectMapper.readTree(feedbackResponse)
					.get("feedback").get(0).get("feedbackId").asString());

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}/feedback/{feedbackId}/complete", thesisId, feedbackId)
							.header("Authorization", adminAuth))
					.andExpect(status().isOk());

			var feedback = thesisFeedbackRepository.findById(feedbackId).orElseThrow();
			assertThat(feedback.getCompletedAt()).isNotNull();
		}

		@Test
		void incompleteFeedback_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String adminAuth = createRandomAdminAuthentication();

			RequestChangesPayload payload = new RequestChangesPayload(
					ThesisFeedbackType.THESIS,
					List.of(new RequestChangesPayload.RequestedChange("Fix this", true))
			);

			String feedbackResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/feedback", thesisId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			UUID feedbackId = UUID.fromString(objectMapper.readTree(feedbackResponse)
					.get("feedback").get(0).get("feedbackId").asString());

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}/feedback/{feedbackId}/incomplete", thesisId, feedbackId)
							.header("Authorization", adminAuth))
					.andExpect(status().isOk());

			var feedback = thesisFeedbackRepository.findById(feedbackId).orElseThrow();
			assertThat(feedback.getCompletedAt()).isNull();
		}

		@Test
		void deleteFeedback_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String adminAuth = createRandomAdminAuthentication();

			RequestChangesPayload payload = new RequestChangesPayload(
					ThesisFeedbackType.THESIS,
					List.of(new RequestChangesPayload.RequestedChange("Fix this", false))
			);

			String feedbackResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/feedback", thesisId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			UUID feedbackId = UUID.fromString(objectMapper.readTree(feedbackResponse)
					.get("feedback").get(0).get("feedbackId").asString());

			mockMvc.perform(MockMvcRequestBuilders.delete("/v2/theses/{thesisId}/feedback/{feedbackId}", thesisId, feedbackId)
							.header("Authorization", adminAuth))
					.andExpect(status().isOk());

			assertThat(thesisFeedbackRepository.findById(feedbackId)).isEmpty();
		}

		@Test
		void deleteFeedback_AccessDenied_AsStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");

			RequestChangesPayload payload = new RequestChangesPayload(
					ThesisFeedbackType.THESIS,
					List.of(new RequestChangesPayload.RequestedChange("Fix this", false))
			);

			String feedbackResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/feedback", thesisId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			UUID feedbackId = UUID.fromString(objectMapper.readTree(feedbackResponse)
					.get("feedback").get(0).get("feedbackId").asString());

			String studentAuth = createRandomAuthentication("student");
			mockMvc.perform(MockMvcRequestBuilders.delete("/v2/theses/{thesisId}/feedback/{feedbackId}", thesisId, feedbackId)
							.header("Authorization", studentAuth))
					.andExpect(status().isForbidden());

			assertThat(thesisFeedbackRepository.findById(feedbackId)).isPresent();
		}
	}

	@Nested
	class ThesisProposalOperations {
		@Test
		void uploadProposal_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			MockMultipartFile proposalFile = new MockMultipartFile(
					"proposal", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "test content".getBytes()
			);
			createTestEmailTemplate("THESIS_PROPOSAL_UPLOADED");

			mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/proposal", thesisId)
							.file(proposalFile)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			assertThat(thesisProposalRepository.count()).isEqualTo(1);
		}

		@Test
		void acceptProposal_Success() throws Exception {
			String authorization = createRandomAdminAuthentication();
			UUID thesisId = createTestThesis("Test Thesis");
			createTestEmailTemplate("THESIS_PROPOSAL_UPLOADED");
			createTestEmailTemplate("THESIS_PROPOSAL_ACCEPTED");

			MockMultipartFile proposalFile = new MockMultipartFile(
					"proposal", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "test content".getBytes()
			);

			mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/proposal", thesisId)
							.file(proposalFile)
							.header("Authorization", authorization))
					.andExpect(status().isOk());

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}/proposal/accept", thesisId)
							.header("Authorization", authorization))
					.andExpect(status().isOk());

			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.WRITING);
		}

		@Test
		void getProposalFile_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			createTestEmailTemplate("THESIS_PROPOSAL_UPLOADED");

			byte[] fileContent = "proposal pdf content".getBytes();
			MockMultipartFile proposalFile = new MockMultipartFile(
					"proposal", "test.pdf", MediaType.APPLICATION_PDF_VALUE, fileContent
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/proposal", thesisId)
							.file(proposalFile)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			UUID proposalId = UUID.fromString(objectMapper.readTree(response)
					.get("proposals").get(0).get("proposalId").asString());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses/{thesisId}/proposal/{proposalId}", thesisId, proposalId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());
		}

		@Test
		void deleteProposal_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			createTestEmailTemplate("THESIS_PROPOSAL_UPLOADED");

			MockMultipartFile proposalFile = new MockMultipartFile(
					"proposal", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "content".getBytes()
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/proposal", thesisId)
							.file(proposalFile)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			UUID proposalId = UUID.fromString(objectMapper.readTree(response)
					.get("proposals").get(0).get("proposalId").asString());

			mockMvc.perform(MockMvcRequestBuilders.delete("/v2/theses/{thesisId}/proposal/{proposalId}", thesisId, proposalId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			assertThat(thesisProposalRepository.findById(proposalId)).isEmpty();
		}

		@Test
		void deleteProposal_AccessDenied_AsStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			createTestEmailTemplate("THESIS_PROPOSAL_UPLOADED");

			MockMultipartFile proposalFile = new MockMultipartFile(
					"proposal", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "content".getBytes()
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/proposal", thesisId)
							.file(proposalFile)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			UUID proposalId = UUID.fromString(objectMapper.readTree(response)
					.get("proposals").get(0).get("proposalId").asString());

			String studentAuth = createRandomAuthentication("student");
			mockMvc.perform(MockMvcRequestBuilders.delete("/v2/theses/{thesisId}/proposal/{proposalId}", thesisId, proposalId)
							.header("Authorization", studentAuth))
					.andExpect(status().isForbidden());

			assertThat(thesisProposalRepository.findById(proposalId)).isPresent();
		}
	}

	@Nested
	class ThesisFileOperations {
		@Test
		void uploadThesisFile_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");

			MockMultipartFile file = new MockMultipartFile(
					"file", "thesis.pdf", MediaType.APPLICATION_PDF_VALUE, "thesis content".getBytes()
			);
			MockMultipartFile type = new MockMultipartFile(
					"type", "", MediaType.TEXT_PLAIN_VALUE, "THESIS".getBytes()
			);

			mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/files", thesisId)
							.file(file)
							.file(type)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			assertThat(thesisFileRepository.count()).isEqualTo(1);
		}

		@Test
		void getThesisFile_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");

			MockMultipartFile file = new MockMultipartFile(
					"file", "thesis.pdf", MediaType.APPLICATION_PDF_VALUE, "thesis content".getBytes()
			);
			MockMultipartFile type = new MockMultipartFile(
					"type", "", MediaType.TEXT_PLAIN_VALUE, "THESIS".getBytes()
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/files", thesisId)
							.file(file)
							.file(type)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			UUID fileId = UUID.fromString(objectMapper.readTree(response)
					.get("files").get(0).get("fileId").asString());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses/{thesisId}/files/{fileId}", thesisId, fileId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());
		}

		@Test
		void deleteThesisFile_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");

			MockMultipartFile file = new MockMultipartFile(
					"file", "thesis.pdf", MediaType.APPLICATION_PDF_VALUE, "thesis content".getBytes()
			);
			MockMultipartFile type = new MockMultipartFile(
					"type", "", MediaType.TEXT_PLAIN_VALUE, "THESIS".getBytes()
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/files", thesisId)
							.file(file)
							.file(type)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			UUID fileId = UUID.fromString(objectMapper.readTree(response)
					.get("files").get(0).get("fileId").asString());

			mockMvc.perform(MockMvcRequestBuilders.delete("/v2/theses/{thesisId}/files/{fileId}", thesisId, fileId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			assertThat(thesisFileRepository.findById(fileId)).isEmpty();
		}

		@Test
		void submitThesis_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			createTestEmailTemplate("THESIS_FINAL_SUBMISSION");

			MockMultipartFile file = new MockMultipartFile(
					"file", "thesis.pdf", MediaType.APPLICATION_PDF_VALUE, "thesis content".getBytes()
			);
			MockMultipartFile type = new MockMultipartFile(
					"type", "", MediaType.TEXT_PLAIN_VALUE, "THESIS".getBytes()
			);

			mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/files", thesisId)
							.file(file)
							.file(type)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}/thesis/final-submission", thesisId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.SUBMITTED);
		}

		@Test
		void submitThesis_NoFile_Fails() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}/thesis/final-submission", thesisId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isBadRequest());
		}
	}

	@Nested
	class ThesisPresentationOperations {
		@Test
		void createPresentation_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");

			ReplacePresentationPayload payload = new ReplacePresentationPayload(
					ThesisPresentationType.INTERMEDIATE,
					ThesisPresentationVisibility.PUBLIC,
					"Room 101",
					"http://stream.url",
					"English",
					Instant.now().plusSeconds(86400)
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/presentations", thesisId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			assertThat(thesisPresentationRepository.count()).isEqualTo(1);
		}

		@Test
		void updatePresentation_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String adminAuth = createRandomAdminAuthentication();

			ReplacePresentationPayload createPayload = new ReplacePresentationPayload(
					ThesisPresentationType.INTERMEDIATE,
					ThesisPresentationVisibility.PUBLIC,
					"Room 101", "http://stream.url", "English",
					Instant.now().plusSeconds(86400)
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/presentations", thesisId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(createPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			UUID presentationId = UUID.fromString(objectMapper.readTree(response)
					.get("presentations").get(0).get("presentationId").asString());

			ReplacePresentationPayload updatePayload = new ReplacePresentationPayload(
					ThesisPresentationType.FINAL,
					ThesisPresentationVisibility.PRIVATE,
					"Room 202", "http://new-stream.url", "German",
					Instant.now().plusSeconds(86400)
			);

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}/presentations/{presentationId}", thesisId, presentationId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(updatePayload)))
					.andExpect(status().isOk());

			var presentation = thesisPresentationRepository.findById(presentationId).orElseThrow();
			assertThat(presentation.getLocation()).isEqualTo("Room 202");
			assertThat(presentation.getStreamUrl()).isEqualTo("http://new-stream.url");
		}

		@Test
		void deletePresentation_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String adminAuth = createRandomAdminAuthentication();

			ReplacePresentationPayload payload = new ReplacePresentationPayload(
					ThesisPresentationType.INTERMEDIATE,
					ThesisPresentationVisibility.PUBLIC,
					"Room 101", "http://stream.url", "English",
					Instant.now().plusSeconds(86400)
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/presentations", thesisId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			UUID presentationId = UUID.fromString(objectMapper.readTree(response)
					.get("presentations").get(0).get("presentationId").asString());

			mockMvc.perform(MockMvcRequestBuilders.delete("/v2/theses/{thesisId}/presentations/{presentationId}", thesisId, presentationId)
							.header("Authorization", adminAuth))
					.andExpect(status().isOk());

			assertThat(thesisPresentationRepository.findById(presentationId)).isEmpty();
		}

		@Test
		void deletePresentation_AccessDenied_AsStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");

			ReplacePresentationPayload payload = new ReplacePresentationPayload(
					ThesisPresentationType.INTERMEDIATE,
					ThesisPresentationVisibility.PUBLIC,
					"Room 101", "http://stream.url", "English",
					Instant.now().plusSeconds(86400)
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/presentations", thesisId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			UUID presentationId = UUID.fromString(objectMapper.readTree(response)
					.get("presentations").get(0).get("presentationId").asString());

			String studentAuth = createRandomAuthentication("student");
			mockMvc.perform(MockMvcRequestBuilders.delete("/v2/theses/{thesisId}/presentations/{presentationId}", thesisId, presentationId)
							.header("Authorization", studentAuth))
					.andExpect(status().isForbidden());

			assertThat(thesisPresentationRepository.findById(presentationId)).isPresent();
		}
	}

	@Nested
	class ThesisCommentOperations {
		@Test
		void getComments_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses/{thesisId}/comments", thesisId)
							.header("Authorization", createRandomAdminAuthentication())
							.param("commentType", ThesisCommentType.THESIS.name()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").isArray()).isTrue();
		}

		@Test
		void createComment_WithFile_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			createTestEmailTemplate("THESIS_COMMENT_POSTED");

			PostThesisCommentPayload payload = new PostThesisCommentPayload(
					"Test comment", ThesisCommentType.THESIS
			);

			MockMultipartFile file = new MockMultipartFile(
					"file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "test content".getBytes()
			);
			MockMultipartFile jsonFile = new MockMultipartFile(
					"data", "", MediaType.APPLICATION_JSON_VALUE,
					objectMapper.writeValueAsString(payload).getBytes()
			);

			mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/comments", thesisId)
							.file(file)
							.file(jsonFile)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			assertThat(thesisCommentRepository.count()).isEqualTo(1);
		}

		@Test
		void deleteComment_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			createTestEmailTemplate("THESIS_COMMENT_POSTED");
			String adminAuth = createRandomAdminAuthentication();

			PostThesisCommentPayload payload = new PostThesisCommentPayload(
					"Test comment", ThesisCommentType.THESIS
			);
			MockMultipartFile jsonFile = new MockMultipartFile(
					"data", "", MediaType.APPLICATION_JSON_VALUE,
					objectMapper.writeValueAsString(payload).getBytes()
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/comments", thesisId)
							.file(jsonFile)
							.header("Authorization", adminAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			UUID commentId = UUID.fromString(objectMapper.readTree(response).get("commentId").asString());

			mockMvc.perform(MockMvcRequestBuilders.delete("/v2/theses/{thesisId}/comments/{commentId}", thesisId, commentId)
							.header("Authorization", adminAuth))
					.andExpect(status().isOk());

			assertThat(thesisCommentRepository.findById(commentId)).isEmpty();
		}

		@Test
		void deleteComment_AccessDenied_AsStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			createTestEmailTemplate("THESIS_COMMENT_POSTED");

			PostThesisCommentPayload payload = new PostThesisCommentPayload(
					"Test comment", ThesisCommentType.THESIS
			);
			MockMultipartFile jsonFile = new MockMultipartFile(
					"data", "", MediaType.APPLICATION_JSON_VALUE,
					objectMapper.writeValueAsString(payload).getBytes()
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/comments", thesisId)
							.file(jsonFile)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			UUID commentId = UUID.fromString(objectMapper.readTree(response).get("commentId").asString());

			String studentAuth = createRandomAuthentication("student");
			mockMvc.perform(MockMvcRequestBuilders.delete("/v2/theses/{thesisId}/comments/{commentId}", thesisId, commentId)
							.header("Authorization", studentAuth))
					.andExpect(status().isForbidden());

			assertThat(thesisCommentRepository.findById(commentId)).isPresent();
		}

		@Test
		void getCommentFile_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			createTestEmailTemplate("THESIS_COMMENT_POSTED");

			PostThesisCommentPayload payload = new PostThesisCommentPayload(
					"Test comment with file", ThesisCommentType.THESIS
			);

			MockMultipartFile file = new MockMultipartFile(
					"file", "attachment.pdf", MediaType.APPLICATION_PDF_VALUE, "file content".getBytes()
			);
			MockMultipartFile jsonFile = new MockMultipartFile(
					"data", "", MediaType.APPLICATION_JSON_VALUE,
					objectMapper.writeValueAsString(payload).getBytes()
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/comments", thesisId)
							.file(file)
							.file(jsonFile)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			UUID commentId = UUID.fromString(objectMapper.readTree(response).get("commentId").asString());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses/{thesisId}/comments/{commentId}/file", thesisId, commentId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());
		}
	}

	@Nested
	class ThesisAssessmentAndGrading {
		@Test
		void createAssessment_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			createTestEmailTemplate("THESIS_ASSESSMENT_ADDED");

			CreateAssessmentPayload payload = new CreateAssessmentPayload(
					"Test summary", "Test positives", "Test negatives", "1.0"
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/assessment", thesisId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.ASSESSED);
			assertThat(thesisAssessmentRepository.count()).isEqualTo(1);
		}

		@Test
		void addGrade_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			createTestEmailTemplate("THESIS_FINAL_GRADE");

			AddThesisGradePayload payload = new AddThesisGradePayload(
					"1.0", "Great work", ThesisVisibility.PUBLIC
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/grade", thesisId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.GRADED);
		}

		@Test
		void gradeThesis_VerifyDatabaseState() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			createTestEmailTemplate("THESIS_FINAL_GRADE");

			AddThesisGradePayload payload = new AddThesisGradePayload(
					"1.3", "Excellent work with minor issues", ThesisVisibility.PUBLIC
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/grade", thesisId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getFinalGrade()).isEqualTo("1.3");
			assertThat(thesis.getFinalFeedback()).isEqualTo("Excellent work with minor issues");
			assertThat(thesis.getVisibility()).isEqualTo(ThesisVisibility.PUBLIC);
			assertThat(thesis.getState()).isEqualTo(ThesisState.GRADED);
		}

		@Test
		void completeThesis_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			createTestEmailTemplate("THESIS_FINAL_GRADE");

			AddThesisGradePayload gradePayload = new AddThesisGradePayload(
					"1.0", "Perfect", ThesisVisibility.PUBLIC
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/grade", thesisId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(gradePayload)))
					.andExpect(status().isOk());

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/complete", thesisId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.FINISHED);
		}

		@Test
		void completeThesis_AccessDenied_AsStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String studentAuth = createRandomAuthentication("student");

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/complete", thesisId)
							.header("Authorization", studentAuth))
					.andExpect(status().isForbidden());

			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.PROPOSAL);
		}

		@Test
		void getAssessmentPdf_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			createTestEmailTemplate("THESIS_ASSESSMENT_ADDED");
			String adminAuth = createRandomAdminAuthentication();

			ReplacePresentationPayload presentationPayload = new ReplacePresentationPayload(
					ThesisPresentationType.FINAL,
					ThesisPresentationVisibility.PUBLIC,
					"Room 101", "http://stream.url", "English",
					Instant.now().plusSeconds(86400)
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/presentations", thesisId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(presentationPayload)))
					.andExpect(status().isOk());

			CreateAssessmentPayload assessmentPayload = new CreateAssessmentPayload(
					"Summary", "Positives", "Negatives", "1.7"
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/assessment", thesisId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(assessmentPayload)))
					.andExpect(status().isOk());

			var result = mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses/{thesisId}/assessment", thesisId)
							.header("Authorization", adminAuth))
					.andExpect(status().isOk())
					.andReturn();

			assertThat(result.getResponse().getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
			assertThat(result.getResponse().getContentLength()).isGreaterThan(0);
		}
	}

	@Nested
	class ThesisLifecycle {
		@Test
		void fullThesisLifecycle() throws Exception {
			createTestEmailTemplate("THESIS_PROPOSAL_UPLOADED");
			createTestEmailTemplate("THESIS_PROPOSAL_ACCEPTED");
			createTestEmailTemplate("THESIS_FINAL_SUBMISSION");
			createTestEmailTemplate("THESIS_ASSESSMENT_ADDED");
			createTestEmailTemplate("THESIS_FINAL_GRADE");

			UUID thesisId = createTestThesis("Lifecycle Thesis");
			String adminAuth = createRandomAdminAuthentication();

			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.PROPOSAL);

			// Upload proposal
			MockMultipartFile proposalFile = new MockMultipartFile(
					"proposal", "proposal.pdf", MediaType.APPLICATION_PDF_VALUE, "proposal content".getBytes()
			);
			mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/proposal", thesisId)
							.file(proposalFile)
							.header("Authorization", adminAuth))
					.andExpect(status().isOk());

			// Accept proposal → WRITING
			mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}/proposal/accept", thesisId)
							.header("Authorization", adminAuth))
					.andExpect(status().isOk());

			thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.WRITING);

			// Upload thesis file
			MockMultipartFile thesisFile = new MockMultipartFile(
					"file", "thesis.pdf", MediaType.APPLICATION_PDF_VALUE, "thesis content".getBytes()
			);
			MockMultipartFile fileType = new MockMultipartFile(
					"type", "", MediaType.TEXT_PLAIN_VALUE, "THESIS".getBytes()
			);
			mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/files", thesisId)
							.file(thesisFile)
							.file(fileType)
							.header("Authorization", adminAuth))
					.andExpect(status().isOk());

			// Submit thesis → SUBMITTED
			mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}/thesis/final-submission", thesisId)
							.header("Authorization", adminAuth))
					.andExpect(status().isOk());

			thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.SUBMITTED);

			// Create presentation (needed for assessment PDF)
			ReplacePresentationPayload presentationPayload = new ReplacePresentationPayload(
					ThesisPresentationType.FINAL, ThesisPresentationVisibility.PUBLIC,
					"Room 101", "http://stream.url", "English", Instant.now().plusSeconds(86400)
			);
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/presentations", thesisId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(presentationPayload)))
					.andExpect(status().isOk());

			// Submit assessment → ASSESSED
			CreateAssessmentPayload assessmentPayload = new CreateAssessmentPayload(
					"Summary", "Positives", "Negatives", "1.3"
			);
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/assessment", thesisId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(assessmentPayload)))
					.andExpect(status().isOk());

			thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.ASSESSED);

			// Grade thesis → GRADED
			AddThesisGradePayload gradePayload = new AddThesisGradePayload(
					"1.3", "Excellent work", ThesisVisibility.PUBLIC
			);
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/grade", thesisId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(gradePayload)))
					.andExpect(status().isOk());

			thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.GRADED);

			// Complete thesis → FINISHED
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/complete", thesisId)
							.header("Authorization", adminAuth))
					.andExpect(status().isOk());

			thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.FINISHED);
			assertThat(thesis.getFinalGrade()).isEqualTo("1.3");
			assertThat(thesis.getFinalFeedback()).isEqualTo("Excellent work");
		}
	}

}
