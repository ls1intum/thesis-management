package de.tum.cit.aet.thesis.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.core.organization.entity.School;
import de.tum.cit.aet.thesis.core.organization.entity.StudyProgram;
import de.tum.cit.aet.thesis.core.organization.repository.SchoolRepository;
import de.tum.cit.aet.thesis.core.organization.repository.StudyProgramRepository;
import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.core.user.repository.UserRepository;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.thesis.controller.payload.CreateThesisPayload;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
class ThesisStudyProgramTest extends BaseIntegrationTest {
	private static final String CIT_PORTAL = "https://portal.cit.example.org/";
	private static final String MGT_PORTAL = "https://portal.mgt.example.org/";

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private SchoolRepository schoolRepository;

	@Autowired
	private StudyProgramRepository studyProgramRepository;

	@Autowired
	private UserRepository userRepository;

	private School createSchool(String portal) {
		School school = new School();
		school.setName("School " + UUID.randomUUID());
		school.setAbbreviation("S" + UUID.randomUUID().toString().substring(0, 8));
		school.setThesisPortalUrl(portal);
		return schoolRepository.save(school);
	}

	private StudyProgram createProgram(School school) {
		StudyProgram program = new StudyProgram();
		program.setKey("KEY_" + UUID.randomUUID());
		program.setName("Program " + UUID.randomUUID());
		program.setSchool(school);
		return studyProgramRepository.save(program);
	}

	private void assignProgram(TestUser student, StudyProgram program) {
		User user = userRepository.findById(student.userId()).orElseThrow();
		user.setStudyProgram(program);
		userRepository.save(user);
	}

	private UUID createGroup(TestUser head, School school) throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("name", "Group " + UUID.randomUUID());
		body.put("abbreviation", UUID.randomUUID().toString());
		body.put("headUsername", head.universityId());
		if (school != null) {
			body.put("schoolId", school.getId().toString());
		}

		String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-groups")
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		return UUID.fromString(objectMapper.readTree(response).get("id").asString());
	}

	private JsonNode createThesis(TestUser staff, TestUser student, UUID groupId, UUID studyProgramId) throws Exception {
		createTestEmailTemplate("THESIS_CREATED");

		CreateThesisPayload payload = new CreateThesisPayload(
				"Thesis " + UUID.randomUUID(), "MASTER", "ENGLISH",
				List.of(student.userId()), List.of(), List.of(staff.userId()), List.of(staff.userId()),
				groupId, studyProgramId);

		String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		return objectMapper.readTree(response);
	}

	private String staffAuth(TestUser staff) {
		return generateTestAuthenticationHeader(staff.universityId(), List.of("supervisor", "advisor"));
	}

	@Test
	void createThesis_WithExplicitStudyProgram_UsesItAndItsSchoolPortal() throws Exception {
		TestUser staff = createRandomTestUser(List.of("supervisor", "advisor"));
		TestUser student = createRandomTestUser(List.of("student"));
		StudyProgram program = createProgram(createSchool(MGT_PORTAL));
		UUID groupId = createGroup(staff, createSchool(CIT_PORTAL));

		JsonNode thesis = createThesis(staff, student, groupId, program.getId());

		assertThat(thesis.get("studyProgram").get("id").asString()).isEqualTo(program.getId().toString());
		assertThat(thesis.get("submissionPortalUrl").asString()).isEqualTo(MGT_PORTAL);
	}

	@Test
	void createThesis_WithoutStudyProgram_TakesTheOneOfTheStudent() throws Exception {
		TestUser staff = createRandomTestUser(List.of("supervisor", "advisor"));
		TestUser student = createRandomTestUser(List.of("student"));
		StudyProgram program = createProgram(createSchool(MGT_PORTAL));
		assignProgram(student, program);
		UUID groupId = createGroup(staff, null);

		JsonNode thesis = createThesis(staff, student, groupId, null);

		assertThat(thesis.get("studyProgram").get("id").asString()).isEqualTo(program.getId().toString());
		assertThat(thesis.get("submissionPortalUrl").asString()).isEqualTo(MGT_PORTAL);
	}

	@Test
	void createThesis_StudyProgramWithoutSchoolPortal_FallsBackToResearchGroupSchool() throws Exception {
		TestUser staff = createRandomTestUser(List.of("supervisor", "advisor"));
		TestUser student = createRandomTestUser(List.of("student"));
		StudyProgram programWithoutSchool = createProgram(null);
		UUID groupId = createGroup(staff, createSchool(CIT_PORTAL));

		JsonNode thesis = createThesis(staff, student, groupId, programWithoutSchool.getId());

		assertThat(thesis.get("submissionPortalUrl").asString()).isEqualTo(CIT_PORTAL);
	}

	@Test
	void createThesis_SchoolWithoutPortal_FallsBackToResearchGroupSchool() throws Exception {
		TestUser staff = createRandomTestUser(List.of("supervisor", "advisor"));
		TestUser student = createRandomTestUser(List.of("student"));
		StudyProgram programWithPortallessSchool = createProgram(createSchool(null));
		UUID groupId = createGroup(staff, createSchool(CIT_PORTAL));

		JsonNode thesis = createThesis(staff, student, groupId, programWithPortallessSchool.getId());

		assertThat(thesis.get("submissionPortalUrl").asString()).isEqualTo(CIT_PORTAL);
	}

	@Test
	void createThesis_NothingDefined_OmitsPortalSoTheInstanceDefaultApplies() throws Exception {
		TestUser staff = createRandomTestUser(List.of("supervisor", "advisor"));
		TestUser student = createRandomTestUser(List.of("student"));
		UUID groupId = createGroup(staff, null);

		JsonNode thesis = createThesis(staff, student, groupId, null);

		assertThat(thesis.has("studyProgram")).isFalse();
		assertThat(thesis.has("submissionPortalUrl")).isFalse();
	}

	@Test
	void createThesis_UnknownStudyProgram_ReturnsNotFound() throws Exception {
		TestUser staff = createRandomTestUser(List.of("supervisor", "advisor"));
		TestUser student = createRandomTestUser(List.of("student"));
		UUID groupId = createGroup(staff, null);
		createTestEmailTemplate("THESIS_CREATED");

		CreateThesisPayload payload = new CreateThesisPayload(
				"Thesis", "MASTER", "ENGLISH", List.of(student.userId()), List.of(), List.of(staff.userId()),
				List.of(staff.userId()), groupId, UUID.randomUUID());

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isNotFound());
	}

	@Test
	void updateStudyProgram_AsSupervisor_ChangesProgramAndPortal() throws Exception {
		TestUser staff = createRandomTestUser(List.of("supervisor", "advisor"));
		TestUser student = createRandomTestUser(List.of("student"));
		UUID groupId = createGroup(staff, createSchool(CIT_PORTAL));
		JsonNode thesis = createThesis(staff, student, groupId, null);
		StudyProgram program = createProgram(createSchool(MGT_PORTAL));

		String response = mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/" + thesis.get("thesisId").asString() + "/study-program")
						.header("Authorization", staffAuth(staff))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("studyProgramId", program.getId().toString()))))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		JsonNode updated = objectMapper.readTree(response);
		assertThat(updated.get("studyProgram").get("id").asString()).isEqualTo(program.getId().toString());
		assertThat(updated.get("submissionPortalUrl").asString()).isEqualTo(MGT_PORTAL);

		String cleared = mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/" + thesis.get("thesisId").asString() + "/study-program")
						.header("Authorization", staffAuth(staff))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(objectMapper.readTree(cleared).has("studyProgram")).isFalse();
		assertThat(objectMapper.readTree(cleared).get("submissionPortalUrl").asString()).isEqualTo(CIT_PORTAL);
	}

	@Test
	void updateStudyProgram_AsSupervisorOfAnotherThesis_ReturnsForbidden() throws Exception {
		TestUser staff = createRandomTestUser(List.of("supervisor", "advisor"));
		TestUser otherStaff = createRandomTestUser(List.of("supervisor", "advisor"));
		TestUser student = createRandomTestUser(List.of("student"));
		JsonNode thesis = createThesis(staff, student, createGroup(staff, null), null);
		StudyProgram program = createProgram(null);

		mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/" + thesis.get("thesisId").asString() + "/study-program")
						.header("Authorization", staffAuth(otherStaff))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("studyProgramId", program.getId().toString()))))
				.andExpect(status().isForbidden());
	}

	@Test
	void updateStudyProgram_DeactivatedProgram_CanBeKeptButNotNewlySelected() throws Exception {
		TestUser staff = createRandomTestUser(List.of("supervisor", "advisor"));
		TestUser student = createRandomTestUser(List.of("student"));
		StudyProgram program = createProgram(createSchool(MGT_PORTAL));
		UUID groupId = createGroup(staff, null);
		JsonNode thesis = createThesis(staff, student, groupId, program.getId());
		String path = "/v2/theses/" + thesis.get("thesisId").asString() + "/study-program";
		String body = objectMapper.writeValueAsString(Map.of("studyProgramId", program.getId().toString()));

		program.setActive(false);
		studyProgramRepository.save(program);

		// the program the thesis already has stays valid
		mockMvc.perform(MockMvcRequestBuilders.put(path).header("Authorization", staffAuth(staff))
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk());

		// but another thesis cannot newly pick it
		JsonNode otherThesis = createThesis(staff, createRandomTestUser(List.of("student")), groupId, null);
		mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/" + otherThesis.get("thesisId").asString() + "/study-program")
						.header("Authorization", staffAuth(staff))
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void updateStudyProgram_AsStudentOfTheThesis_ReturnsForbidden() throws Exception {
		TestUser staff = createRandomTestUser(List.of("supervisor", "advisor"));
		TestUser student = createRandomTestUser(List.of("student"));
		UUID groupId = createGroup(staff, null);
		JsonNode thesis = createThesis(staff, student, groupId, null);
		StudyProgram program = createProgram(null);

		mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/" + thesis.get("thesisId").asString() + "/study-program")
						.header("Authorization", generateTestAuthenticationHeader(student.universityId(), List.of("student")))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("studyProgramId", program.getId().toString()))))
				.andExpect(status().isForbidden());
	}
}
