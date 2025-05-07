package de.tum.cit.aet.thesis.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ApplicationRejectReason {
    TOPIC_FILLED("TOPIC_FILLED"),
    TOPIC_OUTDATED("TOPIC_OUTDATED"),
    FAILED_STUDENT_REQUIREMENTS("FAILED_STUDENT_REQUIREMENTS"),
    FAILED_TOPIC_REQUIREMENTS("FAILED_TOPIC_REQUIREMENTS"),
    TITLE_NOT_INTERESTING("TITLE_NOT_INTERESTING"),
    NO_CAPACITY("NO_CAPACITY");

    private final String value;

    public String getTemplateCase() {
        if (value.equals(TOPIC_FILLED.getValue())) {
            return "APPLICATION_REJECTED_TOPIC_FILLED";
        }

        if (value.equals(TOPIC_OUTDATED.getValue())) {
            return "APPLICATION_REJECTED_TOPIC_OUTDATED";
        }

        if (value.equals(FAILED_STUDENT_REQUIREMENTS.getValue())) {
            return "APPLICATION_REJECTED_STUDENT_REQUIREMENTS";
        }

        if (value.equals(FAILED_TOPIC_REQUIREMENTS.getValue())) {
            return "APPLICATION_REJECTED_TOPIC_REQUIREMENTS";
        }

        if (value.equals(TITLE_NOT_INTERESTING.getValue())) {
            return "APPLICATION_REJECTED_TITLE_NOT_INTERESTING";
        }

        return "APPLICATION_REJECTED";
    }
}
