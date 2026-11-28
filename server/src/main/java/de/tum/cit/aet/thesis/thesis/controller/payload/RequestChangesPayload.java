package de.tum.cit.aet.thesis.thesis.controller.payload;

import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackCategory;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSeverity;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSource;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackType;

import java.util.List;
import java.util.UUID;

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
			// Optional per-item provenance. Lets a single batch mix manually typed rows (HUMAN)
			// with AI-drafted rows the instructor reviewed (AI_REVIEWED_BY_HUMAN). When omitted,
			// the item inherits the batch-level source passed to the service.
			ThesisFeedbackSource source,
			// Optional link to the earlier, completed entry whose defect this one reports as
			// recurring. Must belong to the same thesis and feedback type.
			UUID followsUpId,
			// Optional: the follow-up defers a repeatedly recurring defect to the supervisor.
			Boolean deferredToSupervisor,
			// Optional: the document revision an AI review read when it drafted this entry. Must be a
			// revision of the same thesis and type; omitted, the entry is pinned to the latest one.
			UUID documentVersionId
	) {
		/**
		 * A change that follows up nothing, the shape every manually written entry has.
		 *
		 * @param feedback  the feedback text
		 * @param completed whether the entry starts out completed
		 * @param category  the optional category
		 * @param severity  the optional severity
		 * @param source    the optional per-item source
		 */
		public RequestedChange(String feedback, Boolean completed, ThesisFeedbackCategory category,
				ThesisFeedbackSeverity severity, ThesisFeedbackSource source) {
			this(feedback, completed, category, severity, source, null, null, null);
		}

		/**
		 * A change that may follow up an earlier entry, pinned to the batch's revision.
		 *
		 * @param feedback             the feedback text
		 * @param completed            whether the entry starts out completed
		 * @param category             the optional category
		 * @param severity             the optional severity
		 * @param source               the optional per-item source
		 * @param followsUpId          the optional earlier entry this one follows up
		 * @param deferredToSupervisor whether the follow-up defers to the supervisor
		 */
		public RequestedChange(String feedback, Boolean completed, ThesisFeedbackCategory category,
				ThesisFeedbackSeverity severity, ThesisFeedbackSource source, UUID followsUpId,
				Boolean deferredToSupervisor) {
			this(feedback, completed, category, severity, source, followsUpId, deferredToSupervisor, null);
		}
	}
}
