package de.tum.cit.aet.thesis.controller.payload;

import de.tum.cit.aet.thesis.dto.ResearchGroupSettingsEmailDTO;
import de.tum.cit.aet.thesis.dto.ResearchGroupSettingsPhasesDTO;
import de.tum.cit.aet.thesis.dto.ResearchGroupSettingsPresentationDTO;
import de.tum.cit.aet.thesis.dto.ResearchGroupSettingsRejectDTO;


public record UpdateResearchGroupSettingsPayload(
        ResearchGroupSettingsRejectDTO rejectSettings,
        ResearchGroupSettingsPresentationDTO presentationSettings,
        ResearchGroupSettingsPhasesDTO phaseSettings,
        ResearchGroupSettingsEmailDTO emailSettings
) {
}
