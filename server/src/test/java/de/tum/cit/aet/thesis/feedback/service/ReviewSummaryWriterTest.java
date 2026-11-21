package de.tum.cit.aet.thesis.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.feedback.entity.AIReviewSummary;
import de.tum.cit.aet.thesis.feedback.model.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import de.tum.cit.aet.thesis.feedback.repository.AIReviewSummaryRepository;
import de.tum.cit.aet.thesis.thesis.entity.Thesis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ReviewSummaryWriterTest {

	@Mock
	private AIReviewSummaryRepository repository;

	@Mock
	private ReviewDocuments documents;

	@Mock
	private Thesis thesis;

	private ReviewSummaryWriter writer;

	private final UUID thesisId = UUID.randomUUID();
	private final UUID versionId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		writer = new ReviewSummaryWriter(repository, documents);
		when(thesis.getId()).thenReturn(thesisId);
	}

	private static ReviewResult result(Integer score) {
		return new ReviewResult(AssessmentCategory.GOOD, score, "All good.", List.of());
	}

	@Test
	void upsertsScoreAssessmentAndReviewedVersion() {
		when(documents.isCurrentRevision(thesisId, ReviewType.PROPOSAL, versionId)).thenReturn(true);
		when(repository.findByThesisIdAndType(thesisId, ReviewType.PROPOSAL)).thenReturn(Optional.empty());

		writer.record(thesis, ReviewType.PROPOSAL, result(95), versionId);

		ArgumentCaptor<AIReviewSummary> saved = ArgumentCaptor.forClass(AIReviewSummary.class);
		verify(repository).save(saved.capture());
		assertThat(saved.getValue().getScore()).isEqualTo(95);
		assertThat(saved.getValue().getAssessment()).isEqualTo(AssessmentCategory.GOOD);
		// Without the version the UI cannot tell this summary apart from one describing an older
		// upload, and would keep showing the stale score as current.
		assertThat(saved.getValue().getDocumentVersionId()).isEqualTo(versionId);
		assertThat(saved.getValue().getType()).isEqualTo(ReviewType.PROPOSAL);
	}

	@Test
	void dropsAnOutOfRangeScoreRatherThanPersistingIt() {
		when(documents.isCurrentRevision(thesisId, ReviewType.THESIS, versionId)).thenReturn(true);
		when(repository.findByThesisIdAndType(thesisId, ReviewType.THESIS)).thenReturn(Optional.empty());

		writer.record(thesis, ReviewType.THESIS, result(150), versionId);

		ArgumentCaptor<AIReviewSummary> saved = ArgumentCaptor.forClass(AIReviewSummary.class);
		verify(repository).save(saved.capture());
		assertThat(saved.getValue().getScore()).isNull();
	}

	@Test
	void discardsTheSummaryWhenANewerDocumentWasUploadedDuringTheReview() {
		// The student uploaded a new proposal while the pipeline was running, and a review of that
		// one may already have written the summary — this run's result is about the old revision.
		when(documents.isCurrentRevision(thesisId, ReviewType.PROPOSAL, versionId)).thenReturn(false);

		writer.record(thesis, ReviewType.PROPOSAL, result(40), versionId);

		verify(repository, never()).save(any());
	}

	@Test
	void retriesAsUpdateWhenAConcurrentInsertRacesTheSummary() {
		when(documents.isCurrentRevision(thesisId, ReviewType.PROPOSAL, versionId)).thenReturn(true);

		// Both this run and a concurrent one see no existing row first...
		AIReviewSummary concurrentlyInserted = new AIReviewSummary();
		when(repository.findByThesisIdAndType(thesisId, ReviewType.PROPOSAL))
				.thenReturn(Optional.empty())
				.thenReturn(Optional.of(concurrentlyInserted));
		// ...so this run's own insert violates the (thesis_id, type) unique constraint.
		when(repository.save(ArgumentMatchers.argThat(candidate -> candidate != concurrentlyInserted)))
				.thenThrow(new DataIntegrityViolationException("duplicate key"));

		writer.record(thesis, ReviewType.PROPOSAL, result(90), versionId);

		// The retry updates the row the concurrent run just inserted rather than failing.
		verify(repository).save(concurrentlyInserted);
		assertThat(concurrentlyInserted.getScore()).isEqualTo(90);
		assertThat(concurrentlyInserted.getAssessment()).isEqualTo(AssessmentCategory.GOOD);
		assertThat(concurrentlyInserted.getDocumentVersionId()).isEqualTo(versionId);
	}
}
