package de.tum.cit.aet.thesis.core.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.core.user.repository.UserRepository;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
	class AvatarPrompt {
		@Test
		void getUserInfo_NewUser_AvatarPromptNotDismissed() throws Exception {
			String auth = generateTestAuthenticationHeader("prompt" + System.currentTimeMillis(), List.of("student"));

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
							.header("Authorization", auth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.path("avatar").isNull() || json.path("avatar").isMissingNode()).isTrue();
			assertThat(json.get("avatarPromptDismissed").asBoolean()).isFalse();
		}

		@Test
		void dismissAvatarPrompt_PersistsAndIsIdempotent() throws Exception {
			TestUser user = createRandomTestUser(List.of("student"));
			String auth = generateTestAuthenticationHeader(user.universityId(), List.of("student"));

			for (int i = 0; i < 2; i++) {
				String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/user-info/dismiss-avatar-prompt")
								.header("Authorization", auth))
						.andExpect(status().isOk())
						.andReturn().getResponse().getContentAsString();

				assertThat(objectMapper.readTree(response).get("avatarPromptDismissed").asBoolean()).isTrue();
			}

			String info = mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
							.header("Authorization", auth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			assertThat(objectMapper.readTree(info).get("avatarPromptDismissed").asBoolean()).isTrue();
		}

		@Test
		void dismissAvatarPrompt_Unauthenticated_ReturnsUnauthorized() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/user-info/dismiss-avatar-prompt"))
					.andExpect(status().isUnauthorized());
		}

		@Test
		void uploadAvatar_SetsAvatarAndKeepsRestOfProfile() throws Exception {
			TestUser user = createRandomTestUser(List.of("student"));
			String auth = generateTestAuthenticationHeader(user.universityId(), List.of("student"));

			MockMultipartFile avatarPart = new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[]{1, 2, 3, 4});

			String response = mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/user-info/avatar")
							.file(avatarPart)
							.header("Authorization", auth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("avatar").asString()).endsWith(".png");
			assertThat(json.get("universityId").asString()).isEqualTo(user.universityId());

			MockMultipartFile replacement = new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[]{5, 6, 7, 8});

			String replaced = mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/user-info/avatar")
							.file(replacement)
							.header("Authorization", auth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			assertThat(objectMapper.readTree(replaced).get("avatar").asString())
					.isNotEqualTo(json.get("avatar").asString());
		}

		@Test
		void uploadAvatar_InvalidFileType_IsRejected() throws Exception {
			TestUser user = createRandomTestUser(List.of("student"));
			String auth = generateTestAuthenticationHeader(user.universityId(), List.of("student"));

			MockMultipartFile notAnImage = new MockMultipartFile("avatar", "avatar.exe", "application/octet-stream", new byte[]{1, 2, 3});

			mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/user-info/avatar")
							.file(notAnImage)
							.header("Authorization", auth))
					.andExpect(status().isInternalServerError());
		}
	}

	@Nested
	class ProfilePictureOnProfileUpdate {
		@Autowired
		private UserRepository userRepository;

		private JsonNode updateProfile(TestUser user, MockMultipartFile avatarPart) throws Exception {
			String dataJson = objectMapper.writeValueAsString(Map.of(
					"firstName", "Jane", "lastName", "Doe", "email", "jane@example.com", "customData", Map.of()));
			MockMultipartFile dataPart = new MockMultipartFile("data", "", "application/json", dataJson.getBytes());

			var request = MockMvcRequestBuilders.multipart("/v2/user-info")
					.file(dataPart)
					.with(r -> {
						r.setMethod("PUT");
						return r;
					})
					.header("Authorization", generateTestAuthenticationHeader(user.universityId(), List.of("student")))
					.contentType(MediaType.MULTIPART_FORM_DATA);

			if (avatarPart != null) {
				request.file(avatarPart);
			}

			String response = mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

			return objectMapper.readTree(response);
		}

		private void givePicture(TestUser user, String filename) {
			User entity = userRepository.findById(user.userId()).orElseThrow();
			entity.setAvatar(filename);
			userRepository.save(entity);
		}

		@Test
		void updateProfile_WithoutAvatarPart_KeepsThePicture() throws Exception {
			TestUser user = createRandomTestUser(List.of("student"));
			givePicture(user, "kept.png");

			JsonNode response = updateProfile(user, null);

			assertThat(response.get("avatar").asString()).isEqualTo("kept.png");
		}

		@Test
		void updateProfile_WithEmptyAvatarPart_KeepsThePicture() throws Exception {
			TestUser user = createRandomTestUser(List.of("student"));
			givePicture(user, "kept.png");

			// e.g. a browser that could not render a very large photo submits an empty file
			JsonNode response = updateProfile(user, new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[0]));

			assertThat(response.get("avatar").asString()).isEqualTo("kept.png");
			assertThat(userRepository.findById(user.userId()).orElseThrow().getAvatar()).isEqualTo("kept.png");
		}

		@Test
		void updateProfile_WithNewAvatarPart_ReplacesThePicture() throws Exception {
			TestUser user = createRandomTestUser(List.of("student"));
			givePicture(user, "old.png");

			JsonNode response = updateProfile(user, new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[] {1, 2, 3, 4}));

			assertThat(response.get("avatar").asString()).isNotEqualTo("old.png").endsWith(".png");
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
