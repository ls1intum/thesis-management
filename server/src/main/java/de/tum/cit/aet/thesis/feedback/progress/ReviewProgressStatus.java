package de.tum.cit.aet.thesis.feedback.progress;

/** Lifecycle of one step (LLM call) within an AI review run, as published over the websocket. */
public enum ReviewProgressStatus {
	STARTED,
	COMPLETED,
	FAILED
}
