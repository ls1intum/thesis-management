package de.tum.cit.aet.thesis.thesis.controller.payload;

import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackCategory;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSeverity;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackType;

import java.util.List;

public record RequestChangesPayload(
		ThesisFeedbackType type,
		List<RequestedChange> requestedChanges
) {
	public record RequestedChange(
			String feedback,
			Boolean completed,
			// Optional per-item classification. Instructors can now assign a category and severity
			// when adding manual feedback; when omitted, the item is treated as uncategorized.
			ThesisFeedbackCategory category,
			ThesisFeedbackSeverity severity
	) {}
}
