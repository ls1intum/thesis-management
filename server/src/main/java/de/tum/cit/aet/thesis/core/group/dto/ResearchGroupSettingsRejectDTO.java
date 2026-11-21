package de.tum.cit.aet.thesis.core.group.dto;

import de.tum.cit.aet.thesis.core.group.entity.ResearchGroupSettings;

public record ResearchGroupSettingsRejectDTO(
		boolean automaticRejectEnabled,
		int rejectDuration
) {
	public static ResearchGroupSettingsRejectDTO fromEntity(ResearchGroupSettings settings) {
		return new ResearchGroupSettingsRejectDTO(settings.isAutomaticRejectEnabled(), settings.getRejectDuration());
	}
}
