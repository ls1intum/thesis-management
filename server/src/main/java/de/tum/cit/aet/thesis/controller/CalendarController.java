package de.tum.cit.aet.thesis.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import de.tum.cit.aet.thesis.service.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v2/calendar")
public class CalendarController {
    private final ThesisPresentationService thesisPresentationService;
    private final ResearchGroupService researchGroupService;

    @Autowired
    public CalendarController(ThesisPresentationService thesisPresentationService, ResearchGroupService researchGroupService) {
        this.thesisPresentationService = thesisPresentationService;
        this.researchGroupService = researchGroupService;
    }

    @GetMapping({"/presentations", "/presentations/{researchGroupAbbreviation}"})
    public ResponseEntity<String> getCalendar(@PathVariable(required = false) String researchGroupAbbreviation) {
        UUID researchGroupId = researchGroupService.findByAbbreviation(researchGroupAbbreviation).getId();
        if (researchGroupId == null) {
            log.error("Research group with abbreviation '{}' not found", researchGroupAbbreviation);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=calendar.ics")
                .body(thesisPresentationService.getPresentationCalendar(researchGroupId).toString());
    }
}
