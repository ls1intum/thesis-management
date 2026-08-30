package de.tum.cit.aet.thesis.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.core.exception.request.AccessDeniedException;
import de.tum.cit.aet.thesis.core.group.entity.ResearchGroup;
import de.tum.cit.aet.thesis.feedback.dto.AIPreviewResponseDTO;
import de.tum.cit.aet.thesis.feedback.dto.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.dto.ReviewResultDTO;
import de.tum.cit.aet.thesis.feedback.entity.AIReviewSummary;
import de.tum.cit.aet.thesis.feedback.entity.GuidelinesStatus;
import de.tum.cit.aet.thesis.feedback.entity.ResearchGroupGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.CategoryGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.repository.AIReviewSummaryRepository;
import de.tum.cit.aet.thesis.feedback.repository.ResearchGroupGuidelinesRepository;
import de.tum.cit.aet.thesis.feedback.service.reviewer.ReviewType;
import de.tum.cit.aet.thesis.proposal.entity.ThesisProposal;
import de.tum.cit.aet.thesis.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.thesis.service.ThesisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class AIFeedbackServiceTest {

	@Mock
	private ReviewService reviewService;

	@Mock
	private ThesisService thesisService;

	@Mock
	private ResearchGroupGuidelinesRepository guidelinesRepository;

	@Mock
	private AIReviewSummaryRepository reviewSummaryRepository;

	@Mock
	private Thesis thesis;

	private AIFeedbackService service;

	private final UUID researchGroupId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new AIFeedbackService(reviewService, thesisService, guidelinesRepository, reviewSummaryRepository);

		ResearchGroup researchGroup = new ResearchGroup();
		researchGroup.setId(researchGroupId);
		when(thesis.getResearchGroup()).thenReturn(researchGroup);
	}

	private ResearchGroupGuidelines readyGuidelines() {
		ResearchGroupGuidelines guidelines = new ResearchGroupGuidelines();
		guidelines.setResearchGroupId(researchGroupId);
		guidelines.setStatus(GuidelinesStatus.READY);
		guidelines.setStructuredGuidelines(new StructuredGuidelines(
				"Overview.",
				List.of(new CategoryGuidelines("bibliography", List.of("Cite at least 6 sources.")))));
		return guidelines;
	}

	@Test
	void previewReview_throwsWhenGroupHasNoGuidelines() {
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.previewReview(thesis, ReviewType.PROPOSAL))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessageContaining("not set up for your research group yet")
				.hasMessageContaining("research group lead");

		verify(reviewService, never()).review(any(), any(), any());
	}

	@Test
	void autoReviewAndSave_throwsWhenGuidelinesNotReady() {
		ResearchGroupGuidelines failed = new ResearchGroupGuidelines();
		failed.setResearchGroupId(researchGroupId);
		failed.setStatus(GuidelinesStatus.FAILED);
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(failed));

		assertThatThrownBy(() -> service.autoReviewAndSave(thesis, ReviewType.PROPOSAL))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessageContaining("could not be turned into review rules")
				.hasMessageContaining("research group lead");

		verify(reviewService, never()).review(any(), any(), any());
	}

	@Test
	void previewReview_passesGroupGuidelinesToReviewWhenReady() {
		ResearchGroupGuidelines guidelines = readyGuidelines();
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(guidelines));

		ThesisProposal proposal = org.mockito.Mockito.mock(ThesisProposal.class);
		when(thesis.getProposals()).thenReturn(List.of(proposal));
		Resource pdfResource = new ByteArrayResource("pdf".getBytes());
		when(thesisService.getProposalFile(proposal)).thenReturn(pdfResource);

		when(reviewService.review(eq(pdfResource), eq(ReviewType.PROPOSAL), eq(guidelines.getStructuredGuidelines())))
				.thenReturn(new ReviewResultDTO(AssessmentCategory.GOOD, 90, "All good.", List.of()));

		AIPreviewResponseDTO response = service.previewReview(thesis, ReviewType.PROPOSAL);

		assertThat(response.summary()).isEqualTo("All good.");
		assertThat(response.score()).isEqualTo(90);
		verify(reviewService).review(pdfResource, ReviewType.PROPOSAL, guidelines.getStructuredGuidelines());
		// A preview is supervisor-only and provisional (drafts may be edited/discarded), so it
		// must never persist a summary the student-visible feedback overview would then show.
		verify(reviewSummaryRepository, never()).save(any());
	}

	@Test
	void previewReview_treatsNullScoreAsAbsent() {
		ResearchGroupGuidelines guidelines = readyGuidelines();
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(guidelines));

		ThesisProposal proposal = org.mockito.Mockito.mock(ThesisProposal.class);
		when(thesis.getProposals()).thenReturn(List.of(proposal));
		Resource pdfResource = new ByteArrayResource("pdf".getBytes());
		when(thesisService.getProposalFile(proposal)).thenReturn(pdfResource);

		when(reviewService.review(eq(pdfResource), eq(ReviewType.PROPOSAL), eq(guidelines.getStructuredGuidelines())))
				.thenReturn(new ReviewResultDTO(AssessmentCategory.GOOD, null, "All good.", List.of()));

		AIPreviewResponseDTO response = service.previewReview(thesis, ReviewType.PROPOSAL);

		assertThat(response.score()).isNull();
	}

	@Test
	void previewReview_nullsOutNegativeScore() {
		ResearchGroupGuidelines guidelines = readyGuidelines();
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(guidelines));

		ThesisProposal proposal = org.mockito.Mockito.mock(ThesisProposal.class);
		when(thesis.getProposals()).thenReturn(List.of(proposal));
		Resource pdfResource = new ByteArrayResource("pdf".getBytes());
		when(thesisService.getProposalFile(proposal)).thenReturn(pdfResource);

		when(reviewService.review(eq(pdfResource), eq(ReviewType.PROPOSAL), eq(guidelines.getStructuredGuidelines())))
				.thenReturn(new ReviewResultDTO(AssessmentCategory.GOOD, -5, "All good.", List.of()));

		AIPreviewResponseDTO response = service.previewReview(thesis, ReviewType.PROPOSAL);

		assertThat(response.score()).isNull();
	}

	@Test
	void previewReview_nullsOutScoreAboveMax() {
		ResearchGroupGuidelines guidelines = readyGuidelines();
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(guidelines));

		ThesisProposal proposal = org.mockito.Mockito.mock(ThesisProposal.class);
		when(thesis.getProposals()).thenReturn(List.of(proposal));
		Resource pdfResource = new ByteArrayResource("pdf".getBytes());
		when(thesisService.getProposalFile(proposal)).thenReturn(pdfResource);

		when(reviewService.review(eq(pdfResource), eq(ReviewType.PROPOSAL), eq(guidelines.getStructuredGuidelines())))
				.thenReturn(new ReviewResultDTO(AssessmentCategory.GOOD, 150, "All good.", List.of()));

		AIPreviewResponseDTO response = service.previewReview(thesis, ReviewType.PROPOSAL);

		assertThat(response.score()).isNull();
	}

	@Test
	void autoReviewAndSave_persistsReviewSummaryEvenWithNoActionableFindings() {
		ResearchGroupGuidelines guidelines = readyGuidelines();
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(guidelines));

		ThesisProposal proposal = org.mockito.Mockito.mock(ThesisProposal.class);
		when(thesis.getProposals()).thenReturn(List.of(proposal));
		Resource pdfResource = new ByteArrayResource("pdf".getBytes());
		when(thesisService.getProposalFile(proposal)).thenReturn(pdfResource);

		when(reviewService.review(eq(pdfResource), eq(ReviewType.PROPOSAL), eq(guidelines.getStructuredGuidelines())))
				.thenReturn(new ReviewResultDTO(AssessmentCategory.GOOD, 95, "Nothing to flag.", List.of()));
		UUID thesisId = UUID.randomUUID();
		when(thesis.getId()).thenReturn(thesisId);
		when(reviewSummaryRepository.findByThesisIdAndType(thesisId, ReviewType.PROPOSAL)).thenReturn(Optional.empty());

		Thesis result = service.autoReviewAndSave(thesis, ReviewType.PROPOSAL);

		assertThat(result).isSameAs(thesis);
		verify(thesisService, never()).requestChanges(any(), any(), anyList(), any());

		ArgumentCaptor<AIReviewSummary> savedSummary = ArgumentCaptor.forClass(AIReviewSummary.class);
		verify(reviewSummaryRepository).save(savedSummary.capture());
		assertThat(savedSummary.getValue().getScore()).isEqualTo(95);
	}

	@Test
	void autoReviewAndSave_retriesAsUpdateWhenConcurrentInsertRacesTheSummary() {
		ResearchGroupGuidelines guidelines = readyGuidelines();
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(guidelines));

		ThesisProposal proposal = org.mockito.Mockito.mock(ThesisProposal.class);
		when(thesis.getProposals()).thenReturn(List.of(proposal));
		Resource pdfResource = new ByteArrayResource("pdf".getBytes());
		when(thesisService.getProposalFile(proposal)).thenReturn(pdfResource);

		when(reviewService.review(eq(pdfResource), eq(ReviewType.PROPOSAL), eq(guidelines.getStructuredGuidelines())))
				.thenReturn(new ReviewResultDTO(AssessmentCategory.GOOD, 90, "All good.", List.of()));
		UUID thesisId = UUID.randomUUID();
		when(thesis.getId()).thenReturn(thesisId);

		// Both this run and a concurrent one see no existing row first...
		AIReviewSummary concurrentlyInserted = new AIReviewSummary();
		when(reviewSummaryRepository.findByThesisIdAndType(thesisId, ReviewType.PROPOSAL))
				.thenReturn(Optional.empty())
				.thenReturn(Optional.of(concurrentlyInserted));
		// ...so this run's own insert violates the (thesis_id, type) unique constraint.
		when(reviewSummaryRepository.save(org.mockito.ArgumentMatchers.argThat(
				candidate -> candidate != concurrentlyInserted)))
				.thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key"));

		service.autoReviewAndSave(thesis, ReviewType.PROPOSAL);

		// The retry updates the row the concurrent run just inserted rather than failing.
		verify(reviewSummaryRepository).save(concurrentlyInserted);
		assertThat(concurrentlyInserted.getScore()).isEqualTo(90);
		assertThat(concurrentlyInserted.getAssessment()).isEqualTo(AssessmentCategory.GOOD);
	}
}
