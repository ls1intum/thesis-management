package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;

public record ResearchGroupSettingsEmailDTO(
        String applicationNotificationEmail
) {
    public static ResearchGroupSettingsEmailDTO fromEntity(ResearchGroupSettings settings) {
        return new ResearchGroupSettingsEmailDTO(settings.getApplicationNotificationEmail());
    }
}
