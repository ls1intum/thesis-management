package de.tum.cit.aet.thesis.feedback.progress;

/**
 * Lifecycle of one step (LLM call) within an AI review run, as published over the websocket, plus
 * the one value the subscription handshake uses.
 */
public enum ReviewProgressStatus {
	/**
	 * Acknowledges that the client's subscription to this job's destination is live and receiving —
	 * not a review step. See {@link ReviewProgressSubscriptionController}.
	 */
	SUBSCRIBED,
	STARTED,
	COMPLETED,
	FAILED
}
