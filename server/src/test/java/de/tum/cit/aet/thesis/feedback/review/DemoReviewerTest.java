package de.tum.cit.aet.thesis.feedback.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.model.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import de.tum.cit.aet.thesis.feedback.progress.ProgressReporter;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;

class DemoReviewerTest {

	@Test
	void returnsTheScriptedFindingsAndReportsEveryStep() {
		List<String> events = new ArrayList<>();
		ProgressReporter progress = new ProgressReporter() {
			@Override
			public void stepStarted(String stepId, String label, int index, int total) {
				events.add("start:" + stepId);
			}

			@Override
			public void stepCompleted(String stepId, int index, int total) {
				events.add("done:" + stepId);
			}

			@Override
			public void stepFailed(String stepId, int index, int total, String message) {
				events.add("fail:" + stepId);
			}
		};
		ReviewRequest request = new ReviewRequest(ReviewType.PROPOSAL,
				new StructuredGuidelines("overview", List.of()), mock(Resource.class), progress);

		ReviewResult result = new DemoReviewer(0).review(request);

		assertThat(result.assessment()).isEqualTo(AssessmentCategory.GOOD);
		assertThat(result.findings()).hasSize(4);
		int steps = ReviewCategory.values().length + 1;
		assertThat(events).hasSize(2 * steps).doesNotContainAnyElementsOf(List.of("fail:merge"));
		assertThat(events.getLast()).isEqualTo("done:merge");
	}
}
