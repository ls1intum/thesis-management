package de.tum.cit.aet.thesis.cron;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.constants.ApplicationState;
import de.tum.cit.aet.thesis.controller.payload.CreateApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.ReplaceTopicPayload;
import de.tum.cit.aet.thesis.entity.Application;
import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.repository.ApplicationRepository;
import de.tum.cit.aet.thesis.repository.ResearchGroupSettingsRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.mail.Address;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Testcontainers
class CronJobIntegrationTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private AutomaticRejects automaticRejects;

	@Autowired
	private ApplicationRepository applicationRepository;

	@Autowired
	private ResearchGroupSettingsRepository researchGroupSettingsRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private TransactionTemplate transactionTemplate;

	private record CronSetup(UUID topicId, UUID applicationId, UUID researchGroupId, TestUser advisor) {}

	private CronSetup createTopicWithOldApplicationAndAutoReject() throws Exception {
		createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
		createTestEmailTemplate("APPLICATION_CREATED_STUDENT");
		createTestEmailTemplate("APPLICATION_REJECTED");
		createTestEmailTemplate("APPLICATION_AUTOMATIC_REJECT_REMINDER");

		TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
		UUID researchGroupId = createTestResearchGroup("Cron RG", advisor.universityId());

		// Enable automatic reject for this research group
		ResearchGroupSettings settings = new ResearchGroupSettings();
		settings.setResearchGroupId(researchGroupId);
		settings.setAutomaticRejectEnabled(true);
		settings.setRejectDuration(2); // 2 weeks
		researchGroupSettingsRepository.save(settings);

		// Create topic
		ReplaceTopicPayload topicPayload = new ReplaceTopicPayload(
				"Cron Topic", Set.of("MASTER"),
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
				topicId, null, "MASTER", Instant.now(), "Cron test", null
		);
		String appResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
						.header("Authorization", studentAuth)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(appPayload)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		UUID applicationId = UUID.fromString(objectMapper.readTree(appResponse).get("applicationId").asString());

		// Backdate application to 30 days ago
		transactionTemplate.executeWithoutResult(status -> {
			entityManager.createNativeQuery("UPDATE applications SET created_at = :date WHERE application_id = :id")
					.setParameter("date", Instant.now().minus(30, ChronoUnit.DAYS))
					.setParameter("id", applicationId)
					.executeUpdate();
			entityManager.clear();
		});

		return new CronSetup(topicId, applicationId, researchGroupId, advisor);
	}

	@Nested
	class RejectOldApplications {
		@Test
		void rejectOldApplications_WithAutoRejectEnabled_RejectsOldApplications() throws Exception {
			CronSetup setup = createTopicWithOldApplicationAndAutoReject();

			automaticRejects.rejectOldApplications();

			Application app = applicationRepository.findById(setup.applicationId).orElseThrow();
			assertThat(app.getState()).isEqualTo(ApplicationState.REJECTED);
		}

		@Test
		void rejectOldApplications_WithRecentApplication_DoesNotReject() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");
			createTestEmailTemplate("APPLICATION_REJECTED");
			createTestEmailTemplate("APPLICATION_AUTOMATIC_REJECT_REMINDER");

			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Recent Cron RG", advisor.universityId());

			ResearchGroupSettings settings = new ResearchGroupSettings();
			settings.setResearchGroupId(researchGroupId);
			settings.setAutomaticRejectEnabled(true);
			settings.setRejectDuration(2);
			researchGroupSettingsRepository.save(settings);

			ReplaceTopicPayload topicPayload = new ReplaceTopicPayload(
					"Recent Cron Topic", Set.of("MASTER"),
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

			String studentAuth = createRandomAuthentication("student");
			CreateApplicationPayload appPayload = new CreateApplicationPayload(
					UUID.fromString(objectMapper.readTree(topicResponse).get("topicId").asString()),
					null, "MASTER", Instant.now(), "Recent cron test", null
			);
			String appResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(appPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID applicationId = UUID.fromString(objectMapper.readTree(appResponse).get("applicationId").asString());

			automaticRejects.rejectOldApplications();

			Application app = applicationRepository.findById(applicationId).orElseThrow();
			assertThat(app.getState()).isEqualTo(ApplicationState.NOT_ASSESSED);
		}

		@Test
		void rejectOldApplications_SendsReminderEmailForUpcomingRejects() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");
			createTestEmailTemplate("APPLICATION_REJECTED");
			createTestEmailTemplate("APPLICATION_AUTOMATIC_REJECT_REMINDER");

			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Reminder Cron RG", advisor.universityId());

			ResearchGroupSettings settings = new ResearchGroupSettings();
			settings.setResearchGroupId(researchGroupId);
			settings.setAutomaticRejectEnabled(true);
			settings.setRejectDuration(4); // 4 weeks - application created 8 days ago won't be rejected but will be in "upcoming" list
			researchGroupSettingsRepository.save(settings);

			ReplaceTopicPayload topicPayload = new ReplaceTopicPayload(
					"Reminder Cron Topic", Set.of("MASTER"),
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
					topicId, null, "MASTER", Instant.now(), "Reminder cron test", null
			);
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(appPayload)))
					.andExpect(status().isOk());

			// Backdate application to 21 days ago (will be rejected in 7 days with 4-week duration)
			transactionTemplate.executeWithoutResult(status -> {
				entityManager.createNativeQuery(
							"UPDATE applications SET created_at = :date WHERE application_id IN "
									+ "(SELECT application_id FROM applications WHERE topic_id = :topicId)")
						.setParameter("date", Instant.now().minus(21, ChronoUnit.DAYS))
						.setParameter("topicId", topicId)
						.executeUpdate();
				entityManager.clear();
			});

			clearEmails();

			automaticRejects.rejectOldApplications();

			// Reminder email should have been sent to the advisor
			MimeMessage[] emails = getReceivedEmails();
			assertThat(emails.length).as("At least one reminder email should be sent").isGreaterThanOrEqualTo(1);

			List<String> recipients = Stream.of(emails)
					.flatMap(email -> {
						try {
							return Arrays.stream(email.getAllRecipients());
						} catch (Exception e) {
							return Stream.empty();
						}
					})
					.map(Address::toString)
					.toList();
			assertThat(recipients).as("Advisor should receive a reminder email")
					.anyMatch(addr -> addr.contains(advisor.universityId()));
		}

		@Test
		void rejectOldApplications_WithTopicDeadline_UsesDeadlineAsReference() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");
			createTestEmailTemplate("APPLICATION_REJECTED");
			createTestEmailTemplate("APPLICATION_AUTOMATIC_REJECT_REMINDER");

			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Deadline Cron RG", advisor.universityId());

			ResearchGroupSettings settings = new ResearchGroupSettings();
			settings.setResearchGroupId(researchGroupId);
			settings.setAutomaticRejectEnabled(true);
			settings.setRejectDuration(2);
			researchGroupSettingsRepository.save(settings);

			// Create topic with an application deadline in the past
			ReplaceTopicPayload topicPayload = new ReplaceTopicPayload(
					"Deadline Cron Topic", Set.of("MASTER"),
					"PS", "Req", "Goals", "Refs",
					List.of(advisor.userId()), List.of(advisor.userId()),
					researchGroupId, null, Instant.now().minus(20, ChronoUnit.DAYS), false
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
					topicId, null, "MASTER", Instant.now(), "Deadline test", null
			);
			String appResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(appPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID applicationId = UUID.fromString(objectMapper.readTree(appResponse).get("applicationId").asString());

			// Backdate the application to satisfy the 14-day minimum age requirement
			transactionTemplate.executeWithoutResult(status -> {
				entityManager.createNativeQuery("UPDATE applications SET created_at = :date WHERE application_id = :id")
						.setParameter("date", Instant.now().minus(30, ChronoUnit.DAYS))
						.setParameter("id", applicationId)
						.executeUpdate();
				entityManager.clear();
			});

			automaticRejects.rejectOldApplications();

			// Application should be rejected because the topic deadline is 20 days in the past
			// and rejectDuration is 2 weeks (14 days), and application is older than 14 days
			Application app = applicationRepository.findById(applicationId).orElseThrow();
			assertThat(app.getState()).isEqualTo(ApplicationState.REJECTED);
		}

		@Test
		void rejectOldApplications_WithNoAutoRejectEnabled_DoesNothing() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("No Auto RG", advisor.universityId());

			// Settings with auto-reject DISABLED
			ResearchGroupSettings settings = new ResearchGroupSettings();
			settings.setResearchGroupId(researchGroupId);
			settings.setAutomaticRejectEnabled(false);
			settings.setRejectDuration(2);
			researchGroupSettingsRepository.save(settings);

			ReplaceTopicPayload topicPayload = new ReplaceTopicPayload(
					"No Auto Topic", Set.of("MASTER"),
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
					topicId, null, "MASTER", Instant.now(), "No auto test", null
			);
			String appResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(appPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID applicationId = UUID.fromString(objectMapper.readTree(appResponse).get("applicationId").asString());

			// Backdate
			transactionTemplate.executeWithoutResult(status -> {
				entityManager.createNativeQuery("UPDATE applications SET created_at = :date WHERE application_id = :id")
						.setParameter("date", Instant.now().minus(30, ChronoUnit.DAYS))
						.setParameter("id", applicationId)
						.executeUpdate();
				entityManager.clear();
			});

			automaticRejects.rejectOldApplications();

			Application app = applicationRepository.findById(applicationId).orElseThrow();
			assertThat(app.getState()).isEqualTo(ApplicationState.NOT_ASSESSED);
		}
	}

	// Note: ApplicationReminder.emailReminder() cannot be tested directly because it calls
	// ResearchGroupService.getAll() which requires the request-scoped CurrentUserProvider bean.
	// Calling the cron method outside of a request context throws ScopeNotActiveException.
}
