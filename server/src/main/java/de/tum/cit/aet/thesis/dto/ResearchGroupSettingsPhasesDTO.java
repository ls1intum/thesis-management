package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;

public record ResearchGroupSettingsPhasesDTO(
        boolean proposalPhaseActive
) {
    public static ResearchGroupSettingsPhasesDTO fromEntity(ResearchGroupSettings settings) {
        return new ResearchGroupSettingsPhasesDTO(settings.isProposalPhaseActive());
    }
}
