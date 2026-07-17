package de.tum.cit.aet.thesis.feedback.service.reviewer;

/**
 * Marks whether a review targets a student proposal or a final thesis. Selects which prompt
 * variant is dispatched to the LLM for each {@link ReviewCategory}.
 */
public enum ReviewType {
	PROPOSAL,
	THESIS
}
