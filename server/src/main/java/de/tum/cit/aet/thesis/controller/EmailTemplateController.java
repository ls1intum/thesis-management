package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.controller.payload.CreateEmailTemplatePayload;
import de.tum.cit.aet.thesis.dto.EmailTemplateDto;
import de.tum.cit.aet.thesis.dto.MailVariableDto;
import de.tum.cit.aet.thesis.dto.PaginationDto;
import de.tum.cit.aet.thesis.entity.EmailTemplate;
import de.tum.cit.aet.thesis.service.EmailTemplateService;
import de.tum.cit.aet.thesis.utility.RequestValidator;
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

import java.util.List;
import java.util.UUID;

/** REST controller for managing email templates used in automated notifications. */
@RestController
@RequestMapping("/v2/email-templates")
public class EmailTemplateController {

	private final EmailTemplateService emailTemplateService;

	/**
	 * Injects the email template service.
	 *
	 * @param emailTemplateService the email template service
	 */
	@Autowired
	public EmailTemplateController(EmailTemplateService emailTemplateService) {
		this.emailTemplateService = emailTemplateService;
	}

	/**
	 * Retrieves a paginated list of email templates with optional filtering.
	 *
	 * @param search the search query to filter templates
	 * @param templateCases the template cases to filter by
	 * @param languages the languages to filter by
	 * @param page the page number for pagination
	 * @param limit the maximum number of results per page
	 * @param sortBy the field to sort by
	 * @param sortOrder the sort direction
	 * @return the paginated list of email templates
	 */
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'supervisor', 'advisor')")
    public ResponseEntity<PaginationDto<EmailTemplateDto>> getEmailTemplates(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "") String[] templateCases,
            @RequestParam(required = false, defaultValue = "") String[] languages,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "-1") Integer limit,
            @RequestParam(required = false, defaultValue = "templateCase") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) UUID researchGroupId
    ) {
        Page<EmailTemplate> emailTemplates = emailTemplateService.getAll(
                templateCases,
                languages,
                search,
                page,
                limit,
                sortBy,
                sortOrder,
                researchGroupId
        );

		return ResponseEntity.ok(PaginationDto.fromSpringPage(
				emailTemplates.map(EmailTemplateDto::fromEmailTemplateEntity)));
	}

	/**
	 * Retrieves a single email template by its ID.
	 *
	 * @param emailTemplateId the ID of the email template
	 * @return the email template
	 */
	@GetMapping("/{emailTemplateId}")
	@PreAuthorize("hasAnyRole('admin', 'supervisor', 'advisor')")
	public ResponseEntity<EmailTemplateDto> getEmailTemplate(
			@PathVariable("emailTemplateId") UUID emailTemplateId
	) {
		EmailTemplate emailTemplate = emailTemplateService.findById(emailTemplateId);

		return ResponseEntity.ok(EmailTemplateDto.fromEmailTemplateEntity(emailTemplate));
	}

	/**
	 * Creates a new email template for a research group.
	 *
	 * @param payload the payload containing the email template data
	 * @return the created email template
	 */
	@PostMapping
	@PreAuthorize("hasAnyRole('admin', 'supervisor', 'advisor')")
	public ResponseEntity<EmailTemplateDto> createEmailTemplate(
			@RequestBody CreateEmailTemplatePayload payload
	) {
		EmailTemplate emailTemplate = emailTemplateService.createEmailTemplate(
				payload.researchGroupId(),
				RequestValidator.validateNotNull(payload.templateCase()),
				payload.description(),
				RequestValidator.validateNotNull(payload.subject()),
				RequestValidator.validateNotNull(payload.bodyHtml()),
				RequestValidator.validateNotNull(payload.language())
		);

		return ResponseEntity.ok(EmailTemplateDto.fromEmailTemplateEntity(emailTemplate));
	}

	/**
	 * Updates an existing email template by its ID.
	 *
	 * @param emailTemplateId the ID of the email template to update
	 * @param payload the payload containing the updated email template data
	 * @return the updated email template
	 */
	@PutMapping("/{emailTemplateId}")
	@PreAuthorize("hasAnyRole('admin', 'supervisor', 'advisor')")
	public ResponseEntity<EmailTemplateDto> updateEmailTemplate(
			@PathVariable("emailTemplateId") UUID emailTemplateId,
			@RequestBody CreateEmailTemplatePayload payload
	) {
		EmailTemplate emailTemplate = emailTemplateService.findById(emailTemplateId);

	emailTemplate = emailTemplateService.updateEmailTemplate(
				emailTemplate,
			RequestValidator.validateNotNull(payload.templateCase()),
			payload.description(),
			RequestValidator.validateNotNull(payload.subject()),
			RequestValidator.validateNotNull(payload.bodyHtml()),
			RequestValidator.validateNotNull(payload.language())
		);

		return ResponseEntity.ok(EmailTemplateDto.fromEmailTemplateEntity(emailTemplate));
	}

	/**
	 * Deletes an email template by its ID.
	 *
	 * @param emailTemplateId the ID of the email template to delete
	 * @return a response with no content
	 */
	@DeleteMapping("/{emailTemplateId}")
	@PreAuthorize("hasAnyRole('admin', 'supervisor', 'advisor')")
	public ResponseEntity<Void> deleteEmailTemplate(
			@PathVariable("emailTemplateId") UUID emailTemplateId
	) {
		emailTemplateService.deleteEmailTemplate(emailTemplateId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{emailTemplateId}/variables")
    @PreAuthorize("hasAnyRole('admin', 'supervisor', 'advisor')")
    public ResponseEntity<List<MailVariableDto>> getEmailTemplateVariables(
            @PathVariable("emailTemplateId") UUID emailTemplateId
    ) {
        return ResponseEntity.ok(emailTemplateService.getVariablesForTemplate(emailTemplateId));
    }
}
