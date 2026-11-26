package de.tum.cit.aet.thesis.core.group.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.core.group.entity.ResearchGroupSettings;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ResearchGroupSettingsEmailDTO(
		String applicationNotificationEmail,
		String emailSignature
) {
	public static ResearchGroupSettingsEmailDTO fromEntity(ResearchGroupSettings settings) {
		return new ResearchGroupSettingsEmailDTO(settings.getApplicationNotificationEmail(), settings.getEmailSignature());
	}
}
