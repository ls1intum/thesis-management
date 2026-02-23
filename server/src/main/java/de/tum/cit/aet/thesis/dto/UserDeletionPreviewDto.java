package de.tum.cit.aet.thesis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record UserDeletionPreviewDto(
		boolean canBeFullyDeleted,
		boolean hasActiveTheses,
		int retentionBlockedThesisCount,
		Instant earliestFullDeletionDate,
		boolean isResearchGroupHead,
		String message
) {
}
