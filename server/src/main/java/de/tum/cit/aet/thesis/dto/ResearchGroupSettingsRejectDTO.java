package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;

public record ResearchGroupSettingsRejectDTO(
        boolean automaticRejectEnabled,
        int rejectDuration
) {
    public static ResearchGroupSettingsRejectDTO fromEntity(ResearchGroupSettings settings) {
        return new ResearchGroupSettingsRejectDTO(settings.isAutomaticRejectEnabled(), settings.getRejectDuration());
    }
}
