package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.constants.ApplicationState;
import de.tum.cit.aet.thesis.entity.Application;

import java.util.UUID;

public record ApplicationInterviewProcessDto(
        UUID applicationId,
        String applicantName,
        ApplicationState state
) {

    public static ApplicationInterviewProcessDto from(
            Application application
    ) {
        return new ApplicationInterviewProcessDto(application.getId(), application.getUser().getFirstName() + " " + application.getUser().getLastName(), application.getState());
    }
}
