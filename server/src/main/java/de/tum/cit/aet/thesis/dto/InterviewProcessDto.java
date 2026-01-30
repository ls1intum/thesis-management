package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.entity.InterviewProcess;
import de.tum.cit.aet.thesis.entity.Interviewee;

import java.util.Map;
import java.util.UUID;

public record InterviewProcessDto(
        UUID interviewProcessId,
        String topicTitle,
        boolean completed,
        int totalInterviewees,
        Map<String, Integer> statesNumbers
) {
    public static InterviewProcessDto fromInterviewProcessEntity(InterviewProcess interviewProcess) {
        int totalInterviewees = interviewProcess.getInterviewees().size();

        int uncontactedCount = 0;
        int invitedCount = 0;
        int scheduledCount = 0;
        int completedCount = 0;

        if (totalInterviewees > 0) {
            for (Interviewee interviewee : interviewProcess.getInterviewees()) {
                if (interviewee.getScore() != null && interviewee.getScore() >= 0) {
                    completedCount++;
                } else if (!interviewee.getSlots().isEmpty()) {
                    scheduledCount++;
                } else if (interviewee.getLastInvited() != null) {
                    invitedCount++;
                } else {
                    uncontactedCount++;
                }
            }
        }

        Map<String, Integer> statesNumbers = java.util.Map.of(
                "Uncontacted", uncontactedCount,
                "Invited", invitedCount,
                "Scheduled", scheduledCount,
                "Completed", completedCount
        );

        return new InterviewProcessDto(
                interviewProcess.getId(),
                interviewProcess.getTopic().getTitle(),
                interviewProcess.isCompleted(),
                totalInterviewees,
                statesNumbers
        );
    }
}
