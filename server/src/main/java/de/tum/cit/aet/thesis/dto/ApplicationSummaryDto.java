package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.constants.ApplicationRejectReason;
import de.tum.cit.aet.thesis.constants.ApplicationReviewReason;
import de.tum.cit.aet.thesis.constants.ApplicationState;
import de.tum.cit.aet.thesis.entity.Application;
import de.tum.cit.aet.thesis.entity.ApplicationReviewer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApplicationSummaryDto(
    UUID applicationId,
    String studyDegree,
    String studyProgram,
    String thesisTitle,
    String motivation,
    String interests,
    String projects,
    String specialSkills
) {

    public static ApplicationSummaryDto fromApplicationEntity(Application application) {
        if (application == null) {
            return null;
        }

        return new ApplicationSummaryDto(
            application.getId(),
            application.getUser().getStudyDegree(),
            application.getUser().getStudyProgram(),
            application.getTopic() != null ? application.getTopic().getTitle()
                : application.getThesisTitle(),
            application.getMotivation(),
            application.getUser().getInterests(),
            application.getUser().getProjects(),
            application.getUser().getSpecialSkills()
        );
    }
}
