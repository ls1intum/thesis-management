package de.tum.cit.aet.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.constants.ApplicationState;
import de.tum.cit.aet.thesis.controller.payload.CreateApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.ReplaceTopicPayload;
import de.tum.cit.aet.thesis.cron.model.ApplicationRejectObject;
import de.tum.cit.aet.thesis.entity.Application;
import de.tum.cit.aet.thesis.entity.Topic;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.mock.MutableClock;
import de.tum.cit.aet.thesis.repository.ApplicationRepository;
import de.tum.cit.aet.thesis.repository.TopicRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Testcontainers
@Import(ApplicationServiceIntegrationTest.MutableClockTestConfig.class)
class ApplicationServiceIntegrationTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@TestConfiguration
	static class MutableClockTestConfig {
		@Bean
		@Primary
		MutableClock testClock() {
			return new MutableClock();
		}
	}

	@Autowired
	private Clock clock;

	@AfterEach
	void resetClock() {
		if (clock instanceof MutableClock mutable) {
			mutable.unfreeze();
		}
	}

	@Autowired
	private ApplicationService applicationService;

	@Autowired
	private ApplicationRepository applicationRepository;

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private TransactionTemplate transactionTemplate;

	private record TopicAppSetup(UUID topicId, UUID applicationId, UUID researchGroupId, TestUser advisor) {}

	private TopicAppSetup createTopicWithOldApplication() throws Exception {
		createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
		createTestEmailTemplate("APPLICATION_CREATED_STUDENT");
		createTestEmailTemplate("APPLICATION_REJECTED");

		TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
		UUID researchGroupId = createTestResearchGroup("Auto Reject RG", advisor.universityId());

		ReplaceTopicPayload topicPayload = new ReplaceTopicPayload(
				"Auto Reject Topic", Set.of("MASTER"),
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

		// Create application
		String studentAuth = createRandomAuthentication("student");
		CreateApplicationPayload appPayload = new CreateApplicationPayload(
				topicId, null, "MASTER", Instant.now(), "Auto reject test", null,
		true);
		String appResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
						.header("Authorization", studentAuth)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(appPayload)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		UUID applicationId = UUID.fromString(objectMapper.readTree(appResponse).get("applicationId").asString());

		// Backdate the application to make it old enough for auto-reject
		// Must use native query because @CreationTimestamp prevents JPA from updating createdAt
		transactionTemplate.executeWithoutResult(status -> {
			entityManager.createNativeQuery("UPDATE applications SET created_at = :date WHERE application_id = :id")
					.setParameter("date", Instant.now().minus(30, ChronoUnit.DAYS))
					.setParameter("id", applicationId)
					.executeUpdate();
			entityManager.clear();
		});

		return new TopicAppSetup(topicId, applicationId, researchGroupId, advisor);
	}

	@Nested
	class RejectAllApplicationsAutomatically {
		@Test
		void rejectOldApplications_Success() throws Exception {
			TopicAppSetup setup = createTopicWithOldApplication();
			Topic topic = topicRepository.findById(setup.topicId).orElseThrow();

			// afterDuration=2 weeks, referenceDate=30 days ago
			applicationService.rejectAllApplicationsAutomatically(
					topic, 2,
					Instant.now().minus(30, ChronoUnit.DAYS),
					setup.researchGroupId
			);

			Application app = applicationRepository.findById(setup.applicationId).orElseThrow();
			assertThat(app.getState()).isEqualTo(ApplicationState.REJECTED);
		}

		@Test
		void rejectOldApplications_WithNullReferenceDate_UsesCreatedAt() throws Exception {
			TopicAppSetup setup = createTopicWithOldApplication();
			Topic topic = topicRepository.findById(setup.topicId).orElseThrow();

			applicationService.rejectAllApplicationsAutomatically(
					topic, 2, null, setup.researchGroupId
			);

			Application app = applicationRepository.findById(setup.applicationId).orElseThrow();
			assertThat(app.getState()).isEqualTo(ApplicationState.REJECTED);
		}

		@Test
		void rejectOldApplications_RecentApplication_NotRejected() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Recent RG", advisor.universityId());

			ReplaceTopicPayload topicPayload = new ReplaceTopicPayload(
					"Recent Topic", Set.of("MASTER"),
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

			// Create recent application (not old enough)
			String studentAuth = createRandomAuthentication("student");
			CreateApplicationPayload appPayload = new CreateApplicationPayload(
					topicId, null, "MASTER", Instant.now(), "Recent app", null,
			true);
			String appResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(appPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID applicationId = UUID.fromString(objectMapper.readTree(appResponse).get("applicationId").asString());

			Topic topic = topicRepository.findById(topicId).orElseThrow();
			applicationService.rejectAllApplicationsAutomatically(
					topic, 2, null, researchGroupId
			);

			// Application should still be NOT_ASSESSED
			Application app = applicationRepository.findById(applicationId).orElseThrow();
			assertThat(app.getState()).isEqualTo(ApplicationState.NOT_ASSESSED);
		}
	}

	@Nested
	class GetListOfApplicationsThatWillBeRejected {
		@Test
		void getListToReject_WithOldApplication_ReturnsApplication() throws Exception {
			TopicAppSetup setup = createTopicWithOldApplication();
			Topic topic = topicRepository.findById(setup.topicId).orElseThrow();

			List<ApplicationRejectObject> result = applicationService.getListOfApplicationsThatWillBeRejected(
					topic, 2, Instant.now().minus(30, ChronoUnit.DAYS)
			);

			assertThat(result).hasSize(1);
			assertThat(result.getFirst().applicationId()).isEqualTo(setup.applicationId);
		}

		@Test
		void getListToReject_WithNullReferenceDate_ReturnsApplication() throws Exception {
			TopicAppSetup setup = createTopicWithOldApplication();
			Topic topic = topicRepository.findById(setup.topicId).orElseThrow();

			List<ApplicationRejectObject> result = applicationService.getListOfApplicationsThatWillBeRejected(
					topic, 2, null
			);

			assertThat(result).hasSize(1);
		}

		@Test
		void getListToReject_WithRecentApplication_ReturnsEmpty() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Future RG", advisor.universityId());

			ReplaceTopicPayload topicPayload = new ReplaceTopicPayload(
					"Future Topic", Set.of("MASTER"),
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
					topicId, null, "MASTER", Instant.now(), "Recent app", null,
			true);
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(appPayload)))
					.andExpect(status().isOk());

			Topic topic = topicRepository.findById(topicId).orElseThrow();
			// Use a future reference date so rejection date is far away
			List<ApplicationRejectObject> result = applicationService.getListOfApplicationsThatWillBeRejected(
					topic, 2, Instant.now().plus(60, ChronoUnit.DAYS)
			);

			assertThat(result).isEmpty();
		}
	}

	@Nested
	class RejectListOfApplicationsIfOlderThan {
		@Test
		void rejectList_OldApplications_Rejected() throws Exception {
			TopicAppSetup setup = createTopicWithOldApplication();
			Application app = applicationRepository.findById(setup.applicationId).orElseThrow();

			applicationService.rejectListOfApplicationsIfOlderThan(
					List.of(app), 14, setup.researchGroupId
			);

			Application updated = applicationRepository.findById(setup.applicationId).orElseThrow();
			assertThat(updated.getState()).isEqualTo(ApplicationState.REJECTED);
		}

		@Test
		void rejectList_RecentApplications_NotRejected() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Recent List RG", advisor.universityId());

			ReplaceTopicPayload topicPayload = new ReplaceTopicPayload(
					"Recent List Topic", Set.of("MASTER"),
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
					topicId, null, "MASTER", Instant.now(), "Recent list app", null,
			true);
			String appResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(appPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID applicationId = UUID.fromString(objectMapper.readTree(appResponse).get("applicationId").asString());

			Application app = applicationRepository.findById(applicationId).orElseThrow();

			applicationService.rejectListOfApplicationsIfOlderThan(
					List.of(app), 14, researchGroupId
			);

			Application updated = applicationRepository.findById(applicationId).orElseThrow();
			assertThat(updated.getState()).isEqualTo(ApplicationState.NOT_ASSESSED);
		}
	}

	@Nested
	class CreateApplicationWithDeadline {
		private UUID createTopicWithDeadline(Instant applicationDeadline) throws Exception {
			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Deadline RG " + UUID.randomUUID(), advisor.universityId());

			ReplaceTopicPayload topicPayload = new ReplaceTopicPayload(
					"Deadline Topic", Set.of("MASTER"),
					"PS", "Req", "Goals", "Refs",
					List.of(advisor.userId()), List.of(advisor.userId()),
					researchGroupId, null, applicationDeadline, false
			);
			String topicResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/topics")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(topicPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			return UUID.fromString(objectMapper.readTree(topicResponse).get("topicId").asString());
		}

		private CreateApplicationPayload applyTo(UUID topicId) {
			return new CreateApplicationPayload(
					topicId, null, "MASTER", Instant.now(), "Motivation", null, true);
		}

		@Test
		void createApplication_PastDeadline_RejectsWith400() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			// Create the topic with a future deadline (the topic-creation flow itself
			// does not enforce future deadlines, but we keep this consistent with the
			// realistic workflow of "deadline becomes past as time advances"), then
			// backdate it directly in the database to simulate a passed deadline.
			UUID topicId = createTopicWithDeadline(Instant.now().plus(7, ChronoUnit.DAYS));
			transactionTemplate.executeWithoutResult(status -> {
				entityManager.createNativeQuery("UPDATE topics SET application_deadline = :deadline WHERE topic_id = :id")
						.setParameter("deadline", Instant.now().minus(1, ChronoUnit.DAYS))
						.setParameter("id", topicId)
						.executeUpdate();
				entityManager.clear();
			});

			String studentAuth = createRandomAuthentication("student");
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(applyTo(topicId))))
					.andExpect(status().isBadRequest());

			Topic topic = topicRepository.findById(topicId).orElseThrow();
			assertThat(applicationRepository.findAllByTopic(topic)).isEmpty();
		}

		@Test
		void createApplication_FutureDeadline_Succeeds() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			UUID topicId = createTopicWithDeadline(Instant.now().plus(7, ChronoUnit.DAYS));
			String studentAuth = createRandomAuthentication("student");
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(applyTo(topicId))))
					.andExpect(status().isOk());

			Topic topic = topicRepository.findById(topicId).orElseThrow();
			assertThat(applicationRepository.findAllByTopic(topic)).hasSize(1);
		}

		@Test
		void createApplication_NoDeadline_Succeeds() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			UUID topicId = createTopicWithDeadline(null);
			String studentAuth = createRandomAuthentication("student");
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(applyTo(topicId))))
					.andExpect(status().isOk());

			Topic topic = topicRepository.findById(topicId).orElseThrow();
			assertThat(applicationRepository.findAllByTopic(topic)).hasSize(1);
		}

		@Test
		void createApplication_DeadlineExactlyNow_Rejected() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			// Pin the clock so the deadline-equality boundary is tested deterministically.
			// Truncate to millis because Postgres' timestamp column rounds nanos away,
			// which would otherwise make the clock and the persisted deadline differ
			// by sub-millisecond noise.
			Instant pinned = Instant.now().truncatedTo(ChronoUnit.MILLIS);
			((MutableClock) clock).setInstant(pinned);

			UUID topicId = createTopicWithDeadline(pinned.plus(7, ChronoUnit.DAYS));
			transactionTemplate.executeWithoutResult(status -> {
				entityManager.createNativeQuery("UPDATE topics SET application_deadline = :deadline WHERE topic_id = :id")
						.setParameter("deadline", pinned)
						.setParameter("id", topicId)
						.executeUpdate();
				entityManager.clear();
			});

			String studentAuth = createRandomAuthentication("student");
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(applyTo(topicId))))
					.andExpect(status().isBadRequest());

			Topic topic = topicRepository.findById(topicId).orElseThrow();
			assertThat(applicationRepository.findAllByTopic(topic)).isEmpty();
		}

		@Test
		void createApplication_DeadlineJustAfterNow_Succeeds() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			// Just past the boundary: now < deadline must be accepted.
			Instant pinned = Instant.now().truncatedTo(ChronoUnit.MILLIS);
			((MutableClock) clock).setInstant(pinned);

			UUID topicId = createTopicWithDeadline(pinned.plus(7, ChronoUnit.DAYS));
			transactionTemplate.executeWithoutResult(status -> {
				entityManager.createNativeQuery("UPDATE topics SET application_deadline = :deadline WHERE topic_id = :id")
						.setParameter("deadline", pinned.plus(1, ChronoUnit.MILLIS))
						.setParameter("id", topicId)
						.executeUpdate();
				entityManager.clear();
			});

			String studentAuth = createRandomAuthentication("student");
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(applyTo(topicId))))
					.andExpect(status().isOk());

			Topic topic = topicRepository.findById(topicId).orElseThrow();
			assertThat(applicationRepository.findAllByTopic(topic)).hasSize(1);
		}
	}

	@Nested
	class GetNotAssessedSuggestedOfResearchGroup {
		@Test
		void getNotAssessed_ReturnsSuggestedApplications() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Suggested RG", advisor.universityId());

			// Create a suggested application (no topic, just a thesis title and research group)
			String studentAuth = createRandomAuthentication("student");
			CreateApplicationPayload appPayload = new CreateApplicationPayload(
					null, "My Suggested Thesis", "MASTER", Instant.now(), "Suggested test", researchGroupId,
			true);
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(appPayload)))
					.andExpect(status().isOk());

			List<Application> result = applicationService.getNotAssesedSuggestedOfResearchGroup(researchGroupId);
			assertThat(result).hasSize(1);
			assertThat(result.getFirst().getThesisTitle()).isEqualTo("My Suggested Thesis");
		}
	}
}
