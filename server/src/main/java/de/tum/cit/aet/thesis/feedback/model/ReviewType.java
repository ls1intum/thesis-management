package de.tum.cit.aet.thesis.feedback.model;

/**
 * Marks whether a review targets a student proposal or a final thesis. Every reviewer
 * implementation branches on this to pick the prompt variant and the applicable rules.
 */
public enum ReviewType {
	PROPOSAL,
	THESIS
}
