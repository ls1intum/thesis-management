package de.tum.cit.aet.thesis.dto;

import de.tum.cit.aet.thesis.entity.EmailTemplate;

import java.util.UUID;

public record EmailTemplateDto(
    UUID id,
    LightResearchGroupDto researchGroup,
    String templateCase,
    String description,
    String subject,
    String bodyHtml,
    String language
) {

  public static EmailTemplateDto fromEmailTemplateEntity(EmailTemplate emailTemplate) {
    if (emailTemplate == null) {
      return null;
    }

    return new EmailTemplateDto(
            emailTemplate.getId(),
            LightResearchGroupDto.fromResearchGroupEntity(emailTemplate.getResearchGroup()),
            emailTemplate.getTemplateCase(),
            emailTemplate.getDescription(),
            emailTemplate.getSubject(),
            emailTemplate.getBodyHtml(),
            emailTemplate.getLanguage());
  }
}