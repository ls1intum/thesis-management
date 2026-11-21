package de.tum.cit.aet.thesis.feedback.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Lifecycle status of a research group's AI review guidelines.
 *
 * <p>Only {@link #READY} guidelines unlock the AI review features for the group's members;
 * {@link #FAILED} records preserve the lead's raw input together with the reason the
 * preprocessing step rejected it (for example, guidelines that were too vague to distill into
 * specific, actionable rules).
 */
public enum GuidelinesStatus {
	@JsonProperty("ready")
	READY,
	@JsonProperty("failed")
	FAILED
}
