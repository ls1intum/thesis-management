package thesistrack.ls1.dto;

import thesistrack.ls1.constants.ApplicationRejectReason;
import thesistrack.ls1.constants.ApplicationState;
import thesistrack.ls1.entity.Application;

import java.time.Instant;
import java.util.UUID;

public record ApplicationDto (
    UUID applicationId,
    UserDto user,
    TopicDto topic,
    String thesisTitle,
    String thesisType,
    String motivation,
    ApplicationState state,
    Instant desiredStartDate,
    String comment,
    ApplicationRejectReason rejectReason,
    Instant createdAt,
    LightUserDto reviewedBy,
    Instant reviewedAt
) {
    public static ApplicationDto fromApplicationEntity(Application application, boolean protectedData) {
        if (application == null) {
            return null;
        }

        return new ApplicationDto(
                application.getId(),
                UserDto.fromUserEntity(application.getUser()),
                TopicDto.fromTopicEntity(application.getTopic()),
                application.getTopic() != null ? application.getTopic().getTitle() : application.getThesisTitle(),
                application.getThesisType(),
                application.getMotivation(),
                application.getState(),
                application.getDesiredStartDate(), protectedData ? application.getComment() : null,
                application.getRejectReason(),
                application.getCreatedAt(),
                protectedData ? LightUserDto.fromUserEntity(application.getReviewedBy()) : null,
                application.getReviewedAt()
        );
    }
}
