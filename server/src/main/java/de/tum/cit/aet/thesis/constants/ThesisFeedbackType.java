package de.tum.cit.aet.thesis.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ThesisFeedbackType {
    PROPOSAL("PROPOSAL"),
    THESIS("THESIS"),
    PRESENTATION("PRESENTATION");

    private final String value;
}
