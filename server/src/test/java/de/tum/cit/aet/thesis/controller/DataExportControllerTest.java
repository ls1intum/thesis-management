package de.tum.cit.aet.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.constants.DataExportState;
import de.tum.cit.aet.thesis.entity.DataExport;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.repository.DataExportRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Testcontainers
class DataExportControllerTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private DataExportRepository dataExportRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Nested
	class RequestExport {
		@Test
		void authenticatedUserCanRequestExport() throws Exception {
			String studentAuth = createRandomAuthentication("student");

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/data-exports")
							.header("Authorization", studentAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			assertThat(response).contains("REQUESTED");
			assertThat(dataExportRepository.findAll()).hasSize(1);
			assertThat(dataExportRepository.findAll().getFirst().getState()).isEqualTo(DataExportState.REQUESTED);
		}

		@Test
		void rateLimitReturns429WhenRecentExportExists() throws Exception {
			TestUser student = createRandomTestUser(List.of("student"));
			String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

			// First request succeeds
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/data-exports")
							.header("Authorization", studentAuth))
					.andExpect(status().isOk());

			// Second request is rate-limited
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/data-exports")
							.header("Authorization", studentAuth))
					.andExpect(status().isTooManyRequests());
		}

		@Test
		void allowsNewRequestAfterCooldownPeriod() throws Exception {
			TestUser student = createRandomTestUser(List.of("student"));
			String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

			// Create first export
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/data-exports")
							.header("Authorization", studentAuth))
					.andExpect(status().isOk());

			// Backdate the export to 8 days ago
			transactionTemplate.executeWithoutResult(status -> {
				DataExport export = dataExportRepository.findAll().getFirst();
				entityManager.createNativeQuery(
								"UPDATE data_exports SET created_at = :date WHERE data_export_id = :id")
						.setParameter("date", Instant.now().minus(8, ChronoUnit.DAYS))
						.setParameter("id", export.getId())
						.executeUpdate();
				entityManager.clear();
			});

			// Now a new request should succeed
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/data-exports")
							.header("Authorization", studentAuth))
					.andExpect(status().isOk());

			assertThat(dataExportRepository.findAll()).hasSize(2);
		}

		@Test
		void allowsReRequestAfterFailedExport() throws Exception {
			TestUser student = createRandomTestUser(List.of("student"));
			String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

			// Create first export
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/data-exports")
							.header("Authorization", studentAuth))
					.andExpect(status().isOk());

			// Mark it as FAILED
			transactionTemplate.executeWithoutResult(status -> {
				DataExport export = dataExportRepository.findAll().getFirst();
				entityManager.createNativeQuery(
								"UPDATE data_exports SET state = 'FAILED' WHERE data_export_id = :id")
						.setParameter("id", export.getId())
						.executeUpdate();
				entityManager.clear();
			});

			// New request should succeed even within cooldown
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/data-exports")
							.header("Authorization", studentAuth))
					.andExpect(status().isOk());
		}
	}

	@Nested
	class GetStatus {
		@Test
		void returnsEmptyStatusWhenNoExportExists() throws Exception {
			String studentAuth = createRandomAuthentication("student");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/data-exports/status")
							.header("Authorization", studentAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			assertThat(response).contains("\"canRequest\":true");
		}

		@Test
		void returnsLatestExportStatus() throws Exception {
			TestUser student = createRandomTestUser(List.of("student"));
			String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

			// Create an export
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/data-exports")
							.header("Authorization", studentAuth))
					.andExpect(status().isOk());

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/data-exports/status")
							.header("Authorization", studentAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			assertThat(response).contains("REQUESTED");
			assertThat(response).contains("\"canRequest\":false");
		}
	}

	@Nested
	class DownloadExport {
		@Test
		void returnsNotFoundForNonExistentExport() throws Exception {
			String studentAuth = createRandomAuthentication("student");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/data-exports/00000000-0000-0000-0000-000000000001/download")
							.header("Authorization", studentAuth))
					.andExpect(status().isNotFound());
		}

		@Test
		void returnsForbiddenWhenDownloadingOtherUsersExport() throws Exception {
			TestUser student1 = createRandomTestUser(List.of("student"));
			String student1Auth = generateTestAuthenticationHeader(student1.universityId(), List.of("student"));

			// Create export for student1
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/data-exports")
							.header("Authorization", student1Auth))
					.andExpect(status().isOk());

			DataExport export = dataExportRepository.findAll().getFirst();

			// Mark as EMAIL_SENT with a file path so we can attempt download
			transactionTemplate.executeWithoutResult(status -> {
				entityManager.createNativeQuery(
								"UPDATE data_exports SET state = 'EMAIL_SENT', creation_finished_at = NOW() WHERE data_export_id = :id")
						.setParameter("id", export.getId())
						.executeUpdate();
				entityManager.clear();
			});

			// Student2 tries to download student1's export
			String student2Auth = createRandomAuthentication("student");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/data-exports/" + export.getId() + "/download")
							.header("Authorization", student2Auth))
					.andExpect(status().isForbidden());
		}
	}
}
