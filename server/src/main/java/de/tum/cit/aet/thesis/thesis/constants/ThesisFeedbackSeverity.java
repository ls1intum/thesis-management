package de.tum.cit.aet.thesis.thesis.constants;

/**
 * Severity levels that mirror what the AI review pipeline emits per finding. Ordered from
 * most to least urgent for UI presentation.
 */
public enum ThesisFeedbackSeverity {
	CRITICAL,
	MAJOR,
	MINOR,
	SUGGESTION
}
