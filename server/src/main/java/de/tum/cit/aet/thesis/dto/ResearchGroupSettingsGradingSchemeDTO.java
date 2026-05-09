package de.tum.cit.aet.thesis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ResearchGroupSettingsGradingSchemeDTO(
		List<GradingSchemeComponentDTO> components
) {
}
