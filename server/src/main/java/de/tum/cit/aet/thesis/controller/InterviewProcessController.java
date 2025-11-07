package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.controller.payload.CreateInterviewProcessPayload;
import de.tum.cit.aet.thesis.dto.*;
import de.tum.cit.aet.thesis.entity.InterviewProcess;
import de.tum.cit.aet.thesis.entity.Interviewee;
import de.tum.cit.aet.thesis.service.InterviewProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v2/interview-process")
public class InterviewProcessController {
    private final InterviewProcessService interviewProcessService;

    @Autowired
    public InterviewProcessController(InterviewProcessService interviewProcessService) {
        this.interviewProcessService = interviewProcessService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
    public ResponseEntity<PaginationDto<InterviewProcessDto>> getMyInterviewProcesses(
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

        return ResponseEntity.ok(PaginationDto.fromSpringPage(result.map(InterviewProcessDto::fromInterviewProcessEntity)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
    public ResponseEntity<InterviewProcessDto> createInterviewProcess(@RequestBody CreateInterviewProcessPayload payload) {
        InterviewProcessDto interviewProcessDto = InterviewProcessDto.fromInterviewProcessEntity(
                interviewProcessService.createInterviewProcess(payload.topicId(), payload.intervieweeApplicationIds())
        );
        return ResponseEntity.ok(interviewProcessDto);
    }

    @GetMapping("/{interviewProcessId}/interviewees")
    @PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
    public ResponseEntity<PaginationDto<IntervieweeLightWithNextSlotDto>> getInterviewProcessInterviewees(
            @PathVariable("interviewProcessId") UUID interviewProcessId,
            @RequestParam(required = false) String searchQuery,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int limit,
            @RequestParam(required = false, defaultValue = "lastInvited") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortOrder,
            @RequestParam(required = false, defaultValue = "") String state
    ) {

        Page<Interviewee> interviewProcessDto = interviewProcessService.getInterviewProcessInterviewees(interviewProcessId, searchQuery, page, limit, sortBy, sortOrder, state);

        return ResponseEntity.ok(PaginationDto.fromSpringPage(interviewProcessDto.map(IntervieweeLightWithNextSlotDto::fromIntervieweeEntity)));
    }
}
