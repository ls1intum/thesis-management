package de.tum.cit.aet.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.constants.ThesisVisibility;
import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.entity.ThesisRole;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.repository.ThesisRepository;
import de.tum.cit.aet.thesis.repository.ThesisRoleRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ThesisRepository thesisRepository;

	@Autowired
	private ThesisRoleRepository thesisRoleRepository;

	@Nested
	class GetAvatarAuthenticated {
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

		@Test
		void getAvatar_AuthenticatedRequest_ReturnsAvatarForAnyUser() throws Exception {
			// Authenticated users can access any user's avatar (even without public thesis)
			TestUser student = createRandomTestUser(List.of("student"));
			var user = userRepository.findById(student.userId()).orElseThrow();
			user.setAvatar("test-avatar.png");
			userRepository.save(user);

			// Even though the student has no public thesis, authenticated users should get a 200
			// (or fail with file not found, not 404 from visibility check).
			// The visibility check only applies to unauthenticated requests.
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/avatars/{userId}", student.userId())
							.header("Authorization", createRandomAdminAuthentication()))
					// Will fail with 500 because the file doesn't exist on disk, but that proves
					// the visibility check passed (otherwise it would be 404)
					.andExpect(status().is5xxServerError());
		}
	}

	@Nested
	class GetAvatarUnauthenticated {
		@Test
		void getAvatar_UnauthenticatedRequest_StudentWithNoPublicThesis_Returns404() throws Exception {
			TestUser student = createRandomTestUser(List.of("student"));
			var user = userRepository.findById(student.userId()).orElseThrow();
			user.setAvatar("test-avatar.png");
			userRepository.save(user);

			// Verify the repository query agrees this user is NOT publicly visible
			assertThat(userRepository.isUserPubliclyVisible(student.userId())).isFalse();

			// Unauthenticated request for a student without public visibility → 404
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/avatars/{userId}", student.userId()))
					.andExpect(status().isNotFound());
		}

		@Test
		void getAvatar_UnauthenticatedRequest_UserNotFound_Returns404() throws Exception {
			UUID nonExistentUserId = UUID.randomUUID();

			assertThat(userRepository.isUserPubliclyVisible(nonExistentUserId)).isFalse();

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/avatars/{userId}", nonExistentUserId))
					.andExpect(status().isNotFound());
		}

		@Test
		void getAvatar_UnauthenticatedRequest_ResearchGroupHead_IsAccessible() throws Exception {
			// Research group heads are publicly visible (their info appears on the public
			// research-groups endpoint), so their avatar should be accessible without auth.
			TestUser headUser = createRandomTestUser(List.of("supervisor"));
			createTestResearchGroup("Avatar Test Group", headUser.universityId());

			var user = userRepository.findById(headUser.userId()).orElseThrow();
			user.setAvatar("head-avatar.png");
			userRepository.save(user);

			// Verify the repository query confirms this user IS publicly visible
			assertThat(userRepository.isUserPubliclyVisible(headUser.userId())).isTrue();

			// Unauthenticated request for a research group head → should pass visibility check
			// (will fail with 500 because the file doesn't exist on disk, proving the check passed)
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/avatars/{userId}", headUser.userId()))
					.andExpect(status().is5xxServerError());
		}

		@Test
		void getAvatar_UnauthenticatedRequest_UserWithPublicThesis_IsAccessible() throws Exception {
			// Users who have a finished thesis with PUBLIC visibility should have their
			// avatar accessible without authentication.
			UUID thesisId = createTestThesis("Public Thesis Avatar Test");
			Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
			thesis.setVisibility(ThesisVisibility.PUBLIC);
			thesis.setState(ThesisState.FINISHED);
			thesisRepository.save(thesis);

			// Look up a user on this thesis via ThesisRoleRepository (avoids LazyInitializationException)
			ThesisRole role = thesisRoleRepository.findAll().stream()
					.filter(r -> r.getId().getThesisId().equals(thesisId))
					.findFirst().orElseThrow();
			var thesisUser = userRepository.findById(role.getId().getUserId()).orElseThrow();
			thesisUser.setAvatar("public-thesis-avatar.png");
			userRepository.save(thesisUser);

			// Verify the repository query confirms this user IS publicly visible
			assertThat(userRepository.isUserPubliclyVisible(thesisUser.getId())).isTrue();

			// Unauthenticated request → should pass visibility check
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/avatars/{userId}", thesisUser.getId()))
					.andExpect(status().is5xxServerError());
		}

		@Test
		void getAvatar_UnauthenticatedRequest_UserWithPrivateThesis_Returns404() throws Exception {
			// A student who has only a PRIVATE thesis and is NOT a research group head
			// or topic role holder should not have their avatar accessible unauthenticated.
			// Note: createTestThesis assigns the creator as both thesis role holder AND research
			// group head, which makes them publicly visible. So we use a plain student instead.
			TestUser student = createRandomTestUser(List.of("student"));
			var user = userRepository.findById(student.userId()).orElseThrow();
			user.setAvatar("private-thesis-avatar.png");
			userRepository.save(user);

			// Verify the repository query agrees this user is NOT publicly visible
			assertThat(userRepository.isUserPubliclyVisible(student.userId())).isFalse();

			// This student has no public thesis, no topic role, and is not a research group head
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/avatars/{userId}", student.userId()))
					.andExpect(status().isNotFound());
		}

		@Test
		void getAvatar_UnauthenticatedRequest_UserOnOpenTopic_IsAccessible() throws Exception {
			// Users who are supervisors/examiners on open topics are publicly visible
			// (topic listings are public), so their avatar should be accessible.
			UUID topicId = createTestTopic("Avatar Topic Test");
			// The topic creator (staff member) is assigned as supervisor and examiner

			// Find the topic's role holder
			var topic = mockMvc.perform(MockMvcRequestBuilders.get("/v2/topics/{topicId}", topicId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andReturn().getResponse().getContentAsString();

			var topicJson = objectMapper.readTree(topic);
			UUID supervisorUserId = UUID.fromString(
					topicJson.get("supervisors").get(0).get("userId").asString());

			var user = userRepository.findById(supervisorUserId).orElseThrow();
			user.setAvatar("topic-supervisor-avatar.png");
			userRepository.save(user);

			// Verify the repository query confirms this user IS publicly visible
			assertThat(userRepository.isUserPubliclyVisible(supervisorUserId)).isTrue();

			// Unauthenticated request → should pass visibility check
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/avatars/{userId}", supervisorUserId))
					.andExpect(status().is5xxServerError());
		}
	}
}
