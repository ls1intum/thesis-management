package de.tum.cit.aet.thesis.feedback.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.feedback.dto.AIFeedbackDraftDTO;
import de.tum.cit.aet.thesis.feedback.dto.AIPreviewResponseDTO;
import de.tum.cit.aet.thesis.feedback.dto.FeedbackClassificationDTO;
import de.tum.cit.aet.thesis.feedback.model.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import de.tum.cit.aet.thesis.feedback.service.AIFeedbackService;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackCategory;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSeverity;
import de.tum.cit.aet.thesis.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.thesis.entity.Thesis;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

@Testcontainers
@TestPropertySource(properties = "thesis-management.ai.enabled=true")
class ReviewControllerTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@MockitoBean
	private AIFeedbackService aiFeedbackService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void preview_returnsMockedDraftsForSupervisor() throws Exception {
		UUID thesisId = createTestThesis("AI review preview test");

		stubPreviewResponse();

		String body = "{\"thesisId\":\"" + thesisId + "\",\"reviewType\":\"PROPOSAL\"}";
		mockMvc.perform(post("/v2/ai-review/preview")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", createRandomAdminAuthentication()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.assessment").value("ACCEPTABLE"))
				.andExpect(jsonPath("$.score").value(62))
				.andExpect(jsonPath("$.summary").value("Solid overall but bibliography is thin."))
				.andExpect(jsonPath("$.drafts[0].category").value("CITATION"))
				.andExpect(jsonPath("$.drafts[0].severity").value("MAJOR"));

		verify(aiFeedbackService).previewReview(any(Thesis.class), any(ReviewType.class));
	}

	@Test
	void preview_returnsMockedDraftsForSupervisorGroup() throws Exception {
		UUID thesisId = createTestThesis("AI review preview supervisor test");
		// advisor group + SUPERVISOR thesis role clears both the hasAnyRole('advisor')
		// gate and the inner hasSupervisorAccess check.
		TestUser supervisor = addThesisRole(thesisId, "SUPERVISOR", "advisor");
		stubPreviewResponse();

		String body = "{\"thesisId\":\"" + thesisId + "\",\"reviewType\":\"PROPOSAL\"}";
		mockMvc.perform(post("/v2/ai-review/preview")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", authFor(supervisor, "advisor")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.assessment").value("ACCEPTABLE"));

		verify(aiFeedbackService).previewReview(any(Thesis.class), any(ReviewType.class));
	}

	@Test
	void preview_returnsMockedDraftsForExaminerGroup() throws Exception {
		UUID thesisId = createTestThesis("AI review preview examiner test");
		// supervisor group + EXAMINER thesis role clears both the hasAnyRole('supervisor')
		// gate and the inner hasSupervisorAccess check (via hasExaminerAccess).
		TestUser examiner = addThesisRole(thesisId, "EXAMINER", "supervisor");
		stubPreviewResponse();

		String body = "{\"thesisId\":\"" + thesisId + "\",\"reviewType\":\"PROPOSAL\"}";
		mockMvc.perform(post("/v2/ai-review/preview")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", authFor(examiner, "supervisor")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.assessment").value("ACCEPTABLE"));

		verify(aiFeedbackService).previewReview(any(Thesis.class), any(ReviewType.class));
	}

	@Test
	void preview_returnsForbiddenForStudent() throws Exception {
		UUID thesisId = createTestThesis("AI review preview forbidden test");

		String body = "{\"thesisId\":\"" + thesisId + "\",\"reviewType\":\"PROPOSAL\"}";
		mockMvc.perform(post("/v2/ai-review/preview")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", createRandomAuthentication("student")))
				.andExpect(status().isForbidden());
	}

	@Test
	void auto_returnsForbiddenForRandomOutsider() throws Exception {
		UUID thesisId = createTestThesis("AI review auto outsider test");

		String body = "{\"thesisId\":\"" + thesisId + "\",\"reviewType\":\"PROPOSAL\"}";
		mockMvc.perform(post("/v2/ai-review/auto")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", createRandomAuthentication("student")))
				.andExpect(status().isForbidden());
	}

	@Test
	void preview_returnsUnauthorizedWithoutAuthentication() throws Exception {
		String body = "{\"thesisId\":\"" + UUID.randomUUID() + "\",\"reviewType\":\"PROPOSAL\"}";
		mockMvc.perform(post("/v2/ai-review/preview")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void auto_returnsUnauthorizedWithoutAuthentication() throws Exception {
		String body = "{\"thesisId\":\"" + UUID.randomUUID() + "\",\"reviewType\":\"PROPOSAL\"}";
		mockMvc.perform(post("/v2/ai-review/auto")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void auto_rejectsThesisReviewFromStudentDuringProposalPhase() throws Exception {
		UUID thesisId = createTestThesis("AI review proposal-phase gate test");
		setThesisState(thesisId, ThesisState.PROPOSAL);
		TestUser student = addThesisRole(thesisId, "STUDENT", "student");

		String body = "{\"thesisId\":\"" + thesisId + "\",\"reviewType\":\"THESIS\"}";
		mockMvc.perform(post("/v2/ai-review/auto")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", authFor(student, "student")))
				.andExpect(status().isBadRequest());

		// The wrong-phase request must be rejected before the review pipeline runs.
		verify(aiFeedbackService, never()).autoReviewAndSave(any(Thesis.class), any(ReviewType.class));
	}

	@Test
	void auto_rejectsProposalReviewFromStudentDuringWritingPhase() throws Exception {
		UUID thesisId = createTestThesis("AI review writing-phase gate test");
		setThesisState(thesisId, ThesisState.WRITING);
		TestUser student = addThesisRole(thesisId, "STUDENT", "student");

		String body = "{\"thesisId\":\"" + thesisId + "\",\"reviewType\":\"PROPOSAL\"}";
		mockMvc.perform(post("/v2/ai-review/auto")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", authFor(student, "student")))
				.andExpect(status().isBadRequest());

		verify(aiFeedbackService, never()).autoReviewAndSave(any(Thesis.class), any(ReviewType.class));
	}

	@Test
	void auto_rejectsStudentReviewOutsideProposalAndWritingPhases() throws Exception {
		UUID thesisId = createTestThesis("AI review submitted-phase gate test");
		setThesisState(thesisId, ThesisState.SUBMITTED);
		TestUser student = addThesisRole(thesisId, "STUDENT", "student");

		String body = "{\"thesisId\":\"" + thesisId + "\",\"reviewType\":\"THESIS\"}";
		mockMvc.perform(post("/v2/ai-review/auto")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", authFor(student, "student")))
				.andExpect(status().isBadRequest());

		verify(aiFeedbackService, never()).autoReviewAndSave(any(Thesis.class), any(ReviewType.class));
	}

	@Test
	void auto_allowsMatchingProposalReviewFromStudentDuringProposalPhase() throws Exception {
		UUID thesisId = createTestThesis("AI review matching-phase test");
		setThesisState(thesisId, ThesisState.PROPOSAL);
		TestUser student = addThesisRole(thesisId, "STUDENT", "student");
		when(aiFeedbackService.autoReviewAndSave(any(Thesis.class), any(ReviewType.class)))
				.thenAnswer(invocation -> invocation.getArgument(0, Thesis.class));

		String body = "{\"thesisId\":\"" + thesisId + "\",\"reviewType\":\"PROPOSAL\"}";
		mockMvc.perform(post("/v2/ai-review/auto")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", authFor(student, "student")))
				.andExpect(status().isOk());

		verify(aiFeedbackService).autoReviewAndSave(any(Thesis.class), any(ReviewType.class));
	}

	@Test
	void auto_allowsSupervisorToOverridePhaseGate() throws Exception {
		UUID thesisId = createTestThesis("AI review supervisor-override test");
		setThesisState(thesisId, ThesisState.PROPOSAL);
		// A supervisor (advisor group + SUPERVISOR role) may run a THESIS review even while the
		// thesis is still in the proposal phase — the phase gate only constrains students.
		TestUser supervisor = addThesisRole(thesisId, "SUPERVISOR", "advisor");
		when(aiFeedbackService.autoReviewAndSave(any(Thesis.class), any(ReviewType.class)))
				.thenAnswer(invocation -> invocation.getArgument(0, Thesis.class));

		String body = "{\"thesisId\":\"" + thesisId + "\",\"reviewType\":\"THESIS\"}";
		mockMvc.perform(post("/v2/ai-review/auto")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", authFor(supervisor, "advisor")))
				.andExpect(status().isOk());

		verify(aiFeedbackService).autoReviewAndSave(any(Thesis.class), any(ReviewType.class));
	}

	@Test
	void classifyFeedback_returnsSuggestionForSupervisor() throws Exception {
		UUID thesisId = createTestThesis("AI feedback classification test");
		TestUser supervisor = addThesisRole(thesisId, "SUPERVISOR", "advisor");
		when(aiFeedbackService.classifyFeedbackLine(any(Thesis.class), anyString()))
				.thenReturn(new FeedbackClassificationDTO(
						ThesisFeedbackCategory.CITATION, ThesisFeedbackSeverity.MAJOR));

		String body = "{\"thesisId\":\"" + thesisId + "\",\"feedback\":\"Cite a peer-reviewed source here.\"}";
		mockMvc.perform(post("/v2/ai-review/classify-feedback")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", authFor(supervisor, "advisor")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.category").value("CITATION"))
				.andExpect(jsonPath("$.severity").value("MAJOR"));

		verify(aiFeedbackService).classifyFeedbackLine(any(Thesis.class), eq("Cite a peer-reviewed source here."));
	}

	@Test
	void classifyFeedback_returnsForbiddenForStudent() throws Exception {
		UUID thesisId = createTestThesis("AI feedback classification forbidden test");

		String body = "{\"thesisId\":\"" + thesisId + "\",\"feedback\":\"Cite a peer-reviewed source here.\"}";
		mockMvc.perform(post("/v2/ai-review/classify-feedback")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", createRandomAuthentication("student")))
				.andExpect(status().isForbidden());

		verify(aiFeedbackService, never()).classifyFeedbackLine(any(Thesis.class), anyString());
	}

	@Test
	void classifyFeedback_rejectsBlankFeedback() throws Exception {
		UUID thesisId = createTestThesis("AI feedback classification validation test");

		String body = "{\"thesisId\":\"" + thesisId + "\",\"feedback\":\"   \"}";
		mockMvc.perform(post("/v2/ai-review/classify-feedback")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body)
						.header("Authorization", createRandomAdminAuthentication()))
				.andExpect(status().isBadRequest());

		verify(aiFeedbackService, never()).classifyFeedbackLine(any(Thesis.class), anyString());
	}

	@Test
	void classifyFeedback_returnsUnauthorizedWithoutAuthentication() throws Exception {
		String body = "{\"thesisId\":\"" + UUID.randomUUID() + "\",\"feedback\":\"Cite a source.\"}";
		mockMvc.perform(post("/v2/ai-review/classify-feedback")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isUnauthorized());
	}

	/**
	 * Creates a fresh user with the given group, grants them the given thesis role, and puts them
	 * in the thesis's research group so research-group scoping does not reject their request.
	 */
	private TestUser addThesisRole(UUID thesisId, String role, String group) throws Exception {
		TestUser user = createRandomTestUser(List.of(group));
		jdbcTemplate.update(
				"INSERT INTO thesis_roles (thesis_id, user_id, role, position, assigned_at, assigned_by) "
						+ "VALUES (?::uuid, ?::uuid, ?, 0, NOW(), ?::uuid)",
				thesisId.toString(), user.userId().toString(), role, user.userId().toString());
		jdbcTemplate.update(
				"UPDATE users SET research_group_id = "
						+ "(SELECT research_group_id FROM theses WHERE thesis_id = ?::uuid) "
						+ "WHERE user_id = ?::uuid",
				thesisId.toString(), user.userId().toString());
		return user;
	}

	private void setThesisState(UUID thesisId, ThesisState state) {
		jdbcTemplate.update(
				"UPDATE theses SET state = ? WHERE thesis_id = ?::uuid",
				state.getValue(), thesisId.toString());
	}

	private String authFor(TestUser user, String role) {
		return generateTestAuthenticationHeader(user.universityId(), List.of(role));
	}

	private void stubPreviewResponse() {
		AIPreviewResponseDTO mockResponse = new AIPreviewResponseDTO(
				AssessmentCategory.ACCEPTABLE,
				62,
				"Solid overall but bibliography is thin.",
				List.of(new AIFeedbackDraftDTO(
						"Thin bibliography — increase to at least 6 peer-reviewed sources.",
						ThesisFeedbackCategory.CITATION,
						ThesisFeedbackSeverity.MAJOR)));
		when(aiFeedbackService.previewReview(any(Thesis.class), any(ReviewType.class))).thenReturn(mockResponse);
	}
}
