package de.tum.cit.aet.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.constants.ApplicationRejectReason;
import de.tum.cit.aet.thesis.controller.payload.CloseTopicPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.ReplaceTopicPayload;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
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
	void closeTopic_WithPendingApplications_RejectsAll() throws Exception {
		createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
		createTestEmailTemplate("APPLICATION_CREATED_STUDENT");
		createTestEmailTemplate("APPLICATION_REJECTED_TOPIC_FILLED");

		UUID topicId = createTestTopic("Close With Apps Topic");

		// Create two applications for this topic
		String student1Auth = createRandomAuthentication("student");
		CreateApplicationPayload appPayload1 = new CreateApplicationPayload(
				topicId, null, "MASTER", Instant.now(), "Motivation 1", null,
		true);
		mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
						.header("Authorization", student1Auth)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(appPayload1)))
				.andExpect(status().isOk());

		String student2Auth = createRandomAuthentication("student");
		CreateApplicationPayload appPayload2 = new CreateApplicationPayload(
				topicId, null, "MASTER", Instant.now(), "Motivation 2", null,
		true);
		mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
						.header("Authorization", student2Auth)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(appPayload2)))
				.andExpect(status().isOk());

		CloseTopicPayload closePayload = new CloseTopicPayload(
				ApplicationRejectReason.TOPIC_FILLED, true
		);

		mockMvc.perform(MockMvcRequestBuilders.delete("/v2/topics/{topicId}", topicId)
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(closePayload)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.closedAt").value(notNullValue(String.class)));
	}

	@Test
	void closeTopic_AsStudent_Forbidden() throws Exception {
		UUID topicId = createTestTopic("Close Forbidden Topic");

		CloseTopicPayload closePayload = new CloseTopicPayload(
				ApplicationRejectReason.TOPIC_FILLED, false
		);

		mockMvc.perform(MockMvcRequestBuilders.delete("/v2/topics/{topicId}", topicId)
						.header("Authorization", createRandomAuthentication("student"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(closePayload)))
				.andExpect(status().isForbidden());
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
		assertThat(firstTopic.has("supervisors")).isTrue();
		assertThat(firstTopic.has("examiners")).isTrue();

		// Fields excluded from overview (only in detail TopicDto)
		assertThat(firstTopic.has("problemStatement")).isFalse();
		assertThat(firstTopic.has("requirements")).isFalse();
		assertThat(firstTopic.has("goals")).isFalse();
		assertThat(firstTopic.has("references")).isFalse();
		assertThat(firstTopic.has("closedAt")).isFalse();
		assertThat(firstTopic.has("publishedAt")).isFalse();
		assertThat(firstTopic.has("updatedAt")).isFalse();
	}

	@Nested
	class TopicStateFiltering {
		@Test
		void getTopics_FilterByOpenState_ReturnsOpenTopics() throws Exception {
			createTestTopic("Open Topic");

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/topics")
							.header("Authorization", createRandomAdminAuthentication())
							.param("states", "OPEN"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
		}

		@Test
		void getTopics_FilterByClosedState_ReturnsClosedTopics() throws Exception {
			UUID topicId = createTestTopic("Close Me Topic");

			CloseTopicPayload closePayload = new CloseTopicPayload(
					ApplicationRejectReason.TOPIC_FILLED, false
			);

			mockMvc.perform(MockMvcRequestBuilders.delete("/v2/topics/{topicId}", topicId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(closePayload)))
					.andExpect(status().isOk());

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/topics")
							.header("Authorization", createRandomAdminAuthentication())
							.param("states", "CLOSED"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isGreaterThanOrEqualTo(1);
		}

		@Test
		void getTopics_FilterByDraftState_AsAdmin_ReturnsDrafts() throws Exception {
			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Draft Group", advisor.universityId());

			ReplaceTopicPayload draftPayload = new ReplaceTopicPayload(
					"Draft Topic",
					Set.of("MASTER"),
					"Problem Statement", "Requirements", "Goals", "References",
					List.of(advisor.userId()), List.of(advisor.userId()),
					researchGroupId, null, null, true
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/topics")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(draftPayload)))
					.andExpect(status().isOk());

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/topics")
							.header("Authorization", createRandomAdminAuthentication())
							.param("states", "DRAFT"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isGreaterThanOrEqualTo(1);
		}
	}

	@Nested
	class InterviewTopics {
		@Test
		void getInterviewTopics_AsAdmin_Success() throws Exception {
			// Admin needs a research group to avoid NPE in getPossibleInterviewTopics
			TestUser adminUser = createRandomTestUser(List.of("supervisor", "admin"));
			UUID rg = createTestResearchGroup("Admin Interview RG", adminUser.universityId());

			ReplaceTopicPayload payload = new ReplaceTopicPayload(
					"Interview Eligible Topic", Set.of("MASTER"),
					"PS", "Req", "Goals", "Refs",
					List.of(adminUser.userId()), List.of(adminUser.userId()),
					rg, null, null, false
			);
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/topics")
							.header("Authorization", generateTestAuthenticationHeader(adminUser.universityId(), List.of("supervisor", "admin")))
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/topics/interview-topics")
							.header("Authorization", generateTestAuthenticationHeader(adminUser.universityId(), List.of("supervisor", "admin"))))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", isA(List.class)))
					.andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
		}

		@Test
		void getInterviewTopics_AsStudent_Forbidden() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/topics/interview-topics")
							.header("Authorization", createRandomAuthentication("student")))
					.andExpect(status().isForbidden());
		}

		@Test
		void getInterviewTopics_AsAdvisor_Success() throws Exception {
			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID rg = createTestResearchGroup("Interview RG", advisor.universityId());

			ReplaceTopicPayload payload = new ReplaceTopicPayload(
					"Advisor Interview Topic", Set.of("MASTER"),
					"PS", "Req", "Goals", "Refs",
					List.of(advisor.userId()), List.of(advisor.userId()),
					rg, null, null, false
			);
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/topics")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			String advisorAuth = generateTestAuthenticationHeader(advisor.universityId(), List.of("supervisor", "advisor"));

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/topics/interview-topics")
							.header("Authorization", advisorAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isGreaterThanOrEqualTo(1);
		}
	}

	@Nested
	class TopicDraftAndPublish {
		@Test
		void createTopic_AsDraft_Success() throws Exception {
			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID rg = createTestResearchGroup("Draft RG", advisor.universityId());

			ReplaceTopicPayload draftPayload = new ReplaceTopicPayload(
					"Draft Topic Test", Set.of("MASTER"),
					"PS", "Req", "Goals", "Refs",
					List.of(advisor.userId()), List.of(advisor.userId()),
					rg, null, null, true
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/topics")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(draftPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("state").asString()).isEqualTo("DRAFT");
			assertThat(json.has("publishedAt")).isFalse();
		}

		@Test
		void updateTopic_PublishDraft_SetsPublishedAt() throws Exception {
			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID rg = createTestResearchGroup("Publish RG", advisor.universityId());

			// Create as draft
			ReplaceTopicPayload draftPayload = new ReplaceTopicPayload(
					"To Publish Topic", Set.of("MASTER"),
					"PS", "Req", "Goals", "Refs",
					List.of(advisor.userId()), List.of(advisor.userId()),
					rg, null, null, true
			);
			String createResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/topics")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(draftPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID topicId = UUID.fromString(objectMapper.readTree(createResponse).get("topicId").asString());

			// Publish (set isDraft to false)
			ReplaceTopicPayload publishPayload = new ReplaceTopicPayload(
					"To Publish Topic", Set.of("MASTER"),
					"PS", "Req", "Goals", "Refs",
					List.of(advisor.userId()), List.of(advisor.userId()),
					rg, null, null, false
			);
			String updateResponse = mockMvc.perform(MockMvcRequestBuilders.put("/v2/topics/{topicId}", topicId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(publishPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(updateResponse);
			assertThat(json.get("state").asString()).isEqualTo("OPEN");
			assertThat(json.has("publishedAt")).isTrue();
		}
	}

	@Nested
	class TopicResearchGroupFiltering {
		@Test
		void getTopics_FilterByResearchGroupId_ReturnsOnlyMatchingTopics() throws Exception {
			TestUser advisor1 = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID rg1 = createTestResearchGroup("RG Filter 1", advisor1.universityId());

			ReplaceTopicPayload payload1 = new ReplaceTopicPayload(
					"RG1 Topic", Set.of("MASTER"),
					"PS", "Req", "Goals", "Refs",
					List.of(advisor1.userId()), List.of(advisor1.userId()),
					rg1, null, null, false
			);
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/topics")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload1)))
					.andExpect(status().isOk());

			createTestTopic("Other RG Topic");

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/topics")
							.header("Authorization", createRandomAdminAuthentication())
							.param("researchGroupIds", rg1.toString()))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isEqualTo(1);
			assertThat(json.get("content").get(0).get("title").asString()).isEqualTo("RG1 Topic");
		}

		@Test
		void getTopics_FilterByType_ReturnsMatchingTopics() throws Exception {
			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID rg = createTestResearchGroup("Type Filter Group", advisor.universityId());

			ReplaceTopicPayload payload = new ReplaceTopicPayload(
					"Bachelor Only Topic", Set.of("BACHELOR"),
					"PS", "Req", "Goals", "Refs",
					List.of(advisor.userId()), List.of(advisor.userId()),
					rg, null, null, false
			);
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/topics")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/topics")
							.header("Authorization", createRandomAdminAuthentication())
							.param("type", "BACHELOR"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isGreaterThanOrEqualTo(1);
		}
	}
}
