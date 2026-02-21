package de.tum.cit.aet.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.controller.payload.CreateResearchGroupPayload;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

@Testcontainers
class ResearchGroupControllerTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private ResearchGroupRepository researchGroupRepository;

	@Autowired
	private UserRepository userRepository;

	@Nested
	class GetResearchGroups {
		@Test
		void getResearchGroups_AsAdmin_Success() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			createTestResearchGroup("Test Group", head.universityId());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-groups")
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
					.andExpect(jsonPath("$.totalElements", isA(Number.class)));
		}

		@Test
		void getResearchGroups_WithSearch_FiltersResults() throws Exception {
			TestUser head1 = createRandomTestUser(List.of("supervisor"));
			createTestResearchGroup("Alpha Group", head1.universityId());
			TestUser head2 = createRandomTestUser(List.of("supervisor"));
			createTestResearchGroup("Beta Group", head2.universityId());

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-groups")
							.header("Authorization", createRandomAdminAuthentication())
							.param("search", "Alpha"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).isEqualTo(1);
		}

		@Test
		void getResearchGroups_WithPagination_ReturnsPagedResults() throws Exception {
			for (int i = 0; i < 3; i++) {
				TestUser head = createRandomTestUser(List.of("supervisor"));
				createTestResearchGroup("Group " + i, head.universityId());
			}

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-groups")
							.header("Authorization", createRandomAdminAuthentication())
							.param("page", "0")
							.param("limit", "2"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(2)))
					.andExpect(jsonPath("$.totalPages").value(2));
		}

		@Test
		void getResearchGroups_IncludeArchived_ShowsArchivedGroups() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Archived Group", head.universityId());

			mockMvc.perform(MockMvcRequestBuilders.patch("/v2/research-groups/{id}/archive", groupId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNoContent());

			String withoutArchived = mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-groups")
							.header("Authorization", createRandomAdminAuthentication())
							.param("includeArchived", "false"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			String withArchived = mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-groups")
							.header("Authorization", createRandomAdminAuthentication())
							.param("includeArchived", "true"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode withoutJson = objectMapper.readTree(withoutArchived);
			JsonNode withJson = objectMapper.readTree(withArchived);
			assertThat(withJson.get("totalElements").asInt()).isGreaterThan(withoutJson.get("totalElements").asInt());
		}

		@Test
		void getResearchGroups_AsNonAdmin_ReturnsOwnGroup() throws Exception {
			TestUser advisor = createRandomTestUser(List.of("advisor"));
			UUID groupId = createTestResearchGroup("Advisor Group", advisor.universityId());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-groups")
							.header("Authorization", generateTestAuthenticationHeader(advisor.universityId(), List.of("advisor"))))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", hasSize(1)));
		}
	}

	@Nested
	class GetLightResearchGroups {
		@Test
		void getLightResearchGroups_Success() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			createTestResearchGroup("Light Group", head.universityId());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-groups/light")
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
		}

		@Test
		void getLightResearchGroups_WithSearch_FiltersResults() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			createTestResearchGroup("Unique Name XYZ", head.universityId());

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-groups/light")
							.header("Authorization", createRandomAdminAuthentication())
							.param("search", "Unique Name XYZ"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.size()).isEqualTo(1);
		}
	}

	@Nested
	class GetActiveLightResearchGroups {
		@Test
		void getActiveLightResearchGroups_AsAdmin_ReturnsAll() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			createTestResearchGroup("Active Group", head.universityId());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-groups/light/active")
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
		}

		@Test
		void getActiveLightResearchGroups_AsMember_ReturnsOwnGroup() throws Exception {
			TestUser advisor = createRandomTestUser(List.of("advisor"));
			createTestResearchGroup("Member Group", advisor.universityId());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-groups/light/active")
							.header("Authorization", generateTestAuthenticationHeader(advisor.universityId(), List.of("advisor"))))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(1)));
		}
	}

	@Nested
	class GetResearchGroupById {
		@Test
		void getResearchGroup_Success() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Specific Group", head.universityId());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-groups/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id").value(groupId.toString()));
		}

		@Test
		void getResearchGroup_NotFound() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-groups/{id}", UUID.randomUUID())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNotFound());
		}
	}

	@Nested
	class GetResearchGroupMembers {
		@Test
		void getMembers_AsAdmin_Success() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Members Group", head.universityId());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-groups/{id}/members", groupId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content", isA(List.class)));
		}

		@Test
		void getMembers_AsStudent_Forbidden() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Forbidden Group", head.universityId());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-groups/{id}/members", groupId)
							.header("Authorization", createRandomAuthentication("student")))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class CreateResearchGroup {
		@Test
		void createResearchGroup_AsAdmin_Success() throws Exception {
			TestUser head = createRandomTestUser(List.of("student"));

			CreateResearchGroupPayload payload = new CreateResearchGroupPayload(
					head.universityId(), "New Research Group " + UUID.randomUUID(),
					"NRG-" + UUID.randomUUID().toString().substring(0, 6),
					"Garching", "A test description", "https://example.com"
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-groups")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("name").asString()).contains("New Research Group");
			assertThat(json.get("id").asString()).isNotBlank();
		}

		@Test
		void createResearchGroup_AsStudent_Forbidden() throws Exception {
			TestUser head = createRandomTestUser(List.of("student"));

			CreateResearchGroupPayload payload = new CreateResearchGroupPayload(
					head.universityId(), "Forbidden Group", "FG",
					null, null, null
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-groups")
							.header("Authorization", createRandomAuthentication("student"))
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isForbidden());
		}

		@Test
		void createResearchGroup_DuplicateHead_ReturnsError() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			createTestResearchGroup("First Group", head.universityId());

			CreateResearchGroupPayload payload = new CreateResearchGroupPayload(
					head.universityId(), "Second Group " + UUID.randomUUID(),
					"SG-" + UUID.randomUUID().toString().substring(0, 6),
					null, null, null
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-groups")
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class UpdateResearchGroup {
		@Test
		void updateResearchGroup_Success() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Original Group", head.universityId());

			CreateResearchGroupPayload payload = new CreateResearchGroupPayload(
					head.universityId(), "Updated Group " + UUID.randomUUID(),
					UUID.randomUUID().toString(), null, "Updated description", null
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("name").asString()).contains("Updated Group");
			assertThat(json.get("description").asString()).isEqualTo("Updated description");
		}

		@Test
		void updateResearchGroup_ChangeHead_Success() throws Exception {
			TestUser oldHead = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Head Change Group", oldHead.universityId());

			// Create new head user
			TestUser newHead = createRandomTestUser(List.of("supervisor"));

			CreateResearchGroupPayload payload = new CreateResearchGroupPayload(
					newHead.universityId(), "Head Change Group " + UUID.randomUUID(),
					UUID.randomUUID().toString(), null, "Changed head", null
			);

			String response = mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("head").get("universityId").asString()).isEqualTo(newHead.universityId());
		}

		@Test
		void updateResearchGroup_Archived_ReturnsForbidden() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("To Archive", head.universityId());

			mockMvc.perform(MockMvcRequestBuilders.patch("/v2/research-groups/{id}/archive", groupId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNoContent());

			CreateResearchGroupPayload payload = new CreateResearchGroupPayload(
					head.universityId(), "Trying Update", UUID.randomUUID().toString(),
					null, null, null
			);

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class ArchiveResearchGroup {
		@Test
		void archiveResearchGroup_AsAdmin_Success() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("To Archive Group", head.universityId());

			mockMvc.perform(MockMvcRequestBuilders.patch("/v2/research-groups/{id}/archive", groupId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNoContent());

			assertThat(researchGroupRepository.findById(groupId).get().isArchived()).isTrue();
		}

		@Test
		void archiveResearchGroup_AsStudent_Forbidden() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Student Archive", head.universityId());

			mockMvc.perform(MockMvcRequestBuilders.patch("/v2/research-groups/{id}/archive", groupId)
							.header("Authorization", createRandomAuthentication("student")))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class AssignUserToResearchGroup {
		@Test
		void assignUser_Success() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Assign Group", head.universityId());
			TestUser newUser = createRandomTestUser(List.of("student"));

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/assign/{username}", groupId, newUser.universityId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.universityId").value(newUser.universityId()));
		}

		@Test
		void assignUser_AlreadyAssigned_ReturnsForbidden() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Duplicate Assign", head.universityId());
			TestUser newUser = createRandomTestUser(List.of("student"));

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/assign/{username}", groupId, newUser.universityId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			TestUser head2 = createRandomTestUser(List.of("supervisor"));
			UUID groupId2 = createTestResearchGroup("Another Group", head2.universityId());

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/assign/{username}", groupId2, newUser.universityId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isForbidden());
		}

		@Test
		void assignUser_ArchivedGroup_ReturnsForbidden() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Archived Assign", head.universityId());

			mockMvc.perform(MockMvcRequestBuilders.patch("/v2/research-groups/{id}/archive", groupId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNoContent());

			TestUser newUser = createRandomTestUser(List.of("student"));

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/assign/{username}", groupId, newUser.universityId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class RemoveUserFromResearchGroup {
		@Test
		void removeUser_Success() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Remove Group", head.universityId());
			TestUser member = createRandomTestUser(List.of("student"));

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/assign/{username}", groupId, member.universityId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/remove/{userId}", groupId, member.userId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());
		}

		@Test
		void removeHead_ReturnsForbidden() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Head Remove", head.universityId());

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/remove/{userId}", groupId, head.userId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class UpdateMemberRole {
		@Test
		void updateRole_ToAdvisor_Success() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Role Group", head.universityId());
			TestUser member = createRandomTestUser(List.of("student"));

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/assign/{username}", groupId, member.universityId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/member/{userId}/role", groupId, member.userId())
							.header("Authorization", createRandomAdminAuthentication())
							.param("role", "advisor"))
					.andExpect(status().isOk());
		}

		@Test
		void updateRole_ToSupervisor_Success() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Supervisor Role Group", head.universityId());
			TestUser member = createRandomTestUser(List.of("student"));

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/assign/{username}", groupId, member.universityId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/member/{userId}/role", groupId, member.userId())
							.header("Authorization", createRandomAdminAuthentication())
							.param("role", "supervisor"))
					.andExpect(status().isOk());
		}

		@Test
		void updateRole_InvalidRole_ThrowsException() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Invalid Role Group", head.universityId());
			TestUser member = createRandomTestUser(List.of("student"));

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/assign/{username}", groupId, member.universityId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			String adminAuth = createRandomAdminAuthentication();
			assertThatThrownBy(() ->
					mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/member/{userId}/role", groupId, member.userId())
									.header("Authorization", adminAuth)
									.param("role", "invalid"))
			).hasCauseInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	class ToggleGroupAdmin {
		@Test
		void toggleGroupAdmin_AddRole_Success() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Admin Toggle Group", head.universityId());
			TestUser member = createRandomTestUser(List.of("student"));

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/assign/{username}", groupId, member.universityId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/member/{userId}/group-admin", groupId, member.userId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());
		}

		@Test
		void toggleGroupAdmin_RemoveRole_Success() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Admin Remove Toggle Group", head.universityId());
			TestUser member = createRandomTestUser(List.of("student"));

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/assign/{username}", groupId, member.universityId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			// Add group-admin role
			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/member/{userId}/group-admin", groupId, member.userId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			// Toggle again to remove it
			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/member/{userId}/group-admin", groupId, member.userId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());
		}
	}

	@Nested
	class RemoveUserEdgeCases {
		@Test
		void removeUser_FromArchivedGroup_ReturnsForbidden() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Archived Remove Group", head.universityId());
			TestUser member = createRandomTestUser(List.of("student"));

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/assign/{username}", groupId, member.universityId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			mockMvc.perform(MockMvcRequestBuilders.patch("/v2/research-groups/{id}/archive", groupId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNoContent());

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/remove/{userId}", groupId, member.userId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class UpdateRoleEdgeCases {
		@Test
		void updateRole_ArchivedGroup_ReturnsForbidden() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Archived Role Group", head.universityId());
			TestUser member = createRandomTestUser(List.of("student"));

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/assign/{username}", groupId, member.universityId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			mockMvc.perform(MockMvcRequestBuilders.patch("/v2/research-groups/{id}/archive", groupId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNoContent());

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/member/{userId}/role", groupId, member.userId())
							.header("Authorization", createRandomAdminAuthentication())
							.param("role", "supervisor"))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class UpdateResearchGroupEdgeCases {
		@Test
		void updateResearchGroup_ArchivedGroup_ReturnsForbidden() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Archived Update Group", head.universityId());

			mockMvc.perform(MockMvcRequestBuilders.patch("/v2/research-groups/{id}/archive", groupId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNoContent());

			String adminAuth = createRandomAdminAuthentication();
			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}", groupId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"name\": \"New Name\", \"headUsername\": \"" + head.universityId() + "\"}"))
					.andExpect(status().isForbidden());
		}

		@Test
		void assignUser_ToArchivedGroup_ReturnsForbidden() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Archived Assign Group", head.universityId());
			TestUser member = createRandomTestUser(List.of("student"));

			mockMvc.perform(MockMvcRequestBuilders.patch("/v2/research-groups/{id}/archive", groupId)
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isNoContent());

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/assign/{username}", groupId, member.universityId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class ResearchGroupSearchFilters {
		@Test
		void getResearchGroups_WithSearchQuery_FiltersResults() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			createTestResearchGroup("UniqueSearchXyz Group", head.universityId());
			TestUser head2 = createRandomTestUser(List.of("supervisor"));
			createTestResearchGroup("Other Group", head2.universityId());

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-groups")
							.header("Authorization", createRandomAdminAuthentication())
							.param("search", "UniqueSearchXyz"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("content").size()).as("Only the matching group should be returned").isEqualTo(1);
			assertThat(json.get("content").get(0).get("name").asString()).contains("UniqueSearchXyz");
		}

		@Test
		void getLightResearchGroups_WithSearchQuery_FiltersResults() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			createTestResearchGroup("UniqueLightXyz Group", head.universityId());
			TestUser head2 = createRandomTestUser(List.of("supervisor"));
			createTestResearchGroup("Different Group", head2.universityId());

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-groups/light")
							.header("Authorization", createRandomAdminAuthentication())
							.param("search", "UniqueLightXyz"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.size()).as("Only the matching group should be returned").isEqualTo(1);
			assertThat(json.get(0).get("name").asString()).contains("UniqueLightXyz");
		}
	}

	@Nested
	class MembersSorting {
		@Test
		void getMembers_SortByFirstName_VerifiesOrder() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID groupId = createTestResearchGroup("Member Sort Group", head.universityId());

			// Add a second member so we can verify sort order
			TestUser member = createRandomTestUser(List.of("advisor"));
			mockMvc.perform(MockMvcRequestBuilders.put("/v2/research-groups/{id}/assign/{username}", groupId, member.universityId())
							.header("Authorization", createRandomAdminAuthentication()))
					.andExpect(status().isOk());

			String ascResponse = mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-groups/{id}/members", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.param("sortBy", "firstName")
							.param("sortOrder", "asc"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode ascContent = objectMapper.readTree(ascResponse).get("content");
			assertThat(ascContent.size()).isGreaterThanOrEqualTo(2);
			for (int i = 1; i < ascContent.size(); i++) {
				String prev = ascContent.get(i - 1).get("firstName").asString();
				String curr = ascContent.get(i).get("firstName").asString();
				assertThat(prev.compareToIgnoreCase(curr)).as("Members should be sorted ascending by firstName").isLessThanOrEqualTo(0);
			}

			String descResponse = mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-groups/{id}/members", groupId)
							.header("Authorization", createRandomAdminAuthentication())
							.param("sortBy", "firstName")
							.param("sortOrder", "desc"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode descContent = objectMapper.readTree(descResponse).get("content");
			assertThat(descContent.size()).isGreaterThanOrEqualTo(2);
			for (int i = 1; i < descContent.size(); i++) {
				String prev = descContent.get(i - 1).get("firstName").asString();
				String curr = descContent.get(i).get("firstName").asString();
				assertThat(prev.compareToIgnoreCase(curr)).as("Members should be sorted descending by firstName").isGreaterThanOrEqualTo(0);
			}
		}

		@Test
		void getResearchGroups_SortOrder_VerifiesOrder() throws Exception {
			TestUser head1 = createRandomTestUser(List.of("supervisor"));
			TestUser head2 = createRandomTestUser(List.of("supervisor"));
			createTestResearchGroup("AAA Sort Group", head1.universityId());
			createTestResearchGroup("ZZZ Sort Group", head2.universityId());

			String ascResponse = mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-groups")
							.header("Authorization", createRandomAdminAuthentication())
							.param("sortBy", "name")
							.param("sortOrder", "asc"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode ascContent = objectMapper.readTree(ascResponse).get("content");
			assertThat(ascContent.size()).isGreaterThanOrEqualTo(2);
			String firstName = ascContent.get(0).get("name").asString();
			String lastName = ascContent.get(ascContent.size() - 1).get("name").asString();
			assertThat(firstName.compareToIgnoreCase(lastName)).as("First item should come before last in asc order").isLessThanOrEqualTo(0);

			String descResponse = mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-groups")
							.header("Authorization", createRandomAdminAuthentication())
							.param("sortBy", "name")
							.param("sortOrder", "desc"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode descContent = objectMapper.readTree(descResponse).get("content");
			assertThat(descContent.size()).isGreaterThanOrEqualTo(2);
			String firstDesc = descContent.get(0).get("name").asString();
			String lastDesc = descContent.get(descContent.size() - 1).get("name").asString();
			assertThat(firstDesc.compareToIgnoreCase(lastDesc)).as("First item should come after last in desc order").isGreaterThanOrEqualTo(0);
		}

		@Test
		void getResearchGroups_WithLimitMinusOne_ReturnsAllResults() throws Exception {
			TestUser head = createRandomTestUser(List.of("supervisor"));
			createTestResearchGroup("Unlimited RG", head.universityId());

			mockMvc.perform(MockMvcRequestBuilders.get("/v2/research-groups")
							.header("Authorization", createRandomAdminAuthentication())
							.param("limit", "-1"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content").isArray())
					.andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
		}
	}
}
