package de.tum.cit.aet.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.constants.ApplicationState;
import de.tum.cit.aet.thesis.controller.payload.CreateApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateThesisPayload;
import de.tum.cit.aet.thesis.controller.payload.ReplaceTopicPayload;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.repository.ApplicationRepository;
import de.tum.cit.aet.thesis.repository.ApplicationReviewerRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Testcontainers
class DataRetentionServiceTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private DataRetentionService dataRetentionService;

	@Autowired
	private ApplicationRepository applicationRepository;

	@Autowired
	private ApplicationReviewerRepository applicationReviewerRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AuthenticationService authenticationService;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private TransactionTemplate transactionTemplate;

	private UUID createRejectedApplication(int daysAgoReviewed) throws Exception {
		createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
		createTestEmailTemplate("APPLICATION_CREATED_STUDENT");
		createTestEmailTemplate("APPLICATION_REJECTED");

		TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
		UUID researchGroupId = createTestResearchGroup("Retention RG", advisor.universityId());

		ReplaceTopicPayload topicPayload = new ReplaceTopicPayload(
				"Retention Topic", Set.of("MASTER"),
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

		String studentAuth = createRandomAuthentication("student");
		CreateApplicationPayload appPayload = new CreateApplicationPayload(
				topicId, null, "MASTER", Instant.now(), "Retention test", null
		);
		String appResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
						.header("Authorization", studentAuth)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(appPayload)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		UUID applicationId = UUID.fromString(objectMapper.readTree(appResponse).get("applicationId").asString());

		// Add a reviewer via the review API (creates application_reviewer row for cascade testing)
		String advisorAuth = generateTestAuthenticationHeader(advisor.universityId(), List.of("supervisor", "advisor"));
		mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/" + applicationId + "/review")
						.header("Authorization", advisorAuth)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reason\":\"NOT_INTERESTED\"}"))
				.andReturn();

		// Set state to REJECTED and backdate reviewed_at
		transactionTemplate.executeWithoutResult(status -> {
			entityManager.createNativeQuery(
							"UPDATE applications SET state = 'REJECTED', reviewed_at = :reviewedAt WHERE application_id = :id")
					.setParameter("reviewedAt", Instant.now().minus(daysAgoReviewed, ChronoUnit.DAYS))
					.setParameter("id", applicationId)
					.executeUpdate();
			entityManager.clear();
		});

		return applicationId;
	}

	@Test
	void deletesRejectedApplicationOlderThanRetentionPeriod() throws Exception {
		UUID applicationId = createRejectedApplication(400);

		dataRetentionService.runNightlyCleanup();

		assertThat(applicationRepository.findById(applicationId)).isEmpty();
	}

	@Test
	void doesNotDeleteRecentlyRejectedApplication() throws Exception {
		UUID applicationId = createRejectedApplication(300);

		dataRetentionService.runNightlyCleanup();

		assertThat(applicationRepository.findById(applicationId)).isPresent();
		assertThat(applicationRepository.findById(applicationId).get().getState())
				.isEqualTo(ApplicationState.REJECTED);
	}

	@Test
	void doesNotDeleteNonRejectedApplications() throws Exception {
		createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
		createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

		TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
		UUID researchGroupId = createTestResearchGroup("NonRejected RG", advisor.universityId());

		ReplaceTopicPayload topicPayload = new ReplaceTopicPayload(
				"NonRejected Topic", Set.of("MASTER"),
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

		String studentAuth = createRandomAuthentication("student");
		CreateApplicationPayload appPayload = new CreateApplicationPayload(
				topicId, null, "MASTER", Instant.now(), "Non-rejected test", null
		);
		String appResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
						.header("Authorization", studentAuth)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(appPayload)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		UUID applicationId = UUID.fromString(objectMapper.readTree(appResponse).get("applicationId").asString());

		// Backdate created_at to over 1 year ago but keep NOT_ASSESSED state
		transactionTemplate.executeWithoutResult(status -> {
			entityManager.createNativeQuery("UPDATE applications SET created_at = :date WHERE application_id = :id")
					.setParameter("date", Instant.now().minus(400, ChronoUnit.DAYS))
					.setParameter("id", applicationId)
					.executeUpdate();
			entityManager.clear();
		});

		dataRetentionService.runNightlyCleanup();

		assertThat(applicationRepository.findById(applicationId)).isPresent();
		assertThat(applicationRepository.findById(applicationId).get().getState())
				.isEqualTo(ApplicationState.NOT_ASSESSED);
	}

	@Test
	void cascadeDeletesApplicationReviewers() throws Exception {
		UUID applicationId = createRejectedApplication(400);

		dataRetentionService.runNightlyCleanup();

		assertThat(applicationRepository.findById(applicationId)).isEmpty();
		long reviewersAfter = applicationReviewerRepository.findAll().stream()
				.filter(r -> r.getApplication().getId().equals(applicationId))
				.count();
		assertThat(reviewersAfter).isZero();
	}

	@Test
	void deleteExpiredRejectedApplicationsReturnsCount() throws Exception {
		UUID applicationId = createRejectedApplication(400);

		int deleted = dataRetentionService.deleteExpiredRejectedApplications();

		assertThat(deleted).isPositive();
		assertThat(applicationRepository.findById(applicationId)).isEmpty();
	}

	// --- Inactive user disabling tests ---

	private void backdateUserActivity(UUID userId, int daysAgo) {
		Instant pastDate = Instant.now().minus(daysAgo, ChronoUnit.DAYS);
		transactionTemplate.executeWithoutResult(status -> {
			entityManager.createNativeQuery(
							"UPDATE users SET last_login_at = :date, updated_at = :date, joined_at = :date WHERE user_id = :id")
					.setParameter("date", pastDate)
					.setParameter("id", userId)
					.executeUpdate();
			entityManager.clear();
		});
	}

	@Test
	void disablesStudentInactiveForMoreThanOneYear() throws Exception {
		TestUser student = createRandomTestUser(List.of("student"));
		backdateUserActivity(student.userId(), 400);

		int disabled = dataRetentionService.disableInactiveUsers();

		assertThat(disabled).isPositive();
		User user = userRepository.findById(student.userId()).orElseThrow();
		assertThat(user.isDisabled()).isTrue();
	}

	@Test
	void doesNotDisableRecentlyActiveStudent() throws Exception {
		TestUser student = createRandomTestUser(List.of("student"));
		// Student was just created with a recent last_login_at, so should not be disabled

		int disabled = dataRetentionService.disableInactiveUsers();

		assertThat(disabled).isZero();
		User user = userRepository.findById(student.userId()).orElseThrow();
		assertThat(user.isDisabled()).isFalse();
	}

	@Test
	void doesNotDisableStudentWithRecentThesisActivity() throws Exception {
		createTestEmailTemplate("THESIS_CREATED");

		TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
		UUID researchGroupId = createTestResearchGroup("Active Thesis RG", advisor.universityId());

		TestUser student = createRandomTestUser(List.of("student"));

		CreateThesisPayload thesisPayload = new CreateThesisPayload(
				"Active Thesis Test",
				"MASTER",
				"ENGLISH",
				List.of(student.userId()),
				List.of(advisor.userId()),
				List.of(advisor.userId()),
				researchGroupId
		);
		mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(thesisPayload)))
				.andReturn();

		backdateUserActivity(student.userId(), 400);

		int disabled = dataRetentionService.disableInactiveUsers();

		assertThat(disabled).isZero();
		User user = userRepository.findById(student.userId()).orElseThrow();
		assertThat(user.isDisabled()).isFalse();
	}

	@Test
	void disablesStudentWithOldThesisActivity() throws Exception {
		createTestEmailTemplate("THESIS_CREATED");

		TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
		UUID researchGroupId = createTestResearchGroup("Old Thesis RG", advisor.universityId());

		TestUser student = createRandomTestUser(List.of("student"));

		CreateThesisPayload thesisPayload = new CreateThesisPayload(
				"Old Thesis Test",
				"MASTER",
				"ENGLISH",
				List.of(student.userId()),
				List.of(advisor.userId()),
				List.of(advisor.userId()),
				researchGroupId
		);
		String thesisResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(thesisPayload)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		UUID thesisId = UUID.fromString(objectMapper.readTree(thesisResponse).get("thesisId").asString());

		// Backdate thesis activity and user activity to over 1 year ago
		Instant twoYearsAgo = Instant.now().minus(2 * 365L, ChronoUnit.DAYS);
		transactionTemplate.executeWithoutResult(status -> {
			entityManager.createNativeQuery(
							"UPDATE theses SET created_at = :date WHERE thesis_id = :id")
					.setParameter("date", twoYearsAgo)
					.setParameter("id", thesisId)
					.executeUpdate();
			entityManager.createNativeQuery(
							"UPDATE thesis_state_changes SET changed_at = :date WHERE thesis_id = :id")
					.setParameter("date", twoYearsAgo)
					.setParameter("id", thesisId)
					.executeUpdate();
			entityManager.clear();
		});
		backdateUserActivity(student.userId(), 400);

		int disabled = dataRetentionService.disableInactiveUsers();

		assertThat(disabled).isPositive();
		User user = userRepository.findById(student.userId()).orElseThrow();
		assertThat(user.isDisabled()).isTrue();
	}

	@Test
	void doesNotDisableStudentWithRecentApplication() throws Exception {
		TestUser student = createRandomTestUser(List.of("student"));
		String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

		createTestApplication(studentAuth, "Recent Application");

		backdateUserActivity(student.userId(), 400);

		int disabled = dataRetentionService.disableInactiveUsers();

		assertThat(disabled).isZero();
		User user = userRepository.findById(student.userId()).orElseThrow();
		assertThat(user.isDisabled()).isFalse();
	}

	@Test
	void doesNotDisableSupervisorOrAdmin() throws Exception {
		TestUser supervisor = createRandomTestUser(List.of("supervisor", "advisor"));
		TestUser admin = createRandomTestUser(List.of("admin"));

		backdateUserActivity(supervisor.userId(), 400);
		backdateUserActivity(admin.userId(), 400);

		int disabled = dataRetentionService.disableInactiveUsers();

		assertThat(disabled).isZero();
		assertThat(userRepository.findById(supervisor.userId()).orElseThrow().isDisabled()).isFalse();
		assertThat(userRepository.findById(admin.userId()).orElseThrow().isDisabled()).isFalse();
	}

	@Test
	void reEnablesDisabledUserOnLogin() throws Exception {
		TestUser student = createRandomTestUser(List.of("student"));

		// Manually disable the user
		transactionTemplate.executeWithoutResult(status -> {
			entityManager.createNativeQuery("UPDATE users SET disabled = TRUE WHERE user_id = :id")
					.setParameter("id", student.userId())
					.executeUpdate();
			entityManager.clear();
		});

		// Simulate login via updateAuthenticatedUser
		String authHeader = generateTestAuthenticationHeader(student.universityId(), List.of("student"));
		mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
						.header("Authorization", authHeader))
				.andReturn();

		User user = userRepository.findById(student.userId()).orElseThrow();
		assertThat(user.isDisabled()).isFalse();
	}
}
