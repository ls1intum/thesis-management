package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;

public record ResearchGroupSettingsPresentationDTO(
        int presentationSlotDuration
) {
    public static ResearchGroupSettingsPresentationDTO fromEntity(ResearchGroupSettings settings) {
        int presentationSlotDurationOrDefault = settings.getPresentationSlotDuration() == null ? 30 : settings.getPresentationSlotDuration();
        return new ResearchGroupSettingsPresentationDTO(presentationSlotDurationOrDefault);
    }
}
