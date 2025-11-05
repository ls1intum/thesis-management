package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.controller.payload.CreateInterviewProcessPayload;
import de.tum.cit.aet.thesis.dto.InterviewProcessDto;
import de.tum.cit.aet.thesis.service.InterviewProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/interview-process")
public class InterviewProcessController {
    private final InterviewProcessService interviewProcessService;

    @Autowired
    public InterviewProcessController(InterviewProcessService interviewProcessService) {
        this.interviewProcessService = interviewProcessService;
    }

    @PostMapping
    public ResponseEntity<InterviewProcessDto> createInterviewProcess(@RequestBody CreateInterviewProcessPayload payload) {
        InterviewProcessDto interviewProcessDto = InterviewProcessDto.fromInterviewProcessEntity(
                interviewProcessService.createInterviewProcess(payload.topicId())
        );
        return ResponseEntity.ok(interviewProcessDto);
    }
}
