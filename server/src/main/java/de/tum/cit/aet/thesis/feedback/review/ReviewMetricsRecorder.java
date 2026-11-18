package de.tum.cit.aet.thesis.feedback.review;

import de.tum.cit.aet.thesis.feedback.model.ReviewCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Where a finished run's {@link ReviewRunMetrics} go. In the running application that is the log;
 * an evaluation sweep substitutes a recorder that keeps them, so the measured cost of each run can
 * be written next to its findings instead of scraped back out of log lines.
 *
 * <p>A seam rather than a direct {@code log.info} because the numbers are the point of comparing
 * two strategies, and a comparison whose inputs have to be parsed out of text is one nobody reruns.
 *
 * <p>Besides the totals, a strategy with intermediate stages reports what happened inside them
 * ({@link #recordCandidates}, {@link #recordCategoryFailure}). The application ignores both — the
 * log lines of the stages already cover operations — so they default to doing nothing, and only
 * the evaluation keeps them.
 */
@FunctionalInterface
public interface ReviewMetricsRecorder {

	/** The default: one line per finished review, at INFO. */
	ReviewMetricsRecorder LOGGING = new ReviewMetricsRecorder() {
		private final Logger log = LoggerFactory.getLogger(ReviewMetricsRecorder.class);

		@Override
		public void record(ReviewRunMetrics metrics) {
			log.info("AI review finished: {}", metrics);
		}
	};

	/**
	 * Records the metrics of one finished review.
	 *
	 * @param metrics what the run cost
	 */
	void record(ReviewRunMetrics metrics);

	/**
	 * Records what one category pass produced before the merge. Called concurrently, once per
	 * category that answered.
	 *
	 * @param candidates the category's findings and what verification did with them
	 */
	default void recordCandidates(CategoryCandidates candidates) {
	}

	/**
	 * Records that one category pass failed and was dropped from the run. Called concurrently.
	 *
	 * @param category the category that failed
	 * @param cause    why it failed
	 */
	default void recordCategoryFailure(ReviewCategory category, Throwable cause) {
	}
}
