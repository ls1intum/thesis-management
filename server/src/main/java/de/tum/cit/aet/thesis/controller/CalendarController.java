package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.service.InterviewProcessService;
import de.tum.cit.aet.thesis.service.ResearchGroupService;
import de.tum.cit.aet.thesis.service.ThesisPresentationService;
import de.tum.cit.aet.thesis.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for generating iCalendar feeds for thesis presentations and interviews.
 */
@Slf4j
@RestController
@RequestMapping("/v2/calendar")
public class CalendarController {
	private final ThesisPresentationService thesisPresentationService;
	private final ResearchGroupService researchGroupService;
	private final UserService userService;
	private final InterviewProcessService interviewProcessService;

	/**
	 * Injects the presentation, research group, user, and interview process services.
	 *
	 * @param thesisPresentationService the thesis presentation service
	 * @param researchGroupService the research group service
	 * @param userService the user service
	 * @param interviewProcessService the interview process service
	 */
	@Autowired
	public CalendarController(ThesisPresentationService thesisPresentationService,
		ResearchGroupService researchGroupService, UserService userService,
		InterviewProcessService interviewProcessService) {
		this.thesisPresentationService = thesisPresentationService;
		this.researchGroupService = researchGroupService;
		this.userService = userService;
		this.interviewProcessService = interviewProcessService;
	}

	/**
	 * Returns an iCalendar feed of thesis presentations for a given research group.
	 *
	 * @param researchGroupAbbreviation the abbreviation of the research group
	 * @return the iCalendar feed as a string
	 */
	@GetMapping( "/presentations/{researchGroupAbbreviation}")
	public ResponseEntity<String> getCalendar(@PathVariable(required = false) String researchGroupAbbreviation) {
		ResearchGroup researchGroup = researchGroupService.findByAbbreviation(researchGroupAbbreviation);
		if (researchGroup == null) {
			String safeAbbr = researchGroupAbbreviation != null
					? researchGroupAbbreviation.replaceAll("[\\r\\n]", "_")
					: "<null>";
			log.error("Research group with abbreviation '{}' not found", safeAbbr);
			return ResponseEntity.status(404)
					.contentType(MediaType.TEXT_PLAIN)
					.body("Research group not found");
		}

		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("text/calendar"))
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=calendar.ics")
				.body(thesisPresentationService.getPresentationCalendar(researchGroup.getId()).toString());
	}

	/**
	 * Returns an iCalendar feed of interview appointments for a given user.
	 *
	 * @param userId the ID of the user
	 * @return the iCalendar feed as a string
	 */
	@GetMapping("/interviews/user/{userId}")
	public ResponseEntity<String> getInterviews(@PathVariable UUID userId) {
		userService.findById(userId);

		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("text/calendar"))
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=calendar.ics")
				.body(interviewProcessService.getInterviewCalendarForUser(userId).toString());
	}
}
