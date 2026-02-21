package de.tum.cit.aet.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

@Testcontainers
class UserInfoControllerTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Nested
	class GetUserInfo {
		@Test
		void getUserInfo_Success_ReturnsUserProfile() throws Exception {
			TestUser user = createRandomTestUser(List.of("student"));

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
							.header("Authorization", generateTestAuthenticationHeader(user.universityId(), List.of("student"))))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("userId").asString()).isEqualTo(user.userId().toString());
			assertThat(json.get("universityId").asString()).isEqualTo(user.universityId());
			assertThat(json.has("groups")).isTrue();
		}

		@Test
		void getUserInfo_CreatesUserOnFirstAccess() throws Exception {
			String universityId = "newuser" + System.currentTimeMillis();
			String auth = generateTestAuthenticationHeader(universityId, List.of("student"));

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
							.header("Authorization", auth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("universityId").asString()).isEqualTo(universityId);
			assertThat(json.get("userId").asString()).isNotBlank();
		}

		@Test
		void getUserInfo_Unauthenticated_ReturnsUnauthorized() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info"))
					.andExpect(status().isUnauthorized());
		}
	}

	@Nested
	class UpdateUserInfo {
		@Test
		void updateUserInfo_WithData_Success() throws Exception {
			TestUser user = createRandomTestUser(List.of("student"));
			String auth = generateTestAuthenticationHeader(user.universityId(), List.of("student"));

			java.util.HashMap<String, Object> data = new java.util.HashMap<>();
			data.put("firstName", "John");
			data.put("lastName", "Doe");
			data.put("gender", "male");
			data.put("nationality", "German");
			data.put("email", "john@example.com");
			data.put("studyDegree", "MASTER");
			data.put("studyProgram", "Informatics");
			data.put("specialSkills", "Java, Spring");
			data.put("interests", "AI, ML");
			data.put("projects", "Thesis Management");
			data.put("customData", Map.of("key1", "value1"));
			String dataJson = objectMapper.writeValueAsString(data);

			MockMultipartFile dataPart = new MockMultipartFile("data", "", "application/json", dataJson.getBytes());

			String response = mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/user-info")
							.file(dataPart)
							.with(request -> {
								request.setMethod("PUT");
								return request;
							})
							.header("Authorization", auth)
							.contentType(MediaType.MULTIPART_FORM_DATA))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("firstName").asString()).isEqualTo("John");
			assertThat(json.get("lastName").asString()).isEqualTo("Doe");
			assertThat(json.get("gender").asString()).isEqualTo("male");
			assertThat(json.get("nationality").asString()).isEqualTo("German");
		}

		@Test
		void updateUserInfo_WithAvatar_Success() throws Exception {
			TestUser user = createRandomTestUser(List.of("student"));
			String auth = generateTestAuthenticationHeader(user.universityId(), List.of("student"));

			String dataJson = objectMapper.writeValueAsString(Map.of(
					"firstName", "Jane",
					"lastName", "Doe",
					"email", "jane@example.com",
					"customData", Map.of()
			));

			MockMultipartFile dataPart = new MockMultipartFile("data", "", "application/json", dataJson.getBytes());
			MockMultipartFile avatarPart = new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[]{1, 2, 3, 4});

			mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/user-info")
							.file(dataPart)
							.file(avatarPart)
							.with(request -> {
								request.setMethod("PUT");
								return request;
							})
							.header("Authorization", auth)
							.contentType(MediaType.MULTIPART_FORM_DATA))
					.andExpect(status().isOk());
		}
	}

	@Nested
	class NotificationSettings {
		@Test
		void getNotifications_ReturnsEmptyList() throws Exception {
			TestUser user = createRandomTestUser(List.of("student"));
			String auth = generateTestAuthenticationHeader(user.universityId(), List.of("student"));

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info/notifications")
							.header("Authorization", auth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.size()).isZero();
		}

		@Test
		void updateNotifications_CreatesNewSetting() throws Exception {
			TestUser user = createRandomTestUser(List.of("student"));
			String auth = generateTestAuthenticationHeader(user.universityId(), List.of("student"));

			String payload = objectMapper.writeValueAsString(Map.of(
					"name", "new-applications",
					"email", "notify@example.com"
			));

			String response = mockMvc.perform(MockMvcRequestBuilders.put("/v2/user-info/notifications")
							.header("Authorization", auth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(payload))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.size()).isEqualTo(1);
			assertThat(json.get(0).get("name").asString()).isEqualTo("new-applications");
			assertThat(json.get(0).get("email").asString()).isEqualTo("notify@example.com");
		}

		@Test
		void updateNotifications_UpdatesExistingSetting() throws Exception {
			TestUser user = createRandomTestUser(List.of("student"));
			String auth = generateTestAuthenticationHeader(user.universityId(), List.of("student"));

			String createPayload = objectMapper.writeValueAsString(Map.of(
					"name", "new-applications",
					"email", "old@example.com"
			));

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/user-info/notifications")
							.header("Authorization", auth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(createPayload))
					.andExpect(status().isOk());

			String updatePayload = objectMapper.writeValueAsString(Map.of(
					"name", "new-applications",
					"email", "new@example.com"
			));

			String response = mockMvc.perform(MockMvcRequestBuilders.put("/v2/user-info/notifications")
							.header("Authorization", auth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(updatePayload))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.size()).isEqualTo(1);
			assertThat(json.get(0).get("email").asString()).isEqualTo("new@example.com");
		}
	}
}
