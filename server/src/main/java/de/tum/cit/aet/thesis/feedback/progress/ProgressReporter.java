package de.tum.cit.aet.thesis.feedback.progress;

/**
 * Sink for per-call progress events emitted while a {@link de.tum.cit.aet.thesis.feedback.review.ThesisReviewer}
 * runs. A review is made of several independent LLM calls (one per category, plus a merge); this
 * interface lets the pipeline report which of those calls have started, finished, or failed without
 * knowing anything about who — if anyone — is listening.
 */
public interface ProgressReporter {

	/** A reporter that discards every event, for callers that do not want progress tracking. */
	ProgressReporter NOOP = new ProgressReporter() {
		@Override
		public void stepStarted(String stepId, String label, int index, int total) {
		}

		@Override
		public void stepCompleted(String stepId, int index, int total) {
		}

		@Override
		public void stepFailed(String stepId, int index, int total, String message) {
		}
	};

	/**
	 * Reports that one call has been dispatched.
	 *
	 * @param stepId a stable identifier for the call (e.g. a category slug, or {@code "merge"})
	 * @param label  a human-readable label for the call
	 * @param index  this call's 1-based position among all calls of the run
	 * @param total  the total number of calls the run will make
	 */
	void stepStarted(String stepId, String label, int index, int total);

	/**
	 * Reports that one call finished successfully.
	 *
	 * @param stepId the identifier previously passed to {@link #stepStarted}
	 * @param index  this call's 1-based position among all calls of the run
	 * @param total  the total number of calls the run will make
	 */
	void stepCompleted(String stepId, int index, int total);

	/**
	 * Reports that one call failed.
	 *
	 * @param stepId  the identifier previously passed to {@link #stepStarted}
	 * @param index   this call's 1-based position among all calls of the run
	 * @param total   the total number of calls the run will make
	 * @param message a short, safe-to-display failure reason
	 */
	void stepFailed(String stepId, int index, int total, String message);
}
