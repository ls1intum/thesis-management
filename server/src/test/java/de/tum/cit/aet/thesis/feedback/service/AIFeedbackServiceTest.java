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
import de.tum.cit.aet.thesis.feedback.dto.AIFeedbackDraftDTO;
import de.tum.cit.aet.thesis.feedback.dto.AIPreviewResponseDTO;
import de.tum.cit.aet.thesis.feedback.dto.FeedbackClassificationDTO;
import de.tum.cit.aet.thesis.feedback.dto.ImportedNotesDTO;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.CategoryGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.model.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.model.FeedbackClassificationResult;
import de.tum.cit.aet.thesis.feedback.model.Finding;
import de.tum.cit.aet.thesis.feedback.model.Location;
import de.tum.cit.aet.thesis.feedback.model.NoteEntry;
import de.tum.cit.aet.thesis.feedback.model.NoteSplitResult;
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
import java.util.stream.IntStream;

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
	private FeedbackClassificationService feedbackClassificationService;

	@Mock
	private NoteSplittingService noteSplittingService;

	@Mock
	private Thesis thesis;

	private AIFeedbackService service;

	private static final StructuredGuidelines GUIDELINES = new StructuredGuidelines(
			"Overview.", List.of(new CategoryGuidelines("bibliography", List.of("Cite at least 6 sources."))));

	private final Resource pdf = new ByteArrayResource("pdf".getBytes());
	private final UUID versionId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new AIFeedbackService(reviewer, thesisService, guidelinesGate, documents, summaryWriter,
				feedbackClassificationService, noteSplittingService);
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

	@Test
	void classifyFeedbackLineMapsLenientlySpelledLlmValues() {
		when(guidelinesGate.requireReady(any())).thenReturn(GUIDELINES);
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
	void classifyFeedbackLineDegradesUnknownCategoryAndKeepsMissingSeverityOpen() {
		when(guidelinesGate.requireReady(any())).thenReturn(GUIDELINES);
		when(feedbackClassificationService.classify("Reword the abstract."))
				.thenReturn(new FeedbackClassificationResult("tone-of-voice", null));

		FeedbackClassificationDTO classification = service.classifyFeedbackLine(thesis, "Reword the abstract.");

		// An off-enum category is recorded as OTHER; an omitted severity stays null so the UI leaves
		// that dropdown to the instructor instead of guessing.
		assertThat(classification.category()).isEqualTo(ThesisFeedbackCategory.OTHER);
		assertThat(classification.severity()).isNull();
	}

	@Test
	void classifyFeedbackLineReturnsAnEmptySuggestionWhenTheLlmReturnsNothing() {
		when(guidelinesGate.requireReady(any())).thenReturn(GUIDELINES);
		when(feedbackClassificationService.classify("Add a schedule section.")).thenReturn(null);

		FeedbackClassificationDTO classification = service.classifyFeedbackLine(thesis, "Add a schedule section.");

		assertThat(classification.category()).isNull();
		assertThat(classification.severity()).isNull();
	}

	@Test
	void classifyFeedbackLineCapsTheTextHandedToTheLlm() {
		when(guidelinesGate.requireReady(any())).thenReturn(GUIDELINES);
		when(feedbackClassificationService.classify(any()))
				.thenReturn(new FeedbackClassificationResult("WRITING", "MINOR"));

		service.classifyFeedbackLine(thesis, "x".repeat(5000));

		ArgumentCaptor<String> classified = ArgumentCaptor.forClass(String.class);
		verify(feedbackClassificationService).classify(classified.capture());
		// A pasted wall of text must not turn one dropdown suggestion into an unbounded LLM bill.
		assertThat(classified.getValue()).hasSize(2000);
	}

	@Test
	void classifyFeedbackLineRejectsBlankFeedbackWithoutCallingTheLlm() {
		when(guidelinesGate.requireReady(any())).thenReturn(GUIDELINES);

		assertThatThrownBy(() -> service.classifyFeedbackLine(thesis, "   "))
				.isInstanceOf(ResourceInvalidParametersException.class)
				.hasMessageContaining("empty feedback line");

		verify(feedbackClassificationService, never()).classify(any());
	}

	@Test
	void classifyFeedbackLineAppliesTheSamePerGroupAiGateAsAReview() {
		when(guidelinesGate.requireReady(any())).thenThrow(new AccessDeniedException("not set up"));

		assertThatThrownBy(() -> service.classifyFeedbackLine(thesis, "Cite a source."))
				.isInstanceOf(AccessDeniedException.class);

		verify(feedbackClassificationService, never()).classify(any());
	}

	@Test
	void importNotesKeepsTheOrderOfTheNotesAndMapsLenientlySpelledLlmValues() {
		when(guidelinesGate.requireReady(any())).thenReturn(GUIDELINES);
		when(noteSplittingService.split("fig 3 unreadable; cite Smith")).thenReturn(new NoteSplitResult(List.of(
				new NoteEntry("Figure 3 is unreadable.", "figures", " Major "),
				new NoteEntry("Cite Smith for this claim.", "CITATION", "MAJOR"))));

		ImportedNotesDTO imported = service.importNotes(thesis, "  fig 3 unreadable; cite Smith  ");

		// Notes follow the document, so the instructor's own order survives the import — unlike
		// preview drafts, which are ranked by severity.
		assertThat(imported.entries()).extracting(AIFeedbackDraftDTO::feedback)
				.containsExactly("Figure 3 is unreadable.", "Cite Smith for this claim.");
		assertThat(imported.entries().getFirst().category()).isEqualTo(ThesisFeedbackCategory.FIGURES);
		assertThat(imported.entries().getFirst().severity()).isEqualTo(ThesisFeedbackSeverity.MAJOR);
	}

	@Test
	void importNotesLeavesUnlabelledEntriesForTheInstructorToClassify() {
		when(guidelinesGate.requireReady(any())).thenReturn(GUIDELINES);
		when(noteSplittingService.split("ch 4 thin")).thenReturn(new NoteSplitResult(List.of(
				new NoteEntry("Chapter 4 is thin.", null, null))));

		ImportedNotesDTO imported = service.importNotes(thesis, "ch 4 thin");

		// A terse note often does not say enough to label it. Both dropdowns stay open rather than
		// being guessed at; the instructor picks them or asks for a classification.
		assertThat(imported.entries()).hasSize(1);
		assertThat(imported.entries().getFirst().category()).isNull();
		assertThat(imported.entries().getFirst().severity()).isNull();
	}

	@Test
	void importNotesDropsEntriesWithoutText() {
		when(guidelinesGate.requireReady(any())).thenReturn(GUIDELINES);
		when(noteSplittingService.split("notes")).thenReturn(new NoteSplitResult(List.of(
				new NoteEntry(null, "WRITING", "MINOR"),
				new NoteEntry("   ", "WRITING", "MINOR"),
				new NoteEntry("  Reword the abstract.  ", "WRITING", "MINOR"))));

		ImportedNotesDTO imported = service.importNotes(thesis, "notes");

		// An entry with no text is nothing the instructor could save or edit.
		assertThat(imported.entries()).extracting(AIFeedbackDraftDTO::feedback)
				.containsExactly("Reword the abstract.");
	}

	@Test
	void importNotesCapsTheNumberOfEntriesOneImportCanProduce() {
		when(guidelinesGate.requireReady(any())).thenReturn(GUIDELINES);
		when(noteSplittingService.split(any())).thenReturn(new NoteSplitResult(
				IntStream.range(0, 150).mapToObj(i -> new NoteEntry("Issue " + i, null, null)).toList()));

		ImportedNotesDTO imported = service.importNotes(thesis, "many notes");

		// A model that starts splitting prose into fragments must not leave the instructor with
		// hundreds of rows to delete by hand.
		assertThat(imported.entries()).hasSize(100);
	}

	@Test
	void importNotesCapsTheTextHandedToTheLlm() {
		when(guidelinesGate.requireReady(any())).thenReturn(GUIDELINES);
		when(noteSplittingService.split(any())).thenReturn(new NoteSplitResult(List.of()));

		service.importNotes(thesis, "x".repeat(50000));

		ArgumentCaptor<String> split = ArgumentCaptor.forClass(String.class);
		verify(noteSplittingService).split(split.capture());
		// One paste must not turn into an unbounded LLM bill.
		assertThat(split.getValue()).hasSize(20000);
	}

	@Test
	void importNotesReturnsNoEntriesWhenTheLlmReturnsNothing() {
		when(guidelinesGate.requireReady(any())).thenReturn(GUIDELINES);
		when(noteSplittingService.split("Looks good overall.")).thenReturn(null);

		assertThat(service.importNotes(thesis, "Looks good overall.").entries()).isEmpty();
	}

	@Test
	void importNotesRejectsBlankNotesWithoutCallingTheLlm() {
		when(guidelinesGate.requireReady(any())).thenReturn(GUIDELINES);

		assertThatThrownBy(() -> service.importNotes(thesis, "   "))
				.isInstanceOf(ResourceInvalidParametersException.class)
				.hasMessageContaining("empty notes");

		verify(noteSplittingService, never()).split(any());
	}

	@Test
	void importNotesAppliesTheSamePerGroupAiGateAsAReview() {
		when(guidelinesGate.requireReady(any())).thenThrow(new AccessDeniedException("not set up"));

		assertThatThrownBy(() -> service.importNotes(thesis, "fig 3 unreadable"))
				.isInstanceOf(AccessDeniedException.class);

		verify(noteSplittingService, never()).split(any());
	}
}
