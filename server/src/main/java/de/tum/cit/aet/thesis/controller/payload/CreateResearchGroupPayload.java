package de.tum.cit.aet.thesis.controller.payload;

import java.util.UUID;

public record CreateResearchGroupPayload(
    String headUsername,
    String name,
    String abbreviation,
    String campus,
    String description,
    String websiteUrl
) {

}
