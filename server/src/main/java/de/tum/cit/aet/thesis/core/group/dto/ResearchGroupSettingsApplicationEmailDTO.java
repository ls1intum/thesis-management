package de.tum.cit.aet.thesis.core.group.dto;

import de.tum.cit.aet.thesis.core.group.entity.ResearchGroupSettings;

public record ResearchGroupSettingsApplicationEmailDTO(
		boolean includeApplicationDataInEmail
) {
	public static ResearchGroupSettingsApplicationEmailDTO fromEntity(ResearchGroupSettings settings) {
		return new ResearchGroupSettingsApplicationEmailDTO(settings.isIncludeApplicationDataInEmail());
	}
}
