package de.tum.cit.aet.thesis.controller.payload;

import java.util.List;
import java.util.UUID;

public record AcceptApplicationPayload (
        String thesisTitle,
        String thesisType,
        String language,
        List<UUID> advisorIds,
        List<UUID> supervisorIds,
        Boolean notifyUser,
        Boolean closeTopic
) { }
