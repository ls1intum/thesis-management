package de.tum.cit.aet.thesis.controller.payload;

import java.util.UUID;

public record CreateResearchGroupPayload(
    UUID headId,
    String name,
    String abbreviation,
    String campus,
    String description,
    String websiteUrl
) {

}
