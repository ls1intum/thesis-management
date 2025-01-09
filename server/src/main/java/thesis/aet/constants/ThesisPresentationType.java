package de.tum.cit.aet.thesis.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ThesisPresentationType {
    INTERMEDIATE("INTERMEDIATE"),
    FINAL("FINAL");

    private final String value;
}
