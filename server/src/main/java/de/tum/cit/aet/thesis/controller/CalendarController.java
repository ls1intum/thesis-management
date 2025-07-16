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

    @Autowired
    public CalendarController(ThesisPresentationService thesisPresentationService) {
        this.thesisPresentationService = thesisPresentationService;
    }

    @GetMapping({"/presentations", "/presentations/{researchGroupId}"})
    public ResponseEntity<String> getCalendar(@PathVariable(required = false) UUID researchGroupId) {

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=calendar.ics")
                .body(thesisPresentationService.getPresentationCalendar(researchGroupId).toString());
    }
}
