package de.tum.cit.aet.thesis.feedback.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tum.cit.aet.thesis.feedback.model.EarlierFeedback;
import de.tum.cit.aet.thesis.feedback.model.Finding;
import de.tum.cit.aet.thesis.feedback.model.FollowUp;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class EvalComparisonScoreTest {

	private static final EvalEarlierFeedback TRUTH = new EvalEarlierFeedback(List.of(
			new EvalEarlierFeedback.Entry("Agreement in the abstract.", "WRITING", "MINOR", true, "PERSISTS"),
			new EvalEarlierFeedback.Entry("Number every figure.", "FIGURES", "MINOR", true, "RESOLVED"),
			new EvalEarlierFeedback.Entry("Cite six sources.", "CITATION", "MAJOR", true, "PERSISTS"),
			new EvalEarlierFeedback.Entry("Add related work.", "STRUCTURE", "MAJOR", false, null)));

	private static FollowUp followUp(String ref) {
		return new FollowUp(ref, new Finding("MINOR", "WRITING", "t", "d", List.of()));
	}

	@Test
	void scoresFollowUpsAndAcknowledgementsAgainstTheGroundTruth() {
		ReviewResult result = new ReviewResult(null, null, null, List.of(),
				List.of(followUp("E1"), followUp("E2")), List.of("E2", "E3"));

		EvalComparisonScore score = EvalComparisonScore.score(TRUTH, result);

		assertThat(score.expectedPersists()).containsExactly("E1", "E3");
		assertThat(score.expectedResolved()).containsExactly("E2");
		assertThat(score.followUpPrecision()).isEqualTo(0.5);
		assertThat(score.followUpRecall()).isEqualTo(0.5);
		assertThat(score.acknowledgementPrecision()).isEqualTo(0.5);
		assertThat(score.acknowledgementRecall()).isEqualTo(1.0);
		assertThat(score.falsePraise()).containsExactly("E3");
		assertThat(score.missedRecurrences()).containsExactly("E3");
		assertThat(score.openTouched()).isEmpty();
	}

	@Test
	void anEmptyComparisonHasNoPrecisionAndFlagsTouchedOpenEntries() {
		EvalComparisonScore none = EvalComparisonScore.score(TRUTH, new ReviewResult(null, null, null, List.of()));
		assertThat(none.followUpPrecision()).isNull();
		assertThat(none.acknowledgementPrecision()).isNull();
		assertThat(none.followUpRecall()).isZero();

		EvalComparisonScore touched = EvalComparisonScore.score(TRUTH,
				new ReviewResult(null, null, null, List.of(), List.of(followUp("E4")), List.of("E4")));
		assertThat(touched.openTouched()).containsExactly("E4");
	}

	@Test
	void theSidecarIsReadNextToItsDocumentAndTurnedIntoEarlierFeedback(@TempDir Path dir) throws Exception {
		Path pdf = dir.resolve("proposal-x.pdf");
		Files.writeString(pdf, "not really a pdf");
		Files.writeString(dir.resolve("proposal-x.earlier.json"), """
				{"entries": [
				{"text": "Agreement.", "category": "WRITING", "completed": true, "expected": "PERSISTS"},
				{"text": "Related work.", "completed": false}
				]}
				""");

		List<EarlierFeedback> earlier = EvalEarlierFeedback.sidecarFor(pdf).orElseThrow().toEarlierFeedback();

		assertThat(earlier).extracting(EarlierFeedback::ref).containsExactly("E1", "E2");
		assertThat(earlier).extracting(EarlierFeedback::completed).containsExactly(true, false);
		assertThat(EvalEarlierFeedback.sidecarFor(dir.resolve("other.pdf"))).isEmpty();
	}

	@Test
	void aCompletedEntryWithoutGroundTruthIsRejected(@TempDir Path dir) throws Exception {
		Path pdf = dir.resolve("proposal-x.pdf");
		Files.writeString(dir.resolve("proposal-x.earlier.json"),
				"{\"entries\": [{\"text\": \"Agreement.\", \"completed\": true}]}");

		assertThatThrownBy(() -> EvalEarlierFeedback.sidecarFor(pdf))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("expected");
	}
}
