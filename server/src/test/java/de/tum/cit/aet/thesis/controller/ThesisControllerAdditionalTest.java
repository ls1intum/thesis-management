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
import de.tum.cit.aet.thesis.controller.payload.ReplacePresentationPayload;
import de.tum.cit.aet.thesis.controller.payload.RequestChangesPayload;
import de.tum.cit.aet.thesis.controller.payload.SchedulePresentationPayload;
import de.tum.cit.aet.thesis.controller.payload.UpdateNotePayload;
import de.tum.cit.aet.thesis.controller.payload.UpdateThesisCreditsPayload;
import de.tum.cit.aet.thesis.controller.payload.UpdateThesisInfoPayload;
import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.repository.ThesisAssessmentRepository;
import de.tum.cit.aet.thesis.repository.ThesisFeedbackRepository;
import de.tum.cit.aet.thesis.repository.ThesisPresentationRepository;
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
import java.util.UUID;

@Testcontainers
class ThesisControllerAdditionalTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private ThesisRepository thesisRepository;

	@Autowired
	private ThesisFeedbackRepository thesisFeedbackRepository;

	@Autowired
	private ThesisPresentationRepository thesisPresentationRepository;

	@Autowired
	private ThesisAssessmentRepository thesisAssessmentRepository;

	private UUID createDraftedPresentation(UUID thesisId, String adminAuth) throws Exception {
		ReplacePresentationPayload createPayload = new ReplacePresentationPayload(
				ThesisPresentationType.INTERMEDIATE,
				ThesisPresentationVisibility.PUBLIC,
				"Room 101", "http://stream.url", "English",
				Instant.now().plusSeconds(86400)
		);

		String response = mockMvc.perform(MockMvcRequestBuilders.post(
						"/v2/theses/{thesisId}/presentations", thesisId)
						.header("Authorization", adminAuth)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(createPayload)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		return UUID.fromString(objectMapper.readTree(response)
				.get("presentations").get(0).get("presentationId").asString());
	}

	private UUID createScheduledPresentation(UUID thesisId, String adminAuth) throws Exception {
		createTestEmailTemplate("THESIS_PRESENTATION_SCHEDULED");
		createTestEmailTemplate("THESIS_PRESENTATION_INVITATION");

		UUID presentationId = createDraftedPresentation(thesisId, adminAuth);

		SchedulePresentationPayload schedulePayload = new SchedulePresentationPayload(
				List.of(), false, false
		);
		mockMvc.perform(MockMvcRequestBuilders.post(
						"/v2/theses/{thesisId}/presentations/{presentationId}/schedule",
						thesisId, presentationId)
						.header("Authorization", adminAuth)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(schedulePayload)))
				.andExpect(status().isOk());

		return presentationId;
	}

	@Nested
	class ThesisPresentationSchedulingAndNotes {
		@Test
		void schedulePresentation_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String adminAuth = createRandomAdminAuthentication();
			createTestEmailTemplate("THESIS_PRESENTATION_SCHEDULED");
			createTestEmailTemplate("THESIS_PRESENTATION_INVITATION");

			UUID presentationId = createDraftedPresentation(thesisId, adminAuth);

			SchedulePresentationPayload schedulePayload = new SchedulePresentationPayload(
					List.of(), true, true
			);

			mockMvc.perform(MockMvcRequestBuilders.post(
							"/v2/theses/{thesisId}/presentations/{presentationId}/schedule",
							thesisId, presentationId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(schedulePayload)))
					.andExpect(status().isOk());

			var presentation = thesisPresentationRepository.findById(presentationId).orElseThrow();
			assertThat(presentation.getLocation()).isEqualTo("Room 101");
		}

		@Test
		void schedulePresentation_AccessDenied_AsStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String adminAuth = createRandomAdminAuthentication();

			UUID presentationId = createDraftedPresentation(thesisId, adminAuth);

			SchedulePresentationPayload schedulePayload = new SchedulePresentationPayload(
					List.of(), false, false
			);

			String studentAuth = createRandomAuthentication("student");
			mockMvc.perform(MockMvcRequestBuilders.post(
							"/v2/theses/{thesisId}/presentations/{presentationId}/schedule",
							thesisId, presentationId)
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(schedulePayload)))
					.andExpect(status().isForbidden());
		}

		@Test
		void updateNote_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String adminAuth = createRandomAdminAuthentication();

			UUID presentationId = createScheduledPresentation(thesisId, adminAuth);

			UpdateNotePayload notePayload = new UpdateNotePayload("This is a presentation note");

			mockMvc.perform(MockMvcRequestBuilders.put(
							"/v2/theses/{thesisId}/presentations/{presentationId}/note",
							thesisId, presentationId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(notePayload)))
					.andExpect(status().isOk());
		}

		@Test
		void updateNote_AccessDenied_AsNonMemberStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String adminAuth = createRandomAdminAuthentication();

			UUID presentationId = createScheduledPresentation(thesisId, adminAuth);

			UpdateNotePayload notePayload = new UpdateNotePayload("Unauthorized note");

			String studentAuth = createRandomAuthentication("student");
			mockMvc.perform(MockMvcRequestBuilders.put(
							"/v2/theses/{thesisId}/presentations/{presentationId}/note",
							thesisId, presentationId)
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(notePayload)))
					.andExpect(status().isForbidden());
		}

		@Test
		void updateNote_DraftedPresentation_Fails() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String adminAuth = createRandomAdminAuthentication();

			UUID presentationId = createDraftedPresentation(thesisId, adminAuth);

			UpdateNotePayload notePayload = new UpdateNotePayload("Note on drafted presentation");

			// The controller uses AccessDeniedException for this state validation,
			// so it returns 403 even though this is a business rule (not an auth check).
			mockMvc.perform(MockMvcRequestBuilders.put(
							"/v2/theses/{thesisId}/presentations/{presentationId}/note",
							thesisId, presentationId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(notePayload)))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class ThesisSearchAndPagination {
		private UUID createUniqueThesis(String title) throws Exception {
			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup(
					"Test Group " + UUID.randomUUID(), advisor.universityId());
			createTestEmailTemplate("THESIS_CREATED");

			CreateThesisPayload payload = new CreateThesisPayload(
					title, "MASTER", "ENGLISH",
					List.of(advisor.userId()), List.of(advisor.userId()),
					List.of(advisor.userId()), researchGroupId
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			return UUID.fromString(objectMapper.readTree(response).get("thesisId").asString());
		}

		@Test
		void getTheses_Pagination() throws Exception {
			createUniqueThesis("Thesis A");
			createUniqueThesis("Thesis B");
			createUniqueThesis("Thesis C");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.param("fetchAll", "true")
							.param("page", "0")
							.param("limit", "2"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isEqualTo(2);
			assertThat(json.get("totalElements").asInt()).isEqualTo(3);
			assertThat(json.get("totalPages").asInt()).isEqualTo(2);
		}

		@Test
		void getTheses_SearchByTitle() throws Exception {
			createUniqueThesis("UniqueSearchableTitle");
			createUniqueThesis("Other Thesis");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.param("fetchAll", "true")
							.param("search", "UniqueSearchable"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isEqualTo(1);
			assertThat(json.get("content").get(0).get("title").asString()).isEqualTo("UniqueSearchableTitle");
		}

		@Test
		void getTheses_FilterByType() throws Exception {
			createTestThesis("Master Thesis");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.param("fetchAll", "true")
							.param("type", "MASTER"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isEqualTo(1);
			assertThat(json.get("content").get(0).get("type").asString()).isEqualTo("MASTER");

			String emptyResponse = mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.param("fetchAll", "true")
							.param("type", "BACHELOR"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode emptyJson = objectMapper.readTree(emptyResponse);
			assertThat(emptyJson.path("content").size()).isZero();
		}

		@Test
		void getTheses_MultipleFilters() throws Exception {
			createTestThesis("Multi Filter Thesis");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.param("fetchAll", "true")
							.param("state", "PROPOSAL")
							.param("type", "MASTER"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isGreaterThanOrEqualTo(1);
			for (JsonNode thesis : json.get("content")) {
				assertThat(thesis.get("state").asString()).isEqualTo("PROPOSAL");
				assertThat(thesis.get("type").asString()).isEqualTo("MASTER");
			}
		}

		@Test
		void getTheses_EmptyResult() throws Exception {
			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.param("fetchAll", "true")
							.param("search", "nonexistentthesistitle999"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.path("content").size()).isZero();
			assertThat(json.get("totalElements").asInt()).isZero();
		}

		@Test
		void getTheses_VerifyResponseStructure() throws Exception {
			createTestThesis("Structure Test");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.param("fetchAll", "true"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.has("content")).isTrue();
			assertThat(json.has("totalElements")).isTrue();
			assertThat(json.has("totalPages")).isTrue();

			JsonNode firstThesis = json.get("content").get(0);
			assertThat(firstThesis.has("thesisId")).isTrue();
			assertThat(firstThesis.has("title")).isTrue();
			assertThat(firstThesis.has("type")).isTrue();
			assertThat(firstThesis.has("state")).isTrue();
			assertThat(firstThesis.has("students")).isTrue();
			assertThat(firstThesis.has("advisors")).isTrue();
			// Overview DTO does not include language or visibility
			assertThat(firstThesis.has("language")).isFalse();
			assertThat(firstThesis.has("visibility")).isFalse();
		}

		@Test
		void getTheses_WithPresentation_IncludesPresentationOverview() throws Exception {
			UUID thesisId = createTestThesis("Thesis With Presentation");
			String adminAuth = createRandomAdminAuthentication();

			ReplacePresentationPayload presentationPayload = new ReplacePresentationPayload(
					ThesisPresentationType.INTERMEDIATE,
					ThesisPresentationVisibility.PUBLIC,
					"Room 101",
					"http://stream.url",
					"English",
					Instant.now().plusSeconds(86400)
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/presentations", thesisId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(presentationPayload)))
					.andExpect(status().isOk());

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses")
							.header("Authorization", adminAuth)
							.param("fetchAll", "true")
							.param("search", "Thesis With Presentation"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			JsonNode firstThesis = json.get("content").get(0);
			assertThat(firstThesis.has("presentations")).isTrue();

			JsonNode presentations = firstThesis.get("presentations");
			assertThat(presentations.isArray()).isTrue();
			assertThat(presentations.size()).isEqualTo(1);
			assertThat(presentations.get(0).has("presentationId")).isTrue();
			assertThat(presentations.get(0).has("type")).isTrue();
			assertThat(presentations.get(0).has("scheduledAt")).isTrue();
			assertThat(presentations.get(0).get("type").asString()).isEqualTo("INTERMEDIATE");
		}
	}

	@Nested
	class ThesisDatabaseVerification {
		@Test
		void createThesis_VerifyDatabaseState() throws Exception {
			TestUser advisor = createTestUser("db-supervisor", List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("DB Test Group", advisor.universityId());
			createTestEmailTemplate("THESIS_CREATED");

			CreateThesisPayload payload = new CreateThesisPayload(
					"Database State Thesis",
					"BACHELOR",
					"GERMAN",
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

			UUID thesisId = UUID.fromString(objectMapper.readTree(response).get("thesisId").asString());

			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getTitle()).isEqualTo("Database State Thesis");
			assertThat(thesis.getType()).isEqualTo("BACHELOR");
			assertThat(thesis.getLanguage()).isEqualTo("GERMAN");
			assertThat(thesis.getState()).isEqualTo(ThesisState.PROPOSAL);
			assertThat(thesis.getVisibility()).isEqualTo(ThesisVisibility.INTERNAL);
			assertThat(thesis.getCreatedAt()).isNotNull();
			assertThat(thesis.getResearchGroup()).isNotNull();
			assertThat(thesis.getResearchGroup().getId()).isEqualTo(researchGroupId);
		}
	}

	@Nested
	class ThesisAdditionalAccessControl {
		@Test
		void requestChanges_AccessDenied_AsStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String studentAuth = createRandomAuthentication("student");

			RequestChangesPayload payload = new RequestChangesPayload(
					ThesisFeedbackType.THESIS,
					List.of(new RequestChangesPayload.RequestedChange("Fix this", false))
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/feedback", thesisId)
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isForbidden());

			assertThat(thesisFeedbackRepository.count()).isZero();
		}

		@Test
		void createAssessment_AccessDenied_AsStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String studentAuth = createRandomAuthentication("student");

			CreateAssessmentPayload payload = new CreateAssessmentPayload(
					"Summary", "Positives", "Negatives", "1.0"
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/assessment", thesisId)
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isForbidden());

			assertThat(thesisAssessmentRepository.count()).isZero();
		}

		@Test
		void addGrade_AccessDenied_AsStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String studentAuth = createRandomAuthentication("student");

			AddThesisGradePayload payload = new AddThesisGradePayload(
					"1.0", "Feedback", ThesisVisibility.PUBLIC
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/grade", thesisId)
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isForbidden());
		}

		@Test
		void submitThesis_AccessDenied_AsNonMemberStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String studentAuth = createRandomAuthentication("student");

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}/thesis/final-submission", thesisId)
							.header("Authorization", studentAuth))
					.andExpect(status().isForbidden());
		}

		@Test
		void createPresentation_AccessDenied_AsNonMemberStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String studentAuth = createRandomAuthentication("student");

			ReplacePresentationPayload payload = new ReplacePresentationPayload(
					ThesisPresentationType.INTERMEDIATE,
					ThesisPresentationVisibility.PUBLIC,
					"Room 101", "http://stream.url", "English",
					Instant.now().plusSeconds(86400)
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/presentations", thesisId)
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isForbidden());
		}

		@Test
		void uploadThesisFile_AccessDenied_AsNonMemberStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String studentAuth = createRandomAuthentication("student");

			MockMultipartFile file = new MockMultipartFile(
					"file", "thesis.pdf", MediaType.APPLICATION_PDF_VALUE, "content".getBytes()
			);
			MockMultipartFile type = new MockMultipartFile(
					"type", "", MediaType.TEXT_PLAIN_VALUE, "THESIS".getBytes()
			);

			mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/files", thesisId)
							.file(file)
							.file(type)
							.header("Authorization", studentAuth))
					.andExpect(status().isForbidden());
		}

		@Test
		void uploadProposal_AccessDenied_AsNonMemberStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String studentAuth = createRandomAuthentication("student");

			MockMultipartFile proposalFile = new MockMultipartFile(
					"proposal", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "test content".getBytes()
			);

			mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/proposal", thesisId)
							.file(proposalFile)
							.header("Authorization", studentAuth))
					.andExpect(status().isForbidden());
		}

		@Test
		void getComments_AsAdvisorType_Success() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses/{thesisId}/comments", thesisId)
							.header("Authorization", createRandomAdminAuthentication())
							.param("commentType", ThesisCommentType.ADVISOR.name()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.path("content").size()).isZero();
			assertThat(json.get("totalElements").asInt()).isZero();
		}

		@Test
		void getComments_AsAdvisorType_AccessDenied_AsNonMemberStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String studentAuth = createRandomAuthentication("student");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses/{thesisId}/comments", thesisId)
							.header("Authorization", studentAuth)
							.param("commentType", ThesisCommentType.ADVISOR.name()))
					.andExpect(status().isForbidden());
		}

		@Test
		void acceptProposal_AccessDenied_AsStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			createTestEmailTemplate("THESIS_PROPOSAL_UPLOADED");

			MockMultipartFile proposalFile = new MockMultipartFile(
					"proposal", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "test content".getBytes()
			);
			mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/proposal", thesisId)
							.file(proposalFile)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			String studentAuth = createRandomAuthentication("student");
			mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}/proposal/accept", thesisId)
							.header("Authorization", studentAuth))
					.andExpect(status().isForbidden());

			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.PROPOSAL);
		}

		@Test
		void updatePresentation_AccessDenied_AsNonMemberStudent() throws Exception {
			UUID thesisId = createTestThesis("Test Thesis");
			String adminAuth = createRandomAdminAuthentication();

			UUID presentationId = createDraftedPresentation(thesisId, adminAuth);

			ReplacePresentationPayload updatePayload = new ReplacePresentationPayload(
					ThesisPresentationType.FINAL,
					ThesisPresentationVisibility.PRIVATE,
					"Room 202", "http://new.url", "German",
					Instant.now().plusSeconds(86400)
			);

			String studentAuth = createRandomAuthentication("student");
			mockMvc.perform(MockMvcRequestBuilders.put(
							"/v2/theses/{thesisId}/presentations/{presentationId}",
							thesisId, presentationId)
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(updatePayload)))
					.andExpect(status().isForbidden());
		}

		@Test
		void getTheses_Unauthenticated_ReturnsUnauthorized() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses"))
					.andExpect(status().isUnauthorized());
		}
	}

	@Nested
	class ThesisSupervisorRoleAccess {
		@Test
		void gradeThesis_Success_AsSupervisor() throws Exception {
			TestUser supervisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup(
					"Supervisor Test Group", supervisor.universityId());
			createTestEmailTemplate("THESIS_CREATED");
			createTestEmailTemplate("THESIS_FINAL_GRADE");

			CreateThesisPayload thesisPayload = new CreateThesisPayload(
					"Supervisor Grade Test", "MASTER", "ENGLISH",
					List.of(supervisor.userId()), List.of(supervisor.userId()),
					List.of(supervisor.userId()), researchGroupId
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(thesisPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID thesisId = UUID.fromString(objectMapper.readTree(response).get("thesisId").asString());

			String supervisorAuth = generateTestAuthenticationHeader(
					supervisor.universityId(), List.of("supervisor", "advisor"));

			AddThesisGradePayload gradePayload = new AddThesisGradePayload(
					"1.3", "Good work", ThesisVisibility.PUBLIC
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/grade", thesisId)
							.header("Authorization", supervisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(gradePayload)))
					.andExpect(status().isOk());

			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.GRADED);
			assertThat(thesis.getFinalGrade()).isEqualTo("1.3");
		}

		@Test
		void completeThesis_Success_AsSupervisor() throws Exception {
			TestUser supervisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup(
					"Supervisor Complete Group", supervisor.universityId());
			createTestEmailTemplate("THESIS_CREATED");
			createTestEmailTemplate("THESIS_FINAL_GRADE");

			CreateThesisPayload thesisPayload = new CreateThesisPayload(
					"Supervisor Complete Test", "MASTER", "ENGLISH",
					List.of(supervisor.userId()), List.of(supervisor.userId()),
					List.of(supervisor.userId()), researchGroupId
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(thesisPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID thesisId = UUID.fromString(objectMapper.readTree(response).get("thesisId").asString());

			String supervisorAuth = generateTestAuthenticationHeader(
					supervisor.universityId(), List.of("supervisor", "advisor"));

			AddThesisGradePayload gradePayload = new AddThesisGradePayload(
					"1.0", "Perfect", ThesisVisibility.PUBLIC
			);
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/grade", thesisId)
							.header("Authorization", supervisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(gradePayload)))
					.andExpect(status().isOk());

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/complete", thesisId)
							.header("Authorization", supervisorAuth))
					.andExpect(status().isOk());

			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.FINISHED);
		}

		@Test
		void gradeThesis_AccessDenied_AsAdvisorOnly() throws Exception {
			TestUser advisor = createRandomTestUser(List.of("advisor"));
			TestUser supervisor = createRandomTestUser(List.of("supervisor"));
			UUID researchGroupId = createTestResearchGroup(
					"Advisor Grade Group", supervisor.universityId());
			createTestEmailTemplate("THESIS_CREATED");

			CreateThesisPayload thesisPayload = new CreateThesisPayload(
					"Advisor Grade Test", "MASTER", "ENGLISH",
					List.of(advisor.userId()), List.of(advisor.userId()),
					List.of(supervisor.userId()), researchGroupId
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(thesisPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID thesisId = UUID.fromString(objectMapper.readTree(response).get("thesisId").asString());

			// Advisor (non-supervisor) should not be able to grade
			String advisorAuth = generateTestAuthenticationHeader(
					advisor.universityId(), List.of("advisor"));

			AddThesisGradePayload gradePayload = new AddThesisGradePayload(
					"1.0", "Feedback", ThesisVisibility.PUBLIC
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/grade", thesisId)
							.header("Authorization", advisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(gradePayload)))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class ThesisInfoUpdate {
		@Test
		void updateThesisInfo_Success_AsAdvisor() throws Exception {
			TestUser student = createRandomTestUser(List.of("student"));
			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Info Group", advisor.universityId());
			createTestEmailTemplate("THESIS_CREATED");

			CreateThesisPayload thesisPayload = new CreateThesisPayload(
					"Info Update Thesis", "MASTER", "ENGLISH",
					List.of(student.userId()), List.of(advisor.userId()),
					List.of(advisor.userId()), researchGroupId
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(thesisPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID thesisId = UUID.fromString(objectMapper.readTree(response).get("thesisId").asString());

			String advisorAuth = generateTestAuthenticationHeader(
					advisor.universityId(), List.of("supervisor", "advisor"));

			UpdateThesisInfoPayload infoPayload = new UpdateThesisInfoPayload(
					"This is the abstract", "Additional info text",
					"Primary Title", Map.of("de", "German Title")
			);

			String updateResponse = mockMvc.perform(MockMvcRequestBuilders.put(
									"/v2/theses/{thesisId}/info", thesisId)
							.header("Authorization", advisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(infoPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(updateResponse);
			assertThat(json.get("abstractText").asString()).isEqualTo("This is the abstract");
		}

		@Test
		void updateThesisInfo_AccessDenied_AsNonMember() throws Exception {
			UUID thesisId = createTestThesis("Non-member Info Thesis");
			String studentAuth = createRandomAuthentication("student");

			UpdateThesisInfoPayload infoPayload = new UpdateThesisInfoPayload(
					"Unauthorized abstract", null, null, null
			);

			mockMvc.perform(MockMvcRequestBuilders.put(
							"/v2/theses/{thesisId}/info", thesisId)
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(infoPayload)))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class ThesisCreditsUpdate {
		@Test
		void updateThesisCredits_Success_AsAdvisor() throws Exception {
			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Credits Group", advisor.universityId());
			createTestEmailTemplate("THESIS_CREATED");

			CreateThesisPayload thesisPayload = new CreateThesisPayload(
					"Credits Update Thesis", "MASTER", "ENGLISH",
					List.of(advisor.userId()), List.of(advisor.userId()),
					List.of(advisor.userId()), researchGroupId
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(thesisPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID thesisId = UUID.fromString(objectMapper.readTree(response).get("thesisId").asString());

			String advisorAuth = generateTestAuthenticationHeader(
					advisor.universityId(), List.of("supervisor", "advisor"));

			UpdateThesisCreditsPayload creditsPayload = new UpdateThesisCreditsPayload(
					Map.of(advisor.userId(), 30)
			);

			mockMvc.perform(MockMvcRequestBuilders.put(
							"/v2/theses/{thesisId}/credits", thesisId)
							.header("Authorization", advisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(creditsPayload)))
					.andExpect(status().isOk());
		}

		@Test
		void updateThesisCredits_AccessDenied_AsStudent() throws Exception {
			UUID thesisId = createTestThesis("Student Credits Thesis");
			String studentAuth = createRandomAuthentication("student");

			UpdateThesisCreditsPayload creditsPayload = new UpdateThesisCreditsPayload(
					Map.of(UUID.randomUUID(), 30)
			);

			mockMvc.perform(MockMvcRequestBuilders.put(
							"/v2/theses/{thesisId}/credits", thesisId)
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(creditsPayload)))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class ThesisLifecycle {
		private record ThesisSetup(UUID thesisId, String advisorAuth, TestUser advisor) {}

		private ThesisSetup createThesisWithAdvisor() throws Exception {
			TestUser student = createRandomTestUser(List.of("student"));
			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Lifecycle Group", advisor.universityId());
			createTestEmailTemplate("THESIS_CREATED");

			CreateThesisPayload thesisPayload = new CreateThesisPayload(
					"Lifecycle Thesis", "MASTER", "ENGLISH",
					List.of(student.userId()), List.of(advisor.userId()),
					List.of(advisor.userId()), researchGroupId
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(thesisPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID thesisId = UUID.fromString(objectMapper.readTree(response).get("thesisId").asString());
			String advisorAuth = generateTestAuthenticationHeader(
					advisor.universityId(), List.of("supervisor", "advisor"));
			return new ThesisSetup(thesisId, advisorAuth, advisor);
		}

		@Test
		void proposalUploadAndAccept_FullFlow() throws Exception {
			createTestEmailTemplate("THESIS_PROPOSAL_UPLOADED");
			createTestEmailTemplate("THESIS_PROPOSAL_ACCEPTED");

			ThesisSetup setup = createThesisWithAdvisor();

			// Upload proposal (as advisor since they're also in the thesis)
			MockMultipartFile proposalFile = new MockMultipartFile(
					"proposal", "proposal.pdf", MediaType.APPLICATION_PDF_VALUE, "proposal content".getBytes()
			);
			mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/proposal", setup.thesisId)
							.file(proposalFile)
							.header("Authorization", setup.advisorAuth))
					.andExpect(status().isOk());

			Thesis thesis = thesisRepository.findById(setup.thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.PROPOSAL);

			// Accept proposal
			mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}/proposal/accept", setup.thesisId)
							.header("Authorization", setup.advisorAuth))
					.andExpect(status().isOk());

			thesis = thesisRepository.findById(setup.thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.WRITING);
		}

		@Test
		void requestChanges_ProposalFeedback_Success() throws Exception {
			createTestEmailTemplate("THESIS_PROPOSAL_UPLOADED");
			createTestEmailTemplate("THESIS_PROPOSAL_REJECTED");

			ThesisSetup setup = createThesisWithAdvisor();

			// Upload proposal first
			MockMultipartFile proposalFile = new MockMultipartFile(
					"proposal", "proposal.pdf", MediaType.APPLICATION_PDF_VALUE, "proposal content".getBytes()
			);
			mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/proposal", setup.thesisId)
							.file(proposalFile)
							.header("Authorization", setup.advisorAuth))
					.andExpect(status().isOk());

			// Request changes
			RequestChangesPayload changesPayload = new RequestChangesPayload(
					ThesisFeedbackType.PROPOSAL,
					List.of(new RequestChangesPayload.RequestedChange("Improve the introduction", false))
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/feedback", setup.thesisId)
							.header("Authorization", setup.advisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(changesPayload)))
					.andExpect(status().isOk());

			assertThat(thesisFeedbackRepository.count()).isEqualTo(1);
		}

		@Test
		void assessmentAndGrade_FullFlow() throws Exception {
			createTestEmailTemplate("THESIS_PROPOSAL_UPLOADED");
			createTestEmailTemplate("THESIS_PROPOSAL_ACCEPTED");
			createTestEmailTemplate("THESIS_FINAL_SUBMISSION");
			createTestEmailTemplate("THESIS_ASSESSMENT_ADDED");
			createTestEmailTemplate("THESIS_FINAL_GRADE");

			ThesisSetup setup = createThesisWithAdvisor();

			// Upload and accept proposal to get to WRITING state
			MockMultipartFile proposalFile = new MockMultipartFile(
					"proposal", "proposal.pdf", MediaType.APPLICATION_PDF_VALUE, "proposal content".getBytes()
			);
			mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/proposal", setup.thesisId)
							.file(proposalFile)
							.header("Authorization", setup.advisorAuth))
					.andExpect(status().isOk());
			mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}/proposal/accept", setup.thesisId)
							.header("Authorization", setup.advisorAuth))
					.andExpect(status().isOk());

			// Upload thesis file to enable final submission
			MockMultipartFile thesisFile = new MockMultipartFile(
					"file", "thesis.pdf", MediaType.APPLICATION_PDF_VALUE, "thesis content".getBytes()
			);
			MockMultipartFile thesisType = new MockMultipartFile(
					"type", "", MediaType.TEXT_PLAIN_VALUE, "THESIS".getBytes()
			);
			mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/files", setup.thesisId)
							.file(thesisFile)
							.file(thesisType)
							.header("Authorization", setup.advisorAuth))
					.andExpect(status().isOk());

			// Submit thesis
			mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}/thesis/final-submission", setup.thesisId)
							.header("Authorization", setup.advisorAuth))
					.andExpect(status().isOk());

			Thesis thesis = thesisRepository.findById(setup.thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.SUBMITTED);

			// Create assessment
			CreateAssessmentPayload assessmentPayload = new CreateAssessmentPayload(
					"Good thesis", "Clear structure", "Minor issues", "1.3"
			);
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/assessment", setup.thesisId)
							.header("Authorization", setup.advisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(assessmentPayload)))
					.andExpect(status().isOk());

			thesis = thesisRepository.findById(setup.thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.ASSESSED);

			// Grade thesis
			AddThesisGradePayload gradePayload = new AddThesisGradePayload(
					"1.3", "Well done", ThesisVisibility.PUBLIC
			);
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/grade", setup.thesisId)
							.header("Authorization", setup.advisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(gradePayload)))
					.andExpect(status().isOk());

			thesis = thesisRepository.findById(setup.thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.GRADED);
			assertThat(thesis.getFinalGrade()).isEqualTo("1.3");
		}

		@Test
		void deletePresentation_Success() throws Exception {
			createTestEmailTemplate("THESIS_PRESENTATION_DELETED");
			createTestEmailTemplate("THESIS_PRESENTATION_INVITATION_CANCELLED");

			ThesisSetup setup = createThesisWithAdvisor();
			UUID presentationId = createScheduledPresentation(setup.thesisId, setup.advisorAuth);

			mockMvc.perform(MockMvcRequestBuilders.delete(
							"/v2/theses/{thesisId}/presentations/{presentationId}",
							setup.thesisId, presentationId)
							.header("Authorization", setup.advisorAuth))
					.andExpect(status().isOk());
		}
	}

	@Nested
	class ThesisClose {
		@Test
		void closeThesis_Success_AsAdvisor() throws Exception {
			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Close Group", advisor.universityId());
			createTestEmailTemplate("THESIS_CREATED");
			createTestEmailTemplate("THESIS_CLOSED");

			CreateThesisPayload thesisPayload = new CreateThesisPayload(
					"Close Thesis Test", "MASTER", "ENGLISH",
					List.of(advisor.userId()), List.of(advisor.userId()),
					List.of(advisor.userId()), researchGroupId
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(thesisPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID thesisId = UUID.fromString(objectMapper.readTree(response).get("thesisId").asString());

			String advisorAuth = generateTestAuthenticationHeader(
					advisor.universityId(), List.of("supervisor", "advisor"));

			String closeResponse = mockMvc.perform(MockMvcRequestBuilders.delete(
							"/v2/theses/{thesisId}", thesisId)
							.header("Authorization", advisorAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(closeResponse);
			assertThat(json.get("state").asString()).isEqualTo("DROPPED_OUT");

			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			assertThat(thesis.getState()).isEqualTo(ThesisState.DROPPED_OUT);
		}

		@Test
		void closeThesis_AccessDenied_AsStudent() throws Exception {
			UUID thesisId = createTestThesis("Student Close Thesis");
			String studentAuth = createRandomAuthentication("student");

			mockMvc.perform(MockMvcRequestBuilders.delete(
							"/v2/theses/{thesisId}", thesisId)
							.header("Authorization", studentAuth))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class UpdateThesisInfo {
		@Test
		void updateThesisInfo_Success() throws Exception {
			createTestEmailTemplate("THESIS_CREATED");
			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Info RG", advisor.universityId());

			CreateThesisPayload createPayload = new CreateThesisPayload(
					"Info Test Thesis", "MASTER", "ENGLISH",
					List.of(advisor.userId()), List.of(advisor.userId()),
					List.of(advisor.userId()), researchGroupId
			);
			String adminAuth = createRandomAdminAuthentication();
			String createResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(createPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID thesisId = UUID.fromString(objectMapper.readTree(createResponse).get("thesisId").asString());

			String advisorAuth = generateTestAuthenticationHeader(advisor.universityId(), List.of("supervisor", "advisor"));

			UpdateThesisInfoPayload infoPayload = new UpdateThesisInfoPayload(
					"Test abstract text", "Test info text", "Updated Title", null
			);
			String response = mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}/info", thesisId)
							.header("Authorization", advisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(infoPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("abstractText").asString()).isEqualTo("Test abstract text");
			assertThat(json.get("infoText").asString()).isEqualTo("Test info text");
		}

		@Test
		void updateThesisInfo_WithNullValues_Success() throws Exception {
			createTestEmailTemplate("THESIS_CREATED");
			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Null Info RG", advisor.universityId());

			CreateThesisPayload createPayload = new CreateThesisPayload(
					"Null Info Thesis", "MASTER", "ENGLISH",
					List.of(advisor.userId()), List.of(advisor.userId()),
					List.of(advisor.userId()), researchGroupId
			);
			String adminAuth = createRandomAdminAuthentication();
			String createResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(createPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID thesisId = UUID.fromString(objectMapper.readTree(createResponse).get("thesisId").asString());

			String advisorAuth = generateTestAuthenticationHeader(advisor.universityId(), List.of("supervisor", "advisor"));

			UpdateThesisInfoPayload infoPayload = new UpdateThesisInfoPayload(
					null, null, null, null
			);
			mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}/info", thesisId)
							.header("Authorization", advisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(infoPayload)))
					.andExpect(status().isOk());
		}
	}

	@Nested
	class UpdateThesisCredits {
		@Test
		void updateThesisCredits_Success() throws Exception {
			createTestEmailTemplate("THESIS_CREATED");
			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Credits RG", advisor.universityId());

			CreateThesisPayload createPayload = new CreateThesisPayload(
					"Credits Test Thesis", "MASTER", "ENGLISH",
					List.of(advisor.userId()), List.of(advisor.userId()),
					List.of(advisor.userId()), researchGroupId
			);
			String adminAuth = createRandomAdminAuthentication();
			String createResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(createPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID thesisId = UUID.fromString(objectMapper.readTree(createResponse).get("thesisId").asString());

			String advisorAuth = generateTestAuthenticationHeader(advisor.universityId(), List.of("supervisor", "advisor"));

			UpdateThesisCreditsPayload creditsPayload = new UpdateThesisCreditsPayload(Map.of(advisor.userId(), 30));
			mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}/credits", thesisId)
							.header("Authorization", advisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(creditsPayload)))
					.andExpect(status().isOk());
		}
	}
}
