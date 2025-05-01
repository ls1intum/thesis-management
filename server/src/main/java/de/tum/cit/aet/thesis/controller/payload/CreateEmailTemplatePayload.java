package de.tum.cit.aet.thesis.controller.payload;

import java.util.UUID;

public record CreateEmailTemplatePayload(
        UUID researchGroupId,
        String templateCase,
        String description,
        String subject,
        String bodyHtml,
        String language
) {

}
