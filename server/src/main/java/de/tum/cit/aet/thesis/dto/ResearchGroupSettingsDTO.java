package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;

public record ResearchGroupSettingsDTO(
        ResearchGroupSettingsRejectDTO rejectSettings,
        ResearchGroupSettingsPresentationDTO presentationSettings,
        ResearchGroupSettingsPhasesDTO phaseSettings,
        ResearchGroupSettingsEmailDTO emailSettings
) {
    public static ResearchGroupSettingsDTO fromEntity(ResearchGroupSettings settings) {
        return new ResearchGroupSettingsDTO(
                ResearchGroupSettingsRejectDTO.fromEntity(settings),
                ResearchGroupSettingsPresentationDTO.fromEntity(settings),
                ResearchGroupSettingsPhasesDTO.fromEntity(settings),
                ResearchGroupSettingsEmailDTO.fromEntity(settings)
        );
    }
}
