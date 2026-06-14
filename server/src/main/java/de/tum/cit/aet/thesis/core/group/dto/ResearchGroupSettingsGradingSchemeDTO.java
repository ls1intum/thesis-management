package de.tum.cit.aet.thesis.core.group.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.tum.cit.aet.thesis.thesis.dto.GradingSchemeComponentDTO;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ResearchGroupSettingsGradingSchemeDTO(
		List<GradingSchemeComponentDTO> components
) {
}
