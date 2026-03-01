package de.tum.cit.aet.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.controller.payload.CreateThesisPayload;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.repository.ApplicationRepository;
import de.tum.cit.aet.thesis.repository.NotificationSettingRepository;
import de.tum.cit.aet.thesis.repository.ThesisRoleRepository;
import de.tum.cit.aet.thesis.repository.TopicRoleRepository;
import de.tum.cit.aet.thesis.repository.UserGroupRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Testcontainers
class UserDeletionServiceTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private UserDeletionService userDeletionService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ApplicationRepository applicationRepository;

	@Autowired
	private ThesisRoleRepository thesisRoleRepository;

	@Autowired
	private TopicRoleRepository topicRoleRepository;

	@Autowired
	private UserGroupRepository userGroupRepository;

	@Autowired
	private NotificationSettingRepository notificationSettingRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private TransactionTemplate transactionTemplate;

	// --- Helper: assert that a user row is an anonymized tombstone ---
	private void assertTombstone(UUID userId) {
		User tombstone = userRepository.findById(userId).orElseThrow(
				() -> new AssertionError("Expected tombstone user row to exist for " + userId));
		assertThat(tombstone.isAnonymized()).isTrue();
		assertThat(tombstone.isDisabled()).isTrue();
		assertThat(tombstone.getDeletionRequestedAt()).isNotNull();
		assertThat(tombstone.getFirstName()).isNull();
		assertThat(tombstone.getLastName()).isNull();
		assertThat(tombstone.getEmail()).isNull();
		assertThat(tombstone.getUniversityId()).isNotNull(); // preserved for SSO identification
	}

	// --- Helper: create a student with a completed thesis (backdated) ---
	private record StudentWithThesis(TestUser student, UUID thesisId, UUID researchGroupId) {}

	private StudentWithThesis createStudentWithCompletedThesis(int yearsAgoCompleted) throws Exception {
		createTestEmailTemplate("THESIS_CREATED");

		TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
		UUID researchGroupId = createTestResearchGroup("Deletion RG", advisor.universityId());
		TestUser student = createRandomTestUser(List.of("student"));

		CreateThesisPayload thesisPayload = new CreateThesisPayload(
				"Deletion Test Thesis",
				"MASTER",
				"ENGLISH",
				List.of(student.userId()),
				List.of(advisor.userId()),
				List.of(advisor.userId()),
				researchGroupId
		);
		String thesisResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(thesisPayload)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		UUID thesisId = UUID.fromString(objectMapper.readTree(thesisResponse).get("thesisId").asString());

		// Set thesis state to FINISHED and backdate created_at and all state changes
		Instant pastDate = Instant.now().minus(yearsAgoCompleted * 365L, ChronoUnit.DAYS);
		transactionTemplate.executeWithoutResult(status -> {
			entityManager.createNativeQuery(
							"UPDATE theses SET state = 'FINISHED', created_at = :date WHERE thesis_id = :id")
					.setParameter("date", pastDate)
					.setParameter("id", thesisId)
					.executeUpdate();
			entityManager.createNativeQuery(
							"UPDATE thesis_state_changes SET changed_at = :date WHERE thesis_id = :id")
					.setParameter("date", pastDate)
					.setParameter("id", thesisId)
					.executeUpdate();
			entityManager.clear();
		});

		return new StudentWithThesis(student, thesisId, researchGroupId);
	}

	private UUID createRejectedApplicationForUser(TestUser student, TestUser reviewer) throws Exception {
		createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
		createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

		String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));
		UUID applicationId = createTestApplication(studentAuth, "Deletion App");

		transactionTemplate.executeWithoutResult(status -> {
			entityManager.createNativeQuery(
							"UPDATE applications SET state = 'REJECTED', reviewed_at = :date WHERE application_id = :id")
					.setParameter("date", Instant.now().minus(30, ChronoUnit.DAYS))
					.setParameter("id", applicationId)
					.executeUpdate();
			// Add an application reviewer to test that reviewers are cleaned up during deletion
			if (reviewer != null) {
				entityManager.createNativeQuery(
								"INSERT INTO application_reviewers (application_id, user_id, reason, reviewed_at) "
										+ "VALUES (:appId, :userId, 'NOT_INTERESTED', :date)")
						.setParameter("appId", applicationId)
						.setParameter("userId", reviewer.userId())
						.setParameter("date", Instant.now().minus(30, ChronoUnit.DAYS))
						.executeUpdate();
			}
			entityManager.clear();
		});

		return applicationId;
	}

	@Nested
	class PreviewDeletion {
		@Test
		void previewForUserWithNoTheses_ShowsFullDeletion() throws Exception {
			TestUser student = createRandomTestUser(List.of("student"));

			var preview = userDeletionService.previewDeletion(student.userId());

			assertThat(preview.canBeFullyDeleted()).isTrue();
			assertThat(preview.retentionBlockedThesisCount()).isZero();
			assertThat(preview.earliestFullDeletionDate()).isNull();
			assertThat(preview.isResearchGroupHead()).isFalse();
		}

		@Test
		void previewForUserWithActiveThesis_ShowsRetentionBlocked() throws Exception {
			createTestEmailTemplate("THESIS_CREATED");

			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Preview RG", advisor.universityId());
			TestUser student = createRandomTestUser(List.of("student"));

			CreateThesisPayload payload = new CreateThesisPayload(
					"Active Thesis", "MASTER", "ENGLISH",
					List.of(student.userId()), List.of(advisor.userId()),
					List.of(advisor.userId()), researchGroupId
			);
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			var preview = userDeletionService.previewDeletion(student.userId());

			assertThat(preview.canBeFullyDeleted()).isFalse();
			assertThat(preview.retentionBlockedThesisCount()).isPositive();
			assertThat(preview.earliestFullDeletionDate()).isNotNull();
		}

		@Test
		void previewForResearchGroupHead_BlocksDeletion() throws Exception {
			TestUser supervisor = createRandomTestUser(List.of("supervisor", "advisor"));
			createTestResearchGroup("Head RG", supervisor.universityId());

			var preview = userDeletionService.previewDeletion(supervisor.userId());

			assertThat(preview.canBeFullyDeleted()).isFalse();
			assertThat(preview.isResearchGroupHead()).isTrue();
		}

		@Test
		void previewForUserWithRecentCompletedThesis_ShowsRetentionBlocked() throws Exception {
			StudentWithThesis swt = createStudentWithCompletedThesis(2);

			var preview = userDeletionService.previewDeletion(swt.student().userId());

			assertThat(preview.canBeFullyDeleted()).isFalse();
			assertThat(preview.retentionBlockedThesisCount()).isPositive();
			assertThat(preview.earliestFullDeletionDate()).isNotNull();
			assertThat(preview.earliestFullDeletionDate()).isAfter(Instant.now());
		}

		@Test
		void previewForUserWithOldCompletedThesis_ShowsFullDeletion() throws Exception {
			StudentWithThesis swt = createStudentWithCompletedThesis(7);

			var preview = userDeletionService.previewDeletion(swt.student().userId());

			assertThat(preview.canBeFullyDeleted()).isTrue();
			assertThat(preview.retentionBlockedThesisCount()).isZero();
		}

		@Test
		void previewForThesisWithRecentEndDate_UsesEndDateForRetention() throws Exception {
			StudentWithThesis swt = createStudentWithCompletedThesis(8);

			// Set endDate to 2 years ago (more recent than the 8-year-old createdAt/stateChanges)
			Instant recentEndDate = Instant.now().minus(2 * 365L, ChronoUnit.DAYS);
			transactionTemplate.executeWithoutResult(status -> {
				entityManager.createNativeQuery(
								"UPDATE theses SET end_date = :date WHERE thesis_id = :id")
						.setParameter("date", recentEndDate)
						.setParameter("id", swt.thesisId())
						.executeUpdate();
				entityManager.clear();
			});

			var preview = userDeletionService.previewDeletion(swt.student().userId());

			// endDate is only 2 years ago so retention should still be active
			assertThat(preview.canBeFullyDeleted()).isFalse();
			assertThat(preview.retentionBlockedThesisCount()).isPositive();
			assertThat(preview.earliestFullDeletionDate()).isAfter(Instant.now());
		}

		@Test
		void previewForThesisWithNoStateChanges_FallsBackToCreatedAt() throws Exception {
			createTestEmailTemplate("THESIS_CREATED");

			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("NoStates RG", advisor.universityId());
			TestUser student = createRandomTestUser(List.of("student"));

			CreateThesisPayload payload = new CreateThesisPayload(
					"No States Thesis", "MASTER", "ENGLISH",
					List.of(student.userId()), List.of(advisor.userId()),
					List.of(advisor.userId()), researchGroupId
			);
			String thesisResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID thesisId = UUID.fromString(objectMapper.readTree(thesisResponse).get("thesisId").asString());

			// Delete all state changes and backdate createdAt to 8 years ago
			Instant eightYearsAgo = Instant.now().minus(8 * 365L, ChronoUnit.DAYS);
			transactionTemplate.executeWithoutResult(status -> {
				entityManager.createNativeQuery(
								"DELETE FROM thesis_state_changes WHERE thesis_id = :id")
						.setParameter("id", thesisId)
						.executeUpdate();
				entityManager.createNativeQuery(
								"UPDATE theses SET created_at = :date WHERE thesis_id = :id")
						.setParameter("date", eightYearsAgo)
						.setParameter("id", thesisId)
						.executeUpdate();
				entityManager.clear();
			});

			var preview = userDeletionService.previewDeletion(student.userId());

			// createdAt is 8 years ago so retention should have expired
			assertThat(preview.canBeFullyDeleted()).isTrue();
			assertThat(preview.retentionBlockedThesisCount()).isZero();
		}
	}

	@Nested
	class FullDeletion {
		@Test
		void deletesUserWithNoThesesCompletely() throws Exception {
			TestUser student = createRandomTestUser(List.of("student"));

			var result = userDeletionService.deleteOrAnonymizeUser(student.userId());

			assertThat(result.result()).isEqualTo("DELETED");
			assertTombstone(student.userId());
		}

		@Test
		void deletesUserWithRejectedApplicationAndReviewerCompletely() throws Exception {
			TestUser student = createRandomTestUser(List.of("student"));
			TestUser advisor = createRandomTestUser(List.of("advisor"));
			UUID appId = createRejectedApplicationForUser(student, advisor);

			assertThat(applicationRepository.findById(appId)).isPresent();

			var result = userDeletionService.deleteOrAnonymizeUser(student.userId());

			assertThat(result.result()).isEqualTo("DELETED");
			assertTombstone(student.userId());
			assertThat(applicationRepository.findById(appId)).isEmpty();
		}

		@Test
		void deletesUserWithExpiredRetentionThesisCompletely() throws Exception {
			StudentWithThesis swt = createStudentWithCompletedThesis(7);

			var result = userDeletionService.deleteOrAnonymizeUser(swt.student().userId());

			assertThat(result.result()).isEqualTo("DELETED");
			assertTombstone(swt.student().userId());
			assertThat(thesisRoleRepository.findAllByIdUserId(swt.student().userId())).isEmpty();
		}

		@Test
		void cascadeDeletesUserGroupsAndNotificationSettings() throws Exception {
			TestUser student = createRandomTestUser(List.of("student"));

			// Verify user groups exist before deletion
			assertThat(userGroupRepository.findAll().stream()
					.anyMatch(ug -> ug.getId().getUserId().equals(student.userId()))).isTrue();

			userDeletionService.deleteOrAnonymizeUser(student.userId());

			assertThat(userGroupRepository.findAll().stream()
					.anyMatch(ug -> ug.getId().getUserId().equals(student.userId()))).isFalse();
		}
	}

	@Nested
	class SoftDeletion {
		@Test
		void softDeletesUserWithRecentCompletedThesis() throws Exception {
			StudentWithThesis swt = createStudentWithCompletedThesis(2);

			var result = userDeletionService.deleteOrAnonymizeUser(swt.student().userId());

			assertThat(result.result()).isEqualTo("DEACTIVATED");
			User user = userRepository.findById(swt.student().userId()).orElseThrow();
			assertThat(user.isDisabled()).isTrue();
			assertThat(user.getDeletionRequestedAt()).isNotNull();
			assertThat(user.getDeletionScheduledFor()).isNotNull();
			assertThat(user.getDeletionScheduledFor()).isAfter(Instant.now());
		}

		@Test
		void preservesProfileDataDuringRetention() throws Exception {
			StudentWithThesis swt = createStudentWithCompletedThesis(2);

			// Remember original name
			User originalUser = userRepository.findById(swt.student().userId()).orElseThrow();
			String originalFirstName = originalUser.getFirstName();
			String originalLastName = originalUser.getLastName();
			String originalUniversityId = originalUser.getUniversityId();

			userDeletionService.deleteOrAnonymizeUser(swt.student().userId());

			User user = userRepository.findById(swt.student().userId()).orElseThrow();
			// Name must be preserved so professors can find the thesis
			assertThat(user.getFirstName()).isEqualTo(originalFirstName);
			assertThat(user.getLastName()).isEqualTo(originalLastName);
			assertThat(user.getUniversityId()).isEqualTo(originalUniversityId);
		}

		@Test
		void clearsNonEssentialDataDuringRetention() throws Exception {
			StudentWithThesis swt = createStudentWithCompletedThesis(2);

			userDeletionService.deleteOrAnonymizeUser(swt.student().userId());

			User user = userRepository.findById(swt.student().userId()).orElseThrow();
			assertThat(user.getAvatar()).isNull();
			assertThat(user.getProjects()).isNull();
			assertThat(user.getInterests()).isNull();
			assertThat(user.getSpecialSkills()).isNull();
		}

		@Test
		void deletesUserGroupsDuringRetention() throws Exception {
			StudentWithThesis swt = createStudentWithCompletedThesis(2);

			userDeletionService.deleteOrAnonymizeUser(swt.student().userId());

			assertThat(userGroupRepository.findAll().stream()
					.anyMatch(ug -> ug.getId().getUserId().equals(swt.student().userId()))).isFalse();
		}

		@Test
		void preservesThesisRolesDuringRetention() throws Exception {
			StudentWithThesis swt = createStudentWithCompletedThesis(2);

			userDeletionService.deleteOrAnonymizeUser(swt.student().userId());

			assertThat(thesisRoleRepository.findAllByIdUserId(swt.student().userId())).isNotEmpty();
		}

		@Test
		void softDeletesUserWithActiveNonTerminalThesis() throws Exception {
			createTestEmailTemplate("THESIS_CREATED");

			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Active Soft RG", advisor.universityId());
			TestUser student = createRandomTestUser(List.of("student"));

			CreateThesisPayload payload = new CreateThesisPayload(
					"Active Soft Thesis", "MASTER", "ENGLISH",
					List.of(student.userId()), List.of(advisor.userId()),
					List.of(advisor.userId()), researchGroupId
			);
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			// Thesis is in non-terminal state (PROPOSAL) with recent activity
			var result = userDeletionService.deleteOrAnonymizeUser(student.userId());

			// Should soft-delete (not block), with retention active
			assertThat(result.result()).isEqualTo("DEACTIVATED");
			User user = userRepository.findById(student.userId()).orElseThrow();
			assertThat(user.isDisabled()).isTrue();
			assertThat(user.getDeletionRequestedAt()).isNotNull();
			assertThat(user.getDeletionScheduledFor()).isNotNull();
			assertThat(user.getDeletionScheduledFor()).isAfter(Instant.now());
			assertThat(thesisRoleRepository.findAllByIdUserId(student.userId())).isNotEmpty();
		}
	}

	@Nested
	class PreconditionValidation {
		@Test
		void blocksResearchGroupHead() throws Exception {
			TestUser supervisor = createRandomTestUser(List.of("supervisor", "advisor"));
			createTestResearchGroup("Block RG", supervisor.universityId());

			org.junit.jupiter.api.Assertions.assertThrows(
					de.tum.cit.aet.thesis.exception.request.AccessDeniedException.class,
					() -> userDeletionService.deleteOrAnonymizeUser(supervisor.userId())
			);

			assertThat(userRepository.findById(supervisor.userId())).isPresent();
		}

		@Test
		void deletesUserWithThesisStuckInWritingForSevenYears() throws Exception {
			createTestEmailTemplate("THESIS_CREATED");

			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Stuck RG", advisor.universityId());
			TestUser student = createRandomTestUser(List.of("student"));

			CreateThesisPayload payload = new CreateThesisPayload(
					"Stuck Thesis", "MASTER", "ENGLISH",
					List.of(student.userId()), List.of(advisor.userId()),
					List.of(advisor.userId()), researchGroupId
			);
			String thesisResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			UUID thesisId = UUID.fromString(objectMapper.readTree(thesisResponse).get("thesisId").asString());

			// Backdate the thesis creation and state changes to 8 years ago
			Instant eightYearsAgo = Instant.now().minus(8 * 365L, ChronoUnit.DAYS);
			transactionTemplate.executeWithoutResult(status -> {
				entityManager.createNativeQuery(
								"UPDATE theses SET created_at = :date WHERE thesis_id = :id")
						.setParameter("date", eightYearsAgo)
						.setParameter("id", thesisId)
						.executeUpdate();
				entityManager.createNativeQuery(
								"UPDATE thesis_state_changes SET changed_at = :date WHERE thesis_id = :id")
						.setParameter("date", eightYearsAgo)
						.setParameter("id", thesisId)
						.executeUpdate();
				entityManager.clear();
			});

			var result = userDeletionService.deleteOrAnonymizeUser(student.userId());

			assertThat(result.result()).isEqualTo("DELETED");
			assertTombstone(student.userId());
		}

		@Test
		void blocksAlreadyDeletedUser() throws Exception {
			TestUser student = createRandomTestUser(List.of("student"));
			userDeletionService.deleteOrAnonymizeUser(student.userId());

			org.junit.jupiter.api.Assertions.assertThrows(
					de.tum.cit.aet.thesis.exception.request.AccessDeniedException.class,
					() -> userDeletionService.deleteOrAnonymizeUser(student.userId())
			);
		}

		@Test
		void blocksSoftDeletedUser() throws Exception {
			StudentWithThesis swt = createStudentWithCompletedThesis(2);
			userDeletionService.deleteOrAnonymizeUser(swt.student().userId());

			org.junit.jupiter.api.Assertions.assertThrows(
					de.tum.cit.aet.thesis.exception.request.AccessDeniedException.class,
					() -> userDeletionService.deleteOrAnonymizeUser(swt.student().userId())
			);
		}
	}

	@Nested
	class DeferredDeletion {
		@Test
		void processDeferredDeletions_DeletesUserWithExpiredRetention() throws Exception {
			StudentWithThesis swt = createStudentWithCompletedThesis(2);
			userDeletionService.deleteOrAnonymizeUser(swt.student().userId());

			// Verify user still exists (soft-deleted)
			assertThat(userRepository.findById(swt.student().userId())).isPresent();

			// Backdate thesis and state changes to make retention expire (7 years ago)
			Instant sevenYearsAgo = Instant.now().minus(7 * 365L, ChronoUnit.DAYS);
			transactionTemplate.executeWithoutResult(status -> {
				entityManager.createNativeQuery(
								"UPDATE theses SET created_at = :date WHERE thesis_id = :id")
						.setParameter("date", sevenYearsAgo)
						.setParameter("id", swt.thesisId())
						.executeUpdate();
				entityManager.createNativeQuery(
								"UPDATE thesis_state_changes SET changed_at = :date WHERE thesis_id = :id")
						.setParameter("date", sevenYearsAgo)
						.setParameter("id", swt.thesisId())
						.executeUpdate();
				entityManager.clear();
			});

			userDeletionService.processDeferredDeletions();

			// After deferred deletion, user is fully anonymized tombstone with no scheduled deletion
			User tombstone = userRepository.findById(swt.student().userId()).orElseThrow();
			assertThat(tombstone.isAnonymized()).isTrue();
			assertThat(tombstone.getFirstName()).isNull();
			assertThat(tombstone.getLastName()).isNull();
			assertThat(tombstone.getDeletionScheduledFor()).isNull();
		}

		@Test
		void processDeferredDeletions_KeepsUserWithActiveRetention() throws Exception {
			StudentWithThesis swt = createStudentWithCompletedThesis(2);
			userDeletionService.deleteOrAnonymizeUser(swt.student().userId());

			userDeletionService.processDeferredDeletions();

			// User should still exist because retention hasn't expired
			assertThat(userRepository.findById(swt.student().userId())).isPresent();
		}
	}

	@Nested
	class AuthGuard {
		@Test
		void softDeletedUserCannotLogin() throws Exception {
			StudentWithThesis swt = createStudentWithCompletedThesis(2);
			userDeletionService.deleteOrAnonymizeUser(swt.student().userId());

			String authHeader = generateTestAuthenticationHeader(
					swt.student().universityId(), List.of("student"));

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
							.header("Authorization", authHeader))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class ControllerEndpoints {
		@Test
		void selfPreview_Authenticated_ReturnsPreview() throws Exception {
			TestUser student = createRandomTestUser(List.of("student"));
			String auth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-deletion/me/preview")
							.header("Authorization", auth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			assertThat(response).contains("canBeFullyDeleted");
			assertThat(response).contains("true");
		}

		@Test
		void selfPreview_Unauthenticated_Returns401() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-deletion/me/preview"))
					.andExpect(status().isUnauthorized());
		}

		@Test
		void selfDelete_DeletesAndReturnsResult() throws Exception {
			TestUser student = createRandomTestUser(List.of("student"));
			String auth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

			String response = mockMvc.perform(MockMvcRequestBuilders.delete("/v2/user-deletion/me")
							.header("Authorization", auth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			assertThat(response).contains("DELETED");
			assertTombstone(student.userId());
		}

		@Test
		void adminPreview_AsAdmin_ReturnsPreview() throws Exception {
			TestUser student = createRandomTestUser(List.of("student"));
			String adminAuth = createRandomAdminAuthentication();

			String response = mockMvc.perform(MockMvcRequestBuilders.get(
									"/v2/user-deletion/" + student.userId() + "/preview")
							.header("Authorization", adminAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			assertThat(response).contains("canBeFullyDeleted");
		}

		@Test
		void adminPreview_AsStudent_Returns403() throws Exception {
			TestUser student = createRandomTestUser(List.of("student"));
			TestUser otherStudent = createRandomTestUser(List.of("student"));
			String auth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

			mockMvc.perform(MockMvcRequestBuilders.get(
							"/v2/user-deletion/" + otherStudent.userId() + "/preview")
							.header("Authorization", auth))
					.andExpect(status().isForbidden());
		}

		@Test
		void adminDelete_AsAdmin_DeletesUser() throws Exception {
			TestUser student = createRandomTestUser(List.of("student"));
			String adminAuth = createRandomAdminAuthentication();

			String response = mockMvc.perform(MockMvcRequestBuilders.delete(
									"/v2/user-deletion/" + student.userId())
							.header("Authorization", adminAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			assertThat(response).contains("DELETED");
			assertTombstone(student.userId());
		}

		@Test
		void adminDelete_AsStudent_Returns403() throws Exception {
			TestUser student = createRandomTestUser(List.of("student"));
			TestUser otherStudent = createRandomTestUser(List.of("student"));
			String auth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

			mockMvc.perform(MockMvcRequestBuilders.delete(
							"/v2/user-deletion/" + otherStudent.userId())
							.header("Authorization", auth))
					.andExpect(status().isForbidden());

			assertThat(userRepository.findById(otherStudent.userId())).isPresent();
		}
	}
}
