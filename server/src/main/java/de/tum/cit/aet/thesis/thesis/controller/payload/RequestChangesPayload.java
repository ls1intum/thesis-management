package de.tum.cit.aet.thesis.thesis.controller.payload;

import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackType;

import java.util.List;

public record RequestChangesPayload(
		ThesisFeedbackType type,
		List<RequestedChange> requestedChanges
) {
	public record RequestedChange(
			String feedback,
			Boolean completed
	) {}
}
