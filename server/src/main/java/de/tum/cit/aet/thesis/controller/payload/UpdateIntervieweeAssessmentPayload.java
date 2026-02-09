package de.tum.cit.aet.thesis.controller.payload;

import de.tum.cit.aet.thesis.entity.InterviewAssessment;

import java.util.List;
import java.util.UUID;

public record UpdateIntervieweeAssessmentPayload(String intervieweeNote, int score) {
}
