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
import de.tum.cit.aet.thesis.feedback.dto.AIPreviewResponseDTO;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.CategoryGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.model.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.model.Finding;
import de.tum.cit.aet.thesis.feedback.model.Location;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import de.tum.cit.aet.thesis.feedback.review.ReviewRequest;
import de.tum.cit.aet.thesis.feedback.review.ThesisReviewer;
import de.tum.cit.aet.thesis.feedback.service.ReviewDocuments.ReviewDocument;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackCategory;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSeverity;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSource;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackType;
import de.tum.cit.aet.thesis.thesis.controller.payload.RequestChangesPayload.RequestedChange;
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
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class AIFeedbackServiceTest {

	@Mock
	private ThesisReviewer reviewer;

	@Mock
	private ThesisService thesisService;

	@Mock
	private GuidelinesGate guidelinesGate;

	@Mock
	private ReviewDocuments documents;

	@Mock
	private ReviewSummaryWriter summaryWriter;

	@Mock
	private Thesis thesis;

	private AIFeedbackService service;

	private static final StructuredGuidelines GUIDELINES = new StructuredGuidelines(
			"Overview.", List.of(new CategoryGuidelines("bibliography", List.of("Cite at least 6 sources."))));

	private final Resource pdf = new ByteArrayResource("pdf".getBytes());
	private final UUID versionId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new AIFeedbackService(reviewer, thesisService, guidelinesGate, documents, summaryWriter);
	}

	/** Wires the happy path up to (but excluding) the review call itself. */
	private void givenReadyGuidelinesAndDocument(ReviewType reviewType) {
		when(guidelinesGate.requireReady(any())).thenReturn(GUIDELINES);
		when(documents.load(thesis, reviewType)).thenReturn(new ReviewDocument(pdf, versionId));
	}

	private void givenReviewResult(ReviewResult result) {
		when(reviewer.review(any(ReviewRequest.class))).thenReturn(result);
	}

	@Test
	void previewReviewHandsTheDocumentAndGroupGuidelinesToTheReviewer() {
		givenReadyGuidelinesAndDocument(ReviewType.PROPOSAL);
		givenReviewResult(new ReviewResult(AssessmentCategory.GOOD, 90, "All good.", List.of()));

		AIPreviewResponseDTO response = service.previewReview(thesis, ReviewType.PROPOSAL);

		assertThat(response.summary()).isEqualTo("All good.");
		assertThat(response.score()).isEqualTo(90);
		assertThat(response.assessment()).isEqualTo(AssessmentCategory.GOOD);

		ArgumentCaptor<ReviewRequest> request = ArgumentCaptor.forClass(ReviewRequest.class);
		verify(reviewer).review(request.capture());
		assertThat(request.getValue().type()).isEqualTo(ReviewType.PROPOSAL);
		assertThat(request.getValue().guidelines()).isSameAs(GUIDELINES);
		assertThat(request.getValue().document()).isSameAs(pdf);

		// A preview is supervisor-only and provisional (drafts may be edited/discarded), so it must
		// never persist a summary the student-visible feedback overview would then show.
		verify(summaryWriter, never()).record(any(), any(), any(), any());
	}

	@Test
	void previewReviewDropsAnOutOfRangeScore() {
		givenReadyGuidelinesAndDocument(ReviewType.PROPOSAL);
		givenReviewResult(new ReviewResult(AssessmentCategory.GOOD, 150, "All good.", List.of()));

		assertThat(service.previewReview(thesis, ReviewType.PROPOSAL).score()).isNull();
	}

	@Test
	void previewReviewRanksDraftsMostSevereFirst() {
		givenReadyGuidelinesAndDocument(ReviewType.THESIS);
		givenReviewResult(new ReviewResult(AssessmentCategory.NEEDS_WORK, 30, "Needs work.", List.of(
				new Finding("SUGGESTION", "WRITING", "Tighten the abstract", null, List.of()),
				new Finding("CRITICAL", "STRUCTURE", "No evaluation chapter", null, List.of()),
				new Finding("MINOR", "FORMATTING", "Heading case", null, List.of()))));

		AIPreviewResponseDTO response = service.previewReview(thesis, ReviewType.THESIS);

		assertThat(response.drafts()).extracting(draft -> draft.severity())
				.containsExactly(ThesisFeedbackSeverity.CRITICAL, ThesisFeedbackSeverity.MINOR,
						ThesisFeedbackSeverity.SUGGESTION);
	}

	@Test
	void previewReviewGatesOnGuidelinesBeforeLookingForADocument() {
		// A group that never set the feature up should hear about that, not about a missing upload.
		when(guidelinesGate.requireReady(any())).thenThrow(new AccessDeniedException("not set up"));

		assertThatThrownBy(() -> service.previewReview(thesis, ReviewType.PROPOSAL))
				.isInstanceOf(AccessDeniedException.class);

		verify(documents, never()).load(any(), any());
		verify(reviewer, never()).review(any());
	}

	@Test
	void autoReviewAndSavePersistsEachFindingAsAiFeedback() {
		givenReadyGuidelinesAndDocument(ReviewType.PROPOSAL);
		givenReviewResult(new ReviewResult(AssessmentCategory.NEEDS_WORK, 40, "Needs work.", List.of(
				new Finding("MAJOR", "STRUCTURE", "Missing related work", "Add a section.",
						List.of(new Location(4, "Introduction", "..."))))));
		Thesis updated = org.mockito.Mockito.mock(Thesis.class);
		when(thesisService.requestChanges(any(), any(), anyList(), any())).thenReturn(updated);

		assertThat(service.autoReviewAndSave(thesis, ReviewType.PROPOSAL)).isSameAs(updated);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<RequestedChange>> changes = ArgumentCaptor.forClass(List.class);
		verify(thesisService).requestChanges(eq(thesis), eq(ThesisFeedbackType.PROPOSAL), changes.capture(),
				eq(ThesisFeedbackSource.AI));
		assertThat(changes.getValue()).hasSize(1);
		RequestedChange change = changes.getValue().getFirst();
		assertThat(change.feedback()).isEqualTo("Missing related work — Add a section. (Page 4, Introduction)");
		assertThat(change.category()).isEqualTo(ThesisFeedbackCategory.STRUCTURE);
		assertThat(change.severity()).isEqualTo(ThesisFeedbackSeverity.MAJOR);

		verify(summaryWriter).record(eq(thesis), eq(ReviewType.PROPOSAL), any(), eq(versionId));
	}

	@Test
	void autoReviewAndSaveRecordsTheSummaryEvenWithNoActionableFindings() {
		givenReadyGuidelinesAndDocument(ReviewType.PROPOSAL);
		givenReviewResult(new ReviewResult(AssessmentCategory.GOOD, 95, "Nothing to flag.", List.of()));

		assertThat(service.autoReviewAndSave(thesis, ReviewType.PROPOSAL)).isSameAs(thesis);

		verify(thesisService, never()).requestChanges(any(), any(), anyList(), any());
		verify(summaryWriter).record(eq(thesis), eq(ReviewType.PROPOSAL), any(), eq(versionId));
	}

	@Test
	void autoReviewAndSaveDoesNotRecordTheSummaryWhenSavingTheFindingsFails() {
		givenReadyGuidelinesAndDocument(ReviewType.PROPOSAL);
		givenReviewResult(new ReviewResult(AssessmentCategory.NEEDS_WORK, 40, "Needs work.", List.of(
				new Finding("MAJOR", "STRUCTURE", "Missing related work", "Add a section.", List.of()))));

		// requestChanges is transactional, so a rejected batch leaves no feedback rows behind — the
		// score describing those rows must not be persisted on its own either.
		when(thesisService.requestChanges(any(), any(), anyList(), any()))
				.thenThrow(new ResourceInvalidParametersException("Feedback text too long"));

		assertThatThrownBy(() -> service.autoReviewAndSave(thesis, ReviewType.PROPOSAL))
				.isInstanceOf(ResourceInvalidParametersException.class);

		verify(summaryWriter, never()).record(any(), any(), any(), any());
	}

	@Test
	void assertHasDocumentDelegatesToTheDocumentLookup() {
		when(documents.load(thesis, ReviewType.THESIS))
				.thenThrow(new ResourceInvalidParametersException("Thesis has no uploaded thesis document"));

		assertThatThrownBy(() -> service.assertHasDocument(thesis, ReviewType.THESIS))
				.isInstanceOf(ResourceInvalidParametersException.class);
	}
}
