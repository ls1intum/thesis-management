package de.tum.cit.aet.thesis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record UserDeletionPreviewDto(
		Boolean canBeFullyDeleted,
		int retentionBlockedThesisCount,
		Instant earliestFullDeletionDate,
		Boolean isResearchGroupHead,
		String message
) {
}
