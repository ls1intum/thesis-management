package de.tum.cit.aet.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.constants.ThesisPresentationType;
import de.tum.cit.aet.thesis.constants.ThesisPresentationVisibility;
import de.tum.cit.aet.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.constants.ThesisVisibility;
import de.tum.cit.aet.thesis.controller.payload.AddThesisGradePayload;
import de.tum.cit.aet.thesis.controller.payload.CreateAssessmentPayload;
import de.tum.cit.aet.thesis.controller.payload.GradeComponentPayload;
import de.tum.cit.aet.thesis.controller.payload.ReplacePresentationPayload;
import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.repository.ThesisAssessmentRepository;
import de.tum.cit.aet.thesis.repository.ThesisRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Testcontainers
class ThesisAssessmentControllerTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private ThesisRepository thesisRepository;

	@Autowired
	private ThesisAssessmentRepository thesisAssessmentRepository;

	@Test
	void createAssessment_Success() throws Exception {
		UUID thesisId = createTestThesis("Test Thesis");
		createTestEmailTemplate("THESIS_ASSESSMENT_ADDED");

		CreateAssessmentPayload payload = new CreateAssessmentPayload(
				"Test summary", "Test positives", "Test negatives", "1.0", null
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/assessment", thesisId)
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isOk());

		Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
		assertThat(thesis.getState()).isEqualTo(ThesisState.ASSESSED);
		assertThat(thesisAssessmentRepository.count()).isEqualTo(1);
	}

	@Test
	void createAssessment_WithGradeComponents_Success() throws Exception {
		UUID thesisId = createTestThesis("Test Thesis With Components");
		createTestEmailTemplate("THESIS_ASSESSMENT_ADDED");

		CreateAssessmentPayload payload = new CreateAssessmentPayload(
				"Summary", "Positives", "Negatives", "1.5",
				List.of(
						new GradeComponentPayload("Content", new BigDecimal("40.00"), false, new BigDecimal("1.3")),
						new GradeComponentPayload("Methodology", new BigDecimal("30.00"), false, new BigDecimal("1.7")),
						new GradeComponentPayload("Presentation", new BigDecimal("30.00"), false, new BigDecimal("2.0"))
				)
		);

		String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/assessment", thesisId)
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		JsonNode json = objectMapper.readTree(response);
		assertThat(json.get("state").asString()).isEqualTo("ASSESSED");

		JsonNode assessment = json.get("assessment");
		assertThat(assessment).isNotNull();
		assertThat(assessment.get("summary").asString()).isEqualTo("Summary");
		assertThat(assessment.get("gradeSuggestion").asString()).isEqualTo("1.5");

		JsonNode gradeComponents = assessment.get("gradeComponents");
		assertThat(gradeComponents.size()).isEqualTo(3);

		assertThat(gradeComponents.get(0).get("name").asString()).isEqualTo("Content");
		assertThat(gradeComponents.get(0).get("weight").asDouble()).isEqualTo(40.0);
		assertThat(gradeComponents.get(0).get("isBonus").asBoolean()).isFalse();
		assertThat(gradeComponents.get(0).get("grade").asDouble()).isEqualTo(1.3);
		assertThat(gradeComponents.get(0).get("position").asInt()).isEqualTo(0);

		assertThat(gradeComponents.get(1).get("name").asString()).isEqualTo("Methodology");
		assertThat(gradeComponents.get(1).get("weight").asDouble()).isEqualTo(30.0);
		assertThat(gradeComponents.get(1).get("grade").asDouble()).isEqualTo(1.7);
		assertThat(gradeComponents.get(1).get("position").asInt()).isEqualTo(1);

		assertThat(gradeComponents.get(2).get("name").asString()).isEqualTo("Presentation");
		assertThat(gradeComponents.get(2).get("weight").asDouble()).isEqualTo(30.0);
		assertThat(gradeComponents.get(2).get("grade").asDouble()).isEqualTo(2.0);
		assertThat(gradeComponents.get(2).get("position").asInt()).isEqualTo(2);
	}

	@Test
	void createAssessment_WithBonusComponent_Success() throws Exception {
		UUID thesisId = createTestThesis("Test Thesis With Bonus");
		createTestEmailTemplate("THESIS_ASSESSMENT_ADDED");

		CreateAssessmentPayload payload = new CreateAssessmentPayload(
				"Summary", "Positives", "Negatives", "1.0",
				List.of(
						new GradeComponentPayload("Content", new BigDecimal("50.00"), false, new BigDecimal("1.3")),
						new GradeComponentPayload("Methods", new BigDecimal("50.00"), false, new BigDecimal("1.7")),
						new GradeComponentPayload("Bonus", new BigDecimal("0.00"), true, new BigDecimal("0.3"))
				)
		);

		String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/assessment", thesisId)
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		JsonNode json = objectMapper.readTree(response);
		JsonNode gradeComponents = json.get("assessment").get("gradeComponents");
		assertThat(gradeComponents.size()).isEqualTo(3);
		assertThat(gradeComponents.get(2).get("isBonus").asBoolean()).isTrue();
		assertThat(gradeComponents.get(2).get("grade").asDouble()).isEqualTo(0.3);
	}

	@Test
	void createAssessment_WithGradeComponents_WeightsNotSumTo100_Fails() throws Exception {
		UUID thesisId = createTestThesis("Test Thesis Invalid Weights");
		createTestEmailTemplate("THESIS_ASSESSMENT_ADDED");

		CreateAssessmentPayload payload = new CreateAssessmentPayload(
				"Summary", "Positives", "Negatives", "1.5",
				List.of(
						new GradeComponentPayload("Content", new BigDecimal("40.00"), false, new BigDecimal("1.3")),
						new GradeComponentPayload("Methods", new BigDecimal("30.00"), false, new BigDecimal("1.7"))
				)
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/assessment", thesisId)
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAssessment_WithGradeComponents_InvalidGradeRange_Fails() throws Exception {
		UUID thesisId = createTestThesis("Test Thesis Invalid Grade");
		createTestEmailTemplate("THESIS_ASSESSMENT_ADDED");

		CreateAssessmentPayload payload = new CreateAssessmentPayload(
				"Summary", "Positives", "Negatives", "1.5",
				List.of(
						new GradeComponentPayload("Content", new BigDecimal("100.00"), false, new BigDecimal("6.0"))
				)
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/assessment", thesisId)
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAssessment_WithGradeComponents_EmptyName_Fails() throws Exception {
		UUID thesisId = createTestThesis("Test Thesis Empty Name");
		createTestEmailTemplate("THESIS_ASSESSMENT_ADDED");

		CreateAssessmentPayload payload = new CreateAssessmentPayload(
				"Summary", "Positives", "Negatives", "1.5",
				List.of(
						new GradeComponentPayload("", new BigDecimal("100.00"), false, new BigDecimal("1.3"))
				)
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/assessment", thesisId)
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAssessment_WithNegativeBonusGrade_Fails() throws Exception {
		UUID thesisId = createTestThesis("Test Thesis Negative Bonus");
		createTestEmailTemplate("THESIS_ASSESSMENT_ADDED");

		CreateAssessmentPayload payload = new CreateAssessmentPayload(
				"Summary", "Positives", "Negatives", "1.0",
				List.of(
						new GradeComponentPayload("Content", new BigDecimal("100.00"), false, new BigDecimal("1.3")),
						new GradeComponentPayload("Bonus", new BigDecimal("0.00"), true, new BigDecimal("-0.3"))
				)
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/assessment", thesisId)
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAssessment_WithBonusGradeExceedingMax_Fails() throws Exception {
		UUID thesisId = createTestThesis("Test Thesis Bonus Too High");
		createTestEmailTemplate("THESIS_ASSESSMENT_ADDED");

		CreateAssessmentPayload payload = new CreateAssessmentPayload(
				"Summary", "Positives", "Negatives", "1.0",
				List.of(
						new GradeComponentPayload("Content", new BigDecimal("100.00"), false, new BigDecimal("1.3")),
						new GradeComponentPayload("Bonus", new BigDecimal("0.00"), true, new BigDecimal("5.1"))
				)
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/assessment", thesisId)
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAssessment_WithBonusZero_Success() throws Exception {
		UUID thesisId = createTestThesis("Test Thesis Bonus Zero");
		createTestEmailTemplate("THESIS_ASSESSMENT_ADDED");

		CreateAssessmentPayload payload = new CreateAssessmentPayload(
				"Summary", "Positives", "Negatives", "1.0",
				List.of(
						new GradeComponentPayload("Content", new BigDecimal("100.00"), false, new BigDecimal("1.3")),
						new GradeComponentPayload("Bonus", new BigDecimal("0.00"), true, new BigDecimal("0.0"))
				)
		);

		String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/assessment", thesisId)
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		JsonNode json = objectMapper.readTree(response);
		JsonNode gradeComponents = json.get("assessment").get("gradeComponents");
		assertThat(gradeComponents.size()).isEqualTo(2);
		assertThat(gradeComponents.get(1).get("isBonus").asBoolean()).isTrue();
		assertThat(gradeComponents.get(1).get("grade").asDouble()).isEqualTo(0.0);
	}

	@Test
	void createAssessment_WithGradePrecisionExceeded_Fails() throws Exception {
		UUID thesisId = createTestThesis("Test Thesis Precision");
		createTestEmailTemplate("THESIS_ASSESSMENT_ADDED");

		CreateAssessmentPayload payload = new CreateAssessmentPayload(
				"Summary", "Positives", "Negatives", "1.0",
				List.of(
						new GradeComponentPayload("Content", new BigDecimal("100.00"), false, new BigDecimal("1.35"))
				)
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/assessment", thesisId)
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAssessment_WithNullGrade_Fails() throws Exception {
		UUID thesisId = createTestThesis("Test Thesis Null Grade");
		createTestEmailTemplate("THESIS_ASSESSMENT_ADDED");

		CreateAssessmentPayload payload = new CreateAssessmentPayload(
				"Summary", "Positives", "Negatives", "1.0",
				List.of(
						new GradeComponentPayload("Content", new BigDecimal("100.00"), false, null)
				)
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/assessment", thesisId)
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAssessment_WithoutGradeComponents_BackwardCompatible() throws Exception {
		UUID thesisId = createTestThesis("Test Thesis No Components");
		createTestEmailTemplate("THESIS_ASSESSMENT_ADDED");

		CreateAssessmentPayload payload = new CreateAssessmentPayload(
				"Summary", "Positives", "Negatives", "1.5", null
		);

		String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/assessment", thesisId)
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		JsonNode json = objectMapper.readTree(response);
		assertThat(json.get("state").asString()).isEqualTo("ASSESSED");
		assertThat(json.get("assessment").get("gradeSuggestion").asString()).isEqualTo("1.5");
	}

	@Test
	void createAssessment_WithGradeComponents_PdfContainsComponents() throws Exception {
		UUID thesisId = createTestThesis("Test Thesis PDF Components");
		createTestEmailTemplate("THESIS_ASSESSMENT_ADDED");
		String adminAuth = createRandomAdminAuthentication();

		ReplacePresentationPayload presentationPayload = new ReplacePresentationPayload(
				ThesisPresentationType.FINAL,
				ThesisPresentationVisibility.PUBLIC,
				"Room 101", "http://stream.url", "English",
				Instant.now().plusSeconds(86400)
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/presentations", thesisId)
						.header("Authorization", adminAuth)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(presentationPayload)))
				.andExpect(status().isOk());

		CreateAssessmentPayload payload = new CreateAssessmentPayload(
				"Summary", "Positives", "Negatives", "1.5",
				List.of(
						new GradeComponentPayload("Content", new BigDecimal("60.00"), false, new BigDecimal("1.3")),
						new GradeComponentPayload("Methods", new BigDecimal("40.00"), false, new BigDecimal("1.7"))
				)
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/assessment", thesisId)
						.header("Authorization", adminAuth)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isOk());

		var result = mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses/{thesisId}/assessment", thesisId)
						.header("Authorization", adminAuth))
				.andExpect(status().isOk())
				.andReturn();

		assertThat(result.getResponse().getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
		assertThat(result.getResponse().getContentLength()).isGreaterThan(0);
	}

	@Test
	void addGrade_Success() throws Exception {
		UUID thesisId = createTestThesis("Test Thesis");
		createTestEmailTemplate("THESIS_FINAL_GRADE");

		AddThesisGradePayload payload = new AddThesisGradePayload(
				"1.0", "Great work", ThesisVisibility.PUBLIC
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/grade", thesisId)
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isOk());

		Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
		assertThat(thesis.getState()).isEqualTo(ThesisState.GRADED);
	}

	@Test
	void gradeThesis_VerifyDatabaseState() throws Exception {
		UUID thesisId = createTestThesis("Test Thesis");
		createTestEmailTemplate("THESIS_FINAL_GRADE");

		AddThesisGradePayload payload = new AddThesisGradePayload(
				"1.3", "Excellent work with minor issues", ThesisVisibility.PUBLIC
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/grade", thesisId)
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isOk());

		Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
		assertThat(thesis.getFinalGrade()).isEqualTo("1.3");
		assertThat(thesis.getFinalFeedback()).isEqualTo("Excellent work with minor issues");
		assertThat(thesis.getVisibility()).isEqualTo(ThesisVisibility.PUBLIC);
		assertThat(thesis.getState()).isEqualTo(ThesisState.GRADED);
	}

	@Test
	void completeThesis_Success() throws Exception {
		UUID thesisId = createTestThesis("Test Thesis");
		createTestEmailTemplate("THESIS_FINAL_GRADE");

		AddThesisGradePayload gradePayload = new AddThesisGradePayload(
				"1.0", "Perfect", ThesisVisibility.PUBLIC
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/grade", thesisId)
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(gradePayload)))
				.andExpect(status().isOk());

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/complete", thesisId)
						.header("Authorization", createRandomAdminAuthentication()))
				.andExpect(status().isOk());

		Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
		assertThat(thesis.getState()).isEqualTo(ThesisState.FINISHED);
	}

	@Test
	void completeThesis_AccessDenied_AsStudent() throws Exception {
		UUID thesisId = createTestThesis("Test Thesis");
		String studentAuth = createRandomAuthentication("student");

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/complete", thesisId)
						.header("Authorization", studentAuth))
				.andExpect(status().isForbidden());

		Thesis thesis = thesisRepository.findById(thesisId).orElseThrow();
		assertThat(thesis.getState()).isEqualTo(ThesisState.PROPOSAL);
	}

	@Test
	void getAssessmentPdf_Success() throws Exception {
		UUID thesisId = createTestThesis("Test Thesis");
		createTestEmailTemplate("THESIS_ASSESSMENT_ADDED");
		String adminAuth = createRandomAdminAuthentication();

		ReplacePresentationPayload presentationPayload = new ReplacePresentationPayload(
				ThesisPresentationType.FINAL,
				ThesisPresentationVisibility.PUBLIC,
				"Room 101", "http://stream.url", "English",
				Instant.now().plusSeconds(86400)
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/presentations", thesisId)
						.header("Authorization", adminAuth)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(presentationPayload)))
				.andExpect(status().isOk());

		CreateAssessmentPayload assessmentPayload = new CreateAssessmentPayload(
				"Summary", "Positives", "Negatives", "1.7", null
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/assessment", thesisId)
						.header("Authorization", adminAuth)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(assessmentPayload)))
				.andExpect(status().isOk());

		var result = mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses/{thesisId}/assessment", thesisId)
						.header("Authorization", adminAuth))
				.andExpect(status().isOk())
				.andReturn();

		assertThat(result.getResponse().getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
		assertThat(result.getResponse().getContentLength()).isGreaterThan(0);
	}
}
