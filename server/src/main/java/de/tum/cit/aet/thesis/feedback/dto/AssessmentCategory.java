package de.tum.cit.aet.thesis.feedback.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AssessmentCategory {
	@JsonProperty("good")
	GOOD,
	@JsonProperty("acceptable")
	ACCEPTABLE,
	@JsonProperty("needs-work")
	NEEDS_WORK
}
