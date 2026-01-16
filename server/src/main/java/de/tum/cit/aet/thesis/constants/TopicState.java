package de.tum.cit.aet.thesis.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TopicState {
    OPEN("Open"),
    CLOSED("Writing"),
    DRAFT("Draft");

    private final String value;
}
