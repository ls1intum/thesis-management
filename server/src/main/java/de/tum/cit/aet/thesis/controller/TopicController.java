package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.constants.StringLimits;
import de.tum.cit.aet.thesis.controller.payload.CloseTopicPayload;
import de.tum.cit.aet.thesis.controller.payload.ReplaceTopicPayload;
import de.tum.cit.aet.thesis.dto.PaginationDto;
import de.tum.cit.aet.thesis.dto.TopicDto;
import de.tum.cit.aet.thesis.dto.TopicInterviewProcessDto;
import de.tum.cit.aet.thesis.entity.Topic;
import de.tum.cit.aet.thesis.service.ApplicationService;
import de.tum.cit.aet.thesis.service.TopicService;
import de.tum.cit.aet.thesis.utility.RequestValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** REST controller for managing thesis topics and their lifecycle. */
@Slf4j
@RestController
@RequestMapping("/v2/topics")
public class TopicController {
	private final TopicService topicService;
	private final ApplicationService applicationService;

	/**
	 * Injects the topic service and application service.
	 *
	 * @param topicService the topic service
	 * @param applicationService the application service
	 */
	@Autowired
	public TopicController(TopicService topicService, ApplicationService applicationService) {
		this.topicService = topicService;
		this.applicationService = applicationService;
	}

	/**
	 * Retrieves a paginated list of topics filtered by search, type, state, and research group.
	 *
	 * @param search the search query string
	 * @param onlyOwnResearchGroup whether to filter by the current user's research group
	 * @param type the topic types to filter by
	 * @param states the topic states to filter by
	 * @param page the page number
	 * @param limit the number of items per page
	 * @param sortBy the field to sort by
	 * @param sortOrder the sort direction (asc or desc)
	 * @param researchGroupIds the research group IDs to filter by
	 * @return the paginated list of topics
	 */
	@GetMapping
	public ResponseEntity<PaginationDto<TopicDto>> getTopics(
			@RequestParam(required = false) String search,
			@RequestParam(required = false, defaultValue = "true") boolean onlyOwnResearchGroup,
			@RequestParam(required = false, defaultValue = "") String[] type,
			@RequestParam(required = false, defaultValue = "") String[] states,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "50") Integer limit,
			@RequestParam(required = false, defaultValue = "createdAt") String sortBy,
			@RequestParam(required = false, defaultValue = "desc") String sortOrder,
			@RequestParam(required = false, defaultValue = "") UUID[] researchGroupIds
	) {
		Page<Topic> topics = topicService.getAll(
				onlyOwnResearchGroup,
				type,
				states,
				search,
				page,
				limit,
				sortBy,
				sortOrder,
				researchGroupIds
		);

		return ResponseEntity.ok(PaginationDto.fromSpringPage(topics.map(TopicDto::fromTopicEntity)));
	}

	/**
	 * Retrieves a single topic by its identifier.
	 *
	 * @param topicId the topic ID
	 * @return the topic
	 */
	@GetMapping("/{topicId}")
	public ResponseEntity<TopicDto> getTopic(@PathVariable UUID topicId) {
		Topic topic = topicService.findById(topicId);

		return ResponseEntity.ok(TopicDto.fromTopicEntity(topic));
	}

	/**
	 * Creates a new topic with the specified details, supervisors, and advisors.
	 *
	 * @param payload the topic creation payload
	 * @return the created topic
	 */
	@PostMapping
	@PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
	public ResponseEntity<TopicDto> createTopic(
			@RequestBody ReplaceTopicPayload payload
	) {
		Topic topic = topicService.createTopic(
				RequestValidator.validateStringMaxLength(payload.title(), StringLimits.THESIS_TITLE.getLimit()),
				RequestValidator.validateStringSetItemMaxLengthAllowNull(payload.thesisTypes(), StringLimits.SHORTTEXT.getLimit()),
				RequestValidator.validateStringMaxLength(payload.problemStatement(), StringLimits.UNLIMITED_TEXT.getLimit()),
				RequestValidator.validateStringMaxLength(payload.requirements(), StringLimits.UNLIMITED_TEXT.getLimit()),
				RequestValidator.validateStringMaxLength(payload.goals(), StringLimits.UNLIMITED_TEXT.getLimit()),
				RequestValidator.validateStringMaxLength(payload.references(), StringLimits.UNLIMITED_TEXT.getLimit()),
				RequestValidator.validateNotNull(payload.supervisorIds()),
				RequestValidator.validateNotNull(payload.advisorIds()),
				RequestValidator.validateNotNull(payload.researchGroupId()),
				payload.intendedStart(),
				payload.applicationDeadline(),
				payload.isDraft() != null ? payload.isDraft() : false
		);

		return ResponseEntity.ok(TopicDto.fromTopicEntity(topic));
	}

	/**
	 * Updates an existing topic with new details, supervisors, and advisors.
	 *
	 * @param topicId the topic ID to update
	 * @param payload the topic update payload
	 * @return the updated topic
	 */
	@PutMapping("/{topicId}")
	@PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
	public ResponseEntity<TopicDto> updateTopic(
			@PathVariable UUID topicId,
			@RequestBody ReplaceTopicPayload payload
	) {
		Topic topic = topicService.findById(topicId);

		topic = topicService.updateTopic(
				topic,
				RequestValidator.validateStringMaxLength(payload.title(), StringLimits.THESIS_TITLE.getLimit()),
				RequestValidator.validateStringSetItemMaxLengthAllowNull(payload.thesisTypes(), StringLimits.SHORTTEXT.getLimit()),
				RequestValidator.validateStringMaxLength(payload.problemStatement(), StringLimits.UNLIMITED_TEXT.getLimit()),
				RequestValidator.validateStringMaxLength(payload.requirements(), StringLimits.UNLIMITED_TEXT.getLimit()),
				RequestValidator.validateStringMaxLength(payload.goals(), StringLimits.UNLIMITED_TEXT.getLimit()),
				RequestValidator.validateStringMaxLength(payload.references(), StringLimits.UNLIMITED_TEXT.getLimit()),
				RequestValidator.validateNotNull(payload.supervisorIds()),
				RequestValidator.validateNotNull(payload.advisorIds()),
				RequestValidator.validateNotNull(payload.researchGroupId()),
				payload.intendedStart(),
				payload.applicationDeadline(),
				payload.isDraft() != null ? payload.isDraft() : false
		);

		return ResponseEntity.ok(TopicDto.fromTopicEntity(topic));
	}

	/**
	 * Closes a topic with a given reason and optionally notifies affected users.
	 *
	 * @param topicId the topic ID to close
	 * @param payload the close topic payload with reason and notification flag
	 * @return the closed topic
	 */
	@DeleteMapping("/{topicId}")
	@PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
	public ResponseEntity<TopicDto> closeTopic(
			@PathVariable UUID topicId,
			@RequestBody CloseTopicPayload payload
	) {
		Topic topic = topicService.findById(topicId);

		topic = applicationService.closeTopic(
				topic,
				RequestValidator.validateNotNull(payload.reason()),
				RequestValidator.validateNotNull(payload.notifyUser())
		);

		return ResponseEntity.ok(TopicDto.fromTopicEntity(topic));
	}

	/**
	 * Retrieves a paginated list of topics eligible for the interview process.
	 *
	 * @param search the search query to filter topics
	 * @param page the page number
	 * @param limit the number of items per page
	 * @param excludeSupervised whether to exclude topics supervised by the current user
	 * @return the paginated list of interview-eligible topics
	 */
	@GetMapping("/interview-topics")
	@PreAuthorize("hasAnyRole('admin', 'advisor', 'supervisor')")
	public ResponseEntity<PaginationDto<TopicInterviewProcessDto>> getPossibleInterviewTopics(
			@RequestParam(required = false) String search,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "50") Integer limit,
			@RequestParam(required = false, defaultValue = "false") boolean excludeSupervised
	) {
		Page<TopicInterviewProcessDto> topics = topicService.getPossibleInterviewTopics(
				search,
				page,
				limit,
				excludeSupervised
		);

		return ResponseEntity.ok(PaginationDto.fromSpringPage(topics));
	}
}
