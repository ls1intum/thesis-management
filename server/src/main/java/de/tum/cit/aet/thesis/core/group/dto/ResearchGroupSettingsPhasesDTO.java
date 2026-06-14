package de.tum.cit.aet.thesis.core.group.dto;

import de.tum.cit.aet.thesis.core.group.entity.ResearchGroupSettings;

public record ResearchGroupSettingsPhasesDTO(
		boolean proposalPhaseActive
) {
	public static ResearchGroupSettingsPhasesDTO fromEntity(ResearchGroupSettings settings) {
		return new ResearchGroupSettingsPhasesDTO(settings.isProposalPhaseActive());
	}
}
