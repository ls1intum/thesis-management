package de.tum.cit.aet.thesis.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ThesisCommentType {
    ADVISOR("ADVISOR"),
    THESIS("THESIS");

    private final String value;
}
