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
import de.tum.cit.aet.thesis.core.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.core.group.entity.ResearchGroup;
import de.tum.cit.aet.thesis.feedback.dto.AIPreviewResponseDTO;
import de.tum.cit.aet.thesis.feedback.dto.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.dto.FeedbackClassificationDTO;
import de.tum.cit.aet.thesis.feedback.dto.FeedbackClassificationResult;
import de.tum.cit.aet.thesis.feedback.dto.FindingDTO;
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
import de.tum.cit.aet.thesis.proposal.repository.ThesisProposalRepository;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackCategory;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSeverity;
import de.tum.cit.aet.thesis.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.thesis.entity.ThesisFile;
import de.tum.cit.aet.thesis.thesis.repository.ThesisFileRepository;
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
	private ThesisProposalRepository proposalRepository;

	@Mock
	private ThesisFileRepository thesisFileRepository;

	@Mock
	private FeedbackClassificationService feedbackClassificationService;

	@Mock
	private Thesis thesis;

	private AIFeedbackService service;

	private final UUID researchGroupId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new AIFeedbackService(reviewService, thesisService, guidelinesRepository, reviewSummaryRepository,
				proposalRepository, thesisFileRepository, feedbackClassificationService);

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
		when(proposal.getId()).thenReturn(UUID.randomUUID());
		when(thesis.getProposals()).thenReturn(List.of(proposal));
		Resource pdfResource = new ByteArrayResource("pdf".getBytes());
		when(thesisService.getProposalFile(proposal)).thenReturn(pdfResource);

		when(reviewService.review(eq(pdfResource), eq(ReviewType.PROPOSAL), eq(guidelines.getStructuredGuidelines())))
				.thenReturn(new ReviewResultDTO(AssessmentCategory.GOOD, 95, "Nothing to flag.", List.of()));
		UUID thesisId = UUID.randomUUID();
		when(thesis.getId()).thenReturn(thesisId);
		when(proposalRepository.findFirstByThesisIdOrderByCreatedAtDesc(thesisId)).thenReturn(Optional.of(proposal));
		when(reviewSummaryRepository.findByThesisIdAndType(thesisId, ReviewType.PROPOSAL)).thenReturn(Optional.empty());

		Thesis result = service.autoReviewAndSave(thesis, ReviewType.PROPOSAL);

		assertThat(result).isSameAs(thesis);
		verify(thesisService, never()).requestChanges(any(), any(), anyList(), any());

		ArgumentCaptor<AIReviewSummary> savedSummary = ArgumentCaptor.forClass(AIReviewSummary.class);
		verify(reviewSummaryRepository).save(savedSummary.capture());
		assertThat(savedSummary.getValue().getScore()).isEqualTo(95);
	}

	@Test
	void autoReviewAndSave_recordsTheReviewedProposalVersionOnTheSummary() {
		ResearchGroupGuidelines guidelines = readyGuidelines();
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(guidelines));

		UUID proposalId = UUID.randomUUID();
		ThesisProposal proposal = org.mockito.Mockito.mock(ThesisProposal.class);
		when(proposal.getId()).thenReturn(proposalId);
		when(thesis.getProposals()).thenReturn(List.of(proposal));
		Resource pdfResource = new ByteArrayResource("pdf".getBytes());
		when(thesisService.getProposalFile(proposal)).thenReturn(pdfResource);

		when(reviewService.review(eq(pdfResource), eq(ReviewType.PROPOSAL), eq(guidelines.getStructuredGuidelines())))
				.thenReturn(new ReviewResultDTO(AssessmentCategory.GOOD, 95, "Nothing to flag.", List.of()));
		UUID thesisId = UUID.randomUUID();
		when(thesis.getId()).thenReturn(thesisId);
		when(proposalRepository.findFirstByThesisIdOrderByCreatedAtDesc(thesisId)).thenReturn(Optional.of(proposal));
		when(reviewSummaryRepository.findByThesisIdAndType(thesisId, ReviewType.PROPOSAL)).thenReturn(Optional.empty());

		service.autoReviewAndSave(thesis, ReviewType.PROPOSAL);

		// Without the version the UI cannot tell this summary apart from one describing an older
		// upload, and would keep showing the stale score as current.
		ArgumentCaptor<AIReviewSummary> savedSummary = ArgumentCaptor.forClass(AIReviewSummary.class);
		verify(reviewSummaryRepository).save(savedSummary.capture());
		assertThat(savedSummary.getValue().getDocumentVersionId()).isEqualTo(proposalId);
	}

	@Test
	void autoReviewAndSave_recordsTheReviewedThesisFileVersionOnTheSummary() {
		ResearchGroupGuidelines guidelines = readyGuidelines();
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(guidelines));

		UUID fileId = UUID.randomUUID();
		ThesisFile thesisFile = org.mockito.Mockito.mock(ThesisFile.class);
		when(thesisFile.getId()).thenReturn(fileId);
		when(thesis.getLatestFile("THESIS")).thenReturn(Optional.of(thesisFile));
		Resource pdfResource = new ByteArrayResource("pdf".getBytes());
		when(thesisService.getThesisFile(thesisFile)).thenReturn(pdfResource);

		when(reviewService.review(eq(pdfResource), eq(ReviewType.THESIS), eq(guidelines.getStructuredGuidelines())))
				.thenReturn(new ReviewResultDTO(AssessmentCategory.ACCEPTABLE, 70, "Some gaps.", List.of()));
		UUID thesisId = UUID.randomUUID();
		when(thesis.getId()).thenReturn(thesisId);
		when(thesisFileRepository.findFirstByThesisIdAndTypeOrderByUploadedAtDesc(thesisId, "THESIS"))
				.thenReturn(Optional.of(thesisFile));
		when(reviewSummaryRepository.findByThesisIdAndType(thesisId, ReviewType.THESIS)).thenReturn(Optional.empty());

		service.autoReviewAndSave(thesis, ReviewType.THESIS);

		ArgumentCaptor<AIReviewSummary> savedSummary = ArgumentCaptor.forClass(AIReviewSummary.class);
		verify(reviewSummaryRepository).save(savedSummary.capture());
		assertThat(savedSummary.getValue().getDocumentVersionId()).isEqualTo(fileId);
	}

	@Test
	void autoReviewAndSave_doesNotPersistTheSummaryWhenSavingTheFindingsFails() {
		ResearchGroupGuidelines guidelines = readyGuidelines();
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(guidelines));

		ThesisProposal proposal = org.mockito.Mockito.mock(ThesisProposal.class);
		when(thesis.getProposals()).thenReturn(List.of(proposal));
		Resource pdfResource = new ByteArrayResource("pdf".getBytes());
		when(thesisService.getProposalFile(proposal)).thenReturn(pdfResource);

		when(reviewService.review(eq(pdfResource), eq(ReviewType.PROPOSAL), eq(guidelines.getStructuredGuidelines())))
				.thenReturn(new ReviewResultDTO(AssessmentCategory.NEEDS_WORK, 40, "Needs work.", List.of(
						new FindingDTO("MAJOR", "STRUCTURE", "Missing related work", "Add a section.", List.of()))));

		// requestChanges is transactional, so a rejected batch leaves no feedback rows behind —
		// the score describing those rows must not be persisted on its own either.
		when(thesisService.requestChanges(any(), any(), anyList(), any()))
				.thenThrow(new ResourceInvalidParametersException("Feedback text too long"));

		assertThatThrownBy(() -> service.autoReviewAndSave(thesis, ReviewType.PROPOSAL))
				.isInstanceOf(ResourceInvalidParametersException.class);

		verify(reviewSummaryRepository, never()).save(any());
	}

	@Test
	void autoReviewAndSave_discardsTheSummaryWhenANewerDocumentWasUploadedDuringTheReview() {
		ResearchGroupGuidelines guidelines = readyGuidelines();
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(guidelines));

		ThesisProposal reviewedProposal = org.mockito.Mockito.mock(ThesisProposal.class);
		when(reviewedProposal.getId()).thenReturn(UUID.randomUUID());
		when(thesis.getProposals()).thenReturn(List.of(reviewedProposal));
		Resource pdfResource = new ByteArrayResource("pdf".getBytes());
		when(thesisService.getProposalFile(reviewedProposal)).thenReturn(pdfResource);

		when(reviewService.review(eq(pdfResource), eq(ReviewType.PROPOSAL), eq(guidelines.getStructuredGuidelines())))
				.thenReturn(new ReviewResultDTO(AssessmentCategory.NEEDS_WORK, 40, "Stale read.", List.of()));
		UUID thesisId = UUID.randomUUID();
		when(thesis.getId()).thenReturn(thesisId);

		// The student uploaded a new proposal while the pipeline was running, and a review of that
		// one may already have written the summary — this run's result is about the old revision.
		ThesisProposal newerProposal = org.mockito.Mockito.mock(ThesisProposal.class);
		when(newerProposal.getId()).thenReturn(UUID.randomUUID());
		when(proposalRepository.findFirstByThesisIdOrderByCreatedAtDesc(thesisId))
				.thenReturn(Optional.of(newerProposal));

		service.autoReviewAndSave(thesis, ReviewType.PROPOSAL);

		verify(reviewSummaryRepository, never()).save(any());
	}

	@Test
	void autoReviewAndSave_retriesAsUpdateWhenConcurrentInsertRacesTheSummary() {
		ResearchGroupGuidelines guidelines = readyGuidelines();
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(guidelines));

		UUID proposalId = UUID.randomUUID();
		ThesisProposal proposal = org.mockito.Mockito.mock(ThesisProposal.class);
		when(proposal.getId()).thenReturn(proposalId);
		when(thesis.getProposals()).thenReturn(List.of(proposal));
		Resource pdfResource = new ByteArrayResource("pdf".getBytes());
		when(thesisService.getProposalFile(proposal)).thenReturn(pdfResource);

		when(reviewService.review(eq(pdfResource), eq(ReviewType.PROPOSAL), eq(guidelines.getStructuredGuidelines())))
				.thenReturn(new ReviewResultDTO(AssessmentCategory.GOOD, 90, "All good.", List.of()));
		UUID thesisId = UUID.randomUUID();
		when(thesis.getId()).thenReturn(thesisId);
		when(proposalRepository.findFirstByThesisIdOrderByCreatedAtDesc(thesisId)).thenReturn(Optional.of(proposal));

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
		assertThat(concurrentlyInserted.getDocumentVersionId()).isEqualTo(proposalId);
	}

	@Test
	void classifyFeedbackLine_mapsLenientlySpelledLlmValues() {
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(readyGuidelines()));
		// The prompt asks for upper-case enum names, but nothing enforces the casing at the schema
		// level — a lower-case answer must still land on the right dropdown value.
		when(feedbackClassificationService.classify("Cite a peer-reviewed source for this claim."))
				.thenReturn(new FeedbackClassificationResult("citation", " Major "));

		FeedbackClassificationDTO classification =
				service.classifyFeedbackLine(thesis, "  Cite a peer-reviewed source for this claim.  ");

		assertThat(classification.category()).isEqualTo(ThesisFeedbackCategory.CITATION);
		assertThat(classification.severity()).isEqualTo(ThesisFeedbackSeverity.MAJOR);
	}

	@Test
	void classifyFeedbackLine_degradesUnknownCategoryAndKeepsMissingSeverityOpen() {
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(readyGuidelines()));
		when(feedbackClassificationService.classify("Reword the abstract."))
				.thenReturn(new FeedbackClassificationResult("tone-of-voice", null));

		FeedbackClassificationDTO classification = service.classifyFeedbackLine(thesis, "Reword the abstract.");

		// An off-enum category is recorded as OTHER; an omitted severity stays null so the UI
		// leaves that dropdown to the instructor instead of guessing.
		assertThat(classification.category()).isEqualTo(ThesisFeedbackCategory.OTHER);
		assertThat(classification.severity()).isNull();
	}

	@Test
	void classifyFeedbackLine_returnsEmptySuggestionWhenLlmReturnsNothing() {
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(readyGuidelines()));
		when(feedbackClassificationService.classify("Add a schedule section.")).thenReturn(null);

		FeedbackClassificationDTO classification = service.classifyFeedbackLine(thesis, "Add a schedule section.");

		assertThat(classification.category()).isNull();
		assertThat(classification.severity()).isNull();
	}

	@Test
	void classifyFeedbackLine_capsTheTextHandedToTheLlm() {
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(readyGuidelines()));
		when(feedbackClassificationService.classify(any()))
				.thenReturn(new FeedbackClassificationResult("WRITING", "MINOR"));

		service.classifyFeedbackLine(thesis, "x".repeat(5000));

		ArgumentCaptor<String> classified = ArgumentCaptor.forClass(String.class);
		verify(feedbackClassificationService).classify(classified.capture());
		// A pasted wall of text must not turn one dropdown suggestion into an unbounded LLM bill.
		assertThat(classified.getValue()).hasSize(2000);
	}

	@Test
	void classifyFeedbackLine_rejectsBlankFeedbackWithoutCallingTheLlm() {
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(readyGuidelines()));

		assertThatThrownBy(() -> service.classifyFeedbackLine(thesis, "   "))
				.isInstanceOf(ResourceInvalidParametersException.class)
				.hasMessageContaining("empty feedback line");

		verify(feedbackClassificationService, never()).classify(any());
	}

	@Test
	void classifyFeedbackLine_appliesTheSamePerGroupAiGateAsAReview() {
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.classifyFeedbackLine(thesis, "Cite a source."))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessageContaining("not set up for your research group yet");

		verify(feedbackClassificationService, never()).classify(any());
	}
}
