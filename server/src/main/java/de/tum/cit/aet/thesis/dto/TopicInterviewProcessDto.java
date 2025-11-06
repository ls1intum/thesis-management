package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.entity.Topic;

public record TopicInterviewProcessDto(
        String topicTitle,
        Boolean interviewProcessExists
) {
    public static TopicInterviewProcessDto from(Topic topic, Boolean interviewProcessExists) {
        return new TopicInterviewProcessDto(topic.getTitle(), interviewProcessExists);
    }
}
