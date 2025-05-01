package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.controller.payload.CreateEmailTemplatePayload;
import de.tum.cit.aet.thesis.dto.EmailTemplateDto;
import de.tum.cit.aet.thesis.dto.PaginationDto;
import de.tum.cit.aet.thesis.entity.EmailTemplate;
import de.tum.cit.aet.thesis.service.EmailTemplateService;
import de.tum.cit.aet.thesis.utility.RequestValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v2/email-templates")
public class EmailTemplateController {

    private final EmailTemplateService emailTemplateService;

    @Autowired
    public EmailTemplateController(EmailTemplateService emailTemplateService) {
        this.emailTemplateService = emailTemplateService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'supervisor', 'advisor')")
    public ResponseEntity<PaginationDto<EmailTemplateDto>> getEmailTemplates(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "") String[] templateCases,
            @RequestParam(required = false, defaultValue = "") String[] languages,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer limit,
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder
    ) {
        Page<EmailTemplate> emailTemplates = emailTemplateService.getAll(
                templateCases,
                languages,
                search,
                page,
                limit,
                sortBy,
                sortOrder
        );

        return ResponseEntity.ok(PaginationDto.fromSpringPage(
                emailTemplates.map(EmailTemplateDto::fromEmailTemplateEntity)));
    }

    @GetMapping("/{emailTemplateId}")
    @PreAuthorize("hasAnyRole('admin', 'supervisor', 'advisor')")
    public ResponseEntity<EmailTemplateDto> getEmailTemplate(
            @PathVariable("emailTemplateId") UUID emailTemplateId
    ) {
        EmailTemplate emailTemplate = emailTemplateService.findById(emailTemplateId);

        return ResponseEntity.ok(EmailTemplateDto.fromEmailTemplateEntity(emailTemplate));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'supervisor', 'advisor')")
    public ResponseEntity<EmailTemplateDto> createEmailTemplate(
            @RequestBody CreateEmailTemplatePayload payload
    ) {
        EmailTemplate emailTemplate = emailTemplateService.createEmailTemplate(
                RequestValidator.validateNotNull(payload.researchGroupId()),
                RequestValidator.validateNotNull(payload.templateCase()),
                payload.description(),
                RequestValidator.validateNotNull(payload.subject()),
                RequestValidator.validateNotNull(payload.bodyHtml()),
                RequestValidator.validateNotNull(payload.language())
        );

        return ResponseEntity.ok(EmailTemplateDto.fromEmailTemplateEntity(emailTemplate));
    }

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

    @DeleteMapping("/{emailTemplateId}")
    @PreAuthorize("hasAnyRole('admin', 'supervisor', 'advisor')")
    public ResponseEntity<Void> deleteEmailTemplate(
            @PathVariable("emailTemplateId") UUID emailTemplateId
    ) {
        emailTemplateService.deleteEmailTemplate(emailTemplateId);

        return ResponseEntity.noContent().build();
    }
}