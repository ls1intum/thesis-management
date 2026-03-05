package de.tum.cit.aet.thesis.dto;

import java.util.List;

public record ResearchGroupSettingsGradingSchemeDTO(
		List<GradingSchemeComponentDTO> components
) {
}
