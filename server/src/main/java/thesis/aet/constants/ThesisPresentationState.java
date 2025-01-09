package de.tum.cit.aet.thesis.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ThesisPresentationState {
    SCHEDULED("SCHEDULED"),
    DRAFTED("DRAFTED");

    private final String value;
}
