package de.tum.cit.aet.thesis.core.application.controller.payload;

import de.tum.cit.aet.thesis.core.application.constants.ApplicationRejectReason;

public record RejectApplicationPayload(
		ApplicationRejectReason reason,
		Boolean notifyUser
) { }
