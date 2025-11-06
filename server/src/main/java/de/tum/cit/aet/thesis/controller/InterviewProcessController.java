package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.controller.payload.CreateInterviewProcessPayload;
import de.tum.cit.aet.thesis.dto.InterviewProcessDto;
import de.tum.cit.aet.thesis.entity.InterviewProcess;
import de.tum.cit.aet.thesis.service.InterviewProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v2/interview-process")
public class InterviewProcessController {
    private final InterviewProcessService interviewProcessService;

    @Autowired
    public InterviewProcessController(InterviewProcessService interviewProcessService) {
        this.interviewProcessService = interviewProcessService;
    }

    @GetMapping
    public ResponseEntity<Page<InterviewProcessDto>> getMyInterviewProcesses(
            @RequestParam(required = false) String searchQuery,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestParam(required = false, defaultValue = "completed") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortOrder,
            @RequestParam(required = false, defaultValue = "false") boolean excludeSupervised
    ) {

        Page<InterviewProcess> result = interviewProcessService.findAllMyProcesses(
                searchQuery,
                page,
                limit,
                sortBy,
                sortOrder,
                excludeSupervised
        );

        return ResponseEntity.ok(result.map(InterviewProcessDto::fromInterviewProcessEntity));
    }

    @PostMapping
    public ResponseEntity<InterviewProcessDto> createInterviewProcess(@RequestBody CreateInterviewProcessPayload payload) {
        InterviewProcessDto interviewProcessDto = InterviewProcessDto.fromInterviewProcessEntity(
                interviewProcessService.createInterviewProcess(payload.topicId())
        );
        return ResponseEntity.ok(interviewProcessDto);
    }
}
