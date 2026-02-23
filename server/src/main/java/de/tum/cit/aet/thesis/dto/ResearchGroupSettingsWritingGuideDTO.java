package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;

public record ResearchGroupSettingsWritingGuideDTO(
		String scientificWritingGuideLink
) {
	public static ResearchGroupSettingsWritingGuideDTO fromEntity(ResearchGroupSettings settings) {
		return new ResearchGroupSettingsWritingGuideDTO(settings.getScientificWritingGuideLink());
	}
}
