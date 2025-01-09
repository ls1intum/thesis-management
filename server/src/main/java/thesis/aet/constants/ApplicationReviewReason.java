package de.tum.cit.aet.thesis.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ApplicationReviewReason {
    NOT_REVIEWED("NOT_REVIEWED"),
    INTERESTED("INTERESTED"),
    NOT_INTERESTED("NOT_INTERESTED");

    private final String value;
}
