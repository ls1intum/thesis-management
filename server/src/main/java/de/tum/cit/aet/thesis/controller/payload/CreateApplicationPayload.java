package de.tum.cit.aet.thesis.controller.payload;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload for creating or updating a thesis application.
 *
 * @param topicId             the ID of the topic to apply for (null if suggesting a custom thesis title)
 * @param thesisTitle         the suggested thesis title (required if topicId is null)
 * @param thesisType          the type of thesis (e.g. BACHELOR, MASTER)
 * @param desiredStartDate    the desired start date for the thesis
 * @param motivation          the applicant's motivation text
 * @param researchGroupId     the ID of the research group to apply to
 * @param consentToPrivacyPolicy whether the applicant has accepted the privacy statement.
 *                               Must be {@code true} for new applications; the server validates
 *                               this and records a server-side timestamp as proof of consent
 *                               per GDPR Art. 7(1). Ignored for application updates (PUT).
 */
public record CreateApplicationPayload(
	UUID topicId,
	String thesisTitle,
	String thesisType,
	Instant desiredStartDate,
	String motivation,
	UUID researchGroupId,
	Boolean consentToPrivacyPolicy
) {
}
