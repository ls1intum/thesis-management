package de.tum.cit.aet.thesis.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TopicState {
    OPEN("OPEN"),
    CLOSED("WRITING"),
    DRAFT("DRAFT");

    private final String value;
}
