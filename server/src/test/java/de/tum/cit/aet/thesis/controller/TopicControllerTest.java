package de.tum.cit.aet.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.constants.ApplicationRejectReason;
import de.tum.cit.aet.thesis.controller.payload.CloseTopicPayload;
import de.tum.cit.aet.thesis.controller.payload.ReplaceTopicPayload;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Testcontainers
class TopicControllerTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Test
	void getTopics_Success() throws Exception {
		createTestTopic("Test Topic");

		mockMvc.perform(MockMvcRequestBuilders.get("/v2/topics")
						.header("Authorization", createRandomAdminAuthentication()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", isA(List.class)))
				.andExpect(jsonPath("$.content", hasSize(equalTo(1))))
				.andExpect(jsonPath("$.totalElements", isA(Number.class)));
	}

	@Test
	void getTopic_Success() throws Exception {
		UUID topicId = createTestTopic("Test Topic");

		mockMvc.perform(MockMvcRequestBuilders.get("/v2/topics/{topicId}", topicId)
						.header("Authorization", createRandomAdminAuthentication()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topicId").value(topicId.toString()))
				.andExpect(jsonPath("$.title").value("Test Topic"));
	}

	@Test
	void createTopic_Success() throws Exception {
		TestUser advisor = createTestUser("supervisor", List.of("supervisor", "advisor"));
		UUID researchGroupId = createTestResearchGroup("Test Research Group", advisor.universityId());

		ReplaceTopicPayload payload = new ReplaceTopicPayload(
				"Test Topic",
				Set.of("MASTER", "BACHELOR"),
				"Problem Statement",
				"Requirements",
				"Goals",
				"References",
				List.of(advisor.userId()),
				List.of(advisor.userId()),
				researchGroupId,
				null,
				null,
				false
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/topics")
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Test Topic"))
				.andExpect(jsonPath("$.thesisTypes", containsInAnyOrder("MASTER", "BACHELOR")));
	}

	@Test
	void createTopic_AsStudent_Forbidden() throws Exception {
		UUID researchGroupId = createTestResearchGroup("Test Research Group", createTestUser("supervisor", List.of("supervisor", "advisor")).universityId());
		ReplaceTopicPayload payload = new ReplaceTopicPayload(
				"Test Topic",
				Set.of("MASTER"),
				"Problem Statement",
				"Requirements",
				"Goals",
				"References",
				List.of(UUID.randomUUID()),
				List.of(UUID.randomUUID()),
				researchGroupId,
				null,
				null,
				false
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/topics")
						.header("Authorization", createRandomAuthentication("student"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isForbidden());
	}

	@Test
	void updateTopic_Success() throws Exception {
		UUID topicId = createTestTopic("Test Topic");
		TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
		UUID researchGroupId = createTestResearchGroup("Test Research Group", advisor.universityId());

		ReplaceTopicPayload updatePayload = new ReplaceTopicPayload(
				"Updated Topic",
				Set.of("MASTER"),
				"Updated Problem Statement",
				"Updated Requirements",
				"Updated Goals",
				"Updated References",
				List.of(advisor.userId()),
				List.of(advisor.userId()),
				researchGroupId,
				null,
				null,
				false
		);

		mockMvc.perform(MockMvcRequestBuilders.put("/v2/topics/{topicId}", topicId)
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updatePayload)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Updated Topic"))
				.andExpect(jsonPath("$.problemStatement").value("Updated Problem Statement"))
				.andExpect(jsonPath("$.requirements").value("Updated Requirements"))
				.andExpect(jsonPath("$.goals").value("Updated Goals"))
				.andExpect(jsonPath("$.references").value("Updated References"));
	}

	@Test
	void closeTopic_Success() throws Exception {
		UUID topicId = createTestTopic("Test Topic");

		CloseTopicPayload closePayload = new CloseTopicPayload(
				ApplicationRejectReason.TOPIC_FILLED,
				true
		);

		mockMvc.perform(MockMvcRequestBuilders.delete("/v2/topics/{topicId}", topicId)
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(closePayload)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.closedAt").value(notNullValue(String.class)));
	}

	@Test
	void getTopics_WithPagination_Success() throws Exception {
		// Create multiple test topics
		createTestTopic("Topic 1");
		createTestTopic("Topic 2");
		createTestTopic("Topic 3");

		mockMvc.perform(MockMvcRequestBuilders.get("/v2/topics")
						.header("Authorization", createRandomAdminAuthentication())
						.param("page", "0")
						.param("limit", "2")
						.param("sortBy", "createdAt")
						.param("sortOrder", "desc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(2)))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.totalPages").value(2));
	}

	@Test
	void getTopics_WithSearch_Success() throws Exception {
		createTestTopic("Specific Topic Title");

		mockMvc.perform(MockMvcRequestBuilders.get("/v2/topics")
						.header("Authorization", createRandomAdminAuthentication())
						.param("search", "Specific")
						.param("page", "0")
						.param("limit", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(equalTo(1))))
				.andExpect(jsonPath("$.content[0].title", containsString("Specific")));
	}

	@Test
	void getTopics_VerifyResponseStructure() throws Exception {
		createTestTopic("Structure Test Topic");

		String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/topics")
						.header("Authorization", createRandomAdminAuthentication()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		JsonNode json = objectMapper.readTree(response);
		assertThat(json.has("content")).isTrue();
		assertThat(json.has("totalElements")).isTrue();
		assertThat(json.has("totalPages")).isTrue();

		JsonNode firstTopic = json.get("content").get(0);
		// Fields present in TopicOverviewDto
		assertThat(firstTopic.has("topicId")).isTrue();
		assertThat(firstTopic.has("title")).isTrue();
		assertThat(firstTopic.has("state")).isTrue();
		assertThat(firstTopic.has("advisors")).isTrue();
		assertThat(firstTopic.has("supervisors")).isTrue();

		// Fields excluded from overview (only in detail TopicDto)
		assertThat(firstTopic.has("problemStatement")).isFalse();
		assertThat(firstTopic.has("requirements")).isFalse();
		assertThat(firstTopic.has("goals")).isFalse();
		assertThat(firstTopic.has("references")).isFalse();
		assertThat(firstTopic.has("closedAt")).isFalse();
		assertThat(firstTopic.has("publishedAt")).isFalse();
		assertThat(firstTopic.has("updatedAt")).isFalse();
	}
}
