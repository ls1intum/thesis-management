package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;

public record ResearchGroupSettingsDTO(
		ResearchGroupSettingsRejectDTO rejectSettings,
		ResearchGroupSettingsPresentationDTO presentationSettings,
		ResearchGroupSettingsPhasesDTO phaseSettings,
		ResearchGroupSettingsEmailDTO emailSettings,
		ResearchGroupSettingsWritingGuideDTO writingGuideSettings,
		ResearchGroupSettingsApplicationEmailDTO applicationEmailSettings,
		ResearchGroupSettingsGradingSchemeDTO gradingSchemeSettings
) {
	public static ResearchGroupSettingsDTO fromEntity(ResearchGroupSettings settings) {
		return fromEntity(settings, null);
	}

	public static ResearchGroupSettingsDTO fromEntity(ResearchGroupSettings settings, ResearchGroupSettingsGradingSchemeDTO gradingScheme) {
		return new ResearchGroupSettingsDTO(
				ResearchGroupSettingsRejectDTO.fromEntity(settings),
				ResearchGroupSettingsPresentationDTO.fromEntity(settings),
				ResearchGroupSettingsPhasesDTO.fromEntity(settings),
				ResearchGroupSettingsEmailDTO.fromEntity(settings),
				ResearchGroupSettingsWritingGuideDTO.fromEntity(settings),
				ResearchGroupSettingsApplicationEmailDTO.fromEntity(settings),
				gradingScheme
		);
	}
}
