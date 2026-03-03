package de.tum.cit.aet.thesis.controller.payload;

import java.util.List;
import java.util.UUID;

public record AcceptApplicationPayload(
		String thesisTitle,
		String thesisType,
		String language,
		List<UUID> supervisorIds,
		List<UUID> examinerIds,
		Boolean notifyUser,
		Boolean closeTopic
) { }
