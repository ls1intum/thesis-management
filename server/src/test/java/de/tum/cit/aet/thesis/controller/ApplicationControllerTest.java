package de.tum.cit.aet.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.constants.ApplicationRejectReason;
import de.tum.cit.aet.thesis.constants.ApplicationReviewReason;
import de.tum.cit.aet.thesis.constants.ApplicationState;
import de.tum.cit.aet.thesis.controller.payload.AcceptApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.CloseTopicPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateInterviewProcessPayload;
import de.tum.cit.aet.thesis.controller.payload.RejectApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.ReplaceTopicPayload;
import de.tum.cit.aet.thesis.controller.payload.ReviewApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.UpdateApplicationCommentPayload;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.repository.ApplicationRepository;
import de.tum.cit.aet.thesis.repository.ApplicationReviewerRepository;
import de.tum.cit.aet.thesis.repository.InterviewProcessRepository;
import de.tum.cit.aet.thesis.repository.ThesisRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;

import jakarta.mail.internet.MimeMessage;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Testcontainers
class ApplicationControllerTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private ApplicationRepository applicationRepository;

	@Autowired
	private ApplicationReviewerRepository applicationReviewerRepository;

	@Autowired
	private ThesisRepository thesisRepository;

	@Autowired
	private InterviewProcessRepository interviewProcessRepository;

	@Nested
	class ApplicationCreation {
		@Test
		void createApplication_Unauthenticated_ReturnsUnauthorized() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.contentType(MediaType.APPLICATION_JSON)
							.content("{}"))
					.andExpect(status().isUnauthorized());
		}

		@Test
		void createApplication_Success() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			UUID researchGroupId = createDefaultResearchGroup();
			CreateApplicationPayload payload = new CreateApplicationPayload(
					null, "Test Thesis", "MASTER", Instant.now(), "Test motivation", researchGroupId
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("thesisTitle").asString()).isEqualTo("Test Thesis");
			assertThat(json.get("thesisType").asString()).isEqualTo("MASTER");
			assertThat(json.get("motivation").asString()).isEqualTo("Test motivation");
			assertThat(json.get("state").asString()).isEqualTo(ApplicationState.NOT_ASSESSED.getValue());
			assertThat(json.get("applicationId").asString()).isNotBlank();

			assertThat(applicationRepository.count()).isEqualTo(1);
		}

		@Test
		void createApplication_WithTopic_Success() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			UUID topicId = createTestTopic("Test Topic");

			CreateApplicationPayload payload = new CreateApplicationPayload(
					topicId, null, "MASTER", Instant.now(), "Motivation for topic", null
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("topic")).isNotNull();
			assertThat(json.get("motivation").asString()).isEqualTo("Motivation for topic");
			assertThat(json.get("state").asString()).isEqualTo(ApplicationState.NOT_ASSESSED.getValue());
		}

		@Test
		void createApplication_MissingTopicAndTitle_ReturnsBadRequest() throws Exception {
			UUID researchGroupId = createDefaultResearchGroup();
			CreateApplicationPayload payload = new CreateApplicationPayload(
					null, null, "MASTER", Instant.now(), "Motivation", researchGroupId
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isBadRequest());

			assertThat(applicationRepository.count()).isZero();
		}

		@Test
		void createApplication_DuplicateApplication_ReturnsConflict() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			UUID topicId = createTestTopic("Test Topic");
			String auth = createRandomAdminAuthentication();

			CreateApplicationPayload payload = new CreateApplicationPayload(
					topicId, null, "MASTER", Instant.now(), "First application", null
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", auth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", auth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isConflict());

			assertThat(applicationRepository.count()).isEqualTo(1);
		}

		@Test
		void createApplication_VerifyDatabaseState() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			UUID researchGroupId = createDefaultResearchGroup();
			CreateApplicationPayload payload = new CreateApplicationPayload(
					null, "Database Check Thesis", "BACHELOR", Instant.now(), "Verify DB state", researchGroupId
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			UUID applicationId = UUID.fromString(objectMapper.readTree(response).get("applicationId").asString());

			var application = applicationRepository.findById(applicationId).orElseThrow();
			assertThat(application.getThesisTitle()).isEqualTo("Database Check Thesis");
			assertThat(application.getThesisType()).isEqualTo("BACHELOR");
			assertThat(application.getMotivation()).isEqualTo("Verify DB state");
			assertThat(application.getState()).isEqualTo(ApplicationState.NOT_ASSESSED);
			assertThat(application.getComment()).isEmpty();
			assertThat(application.getCreatedAt()).isNotNull();
			assertThat(application.getUser()).isNotNull();
		}
	}

	@Nested
	class ApplicationRetrieval {
		@Test
		void getApplications_Success_AsAdmin() throws Exception {
			// Use different users since a user can only have one pending application with topicId=null
			String studentAuth1 = createRandomAuthentication("student");
			createTestApplication(studentAuth1, "Application 1");
			String studentAuth2 = createRandomAuthentication("student");
			createTestApplication(studentAuth2, "Application 2");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/applications")
							.header("Authorization", createRandomAdminAuthentication())
							.param("fetchAll", "true"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").isArray()).isTrue();
			assertThat(json.get("content").size()).isEqualTo(2);
			assertThat(json.get("totalElements").asInt()).isEqualTo(2);
		}

		@Test
		void getApplications_FilterByState() throws Exception {
			String adminAuth = createRandomAdminAuthentication();
			createTestApplication(adminAuth, "Application 1");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/applications")
							.header("Authorization", createRandomAdminAuthentication())
							.param("fetchAll", "true")
							.param("state", "NOT_ASSESSED"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isEqualTo(1);

			String responseEmpty = mockMvc.perform(MockMvcRequestBuilders.get("/v2/applications")
							.header("Authorization", createRandomAdminAuthentication())
							.param("fetchAll", "true")
							.param("state", "ACCEPTED"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode jsonEmpty = objectMapper.readTree(responseEmpty);
			assertThat(jsonEmpty.path("content").size()).isZero();
		}

		@Test
		void getApplications_FilterByPreviousIds() throws Exception {
			String studentAuth = createRandomAuthentication("student");
			UUID applicationId = createTestApplication(studentAuth, "Previous App");

			// Reject the application so it no longer matches NOT_ASSESSED state
			createTestEmailTemplate("APPLICATION_REJECTED");
			RejectApplicationPayload rejectPayload = new RejectApplicationPayload(
					ApplicationRejectReason.GENERAL, false
			);
			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/reject", applicationId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(rejectPayload)))
					.andExpect(status().isOk());

			// Without previous param, filtering by NOT_ASSESSED should return nothing
			String responseWithout = mockMvc.perform(MockMvcRequestBuilders.get("/v2/applications")
							.header("Authorization", createRandomAdminAuthentication())
							.param("fetchAll", "true")
							.param("state", "NOT_ASSESSED"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode contentWithout = objectMapper.readTree(responseWithout).get("content");
			assertThat(contentWithout == null || contentWithout.size() == 0).isTrue();

			// With previous param, the rejected application should still be included
			String responseWith = mockMvc.perform(MockMvcRequestBuilders.get("/v2/applications")
							.header("Authorization", createRandomAdminAuthentication())
							.param("fetchAll", "true")
							.param("state", "NOT_ASSESSED")
							.param("previous", applicationId.toString()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(responseWith);
			assertThat(json.get("content").size()).isEqualTo(1);
			assertThat(json.get("content").get(0).get("applicationId").asString()).isEqualTo(applicationId.toString());
		}

		@Test
		void getApplications_AsStudent_ReturnsOwnOnly() throws Exception {
			String studentAuth = createRandomAuthentication("student");
			createTestApplication(studentAuth, "Student Application");

			String otherStudentAuth = createRandomAuthentication("student");
			createTestApplication(otherStudentAuth, "Other Application");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/applications")
							.header("Authorization", studentAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isEqualTo(1);
			assertThat(json.get("content").get(0).get("thesisTitle").asString()).isEqualTo("Student Application");
		}

		@Test
		void getApplications_Pagination() throws Exception {
			// Use different users since a user can only have one pending application with topicId=null
			for (int i = 0; i < 3; i++) {
				String userAuth = createRandomAuthentication("student");
				createTestApplication(userAuth, "Application " + i);
			}

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/applications")
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
		void getApplication_Success() throws Exception {
			String adminAuth = createRandomAdminAuthentication();
			UUID applicationId = createTestApplication(adminAuth, "Test Application");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/applications/{applicationId}", applicationId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("applicationId").asString()).isEqualTo(applicationId.toString());
			assertThat(json.get("thesisTitle").asString()).isEqualTo("Test Application");
			assertThat(json.get("state").asString()).isEqualTo(ApplicationState.NOT_ASSESSED.getValue());
		}

		@Test
		void getApplication_NotFound() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/applications/{applicationId}", UUID.randomUUID())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNotFound());
		}

		@Test
		void getApplication_AccessDenied_AsDifferentStudent() throws Exception {
			String ownerAuth = createRandomAuthentication("student");
			UUID applicationId = createTestApplication(ownerAuth, "Private Application");

			String differentStudentAuth = createRandomAuthentication("student");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/applications/{applicationId}", applicationId)
							.header("Authorization", differentStudentAuth))
					.andExpect(status().isForbidden());
		}

		@Test
		void getPossibleInterviewApplications_Success() throws Exception {
			UUID topicId = createTestTopic("Interview Topic");
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			CreateApplicationPayload payload = new CreateApplicationPayload(
					topicId, null, "MASTER", Instant.now(), "Interview motivation", null
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/applications/interview-applications")
							.header("Authorization", createRandomAdminAuthentication())
							.param("topicId", topicId.toString()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").isArray()).isTrue();
			assertThat(json.get("content").size()).isEqualTo(1);
		}

		@Test
		void getPossibleInterviewApplications_AsStudent_Forbidden() throws Exception {
			String studentAuth = createRandomAuthentication("student");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/applications/interview-applications")
							.header("Authorization", studentAuth)
							.param("topicId", UUID.randomUUID().toString()))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class ApplicationUpdate {
		@Test
		void updateApplication_Success() throws Exception {
			String authorization = createRandomAdminAuthentication();
			UUID applicationId = createTestApplication(authorization, "Original Title");

			UUID newResearchGroupId = createDefaultResearchGroup();
			CreateApplicationPayload updatePayload = new CreateApplicationPayload(
					null, "Updated Thesis", "BACHELOR", Instant.now(), "Updated motivation", newResearchGroupId
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}", applicationId)
							.header("Authorization", authorization)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(updatePayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("thesisTitle").asString()).isEqualTo("Updated Thesis");
			assertThat(json.get("thesisType").asString()).isEqualTo("BACHELOR");
			assertThat(json.get("motivation").asString()).isEqualTo("Updated motivation");
			assertThat(json.get("state").asString()).isEqualTo(ApplicationState.NOT_ASSESSED.getValue());

			var application = applicationRepository.findById(applicationId).orElseThrow();
			assertThat(application.getThesisTitle()).isEqualTo("Updated Thesis");
			assertThat(application.getThesisType()).isEqualTo("BACHELOR");
			assertThat(application.getMotivation()).isEqualTo("Updated motivation");
		}

		@Test
		void updateApplication_AccessDenied_AsDifferentStudent() throws Exception {
			String ownerAuth = createRandomAuthentication("student");
			UUID applicationId = createTestApplication(ownerAuth, "Owner Application");

			String differentStudentAuth = createRandomAuthentication("student");
			CreateApplicationPayload updatePayload = new CreateApplicationPayload(
					null, "Hacked Title", "MASTER", Instant.now(), "Hacked motivation",
					createDefaultResearchGroup()
			);

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}", applicationId)
							.header("Authorization", differentStudentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(updatePayload)))
					.andExpect(status().isForbidden());

			var application = applicationRepository.findById(applicationId).orElseThrow();
			assertThat(application.getThesisTitle()).isEqualTo("Owner Application");
		}

		@Test
		void updateApplication_AlreadyReviewed_ReturnsBadRequest() throws Exception {
			String adminAuth = createRandomAdminAuthentication();
			UUID applicationId = createTestApplication(adminAuth, "Reviewed Application");

			ReviewApplicationPayload reviewPayload = new ReviewApplicationPayload(ApplicationReviewReason.INTERESTED);

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/review", applicationId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(reviewPayload)))
					.andExpect(status().isOk());

			CreateApplicationPayload updatePayload = new CreateApplicationPayload(
					null, "Should Fail", "MASTER", Instant.now(), "Should Fail",
					createDefaultResearchGroup()
			);

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}", applicationId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(updatePayload)))
					.andExpect(status().isBadRequest());
		}
	}

	@Nested
	class ApplicationComment {
		@Test
		void updateComment_Success() throws Exception {
			UUID applicationId = createTestApplication(createRandomAdminAuthentication(), "Application");

			UpdateApplicationCommentPayload payload = new UpdateApplicationCommentPayload("Management comment");

			String response = mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/comment", applicationId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("comment").asString()).isEqualTo("Management comment");

			var application = applicationRepository.findById(applicationId).orElseThrow();
			assertThat(application.getComment()).isEqualTo("Management comment");
		}

		@Test
		void updateComment_AccessDenied_AsStudent() throws Exception {
			UUID applicationId = createTestApplication(createRandomAdminAuthentication(), "Application");
			String studentAuth = createRandomAuthentication("student");

			UpdateApplicationCommentPayload payload = new UpdateApplicationCommentPayload("Student comment");

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/comment", applicationId)
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isForbidden());
		}

		@Test
		void updateComment_OverwriteExistingComment() throws Exception {
			UUID applicationId = createTestApplication(createRandomAdminAuthentication(), "Application");
			String adminAuth = createRandomAdminAuthentication();

			UpdateApplicationCommentPayload firstComment = new UpdateApplicationCommentPayload("First comment");
			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/comment", applicationId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(firstComment)))
					.andExpect(status().isOk());

			UpdateApplicationCommentPayload secondComment = new UpdateApplicationCommentPayload("Updated comment");
			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/comment", applicationId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(secondComment)))
					.andExpect(status().isOk());

			var application = applicationRepository.findById(applicationId).orElseThrow();
			assertThat(application.getComment()).isEqualTo("Updated comment");
		}
	}

	@Nested
	class ApplicationReview {
		@Test
		void reviewApplication_Success() throws Exception {
			String adminAuth = createRandomAdminAuthentication();
			UUID applicationId = createTestApplication(adminAuth, "Review Application");

			ReviewApplicationPayload payload = new ReviewApplicationPayload(ApplicationReviewReason.INTERESTED);

			String response = mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/review", applicationId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("applicationId").asString()).isEqualTo(applicationId.toString());

			assertThat(applicationReviewerRepository.count()).isEqualTo(1);
		}

		@Test
		void reviewApplication_NotInterested() throws Exception {
			String adminAuth = createRandomAdminAuthentication();
			UUID applicationId = createTestApplication(adminAuth, "Review Application");

			ReviewApplicationPayload payload = new ReviewApplicationPayload(ApplicationReviewReason.NOT_INTERESTED);

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/review", applicationId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			assertThat(applicationReviewerRepository.count()).isEqualTo(1);
		}

		@Test
		void reviewApplication_AccessDenied_AsStudent() throws Exception {
			UUID applicationId = createTestApplication(createRandomAdminAuthentication(), "Application");
			String studentAuth = createRandomAuthentication("student");

			ReviewApplicationPayload payload = new ReviewApplicationPayload(ApplicationReviewReason.INTERESTED);

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/review", applicationId)
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isForbidden());
		}

		@Test
		void reviewApplication_NotReviewed_RemovesReview() throws Exception {
			String adminAuth = createRandomAdminAuthentication();
			UUID applicationId = createTestApplication(adminAuth, "Review Application");

			ReviewApplicationPayload reviewPayload = new ReviewApplicationPayload(ApplicationReviewReason.INTERESTED);
			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/review", applicationId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(reviewPayload)))
					.andExpect(status().isOk());

			assertThat(applicationReviewerRepository.count()).isEqualTo(1);

			ReviewApplicationPayload removePayload = new ReviewApplicationPayload(ApplicationReviewReason.NOT_REVIEWED);
			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/review", applicationId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(removePayload)))
					.andExpect(status().isOk());

			assertThat(applicationReviewerRepository.count()).isZero();
		}
	}

	@Nested
	class ApplicationAcceptance {
		@Test
		void acceptApplication_Success() throws Exception {
			UUID applicationId = createTestApplication(createRandomAdminAuthentication(), "Accept Application");
			TestUser advisor = createTestUser("advisor-accept", List.of("advisor"));
			TestUser supervisor = createTestUser("supervisor-accept", List.of("supervisor"));
			createTestEmailTemplate("APPLICATION_ACCEPTED");
			createTestEmailTemplate("THESIS_CREATED");

			AcceptApplicationPayload payload = new AcceptApplicationPayload(
					"Final Thesis Title", "MASTER", "ENGLISH",
					List.of(advisor.userId()), List.of(supervisor.userId()),
					true, true
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/accept", applicationId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.isArray()).isTrue();
			assertThat(json.size()).isGreaterThanOrEqualTo(1);

			boolean hasAccepted = false;
			for (JsonNode app : json) {
				if (app.get("state").asString().equals(ApplicationState.ACCEPTED.getValue())) {
					hasAccepted = true;
				}
			}
			assertThat(hasAccepted).isTrue();

			assertThat(thesisRepository.count()).isEqualTo(1);
		}

		@Test
		void acceptApplication_AlreadyAccepted_ReturnsBadRequest() throws Exception {
			UUID applicationId = createTestApplication(createRandomAdminAuthentication(), "Accept Application");
			TestUser advisor = createTestUser("advisor-accept2", List.of("advisor"));
			TestUser supervisor = createTestUser("supervisor-accept2", List.of("supervisor"));
			createTestEmailTemplate("APPLICATION_ACCEPTED");

			AcceptApplicationPayload payload = new AcceptApplicationPayload(
					"Final Thesis", "MASTER", "ENGLISH",
					List.of(advisor.userId()), List.of(supervisor.userId()),
					false, false
			);

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/accept", applicationId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/accept", applicationId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isBadRequest());
		}

		@Test
		void acceptApplication_AccessDenied_AsStudent() throws Exception {
			UUID applicationId = createTestApplication(createRandomAdminAuthentication(), "Application");
			String studentAuth = createRandomAuthentication("student");

			AcceptApplicationPayload payload = new AcceptApplicationPayload(
					"Thesis", "MASTER", "ENGLISH",
					List.of(UUID.randomUUID()), List.of(UUID.randomUUID()),
					false, false
			);

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/accept", applicationId)
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isForbidden());
		}

		@Test
		void acceptApplication_WithCloseTopic_RejectsOtherApplications() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");
			createTestEmailTemplate("APPLICATION_ACCEPTED");
			createTestEmailTemplate("THESIS_CREATED");
			createTestEmailTemplate("APPLICATION_REJECTED_TOPIC_FILLED");

			UUID topicId = createTestTopic("Close Topic Test");

			// Two different students apply for the same topic
			String student1Auth = createRandomAuthentication("student");
			CreateApplicationPayload payload1 = new CreateApplicationPayload(
					topicId, null, "MASTER", Instant.now(), "Motivation 1", null
			);
			String response1 = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", student1Auth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload1)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID appId1 = UUID.fromString(objectMapper.readTree(response1).get("applicationId").asString());

			String student2Auth = createRandomAuthentication("student");
			CreateApplicationPayload payload2 = new CreateApplicationPayload(
					topicId, null, "MASTER", Instant.now(), "Motivation 2", null
			);
			String response2 = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", student2Auth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload2)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID appId2 = UUID.fromString(objectMapper.readTree(response2).get("applicationId").asString());

			TestUser advisor = createTestUser("advisor-close", List.of("advisor"));
			TestUser supervisor = createTestUser("supervisor-close", List.of("supervisor"));

			AcceptApplicationPayload acceptPayload = new AcceptApplicationPayload(
					"Accepted Thesis", "MASTER", "ENGLISH",
					List.of(advisor.userId()), List.of(supervisor.userId()),
					true, true  // notifyUser=true, closeTopic=true
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/accept", appId1)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(acceptPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.isArray()).isTrue();
			// Should contain both the accepted and the rejected application
			assertThat(json.size()).isGreaterThanOrEqualTo(2);

			// Verify the second application was rejected
			var app2 = applicationRepository.findById(appId2).orElseThrow();
			assertThat(app2.getState()).isEqualTo(ApplicationState.REJECTED);
			assertThat(app2.getRejectReason()).isEqualTo(ApplicationRejectReason.TOPIC_FILLED);
		}

		@Test
		void acceptApplication_WithNotify_SameAdvisorSupervisor_UsesNoAdvisorTemplate() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");
			createTestEmailTemplate("APPLICATION_ACCEPTED_NO_ADVISOR");
			createTestEmailTemplate("THESIS_CREATED");

			TestUser student = createRandomTestUser(List.of("student"));
			String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Accept No Advisor", advisor.universityId());

			CreateApplicationPayload appPayload = new CreateApplicationPayload(
					null, "No Advisor Thesis", "MASTER", Instant.now(), "Motivation", researchGroupId
			);

			String appResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(appPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			UUID applicationId = UUID.fromString(objectMapper.readTree(appResponse).get("applicationId").asString());

			clearEmails();

			// Same user as both advisor and supervisor triggers APPLICATION_ACCEPTED_NO_ADVISOR template
			AcceptApplicationPayload acceptPayload = new AcceptApplicationPayload(
					"No Advisor Thesis", "MASTER", "ENGLISH",
					List.of(advisor.userId()),
					List.of(advisor.userId()),
					true, false
			);

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/accept", applicationId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(acceptPayload)))
					.andExpect(status().isOk());

			MimeMessage[] emails = getReceivedEmails();
			assertThat(emails.length).isGreaterThanOrEqualTo(1);
		}

		@Test
		void acceptApplication_VerifyThesisCreated() throws Exception {
			UUID applicationId = createTestApplication(createRandomAdminAuthentication(), "Thesis Source Application");
			TestUser advisor = createTestUser("advisor-verify", List.of("advisor"));
			TestUser supervisor = createTestUser("supervisor-verify", List.of("supervisor"));
			createTestEmailTemplate("APPLICATION_ACCEPTED");

			AcceptApplicationPayload payload = new AcceptApplicationPayload(
					"Created Thesis Title", "BACHELOR", "GERMAN",
					List.of(advisor.userId()), List.of(supervisor.userId()),
					false, false
			);

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/accept", applicationId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			assertThat(thesisRepository.count()).isEqualTo(1);

			var thesis = thesisRepository.findAll().getFirst();
			assertThat(thesis.getTitle()).isEqualTo("Created Thesis Title");
			assertThat(thesis.getType()).isEqualTo("BACHELOR");
			assertThat(thesis.getLanguage()).isEqualTo("GERMAN");
			assertThat(thesis.getApplication()).isNotNull();
			assertThat(thesis.getApplication().getId()).isEqualTo(applicationId);
		}
	}

	@Nested
	class ApplicationRejection {
		@Test
		void rejectApplication_Success() throws Exception {
			UUID applicationId = createTestApplication(createRandomAdminAuthentication(), "Reject Application");

			RejectApplicationPayload payload = new RejectApplicationPayload(
					ApplicationRejectReason.GENERAL, false
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/reject", applicationId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.isArray()).isTrue();

			boolean hasRejected = false;
			for (JsonNode app : json) {
				if (app.get("state").asString().equals(ApplicationState.REJECTED.getValue())) {
					hasRejected = true;
				}
			}
			assertThat(hasRejected).isTrue();

			var application = applicationRepository.findById(applicationId).orElseThrow();
			assertThat(application.getState()).isEqualTo(ApplicationState.REJECTED);
			assertThat(application.getRejectReason()).isEqualTo(ApplicationRejectReason.GENERAL);
			assertThat(application.getReviewedAt()).isNotNull();
		}

		@Test
		void rejectApplication_AccessDenied_AsStudent() throws Exception {
			UUID applicationId = createTestApplication(createRandomAdminAuthentication(), "Application");
			String studentAuth = createRandomAuthentication("student");

			RejectApplicationPayload payload = new RejectApplicationPayload(
					ApplicationRejectReason.GENERAL, false
			);

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/reject", applicationId)
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isForbidden());
		}

		@Test
		void rejectApplication_WithNotification() throws Exception {
			UUID applicationId = createTestApplication(createRandomAdminAuthentication(), "Reject With Notify");
			createTestEmailTemplate("APPLICATION_REJECTED");

			RejectApplicationPayload payload = new RejectApplicationPayload(
					ApplicationRejectReason.GENERAL, true
			);

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/reject", applicationId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			var application = applicationRepository.findById(applicationId).orElseThrow();
			assertThat(application.getState()).isEqualTo(ApplicationState.REJECTED);
		}

		@Test
		void rejectApplication_FailedStudentRequirements_RejectsAllPending() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			String studentAuth = createRandomAuthentication("student");
			// Use different topics so the same student can create multiple applications
			UUID topicId1 = createTestTopic("Topic for App 1");
			UUID topicId2 = createTestTopic("Topic for App 2");

			CreateApplicationPayload payload1 = new CreateApplicationPayload(
					topicId1, null, "MASTER", Instant.now(), "Motivation 1", null
			);
			String response1 = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload1)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID appId1 = UUID.fromString(objectMapper.readTree(response1).get("applicationId").asString());

			CreateApplicationPayload payload2 = new CreateApplicationPayload(
					topicId2, null, "MASTER", Instant.now(), "Motivation 2", null
			);
			String response2 = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload2)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID appId2 = UUID.fromString(objectMapper.readTree(response2).get("applicationId").asString());

			createTestEmailTemplate("APPLICATION_REJECTED_STUDENT_REQUIREMENTS");

			RejectApplicationPayload payload = new RejectApplicationPayload(
					ApplicationRejectReason.FAILED_STUDENT_REQUIREMENTS, false
			);

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/reject", appId1)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			var app1 = applicationRepository.findById(appId1).orElseThrow();
			var app2 = applicationRepository.findById(appId2).orElseThrow();
			assertThat(app1.getState()).isEqualTo(ApplicationState.REJECTED);
			assertThat(app1.getRejectReason()).isEqualTo(ApplicationRejectReason.FAILED_STUDENT_REQUIREMENTS);
			assertThat(app2.getState()).isEqualTo(ApplicationState.REJECTED);
			assertThat(app2.getRejectReason()).isEqualTo(ApplicationRejectReason.FAILED_STUDENT_REQUIREMENTS);
		}

		@Test
		void rejectApplication_DifferentReasons_VerifyDatabaseState() throws Exception {
			UUID applicationId = createTestApplication(createRandomAdminAuthentication(), "Reject Specific Reason");

			RejectApplicationPayload payload = new RejectApplicationPayload(
					ApplicationRejectReason.NO_CAPACITY, false
			);

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/reject", applicationId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			var application = applicationRepository.findById(applicationId).orElseThrow();
			assertThat(application.getState()).isEqualTo(ApplicationState.REJECTED);
			assertThat(application.getRejectReason()).isEqualTo(ApplicationRejectReason.NO_CAPACITY);
		}
	}

	@Nested
	class ApplicationSorting {
		@Test
		void getApplications_SortByCreatedAtDesc_Success() throws Exception {
			String student1Auth = createRandomAuthentication("student");
			createTestApplication(student1Auth, "First Application");
			String student2Auth = createRandomAuthentication("student");
			createTestApplication(student2Auth, "Second Application");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/applications")
							.header("Authorization", createRandomAdminAuthentication())
							.param("fetchAll", "true")
							.param("sortBy", "createdAt")
							.param("sortOrder", "desc"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isEqualTo(2);
			// Last created should be first with desc order
			assertThat(json.get("content").get(0).get("thesisTitle").asString()).isEqualTo("Second Application");
		}

		@Test
		void getApplications_SortByCreatedAtAsc_Success() throws Exception {
			String student1Auth = createRandomAuthentication("student");
			createTestApplication(student1Auth, "First Application");
			String student2Auth = createRandomAuthentication("student");
			createTestApplication(student2Auth, "Second Application");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/applications")
							.header("Authorization", createRandomAdminAuthentication())
							.param("fetchAll", "true")
							.param("sortBy", "createdAt")
							.param("sortOrder", "asc"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isEqualTo(2);
			// First created should be first with asc order
			assertThat(json.get("content").get(0).get("thesisTitle").asString()).isEqualTo("First Application");
		}
	}

	@Nested
	class CloseTopicWithInterviewProcess {
		@Test
		void closeTopic_WithInterviewProcess_MarksProcessCompleted() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");
			createTestEmailTemplate("APPLICATION_REJECTED_TOPIC_FILLED");
			createTestEmailTemplate("INTERVIEW_INVITATION");
			createTestEmailTemplate("INTERVIEW_INVITATION_REMINDER");

			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Close IP Group", advisor.universityId());

			ReplaceTopicPayload topicPayload = new ReplaceTopicPayload(
					"Close IP Topic", Set.of("MASTER"),
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

			// Create application for the topic
			String studentAuth = createRandomAuthentication("student");
			CreateApplicationPayload appPayload = new CreateApplicationPayload(
					topicId, null, "MASTER", Instant.now(), "Interview motivation", null
			);
			String appResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(appPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID applicationId = UUID.fromString(objectMapper.readTree(appResponse).get("applicationId").asString());

			// Create interview process for the topic
			String advisorAuth = generateTestAuthenticationHeader(advisor.universityId(), List.of("supervisor", "advisor"));
			CreateInterviewProcessPayload processPayload = new CreateInterviewProcessPayload(topicId, List.of(applicationId));
			String processResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/interview-process")
							.header("Authorization", advisorAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(processPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID processId = UUID.fromString(objectMapper.readTree(processResponse).get("interviewProcessId").asString());

			// Close the topic
			CloseTopicPayload closePayload = new CloseTopicPayload(
					ApplicationRejectReason.TOPIC_FILLED, true
			);
			mockMvc.perform(MockMvcRequestBuilders.delete("/v2/topics/{topicId}", topicId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(closePayload)))
					.andExpect(status().isOk());

			// Verify interview process is marked completed
			var process = interviewProcessRepository.findById(processId).orElseThrow();
			assertThat(process.isCompleted()).isTrue();

			// Verify application was rejected
			var app = applicationRepository.findById(applicationId).orElseThrow();
			assertThat(app.getState()).isEqualTo(ApplicationState.REJECTED);
		}
	}

	@Nested
	class ApplicationFilterCombinations {
		@Test
		void getApplications_FilterByType_Success() throws Exception {
			String studentAuth = createRandomAuthentication("student");
			createTestApplication(studentAuth, "Bachelor App");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/applications")
							.header("Authorization", createRandomAdminAuthentication())
							.param("fetchAll", "true")
							.param("types", "BACHELOR"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isGreaterThanOrEqualTo(1);
		}

		@Test
		void getApplications_WithSearch_FiltersResults() throws Exception {
			String student1Auth = createRandomAuthentication("student");
			createTestApplication(student1Auth, "Unique Search Term XYZ");
			String student2Auth = createRandomAuthentication("student");
			createTestApplication(student2Auth, "Other Application");

			// Search with a term that doesn't match any user name - should return fewer results
			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/applications")
							.header("Authorization", createRandomAdminAuthentication())
							.param("fetchAll", "true")
							.param("search", "nonexistent-search-term-99999"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("totalElements").asInt()).isEqualTo(0);
		}

		@Test
		void getApplications_WithTopicFilter_Success() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			UUID topicId = createTestTopic("Filter Topic");

			String studentAuth = createRandomAuthentication("student");
			CreateApplicationPayload payload = new CreateApplicationPayload(
					topicId, null, "MASTER", Instant.now(), "Topic filter test", null
			);
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/applications")
							.header("Authorization", createRandomAdminAuthentication())
							.param("fetchAll", "true")
							.param("topic", topicId.toString()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isEqualTo(1);
		}

		@Test
		void getApplications_MultipleStates_Success() throws Exception {
			String studentAuth = createRandomAuthentication("student");
			UUID applicationId = createTestApplication(studentAuth, "Multi State App");

			// Reject one application
			createTestEmailTemplate("APPLICATION_REJECTED");
			RejectApplicationPayload rejectPayload = new RejectApplicationPayload(
					ApplicationRejectReason.GENERAL, false
			);
			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{applicationId}/reject", applicationId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(rejectPayload)))
					.andExpect(status().isOk());

			// Create another application
			String student2Auth = createRandomAuthentication("student");
			createTestApplication(student2Auth, "Not Assessed App");

			// Filter by both states
			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/applications")
							.header("Authorization", createRandomAdminAuthentication())
							.param("fetchAll", "true")
							.param("state", "NOT_ASSESSED", "REJECTED"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isEqualTo(2);
		}
	}
}
