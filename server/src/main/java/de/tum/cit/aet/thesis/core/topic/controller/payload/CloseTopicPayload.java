package de.tum.cit.aet.thesis.core.topic.controller.payload;

import de.tum.cit.aet.thesis.core.application.constants.ApplicationRejectReason;

public record CloseTopicPayload(
		ApplicationRejectReason reason,
		Boolean notifyUser
) {
}
