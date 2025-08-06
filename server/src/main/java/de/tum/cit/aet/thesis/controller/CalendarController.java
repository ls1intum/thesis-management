package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.entity.ResearchGroup;
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
        ResearchGroup researchGroup = researchGroupService.findByAbbreviation(researchGroupAbbreviation);
        if (researchGroup == null) {
            log.error("Research group with abbreviation '{}' not found", researchGroupAbbreviation);
            return ResponseEntity.status(404).body("Research group with abbreviation '" + researchGroupAbbreviation + " not found");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=calendar.ics")
                .body(thesisPresentationService.getPresentationCalendar(researchGroup.getId()).toString());
    }
}
