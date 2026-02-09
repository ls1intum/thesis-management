package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.entity.Topic;

import java.util.UUID;

public record TopicInterviewProcessDto(
        UUID topicId,
        String topicTitle,
        Boolean interviewProcessExists
) {
    public static TopicInterviewProcessDto from(Topic topic, Boolean interviewProcessExists) {
        return new TopicInterviewProcessDto(topic.getId(), topic.getTitle(), interviewProcessExists);
    }
}
