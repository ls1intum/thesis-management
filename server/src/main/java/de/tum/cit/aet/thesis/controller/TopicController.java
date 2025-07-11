package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.constants.StringLimits;
import de.tum.cit.aet.thesis.controller.payload.CloseTopicPayload;
import de.tum.cit.aet.thesis.controller.payload.ReplaceTopicPayload;
import de.tum.cit.aet.thesis.dto.PaginationDto;
import de.tum.cit.aet.thesis.dto.TopicDto;
import de.tum.cit.aet.thesis.entity.Topic;
import de.tum.cit.aet.thesis.service.ApplicationService;
import de.tum.cit.aet.thesis.service.TopicService;
import de.tum.cit.aet.thesis.utility.RequestValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v2/topics")
public class TopicController {
    private final TopicService topicService;
    private final ApplicationService applicationService;

    @Autowired
    public TopicController(TopicService topicService, ApplicationService applicationService) {
        this.topicService = topicService;
        this.applicationService = applicationService;
    }

    @GetMapping
    public ResponseEntity<PaginationDto<TopicDto>> getTopics(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "true") boolean onlyOwnResearchGroup,
            @RequestParam(required = false, defaultValue = "") String[] type,
            @RequestParam(required = false, defaultValue = "false") Boolean includeClosed,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer limit,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(required = false, defaultValue = "") UUID[] researchGroupIds
    ) {
        Page<Topic> topics = topicService.getAll(
                onlyOwnResearchGroup,
                type,
                includeClosed,
                search,
                page,
                limit,
                sortBy,
                sortOrder,
                researchGroupIds
        );

        return ResponseEntity.ok(PaginationDto.fromSpringPage(topics.map(TopicDto::fromTopicEntity)));
    }

    @GetMapping("/{topicId}")
    public ResponseEntity<TopicDto> getTopic(@PathVariable UUID topicId) {
        Topic topic = topicService.findById(topicId);

        return ResponseEntity.ok(TopicDto.fromTopicEntity(topic));
    }

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
                RequestValidator.validateNotNull(payload.researchGroupId())
        );

        return ResponseEntity.ok(TopicDto.fromTopicEntity(topic));
    }

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
                RequestValidator.validateNotNull(payload.researchGroupId())
        );

        return ResponseEntity.ok(TopicDto.fromTopicEntity(topic));
    }

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
}
