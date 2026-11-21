package de.tum.cit.aet.thesis.core.group.controller.payload;

import de.tum.cit.aet.thesis.core.group.dto.ResearchGroupSettingsApplicationEmailDTO;
import de.tum.cit.aet.thesis.core.group.dto.ResearchGroupSettingsEmailDTO;
import de.tum.cit.aet.thesis.core.group.dto.ResearchGroupSettingsGradingSchemeDTO;
import de.tum.cit.aet.thesis.core.group.dto.ResearchGroupSettingsPhasesDTO;
import de.tum.cit.aet.thesis.core.group.dto.ResearchGroupSettingsPresentationDTO;
import de.tum.cit.aet.thesis.core.group.dto.ResearchGroupSettingsRejectDTO;
import de.tum.cit.aet.thesis.core.group.dto.ResearchGroupSettingsWritingGuideDTO;


public record UpdateResearchGroupSettingsPayload(
		ResearchGroupSettingsRejectDTO rejectSettings,
		ResearchGroupSettingsPresentationDTO presentationSettings,
		ResearchGroupSettingsPhasesDTO phaseSettings,
		ResearchGroupSettingsEmailDTO emailSettings,
		ResearchGroupSettingsWritingGuideDTO writingGuideSettings,
		ResearchGroupSettingsApplicationEmailDTO applicationEmailSettings,
		ResearchGroupSettingsGradingSchemeDTO gradingSchemeSettings
) {
}
