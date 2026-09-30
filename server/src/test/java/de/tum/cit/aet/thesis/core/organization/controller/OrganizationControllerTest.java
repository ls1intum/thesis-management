package de.tum.cit.aet.thesis.core.organization.controller;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Testcontainers
class OrganizationControllerTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	private String unique(String prefix) {
		return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
	}

	private String abbreviation() {
		return "A" + UUID.randomUUID().toString().substring(0, 8);
	}

	private JsonNode post(String path, String auth, Map<String, Object> body, int expectedStatus) throws Exception {
		String response = mockMvc.perform(MockMvcRequestBuilders.post(path)
						.header("Authorization", auth)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().is(expectedStatus))
				.andReturn().getResponse().getContentAsString();

		return response.isBlank() ? null : objectMapper.readTree(response);
	}

	private JsonNode put(String path, String auth, Map<String, Object> body, int expectedStatus) throws Exception {
		String response = mockMvc.perform(MockMvcRequestBuilders.put(path)
						.header("Authorization", auth)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().is(expectedStatus))
				.andReturn().getResponse().getContentAsString();

		return response.isBlank() ? null : objectMapper.readTree(response);
	}

	private Map<String, Object> schoolBody(String name, String abbreviation, String portal) {
		Map<String, Object> body = new HashMap<>();
		body.put("name", name);
		body.put("abbreviation", abbreviation);
		body.put("thesisPortalUrl", portal);
		return body;
	}

	private UUID createSchool(String auth, String portal) throws Exception {
		JsonNode school = post("/v2/schools", auth, schoolBody(unique("School"), abbreviation(), portal), 200);
		return UUID.fromString(school.get("id").asString());
	}

	@Nested
	class Schools {
		@Test
		void getSchools_Unauthenticated_ReturnsUnauthorized() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/v2/schools")).andExpect(status().isUnauthorized());
		}

		@Test
		void getSchools_AsStudent_ReturnsSchoolsWithDepartments() throws Exception {
			String admin = createRandomAdminAuthentication();
			UUID schoolId = createSchool(admin, "https://portal.example.org/");
			post("/v2/departments", admin, Map.of("schoolId", schoolId.toString(), "name", unique("Dept")), 200);

			String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/schools")
							.header("Authorization", createRandomAuthentication("student")))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode created = null;
			for (JsonNode school : objectMapper.readTree(response)) {
				if (school.get("id").asString().equals(schoolId.toString())) {
					created = school;
				}
			}

			assertThat(created).isNotNull();
			assertThat(created.get("thesisPortalUrl").asString()).isEqualTo("https://portal.example.org/");
			assertThat(created.get("departments")).hasSize(1);
		}

		@Test
		void createSchool_AsStudent_ReturnsForbidden() throws Exception {
			post("/v2/schools", createRandomAuthentication("student"), schoolBody(unique("School"), abbreviation(), null), 403);
		}

		@Test
		void createSchool_DuplicateAbbreviation_ReturnsConflict() throws Exception {
			String admin = createRandomAdminAuthentication();
			String abbreviation = abbreviation();
			post("/v2/schools", admin, schoolBody(unique("School"), abbreviation, null), 200);

			post("/v2/schools", admin, schoolBody(unique("School"), abbreviation.toLowerCase(), null), 409);
		}

		@Test
		void createSchool_PortalUrlWithUnsafeScheme_ReturnsBadRequest() throws Exception {
			String admin = createRandomAdminAuthentication();

			post("/v2/schools", admin, schoolBody(unique("School"), abbreviation(), "javascript:alert(1)"), 400);
			post("/v2/schools", admin, schoolBody(unique("School"), abbreviation(), "not a url"), 400);
		}

		@Test
		void updateSchool_ChangesPortalAndDeactivates() throws Exception {
			String admin = createRandomAdminAuthentication();
			UUID schoolId = createSchool(admin, "https://old.example.org/");

			Map<String, Object> body = schoolBody(unique("Renamed"), abbreviation(), "https://new.example.org/");
			body.put("active", false);
			JsonNode updated = put("/v2/schools/" + schoolId, admin, body, 200);

			assertThat(updated.get("thesisPortalUrl").asString()).isEqualTo("https://new.example.org/");
			assertThat(updated.has("active") && updated.get("active").asBoolean()).isFalse();
		}

		@Test
		void deleteSchool_WithDepartment_ReturnsBadRequest_ThenSucceedsWhenUnused() throws Exception {
			String admin = createRandomAdminAuthentication();
			UUID schoolId = createSchool(admin, null);
			JsonNode department = post("/v2/departments", admin, Map.of("schoolId", schoolId.toString(), "name", unique("Dept")), 200);

			mockMvc.perform(MockMvcRequestBuilders.delete("/v2/schools/" + schoolId).header("Authorization", admin))
					.andExpect(status().isBadRequest());

			mockMvc.perform(MockMvcRequestBuilders.delete("/v2/departments/" + department.get("id").asString()).header("Authorization", admin))
					.andExpect(status().isNoContent());
			mockMvc.perform(MockMvcRequestBuilders.delete("/v2/schools/" + schoolId).header("Authorization", admin))
					.andExpect(status().isNoContent());
		}
	}

	@Nested
	class Departments {
		@Test
		void updateDepartment_MoveToOtherSchool_ReturnsBadRequest() throws Exception {
			String admin = createRandomAdminAuthentication();
			UUID schoolId = createSchool(admin, null);
			UUID otherSchoolId = createSchool(admin, null);
			JsonNode department = post("/v2/departments", admin, Map.of("schoolId", schoolId.toString(), "name", unique("Dept")), 200);

			put("/v2/departments/" + department.get("id").asString(), admin,
					Map.of("schoolId", otherSchoolId.toString(), "name", "Moved"), 400);
		}

		@Test
		void createDepartment_DuplicateNameInSchool_ReturnsConflict() throws Exception {
			String admin = createRandomAdminAuthentication();
			UUID schoolId = createSchool(admin, null);
			String name = unique("Dept");
			post("/v2/departments", admin, Map.of("schoolId", schoolId.toString(), "name", name), 200);

			post("/v2/departments", admin, Map.of("schoolId", schoolId.toString(), "name", name.toUpperCase()), 409);
		}

		@Test
		void createDepartment_AsStudent_ReturnsForbidden() throws Exception {
			String admin = createRandomAdminAuthentication();
			UUID schoolId = createSchool(admin, null);

			post("/v2/departments", createRandomAuthentication("student"), Map.of("schoolId", schoolId.toString(), "name", unique("Dept")), 403);
		}
	}

	@Nested
	class StudyPrograms {
		@Test
		void createStudyProgram_WithoutKey_GeneratesKeyFromName() throws Exception {
			String admin = createRandomAdminAuthentication();
			UUID schoolId = createSchool(admin, null);
			String name = unique("Data Science");

			JsonNode program = post("/v2/study-programs", admin, Map.of("name", name, "schoolId", schoolId.toString()), 200);

			assertThat(program.get("key").asString()).startsWith("DATA_SCIENCE_");
			assertThat(program.get("school").get("id").asString()).isEqualTo(schoolId.toString());
		}

		@Test
		void createStudyProgram_DuplicateKey_ReturnsConflict() throws Exception {
			String admin = createRandomAdminAuthentication();
			String key = unique("KEY").replace(' ', '_');
			post("/v2/study-programs", admin, Map.of("name", unique("Program"), "key", key), 200);

			post("/v2/study-programs", admin, Map.of("name", unique("Program"), "key", key.toLowerCase()), 409);
		}

		@Test
		void createStudyProgram_AsSupervisor_ReturnsForbidden() throws Exception {
			post("/v2/study-programs", createRandomAuthentication("supervisor"), Map.of("name", unique("Program")), 403);
		}

		@Test
		void deleteStudyProgram_UsedByStudent_ReturnsBadRequest() throws Exception {
			String admin = createRandomAdminAuthentication();
			JsonNode program = post("/v2/study-programs", admin, Map.of("name", unique("Program")), 200);
			TestUser student = createRandomTestUser(List.of("student"));
			updateStudyProgram(student.universityId(), program.get("id").asString(), 200);

			mockMvc.perform(MockMvcRequestBuilders.delete("/v2/study-programs/" + program.get("id").asString()).header("Authorization", admin))
					.andExpect(status().isBadRequest());
		}

		@Test
		void updateUserInfo_StudyProgram_IsStoredAndReturned() throws Exception {
			String admin = createRandomAdminAuthentication();
			JsonNode program = post("/v2/study-programs", admin, Map.of("name", "Informatics " + UUID.randomUUID()), 200);
			TestUser student = createRandomTestUser(List.of("student"));

			JsonNode user = updateStudyProgram(student.universityId(), program.get("id").asString(), 200);

			assertThat(user.get("studyProgramId").asString()).isEqualTo(program.get("id").asString());
			assertThat(user.get("studyProgram").asString()).isEqualTo(program.get("name").asString());
		}

		@Test
		void updateUserInfo_DeactivatedStudyProgram_CannotBeNewlySelected() throws Exception {
			String admin = createRandomAdminAuthentication();
			JsonNode program = post("/v2/study-programs", admin, Map.of("name", unique("Program"), "active", false), 200);
			TestUser student = createRandomTestUser(List.of("student"));

			updateStudyProgram(student.universityId(), program.get("id").asString(), 400);
		}

		@Test
		void updateUserInfo_UnknownStudyProgram_ReturnsNotFound() throws Exception {
			TestUser student = createRandomTestUser(List.of("student"));

			updateStudyProgram(student.universityId(), UUID.randomUUID().toString(), 404);
		}

		private JsonNode updateStudyProgram(String universityId, String studyProgramId, int expectedStatus) throws Exception {
			Map<String, Object> data = new HashMap<>();
			data.put("firstName", "Ada");
			data.put("lastName", "Lovelace");
			data.put("email", "ada@example.com");
			data.put("studyProgramId", studyProgramId);
			data.put("customData", Map.of());

			MockMultipartFile dataPart = new MockMultipartFile("data", "", "application/json", objectMapper.writeValueAsBytes(data));

			String response = mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/user-info")
							.file(dataPart)
							.with(request -> {
								request.setMethod("PUT");
								return request;
							})
							.header("Authorization", generateTestAuthenticationHeader(universityId, List.of("student")))
							.contentType(MediaType.MULTIPART_FORM_DATA))
					.andExpect(status().is(expectedStatus))
					.andReturn().getResponse().getContentAsString();

			return response.isBlank() ? null : objectMapper.readTree(response);
		}
	}

	@Nested
	class ResearchGroups {
		private Map<String, Object> groupBody(TestUser head, UUID schoolId, UUID departmentId) {
			Map<String, Object> body = new HashMap<>();
			body.put("name", unique("Group"));
			body.put("abbreviation", UUID.randomUUID().toString());
			body.put("headUsername", head.universityId());
			if (schoolId != null) {
				body.put("schoolId", schoolId.toString());
			}
			if (departmentId != null) {
				body.put("departmentId", departmentId.toString());
			}
			return body;
		}

		@Test
		void createResearchGroup_WithSchoolAndDepartment_ReturnsBoth() throws Exception {
			String admin = createRandomAdminAuthentication();
			UUID schoolId = createSchool(admin, null);
			JsonNode department = post("/v2/departments", admin, Map.of("schoolId", schoolId.toString(), "name", unique("Dept")), 200);
			TestUser head = createRandomTestUser(List.of("supervisor"));

			JsonNode group = post("/v2/research-groups", admin, groupBody(head, schoolId, UUID.fromString(department.get("id").asString())), 200);

			assertThat(group.get("school").get("id").asString()).isEqualTo(schoolId.toString());
			assertThat(group.get("department").get("id").asString()).isEqualTo(department.get("id").asString());
		}

		@Test
		void createResearchGroup_DepartmentOfOtherSchool_ReturnsBadRequest() throws Exception {
			String admin = createRandomAdminAuthentication();
			UUID schoolId = createSchool(admin, null);
			UUID otherSchoolId = createSchool(admin, null);
			JsonNode department = post("/v2/departments", admin, Map.of("schoolId", otherSchoolId.toString(), "name", unique("Dept")), 200);
			TestUser head = createRandomTestUser(List.of("supervisor"));

			post("/v2/research-groups", admin, groupBody(head, schoolId, UUID.fromString(department.get("id").asString())), 400);
		}

		@Test
		void createResearchGroup_DepartmentWithoutSchool_ReturnsBadRequest() throws Exception {
			String admin = createRandomAdminAuthentication();
			UUID schoolId = createSchool(admin, null);
			JsonNode department = post("/v2/departments", admin, Map.of("schoolId", schoolId.toString(), "name", unique("Dept")), 200);
			TestUser head = createRandomTestUser(List.of("supervisor"));

			post("/v2/research-groups", admin, groupBody(head, null, UUID.fromString(department.get("id").asString())), 400);
		}

		@Test
		void updateResearchGroup_ChangesAndClearsSchool() throws Exception {
			String admin = createRandomAdminAuthentication();
			UUID schoolId = createSchool(admin, null);
			TestUser head = createRandomTestUser(List.of("supervisor"));
			JsonNode group = post("/v2/research-groups", admin, groupBody(head, null, null), 200);
			assertThat(group.has("school")).isFalse();

			Map<String, Object> update = groupBody(head, schoolId, null);
			update.put("name", group.get("name").asString());
			update.put("abbreviation", group.get("abbreviation").asString());
			JsonNode updated = put("/v2/research-groups/" + group.get("id").asString(), admin, update, 200);
			assertThat(updated.get("school").get("id").asString()).isEqualTo(schoolId.toString());

			update.remove("schoolId");
			JsonNode cleared = put("/v2/research-groups/" + group.get("id").asString(), admin, update, 200);
			assertThat(cleared.has("school")).isFalse();
		}
	}
}
