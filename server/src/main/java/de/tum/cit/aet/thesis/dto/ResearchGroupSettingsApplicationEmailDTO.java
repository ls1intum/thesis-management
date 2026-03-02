package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;

public record ResearchGroupSettingsApplicationEmailDTO(
		boolean includeApplicationDataInEmail
) {
	public static ResearchGroupSettingsApplicationEmailDTO fromEntity(ResearchGroupSettings settings) {
		return new ResearchGroupSettingsApplicationEmailDTO(settings.isIncludeApplicationDataInEmail());
	}
}
