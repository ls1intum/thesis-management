package de.tum.cit.aet.thesis.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ThesisVisibility {
    PRIVATE("PRIVATE"),
    INTERNAL("INTERNAL"),
    STUDENT("STUDENT"),
    PUBLIC("PUBLIC");

    private final String value;
}
