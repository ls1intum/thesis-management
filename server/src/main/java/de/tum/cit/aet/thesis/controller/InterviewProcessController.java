package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.controller.payload.*;
import de.tum.cit.aet.thesis.dto.*;
import de.tum.cit.aet.thesis.entity.InterviewProcess;
import de.tum.cit.aet.thesis.entity.InterviewSlot;
import de.tum.cit.aet.thesis.entity.Interviewee;
import de.tum.cit.aet.thesis.service.InterviewProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @GetMapping("/{interviewProcessId}/topic")
    public ResponseEntity<TopicDto> getInterviewProcessTopic(
            @PathVariable("interviewProcessId") UUID interviewProcessId
    ) {
        InterviewProcess interviewProcess = interviewProcessService.findById(interviewProcessId);

        return ResponseEntity.ok(TopicDto.fromTopicEntity(interviewProcess.getTopic()));
    }

    @PostMapping("/interview-slots")
    @PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
    public ResponseEntity<List<InterviewSlotDto>> addInterviewProcess(@RequestBody CreateInterviewSlotsPayload payload) {
        List<InterviewSlot> adaptedInterviewSlots = interviewProcessService.addInterviewSlotsToProcess(
                payload.interviewProcessId(),
                payload.interviewSlots()
        );

        List<InterviewSlotDto> interviewSlotDtos = adaptedInterviewSlots.stream().map((InterviewSlotDto::fromInterviewSlot)).toList();

        return ResponseEntity.ok(interviewSlotDtos );
    }

    @GetMapping("/{interviewProcessId}/interview-slots")
    //Not preauthorized to allow interviewees to fetch available slots -> check inside service method if process is accessible to the user
    public ResponseEntity<List<InterviewSlotDto>> getInterviewProcessInterviewSlots(
            @PathVariable("interviewProcessId") UUID interviewProcessId,
            @RequestParam(required = false, defaultValue = "false") boolean excludeBooked
    ) {
        List<InterviewSlot> interviewSlots = interviewProcessService.getInterviewProcessInterviewSlots(interviewProcessId, excludeBooked);
        List<InterviewSlotDto> interviewSlotDtos = interviewSlots.stream().map((InterviewSlotDto::fromInterviewSlot)).toList();
        return ResponseEntity.ok(interviewSlotDtos);
    }

    @GetMapping("/{interviewProcessId}/interviewee/{intervieweeId}")
    @PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
    public ResponseEntity<IntervieweeDTO> getInterviewee(
            @PathVariable("interviewProcessId") UUID interviewProcessId,
            @PathVariable("intervieweeId") UUID intervieweeId
    ) {
        Interviewee interviewee = interviewProcessService.getInterviewee(intervieweeId);
        if (!interviewee.getInterviewProcess().getId().equals(interviewProcessId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(IntervieweeDTO.fromIntervieweeEntity(interviewee));
    }

    @PostMapping("/{interviewProcessId}/interviewee/{intervieweeId}")
    @PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
    public ResponseEntity<IntervieweeDTO> updateInterviewee(
            @PathVariable("interviewProcessId") UUID interviewProcessId,
            @PathVariable("intervieweeId") UUID intervieweeId,
            @RequestBody UpdateIntervieweeAssessmentPayload payload
    ) {
        Interviewee interviewee = interviewProcessService.getInterviewee(intervieweeId);
        if (!interviewee.getInterviewProcess().getId().equals(interviewProcessId)) {
            return ResponseEntity.notFound().build();
        }

        Interviewee updatedInterviewee = interviewProcessService.updateIntervieweeAssessment(interviewee, payload.intervieweeNote(), payload.score());
        return ResponseEntity.ok(IntervieweeDTO.fromIntervieweeEntity(updatedInterviewee));
    }

    @PostMapping("/{interviewProcessId}/invite")
    @PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
    public ResponseEntity<List<IntervieweeLightWithNextSlotDto>> inviteInterviewees(
            @PathVariable("interviewProcessId") UUID interviewProcessId,
            @RequestBody InviteIntervieweesPayload payload
    ) {
        List<Interviewee> interviewee = interviewProcessService.inviteInterviewees(interviewProcessId, payload.intervieweeIds());
        return ResponseEntity.ok(interviewee.stream().map(IntervieweeLightWithNextSlotDto::fromIntervieweeEntity).toList());
    }

    @PutMapping("/{interviewProcessId}/slot/{slotId}/book")
    public ResponseEntity<InterviewSlotDto> bookInterviewSlot(
            @PathVariable("interviewProcessId") UUID interviewProcessId,
            @PathVariable("slotId") UUID slotId,
            @RequestBody BookInterviewSlotPayload payload
            ) {
        InterviewSlot interviewSlot = interviewProcessService.bookInterviewSlot(interviewProcessId, slotId, payload.intervieweeUserId());
        return ResponseEntity.ok(InterviewSlotDto.fromInterviewSlot(interviewSlot));
    }

    @PutMapping("/{interviewProcessId}/slot/{slotId}/cancel")
    @PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
    public ResponseEntity<InterviewSlotDto> cancelInterviewSlotBooking(
            @PathVariable("interviewProcessId") UUID interviewProcessId,
            @PathVariable("slotId") UUID slotId
    ) {
        InterviewSlot interviewSlot = interviewProcessService.cancelInterviewSlotBooking(interviewProcessId, slotId);
        return ResponseEntity.ok(InterviewSlotDto.fromInterviewSlot(interviewSlot));
    }
}
