package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.entity.EmailTemplate;
import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.EmailTemplateRepository;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.utility.HibernateHelper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class EmailTemplateService {

    private final EmailTemplateRepository emailTemplateRepository;
    private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;
    private final ResearchGroupRepository researchGroupRepository;

    @Autowired
    public EmailTemplateService(EmailTemplateRepository emailTemplateRepository, ObjectProvider<CurrentUserProvider> currentUserProviderProvider, ResearchGroupRepository researchGroupRepository) {
        this.emailTemplateRepository = emailTemplateRepository;
        this.currentUserProviderProvider = currentUserProviderProvider;
        this.researchGroupRepository = researchGroupRepository;
    }

    private CurrentUserProvider currentUserProvider() {
        return currentUserProviderProvider.getObject();
    }

    public Page<EmailTemplate> getAll(
            String[] templateCases,
            String[] languages,
            String searchQuery,
            int page,
            int limit,
            String sortBy,
            String sortOrder
    ) {

        UUID researchGroupId = currentUserProvider().isAdmin() ?
                null : currentUserProvider().getResearchGroupOrThrow().getId();
        String searchQueryFilter =
                searchQuery == null || searchQuery.isEmpty() ? null : searchQuery.toLowerCase();
        String[] templateCasesFilter = templateCases == null || templateCases.length == 0 ? null : templateCases;
        String[] languagesFilter = languages == null || languages.length == 0 ? null : languages;

        Sort.Order order = new Sort.Order(
                sortOrder.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
                HibernateHelper.getColumnName(EmailTemplate.class, sortBy)
        );

        Pageable pageable = limit == -1
                ? PageRequest.of(0, Integer.MAX_VALUE, Sort.by(order))
                : PageRequest.of(page, limit, Sort.by(order));

        return emailTemplateRepository.searchEmailTemplate(
                researchGroupId,
                templateCasesFilter,
                languagesFilter,
                searchQueryFilter,
                pageable
        );
    }

    public EmailTemplate findById(UUID emailTemplateId) {
        EmailTemplate emailTemplate = emailTemplateRepository.findById(emailTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Email Template with id %s not found.", emailTemplateId)));
        currentUserProvider().assertCanAccessResearchGroup(emailTemplate.getResearchGroup());
        return emailTemplate;
    }

    @Transactional
    public EmailTemplate createEmailTemplate(
            UUID researchGroupId,
            String templateCase,
            String description,
            String subject,
            String bodyHtml,
            String language
    ) {
        ResearchGroup researchGroup = null;
        if (researchGroupId != null) {
            researchGroup = researchGroupRepository.findById(researchGroupId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format("Research Group with id %s not found.", researchGroupId)));
        }
        currentUserProvider().assertCanAccessResearchGroup(researchGroup);

        EmailTemplate emailTemplate = new EmailTemplate();
        emailTemplate.setTemplateCase(templateCase);
        emailTemplate.setDescription(description);
        emailTemplate.setSubject(subject);
        emailTemplate.setBodyHtml(bodyHtml);
        emailTemplate.setLanguage(language);
        emailTemplate.setResearchGroup(researchGroup);
        emailTemplate.setCreatedAt(Instant.now());
        emailTemplate.setUpdatedAt(Instant.now());
        emailTemplate.setUpdatedBy(currentUserProvider().getUser());

        return emailTemplateRepository.save(emailTemplate);
    }

    @Transactional
    public EmailTemplate updateEmailTemplate(
            EmailTemplate emailTemplate,
            String templateCase,
            String description,
            String subject,
            String bodyHtml,
            String language
    ) {
        currentUserProvider().assertCanAccessResearchGroup(emailTemplate.getResearchGroup());
        emailTemplate.setTemplateCase(templateCase);
        emailTemplate.setDescription(description);
        emailTemplate.setSubject(subject);
        emailTemplate.setBodyHtml(bodyHtml);
        emailTemplate.setLanguage(language);
        emailTemplate.setUpdatedAt(Instant.now());
        emailTemplate.setUpdatedBy(currentUserProvider().getUser());

        return emailTemplateRepository.save(emailTemplate);
    }

    @Transactional
    public void deleteEmailTemplate(UUID emailTemplateId) {
        EmailTemplate emailTemplate = emailTemplateRepository.findById(emailTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Email Template with id %s not found.", emailTemplateId)));
        currentUserProvider().assertCanAccessResearchGroup(emailTemplate.getResearchGroup());

        emailTemplateRepository.deleteById(emailTemplateId);
    }
}