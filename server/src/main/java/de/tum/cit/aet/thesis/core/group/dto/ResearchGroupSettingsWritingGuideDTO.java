package de.tum.cit.aet.thesis.core.group.dto;

import de.tum.cit.aet.thesis.core.group.entity.ResearchGroupSettings;

public record ResearchGroupSettingsWritingGuideDTO(
		String scientificWritingGuideLink
) {
	public static ResearchGroupSettingsWritingGuideDTO fromEntity(ResearchGroupSettings settings) {
		return new ResearchGroupSettingsWritingGuideDTO(settings.getScientificWritingGuideLink());
	}
}
