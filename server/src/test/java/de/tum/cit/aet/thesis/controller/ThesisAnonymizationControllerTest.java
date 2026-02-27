package de.tum.cit.aet.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.constants.ThesisCommentType;
import de.tum.cit.aet.thesis.constants.ThesisFeedbackType;
import de.tum.cit.aet.thesis.constants.ThesisRoleName;
import de.tum.cit.aet.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.constants.ThesisVisibility;
import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.entity.ThesisAssessment;
import de.tum.cit.aet.thesis.entity.ThesisComment;
import de.tum.cit.aet.thesis.entity.ThesisFeedback;
import de.tum.cit.aet.thesis.entity.ThesisProposal;
import de.tum.cit.aet.thesis.entity.ThesisRole;
import de.tum.cit.aet.thesis.entity.ThesisStateChange;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.entity.jsonb.ThesisMetadata;
import de.tum.cit.aet.thesis.entity.key.ThesisRoleId;
import de.tum.cit.aet.thesis.entity.key.ThesisStateChangeId;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.repository.ThesisAssessmentRepository;
import de.tum.cit.aet.thesis.repository.ThesisCommentRepository;
import de.tum.cit.aet.thesis.repository.ThesisFeedbackRepository;
import de.tum.cit.aet.thesis.repository.ThesisProposalRepository;
import de.tum.cit.aet.thesis.repository.ThesisRepository;
import de.tum.cit.aet.thesis.repository.ThesisRoleRepository;
import de.tum.cit.aet.thesis.repository.ThesisStateChangeRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Testcontainers
class ThesisAnonymizationControllerTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private ThesisRepository thesisRepository;

	@Autowired
	private ThesisRoleRepository thesisRoleRepository;

	@Autowired
	private ThesisStateChangeRepository thesisStateChangeRepository;

	@Autowired
	private ThesisCommentRepository thesisCommentRepository;

	@Autowired
	private ThesisProposalRepository thesisProposalRepository;

	@Autowired
	private ThesisAssessmentRepository thesisAssessmentRepository;

	@Autowired
	private ThesisFeedbackRepository thesisFeedbackRepository;

	@Autowired
	private ResearchGroupRepository researchGroupRepository;

	@Autowired
	private UserRepository userRepo;

	private Thesis createTestThesisWithChildren(ThesisState state, Instant createdAt, Instant endDate) throws Exception {
		TestUser supervisor = createRandomTestUser(List.of("supervisor"));
		UUID researchGroupId = createTestResearchGroup("Anon Controller Test RG", supervisor.universityId());
		TestUser advisor = createRandomTestUser(List.of("advisor"));
		TestUser student = createRandomTestUser(List.of("student"));

		Thesis thesis = new Thesis();
		thesis.setTitle("Test Thesis for Anonymization Controller");
		thesis.setType("MASTER");
		thesis.setLanguage("ENGLISH");
		thesis.setMetadata(new ThesisMetadata(new HashMap<>(), new HashMap<>()));
		thesis.setInfo("Some info text");
		thesis.setAbstractField("Some abstract");
		thesis.setState(state);
		thesis.setVisibility(ThesisVisibility.PRIVATE);
		thesis.setKeywords(new HashSet<>(Set.of("test", "anonymization")));
		thesis.setCreatedAt(createdAt);
		thesis.setStartDate(createdAt);
		thesis.setEndDate(endDate);
		thesis.setFinalGrade("1.7");
		thesis.setFinalFeedback("Good work");
		thesis.setResearchGroup(researchGroupRepository.findById(researchGroupId).orElseThrow());

		thesis = thesisRepository.save(thesis);

		// Add roles
		addRole(thesis, supervisor.userId(), ThesisRoleName.SUPERVISOR, 0);
		addRole(thesis, advisor.userId(), ThesisRoleName.ADVISOR, 0);
		addRole(thesis, student.userId(), ThesisRoleName.STUDENT, 0);

		// Add state changes
		addStateChange(thesis, ThesisState.PROPOSAL, createdAt);
		addStateChange(thesis, ThesisState.WRITING, createdAt.plus(10, ChronoUnit.DAYS));
		addStateChange(thesis, state, endDate != null ? endDate : createdAt.plus(100, ChronoUnit.DAYS));

		// Add comment
		ThesisComment comment = new ThesisComment();
		comment.setThesis(thesis);
		comment.setType(ThesisCommentType.THESIS);
		comment.setMessage("Test comment");
		comment.setCreatedAt(createdAt.plus(50, ChronoUnit.DAYS));
		comment.setCreatedBy(userRepo.findById(advisor.userId()).orElseThrow());
		thesisCommentRepository.save(comment);

		// Add proposal
		ThesisProposal proposal = new ThesisProposal();
		proposal.setThesis(thesis);
		proposal.setProposalFilename("test_proposal.pdf");
		proposal.setCreatedAt(createdAt.plus(5, ChronoUnit.DAYS));
		proposal.setCreatedBy(userRepo.findById(student.userId()).orElseThrow());
		thesisProposalRepository.save(proposal);

		// Add assessment
		ThesisAssessment assessment = new ThesisAssessment();
		assessment.setThesis(thesis);
		assessment.setSummary("Test summary");
		assessment.setPositives("Test positives");
		assessment.setNegatives("Test negatives");
		assessment.setGradeSuggestion("1.7");
		assessment.setCreatedAt(createdAt.plus(90, ChronoUnit.DAYS));
		assessment.setCreatedBy(userRepo.findById(advisor.userId()).orElseThrow());
		thesisAssessmentRepository.save(assessment);

		// Add feedback
		ThesisFeedback feedback = new ThesisFeedback();
		feedback.setThesis(thesis);
		feedback.setType(ThesisFeedbackType.THESIS);
		feedback.setFeedback("Test feedback");
		feedback.setRequestedAt(createdAt.plus(80, ChronoUnit.DAYS));
		feedback.setRequestedBy(userRepo.findById(advisor.userId()).orElseThrow());
		thesisFeedbackRepository.save(feedback);

		return thesis;
	}

	private void addRole(Thesis thesis, UUID userId, ThesisRoleName role, int position) {
		User user = userRepo.findById(userId).orElseThrow();
		ThesisRole thesisRole = new ThesisRole();
		ThesisRoleId roleId = new ThesisRoleId();
		roleId.setThesisId(thesis.getId());
		roleId.setUserId(userId);
		roleId.setRole(role);
		thesisRole.setId(roleId);
		thesisRole.setUser(user);
		thesisRole.setAssignedBy(user);
		thesisRole.setAssignedAt(Instant.now());
		thesisRole.setThesis(thesis);
		thesisRole.setPosition(position);
		thesisRoleRepository.save(thesisRole);
	}

	private void addStateChange(Thesis thesis, ThesisState state, Instant changedAt) {
		ThesisStateChangeId id = new ThesisStateChangeId();
		id.setThesisId(thesis.getId());
		id.setState(state);
		ThesisStateChange sc = new ThesisStateChange();
		sc.setId(id);
		sc.setThesis(thesis);
		sc.setChangedAt(changedAt);
		thesisStateChangeRepository.save(sc);
	}

	@Nested
	class GetAnonymizationWarnings {

		@Test
		void noWarningsForExpiredFinishedThesis() throws Exception {
			String adminAuth = createRandomAdminAuthentication();
			Instant createdAt = Instant.now().minus(2600, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(2400, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.FINISHED, createdAt, endDate);

			String response = mockMvc.perform(MockMvcRequestBuilders.get(
							"/v2/theses/{thesisId}/anonymize/warnings", thesis.getId())
							.header("Authorization", adminAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("warnings").size()).isEqualTo(0);
		}

		@Test
		void retentionWarningForRecentFinishedThesis() throws Exception {
			String adminAuth = createRandomAdminAuthentication();
			Instant createdAt = Instant.now().minus(800, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(620, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.FINISHED, createdAt, endDate);

			String response = mockMvc.perform(MockMvcRequestBuilders.get(
							"/v2/theses/{thesisId}/anonymize/warnings", thesis.getId())
							.header("Authorization", adminAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("warnings").size()).isEqualTo(1);
			assertThat(json.get("warnings").get(0).asString()).contains("retention period");
		}

		@Test
		void stateWarningForActiveThesis() throws Exception {
			String adminAuth = createRandomAdminAuthentication();
			Instant createdAt = Instant.now().minus(2600, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(2400, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.WRITING, createdAt, endDate);

			String response = mockMvc.perform(MockMvcRequestBuilders.get(
							"/v2/theses/{thesisId}/anonymize/warnings", thesis.getId())
							.header("Authorization", adminAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("warnings").size()).isGreaterThanOrEqualTo(1);
			boolean hasStateWarning = false;
			for (JsonNode warning : json.get("warnings")) {
				if (warning.asString().contains("WRITING")) {
					hasStateWarning = true;
					break;
				}
			}
			assertThat(hasStateWarning).isTrue();
		}

		@Test
		void multipleWarnings() throws Exception {
			String adminAuth = createRandomAdminAuthentication();
			Instant createdAt = Instant.now().minus(800, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(620, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.WRITING, createdAt, endDate);

			String response = mockMvc.perform(MockMvcRequestBuilders.get(
							"/v2/theses/{thesisId}/anonymize/warnings", thesis.getId())
							.header("Authorization", adminAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("warnings").size()).isEqualTo(2);
		}

		@Test
		void alreadyAnonymizedWarning() throws Exception {
			String adminAuth = createRandomAdminAuthentication();
			Instant createdAt = Instant.now().minus(2600, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(2400, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.FINISHED, createdAt, endDate);
			thesis.setAnonymizedAt(Instant.now());
			thesisRepository.save(thesis);

			String response = mockMvc.perform(MockMvcRequestBuilders.get(
							"/v2/theses/{thesisId}/anonymize/warnings", thesis.getId())
							.header("Authorization", adminAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("warnings").size()).isEqualTo(1);
			assertThat(json.get("warnings").get(0).asString()).contains("already been anonymized");
		}

		@Test
		void forbiddenForNonAdmin() throws Exception {
			String advisorAuth = createRandomAuthentication("advisor");
			Instant createdAt = Instant.now().minus(2600, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(2400, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.FINISHED, createdAt, endDate);

			mockMvc.perform(MockMvcRequestBuilders.get(
							"/v2/theses/{thesisId}/anonymize/warnings", thesis.getId())
							.header("Authorization", advisorAuth))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class AnonymizeThesis {

		@Test
		void successForExpiredFinishedThesis() throws Exception {
			String adminAuth = createRandomAdminAuthentication();
			Instant createdAt = Instant.now().minus(2600, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(2400, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.FINISHED, createdAt, endDate);

			String response = mockMvc.perform(MockMvcRequestBuilders.delete(
							"/v2/theses/{thesisId}/anonymize", thesis.getId())
							.header("Authorization", adminAuth))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			JsonNode json = objectMapper.readTree(response);
			assertThat(json.get("anonymizedTheses").asInt()).isEqualTo(1);

			// Verify anonymization
			Thesis anonymized = thesisRepository.findById(thesis.getId()).orElseThrow();
			assertThat(anonymized.isAnonymized()).isTrue();
			assertThat(anonymized.getInfo()).isEmpty();
			assertThat(anonymized.getAbstractField()).isEmpty();
			assertThat(anonymized.getFinalFeedback()).isNull();
			assertThat(anonymized.getKeywords()).isEmpty();

			// Structural fields preserved
			assertThat(anonymized.getTitle()).isEqualTo("Test Thesis for Anonymization Controller");
			assertThat(anonymized.getFinalGrade()).isEqualTo("1.7");

			// Child records deleted
			assertThat(thesisCommentRepository.findAll().stream()
					.filter(c -> c.getThesis().getId().equals(thesis.getId())).toList()).isEmpty();
			assertThat(thesisProposalRepository.findAll().stream()
					.filter(p -> p.getThesis().getId().equals(thesis.getId())).toList()).isEmpty();
			assertThat(thesisAssessmentRepository.findAll().stream()
					.filter(a -> a.getThesis().getId().equals(thesis.getId())).toList()).isEmpty();
			assertThat(thesisFeedbackRepository.findAll().stream()
					.filter(f -> f.getThesis().getId().equals(thesis.getId())).toList()).isEmpty();
			assertThat(thesisRoleRepository.findAll().stream()
					.filter(r -> r.getThesis().getId().equals(thesis.getId())).toList()).isEmpty();
			assertThat(thesisStateChangeRepository.findAll().stream()
					.filter(s -> s.getThesis().getId().equals(thesis.getId())).toList()).isEmpty();
		}

		@Test
		void successForNonTerminalThesis() throws Exception {
			String adminAuth = createRandomAdminAuthentication();
			Instant createdAt = Instant.now().minus(2600, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(2400, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.WRITING, createdAt, endDate);

			mockMvc.perform(MockMvcRequestBuilders.delete(
							"/v2/theses/{thesisId}/anonymize", thesis.getId())
							.header("Authorization", adminAuth))
					.andExpect(status().isOk());

			Thesis anonymized = thesisRepository.findById(thesis.getId()).orElseThrow();
			assertThat(anonymized.isAnonymized()).isTrue();
		}

		@Test
		void successForRecentThesis() throws Exception {
			String adminAuth = createRandomAdminAuthentication();
			Instant createdAt = Instant.now().minus(800, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(620, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.FINISHED, createdAt, endDate);

			mockMvc.perform(MockMvcRequestBuilders.delete(
							"/v2/theses/{thesisId}/anonymize", thesis.getId())
							.header("Authorization", adminAuth))
					.andExpect(status().isOk());

			Thesis anonymized = thesisRepository.findById(thesis.getId()).orElseThrow();
			assertThat(anonymized.isAnonymized()).isTrue();
		}

		@Test
		void conflictForAlreadyAnonymized() throws Exception {
			String adminAuth = createRandomAdminAuthentication();
			Instant createdAt = Instant.now().minus(2600, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(2400, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.FINISHED, createdAt, endDate);
			thesis.setAnonymizedAt(Instant.now());
			thesisRepository.save(thesis);

			mockMvc.perform(MockMvcRequestBuilders.delete(
							"/v2/theses/{thesisId}/anonymize", thesis.getId())
							.header("Authorization", adminAuth))
					.andExpect(status().isConflict());
		}

		@Test
		void notFoundForNonExistentThesis() throws Exception {
			String adminAuth = createRandomAdminAuthentication();

			mockMvc.perform(MockMvcRequestBuilders.delete(
							"/v2/theses/{thesisId}/anonymize", UUID.randomUUID())
							.header("Authorization", adminAuth))
					.andExpect(status().isNotFound());
		}

		@Test
		void forbiddenForNonAdmin() throws Exception {
			String advisorAuth = createRandomAuthentication("advisor");
			Instant createdAt = Instant.now().minus(2600, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(2400, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.FINISHED, createdAt, endDate);

			mockMvc.perform(MockMvcRequestBuilders.delete(
							"/v2/theses/{thesisId}/anonymize", thesis.getId())
							.header("Authorization", advisorAuth))
					.andExpect(status().isForbidden());
		}
	}
}
