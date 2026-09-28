package de.tum.cit.aet.thesis.feedback.progress;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * One progress update for a single step (LLM call) of an AI review run, pushed to the client over
 * {@code /user/queue/ai-review-progress/{jobId}}.
 *
 * @param jobId   the client-supplied id correlating events to one review request
 * @param stepId  a stable identifier for the step (a category slug, or {@code "merge"})
 * @param label   a human-readable label for the step; only sent when the step starts
 * @param status  whether the step started, completed, or failed
 * @param index   this step's 1-based position among all steps of the run
 * @param total   the total number of steps the run will make
 * @param message a short, safe-to-display failure reason, present only when {@code status} is
 *                {@link ReviewProgressStatus#FAILED}
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ReviewProgressEvent(
		UUID jobId,
		String stepId,
		String label,
		ReviewProgressStatus status,
		int index,
		int total,
		String message
) {}
