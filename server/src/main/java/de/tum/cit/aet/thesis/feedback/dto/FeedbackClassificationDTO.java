package de.tum.cit.aet.thesis.feedback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackCategory;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSeverity;

/**
 * The category and severity suggested for one manually written feedback line. Either field may be
 * {@code null} when the LLM did not commit to a value; with {@code NON_EMPTY} serialization such a
 * field is simply absent from the response and the client leaves that dropdown untouched.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record FeedbackClassificationDTO(
		ThesisFeedbackCategory category,
		ThesisFeedbackSeverity severity
) {}
