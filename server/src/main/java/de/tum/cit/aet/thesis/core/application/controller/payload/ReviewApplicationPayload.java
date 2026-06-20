package de.tum.cit.aet.thesis.core.application.controller.payload;

import de.tum.cit.aet.thesis.core.application.constants.ApplicationReviewReason;

public record ReviewApplicationPayload(
		ApplicationReviewReason reason
) {
}
