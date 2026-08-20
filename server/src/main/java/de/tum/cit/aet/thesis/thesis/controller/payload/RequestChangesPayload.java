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
			ThesisFeedbackSeverity severity,
			// Optional per-item provenance proof. The client cannot pick a source directly (that
			// would let it forge AI labels); instead it echoes the opaque, server-signed token the
			// AI preview endpoint issued. The service stamps AI_REVIEWED_BY_HUMAN only when this
			// token validates for the current thesis and user, and HUMAN otherwise.
			String previewToken
	) {}
}
