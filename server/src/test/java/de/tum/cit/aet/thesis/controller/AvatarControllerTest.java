package de.tum.cit.aet.thesis.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

@Testcontainers
class AvatarControllerTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Nested
	class GetAvatar {
		@Test
		void getAvatar_UserWithNoAvatar_Returns404() throws Exception {
			TestUser user = createRandomTestUser(List.of("student"));

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/avatars/{userId}", user.userId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNotFound());
		}

		@Test
		void getAvatar_UserNotFound_Returns404() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/avatars/{userId}", UUID.randomUUID())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNotFound());
		}
	}
}
