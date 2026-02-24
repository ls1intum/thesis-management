package de.tum.cit.aet.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.constants.DataExportState;
import de.tum.cit.aet.thesis.controller.payload.CreateApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateThesisPayload;
import de.tum.cit.aet.thesis.controller.payload.ReplaceTopicPayload;
import de.tum.cit.aet.thesis.entity.DataExport;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.repository.DataExportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Testcontainers
class DataExportServiceTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private DataExportService dataExportService;

	@Autowired
	private DataExportRepository dataExportRepository;

	private final ObjectMapper jsonMapper = new ObjectMapper();

	private void assertExportSucceeded(DataExport export) {
		assertThat(export.getState()).isIn(DataExportState.EMAIL_SENT, DataExportState.EMAIL_FAILED);
		assertThat(export.getFilePath()).isNotNull();
		assertThat(export.getCreationFinishedAt()).isNotNull();
	}

	private JsonNode readZipEntry(ZipFile zip, String entryName) throws Exception {
		ZipEntry entry = zip.getEntry(entryName);
		assertThat(entry).as("ZIP entry '%s' should exist", entryName).isNotNull();
		try (InputStream is = zip.getInputStream(entry)) {
			return jsonMapper.readTree(is);
		}
	}

	/**
	 * Comprehensive test that exercises all data export code paths:
	 * - User profile data with Instant fields (catches Jackson serialization issues)
	 * - Applications with reviewers (catches LazyInitializationException on ApplicationReviewer.user)
	 * - Theses with state changes (catches LazyInitializationException on DataExport.user and Thesis collections)
	 */
	@Test
	void processAllPendingExportsWithFullUserData() throws Exception {
		createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
		createTestEmailTemplate("APPLICATION_CREATED_STUDENT");
		createTestEmailTemplate("THESIS_CREATED");

		// Create advisor and research group
		TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
		UUID researchGroupId = createTestResearchGroup("Export RG", advisor.universityId());
		String advisorAuth = generateTestAuthenticationHeader(advisor.universityId(), List.of("supervisor", "advisor"));

		// Create a topic
		ReplaceTopicPayload topicPayload = new ReplaceTopicPayload(
				"Export Topic", Set.of("MASTER"),
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

		// Create student and submit an application
		TestUser student = createRandomTestUser(List.of("student"));
		String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

		CreateApplicationPayload appPayload = new CreateApplicationPayload(
				topicId, null, "MASTER", Instant.now(), "Test motivation", null
		);
		String appResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
						.header("Authorization", studentAuth)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(appPayload)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		UUID applicationId = UUID.fromString(objectMapper.readTree(appResponse).get("applicationId").asString());

		// Add a reviewer to the application (creates ApplicationReviewer with lazy user)
		mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/" + applicationId + "/review")
						.header("Authorization", advisorAuth)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reason\":\"NOT_INTERESTED\"}"))
				.andReturn();

		// Create a thesis for the student
		CreateThesisPayload thesisPayload = new CreateThesisPayload(
				"Export Test Thesis",
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
				.andExpect(status().isOk());

		// Request a data export
		mockMvc.perform(MockMvcRequestBuilders.post("/v2/data-exports")
						.header("Authorization", studentAuth))
				.andExpect(status().isOk());

		// Process exports (simulates the cron job — no Hibernate session)
		dataExportService.processAllPendingExports();

		// Verify the export succeeded
		List<DataExport> exports = dataExportRepository.findAll();
		assertThat(exports).hasSize(1);

		DataExport export = exports.getFirst();
		assertExportSucceeded(export);

		// Validate ZIP content to catch serialization and lazy loading issues
		try (ZipFile zip = new ZipFile(Path.of(export.getFilePath()).toFile())) {
			assertThat(zip.getEntry("README.txt")).isNotNull();

			// user.json: verify profile fields and Instant serialization
			JsonNode userData = readZipEntry(zip, "user.json");
			assertThat(userData.path("universityId").asString()).isEqualTo(student.universityId());
			assertThat(userData.path("joinedAt").asString()).isNotEmpty();

			// applications.json: verify application with reviewer data (catches lazy User proxy issue)
			JsonNode apps = readZipEntry(zip, "applications.json");
			assertThat(apps.isArray()).isTrue();
			assertThat(apps).hasSize(1);
			assertThat(apps.get(0).path("motivation").asString()).isEqualTo("Test motivation");
			JsonNode reviewers = apps.get(0).path("reviewers");
			assertThat(reviewers.isArray()).isTrue();
			assertThat(reviewers).hasSize(1);
			assertThat(reviewers.get(0).path("reviewerName").asString()).isNotEmpty();

			// theses.json: verify thesis data
			JsonNode theses = readZipEntry(zip, "theses.json");
			assertThat(theses.isArray()).isTrue();
			assertThat(theses).hasSize(1);
			assertThat(theses.get(0).path("title").asString()).isEqualTo("Export Test Thesis");
		}
	}

	@Test
	void processAllPendingExportsCreatesZipForUserWithoutData() throws Exception {
		TestUser student = createRandomTestUser(List.of("student"));
		String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/data-exports")
						.header("Authorization", studentAuth))
				.andExpect(status().isOk());

		dataExportService.processAllPendingExports();

		List<DataExport> exports = dataExportRepository.findAll();
		assertThat(exports).hasSize(1);
		assertExportSucceeded(exports.getFirst());
	}

	@Test
	void processAllPendingExportsSkipsAlreadyClaimedExports() throws Exception {
		TestUser student = createRandomTestUser(List.of("student"));
		String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/data-exports")
						.header("Authorization", studentAuth))
				.andExpect(status().isOk());

		dataExportService.processAllPendingExports();
		dataExportService.processAllPendingExports();

		List<DataExport> exports = dataExportRepository.findAll();
		assertThat(exports).hasSize(1);
		assertExportSucceeded(exports.getFirst());
	}

	@Test
	void processAllPendingExportsHandlesMultipleExports() throws Exception {
		TestUser student1 = createRandomTestUser(List.of("student"));
		TestUser student2 = createRandomTestUser(List.of("student"));

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/data-exports")
						.header("Authorization", generateTestAuthenticationHeader(student1.universityId(), List.of("student"))))
				.andExpect(status().isOk());
		mockMvc.perform(MockMvcRequestBuilders.post("/v2/data-exports")
						.header("Authorization", generateTestAuthenticationHeader(student2.universityId(), List.of("student"))))
				.andExpect(status().isOk());

		dataExportService.processAllPendingExports();

		List<DataExport> exports = dataExportRepository.findAll();
		assertThat(exports).hasSize(2);
		assertThat(exports).allSatisfy(this::assertExportSucceeded);
	}
}
