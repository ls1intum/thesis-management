package de.tum.cit.aet.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;

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
import de.tum.cit.aet.thesis.repository.ThesisFileRepository;
import de.tum.cit.aet.thesis.repository.ThesisProposalRepository;
import de.tum.cit.aet.thesis.repository.ThesisRepository;
import de.tum.cit.aet.thesis.repository.ThesisRoleRepository;
import de.tum.cit.aet.thesis.repository.ThesisStateChangeRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Testcontainers
class ThesisAnonymizationServiceTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private ThesisAnonymizationService thesisAnonymizationService;

	@Autowired
	private ThesisRepository thesisRepository;

	@Autowired
	private ThesisRoleRepository thesisRoleRepository;

	@Autowired
	private ThesisStateChangeRepository thesisStateChangeRepository;

	@Autowired
	private ThesisCommentRepository thesisCommentRepository;

	@Autowired
	private ThesisFileRepository thesisFileRepository;

	@Autowired
	private ThesisProposalRepository thesisProposalRepository;

	@Autowired
	private ThesisAssessmentRepository thesisAssessmentRepository;

	@Autowired
	private ThesisFeedbackRepository thesisFeedbackRepository;

	@Autowired
	private ResearchGroupRepository researchGroupRepository;

	private Thesis createTestThesisWithChildren(ThesisState state, Instant createdAt, Instant endDate) throws Exception {
		TestUser examiner = createRandomTestUser(List.of("supervisor"));
		UUID researchGroupId = createTestResearchGroup("Anon Test RG", examiner.universityId());
		TestUser supervisor = createRandomTestUser(List.of("advisor"));
		TestUser student = createRandomTestUser(List.of("student"));

		Thesis thesis = new Thesis();
		thesis.setTitle("Test Thesis for Anonymization");
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
		addRole(thesis, examiner.userId(), ThesisRoleName.EXAMINER, 0);
		addRole(thesis, supervisor.userId(), ThesisRoleName.SUPERVISOR, 0);
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
		comment.setCreatedBy(userRepository().findById(supervisor.userId()).orElseThrow());
		thesisCommentRepository.save(comment);

		// Add proposal
		ThesisProposal proposal = new ThesisProposal();
		proposal.setThesis(thesis);
		proposal.setProposalFilename("test_proposal.pdf");
		proposal.setCreatedAt(createdAt.plus(5, ChronoUnit.DAYS));
		proposal.setCreatedBy(userRepository().findById(student.userId()).orElseThrow());
		thesisProposalRepository.save(proposal);

		// Add assessment
		ThesisAssessment assessment = new ThesisAssessment();
		assessment.setThesis(thesis);
		assessment.setSummary("Test summary");
		assessment.setPositives("Test positives");
		assessment.setNegatives("Test negatives");
		assessment.setGradeSuggestion("1.7");
		assessment.setCreatedAt(createdAt.plus(90, ChronoUnit.DAYS));
		assessment.setCreatedBy(userRepository().findById(supervisor.userId()).orElseThrow());
		thesisAssessmentRepository.save(assessment);

		// Add feedback
		ThesisFeedback feedback = new ThesisFeedback();
		feedback.setThesis(thesis);
		feedback.setType(ThesisFeedbackType.THESIS);
		feedback.setFeedback("Test feedback");
		feedback.setRequestedAt(createdAt.plus(80, ChronoUnit.DAYS));
		feedback.setRequestedBy(userRepository().findById(supervisor.userId()).orElseThrow());
		thesisFeedbackRepository.save(feedback);

		return thesis;
	}

	@Autowired
	private de.tum.cit.aet.thesis.repository.UserRepository userRepo;

	private de.tum.cit.aet.thesis.repository.UserRepository userRepository() {
		return userRepo;
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
	class AnonymizeExpiredTheses {

		@Test
		void anonymizesExpiredThesis() throws Exception {
			// Thesis finished 7 years ago → retention expired
			Instant createdAt = Instant.now().minus(2600, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(2400, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.FINISHED, createdAt, endDate);

			int count = thesisAnonymizationService.anonymizeExpiredTheses();

			assertThat(count).isEqualTo(1);

			Thesis anonymized = thesisRepository.findById(thesis.getId()).orElseThrow();
			assertThat(anonymized.isAnonymized()).isTrue();
			assertThat(anonymized.getAnonymizedAt()).isNotNull();

			// Structural fields preserved
			assertThat(anonymized.getTitle()).isEqualTo("Test Thesis for Anonymization");
			assertThat(anonymized.getType()).isEqualTo("MASTER");
			assertThat(anonymized.getFinalGrade()).isEqualTo("1.7");
			assertThat(anonymized.getState()).isEqualTo(ThesisState.FINISHED);
			assertThat(anonymized.getStartDate()).isNotNull();
			assertThat(anonymized.getEndDate()).isNotNull();

			// Personal data cleared
			assertThat(anonymized.getInfo()).isEmpty();
			assertThat(anonymized.getAbstractField()).isEmpty();
			assertThat(anonymized.getFinalFeedback()).isNull();
			assertThat(anonymized.getKeywords()).isEmpty();
			assertThat(anonymized.getApplication()).isNull();

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

			// Research group preserved
			assertThat(anonymized.getResearchGroup()).isNotNull();
		}

		@Test
		void skipsNonExpiredThesis() throws Exception {
			// Thesis finished 2 years ago → retention still active
			Instant createdAt = Instant.now().minus(800, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(620, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.FINISHED, createdAt, endDate);

			int count = thesisAnonymizationService.anonymizeExpiredTheses();

			assertThat(count).isEqualTo(0);

			Thesis stillPresent = thesisRepository.findById(thesis.getId()).orElseThrow();
			assertThat(stillPresent.isAnonymized()).isFalse();
			assertThat(stillPresent.getInfo()).isEqualTo("Some info text");
		}

		@Test
		void skipsAlreadyAnonymizedThesis() throws Exception {
			Instant createdAt = Instant.now().minus(2600, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(2400, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.FINISHED, createdAt, endDate);

			// First anonymization
			int count1 = thesisAnonymizationService.anonymizeExpiredTheses();
			assertThat(count1).isEqualTo(1);

			// Second run should find nothing
			int count2 = thesisAnonymizationService.anonymizeExpiredTheses();
			assertThat(count2).isEqualTo(0);
		}

		@Test
		void anonymizesDroppedOutThesis() throws Exception {
			Instant createdAt = Instant.now().minus(2600, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(2400, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.DROPPED_OUT, createdAt, endDate);

			int count = thesisAnonymizationService.anonymizeExpiredTheses();

			assertThat(count).isEqualTo(1);
			Thesis anonymized = thesisRepository.findById(thesis.getId()).orElseThrow();
			assertThat(anonymized.isAnonymized()).isTrue();
		}

		@Test
		void anonymizesDroppedOutThesisWithoutEndDate() throws Exception {
			// DROPPED_OUT theses may not have endDate set — retention should fall back to state change date
			Instant createdAt = Instant.now().minus(2600, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.DROPPED_OUT, createdAt, null);

			int count = thesisAnonymizationService.anonymizeExpiredTheses();

			assertThat(count).isEqualTo(1);
			Thesis anonymized = thesisRepository.findById(thesis.getId()).orElseThrow();
			assertThat(anonymized.isAnonymized()).isTrue();
			assertThat(anonymized.getAnonymizedAt()).isNotNull();
		}
	}

	@Nested
	class ComputeAnonymizationWarnings {

		@Test
		void noWarningsForExpiredFinishedThesis() throws Exception {
			Instant createdAt = Instant.now().minus(2600, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(2400, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.FINISHED, createdAt, endDate);

			List<String> warnings = thesisAnonymizationService.computeAnonymizationWarnings(thesis);
			assertThat(warnings).isEmpty();
		}

		@Test
		void retentionWarningForRecentFinishedThesis() throws Exception {
			Instant createdAt = Instant.now().minus(800, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(620, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.FINISHED, createdAt, endDate);

			List<String> warnings = thesisAnonymizationService.computeAnonymizationWarnings(thesis);
			assertThat(warnings).hasSize(1);
			assertThat(warnings.get(0)).contains("retention period");
		}

		@Test
		void stateWarningForActiveThesis() throws Exception {
			Instant createdAt = Instant.now().minus(2600, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(2400, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.WRITING, createdAt, endDate);

			List<String> warnings = thesisAnonymizationService.computeAnonymizationWarnings(thesis);
			assertThat(warnings).anyMatch(w -> w.contains("WRITING"));
		}

		@Test
		void multipleWarningsForRecentActiveThesis() throws Exception {
			Instant createdAt = Instant.now().minus(800, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(620, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.WRITING, createdAt, endDate);

			List<String> warnings = thesisAnonymizationService.computeAnonymizationWarnings(thesis);
			assertThat(warnings).hasSize(2);
			assertThat(warnings).anyMatch(w -> w.contains("WRITING"));
			assertThat(warnings).anyMatch(w -> w.contains("retention period"));
		}

		@Test
		void alreadyAnonymizedWarning() throws Exception {
			Instant createdAt = Instant.now().minus(2600, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(2400, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.FINISHED, createdAt, endDate);
			thesis.setAnonymizedAt(Instant.now());
			thesisRepository.save(thesis);

			List<String> warnings = thesisAnonymizationService.computeAnonymizationWarnings(thesis);
			assertThat(warnings).hasSize(1);
			assertThat(warnings.get(0)).contains("already been anonymized");
		}

		@Test
		void droppedOutHasNoStateWarning() throws Exception {
			Instant createdAt = Instant.now().minus(2600, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(2400, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.DROPPED_OUT, createdAt, endDate);

			List<String> warnings = thesisAnonymizationService.computeAnonymizationWarnings(thesis);
			assertThat(warnings).noneMatch(w -> w.contains("DROPPED_OUT"));
		}
	}

	@Nested
	class SendAnonymizationNotifications {

		@Test
		void notifiesForThesisApproachingExpiry() throws Exception {
			createTestEmailTemplate("THESIS_ANONYMIZATION_REMINDER");

			// Thesis finished ~5.2 years ago (endDate in 2020) → retention expiry = Dec 31, 2025
			// which is in the past and thus within the 30-day notification horizon.
			// This thesis should definitely be notified.
			Instant createdAt = Instant.now().minus(1950, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(1900, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.FINISHED, createdAt, endDate);

			thesisAnonymizationService.sendAnonymizationNotifications();

			Thesis notified = thesisRepository.findById(thesis.getId()).orElseThrow();
			assertThat(notified.getAnonymizationNotifiedAt()).isNotNull();
		}

		@Test
		void doesNotReNotifyAlreadyNotifiedThesis() throws Exception {
			createTestEmailTemplate("THESIS_ANONYMIZATION_REMINDER");

			Instant createdAt = Instant.now().minus(2600, ChronoUnit.DAYS);
			Instant endDate = Instant.now().minus(2400, ChronoUnit.DAYS);
			Thesis thesis = createTestThesisWithChildren(ThesisState.FINISHED, createdAt, endDate);

			// Mark as already notified
			thesis.setAnonymizationNotifiedAt(Instant.now().minus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS));
			thesisRepository.save(thesis);

			thesisAnonymizationService.sendAnonymizationNotifications();

			Thesis reloaded = thesisRepository.findById(thesis.getId()).orElseThrow();
			// Should still have the original notification timestamp
			assertThat(reloaded.getAnonymizationNotifiedAt())
					.isEqualTo(thesis.getAnonymizationNotifiedAt());
		}
	}
}
