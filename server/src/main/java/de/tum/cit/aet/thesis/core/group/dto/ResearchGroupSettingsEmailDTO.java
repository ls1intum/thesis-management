package de.tum.cit.aet.thesis.core.group.dto;

import de.tum.cit.aet.thesis.core.group.entity.ResearchGroupSettings;

public record ResearchGroupSettingsEmailDTO(
		String applicationNotificationEmail
) {
	public static ResearchGroupSettingsEmailDTO fromEntity(ResearchGroupSettings settings) {
		return new ResearchGroupSettingsEmailDTO(settings.getApplicationNotificationEmail());
	}
}
