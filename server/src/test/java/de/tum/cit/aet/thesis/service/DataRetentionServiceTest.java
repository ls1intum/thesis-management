package de.tum.cit.aet.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.constants.ApplicationState;
import de.tum.cit.aet.thesis.controller.payload.CreateApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.ReplaceTopicPayload;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.repository.ApplicationRepository;
import de.tum.cit.aet.thesis.repository.ApplicationReviewerRepository;
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
}
