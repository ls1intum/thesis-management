package de.tum.cit.aet.thesis.thesis.constants;

/**
 * Classification of a feedback item along dimensions the AI review pipeline also uses. Matches
 * the {@code category} field produced by the LLM merger so AI-generated findings can be stored
 * verbatim. {@code OTHER} is the catch-all when nothing else fits.
 */
public enum ThesisFeedbackCategory {
	FORMATTING,
	STRUCTURE,
	CITATION,
	METHODOLOGY,
	WRITING,
	FIGURES,
	LOGIC,
	COMPLETENESS,
	OTHER
}
