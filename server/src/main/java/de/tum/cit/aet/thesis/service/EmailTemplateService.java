package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.dto.MailVariableDto;
import de.tum.cit.aet.thesis.entity.EmailTemplate;
import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.exception.request.AccessDeniedException;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.mailvariables.MailVariablesBuilder;
import de.tum.cit.aet.thesis.repository.EmailTemplateRepository;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.utility.HibernateHelper;
import de.tum.cit.aet.thesis.utility.TemplateValidator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Manages CRUD operations for email templates scoped to research groups. */
@Service
public class EmailTemplateService {

	private final EmailTemplateRepository emailTemplateRepository;
	private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;
	private final ResearchGroupRepository researchGroupRepository;

	/**
	 * Injects the email template repository, the current user provider, and the research group repository.
	 *
	 * @param emailTemplateRepository the email template repository
	 * @param currentUserProviderProvider the current user provider
	 * @param researchGroupRepository the research group repository
	 */
	@Autowired
	public EmailTemplateService(EmailTemplateRepository emailTemplateRepository, ObjectProvider<CurrentUserProvider> currentUserProviderProvider, ResearchGroupRepository researchGroupRepository) {
		this.emailTemplateRepository = emailTemplateRepository;
		this.currentUserProviderProvider = currentUserProviderProvider;
		this.researchGroupRepository = researchGroupRepository;
	}

	private CurrentUserProvider currentUserProvider() {
		return currentUserProviderProvider.getObject();
	}

	private static final Set<String> VALID_TEMPLATE_CASES = Set.of(
			"APPLICATION_REJECTED_TOPIC_REQUIREMENTS",
			"THESIS_PRESENTATION_UPDATED",
			"THESIS_ASSESSMENT_ADDED",
			"THESIS_PRESENTATION_SCHEDULED",
			"THESIS_CREATED",
			"APPLICATION_REJECTED_TOPIC_OUTDATED",
			"THESIS_FINAL_SUBMISSION",
			"THESIS_PROPOSAL_REJECTED",
			"THESIS_COMMENT_POSTED",
			"THESIS_PRESENTATION_DELETED",
			"THESIS_PROPOSAL_ACCEPTED",
			"APPLICATION_REJECTED",
			"APPLICATION_REJECTED_TITLE_NOT_INTERESTING",
			"APPLICATION_ACCEPTED",
			"APPLICATION_CREATED_CHAIR",
			"APPLICATION_ACCEPTED_NO_SUPERVISOR",
			"APPLICATION_REJECTED_TOPIC_FILLED",
			"THESIS_CLOSED",
			"THESIS_PRESENTATION_INVITATION_UPDATED",
			"APPLICATION_CREATED_STUDENT",
			"THESIS_PRESENTATION_INVITATION_CANCELLED",
			"THESIS_PRESENTATION_INVITATION",
			"THESIS_PROPOSAL_UPLOADED",
			"APPLICATION_REMINDER",
			"THESIS_FINAL_GRADE",
			"APPLICATION_REJECTED_STUDENT_REQUIREMENTS",
			"APPLICATION_AUTOMATIC_REJECT_REMINDER",
			"INTERVIEW_INVITATION",
			"INTERVIEW_INVITATION_REMINDER",
			"INTERVIEW_SLOT_BOOKED_CONFORMATION",
			"INTERVIEW_SLOT_BOOKED_CANCELLATION",
			"THESIS_ANONYMIZATION_REMINDER"
	);
	/**
	 * Returns a paginated and filtered list of email templates for the current user's research group.
	 *
	 * @param templateCases the template cases to filter by
	 * @param languages the languages to filter by
	 * @param searchQuery the search query string
	 * @param page the page number
	 * @param limit the number of results per page
	 * @param sortBy the field to sort by
	 * @param sortOrder the sort direction (asc or desc)
	 * @param researchGroupId optional research group ID filter
	 * @return the paginated list of email templates
	 */
	public Page<EmailTemplate> getAll(
			String[] templateCases,
			String[] languages,
			String searchQuery,
			int page,
			int limit,
			String sortBy,
			String sortOrder,
			UUID researchGroupId
	) {
		if (!currentUserProvider().isAdmin() && researchGroupId != null && !currentUserProvider().getResearchGroupOrThrow().getId().equals(researchGroupId)) {
			throw new AccessDeniedException("You do not have access to this research group.");
		}

		UUID searchResearchGroupId = currentUserProvider().isAdmin() ? researchGroupId : currentUserProvider().getResearchGroupOrThrow().getId();

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
				searchResearchGroupId,
				templateCasesFilter,
				languagesFilter,
				searchQueryFilter,
				pageable
		);
	}

	/**
	 * Finds an email template by its ID and verifies the current user has access to its research group.
	 *
	 * @param emailTemplateId the ID of the email template
	 * @return the found email template
	 */
	public EmailTemplate findById(UUID emailTemplateId) {
		EmailTemplate emailTemplate = emailTemplateRepository.findById(emailTemplateId)
				.orElseThrow(() -> new ResourceNotFoundException(
						String.format("Email Template with id %s not found.", emailTemplateId)));
		if (emailTemplate.getResearchGroup() != null) {
			currentUserProvider().assertCanAccessResearchGroup(emailTemplate.getResearchGroup());
		}
		return emailTemplate;
	}

	// TODO: we should avoid using @Transactional because it can lead to performance issue and concurrency problems
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
		} else if (!currentUserProvider().isAdmin()) { // Allow only admins to create default templates
			throw new AccessDeniedException("Only admins can create default email templates without ResearchGroupId.");
		}

		currentUserProvider().assertCanAccessResearchGroup(researchGroup);
		validateTemplateCase(templateCase);
		TemplateValidator.validateTemplateContent(subject);
		TemplateValidator.validateTemplateContent(bodyHtml);

		// Check if a template with the same case and research group already exists -> update instead of create
		EmailTemplate emailTemplate = emailTemplateRepository
				.findByResearchGroupAndTemplateCase(researchGroupId, templateCase)
				.orElse(new EmailTemplate());

		emailTemplate.setTemplateCase(templateCase);
		emailTemplate.setDescription(description);
		emailTemplate.setSubject(subject);
		emailTemplate.setBodyHtml(bodyHtml);
		emailTemplate.setLanguage(language);
		emailTemplate.setResearchGroup(researchGroup);
		if (emailTemplate.getId() == null) {
			emailTemplate.setCreatedAt(Instant.now()); //only set createdAt if it's a new template
		}
		emailTemplate.setUpdatedAt(Instant.now());
		emailTemplate.setUpdatedBy(currentUserProvider().getUser());

		return emailTemplateRepository.save(emailTemplate);
	}

	// TODO: we should avoid using @Transactional because it can lead to performance issue and concurrency problems
	@Transactional
	public EmailTemplate updateEmailTemplate(
			EmailTemplate emailTemplate,
			String templateCase,
			String description,
			String subject,
			String bodyHtml,
			String language
	) {
		if (emailTemplate.getResearchGroup() == null && !currentUserProvider().isAdmin()) {
			throw new AccessDeniedException("Only admins can update default email templates without ResearchGroupId.");
		}

		currentUserProvider().assertCanAccessResearchGroup(emailTemplate.getResearchGroup());
		validateTemplateCase(templateCase);
		TemplateValidator.validateTemplateContent(subject);
		TemplateValidator.validateTemplateContent(bodyHtml);

		emailTemplate.setTemplateCase(templateCase);
		emailTemplate.setDescription(description);
		emailTemplate.setSubject(subject);
		emailTemplate.setBodyHtml(bodyHtml);
		emailTemplate.setLanguage(language);
		emailTemplate.setUpdatedAt(Instant.now());
		emailTemplate.setUpdatedBy(currentUserProvider().getUser());

		return emailTemplateRepository.save(emailTemplate);
	}

	// TODO: we should avoid using @Transactional because it can lead to performance issue and concurrency problems
	@Transactional
	public void deleteEmailTemplate(UUID emailTemplateId) {
		EmailTemplate emailTemplate = emailTemplateRepository.findById(emailTemplateId)
				.orElseThrow(() -> new ResourceNotFoundException(
						String.format("Email Template with id %s not found.", emailTemplateId)));

		if (emailTemplate.getResearchGroup() == null && !currentUserProvider().isAdmin()) {
			throw new AccessDeniedException("Only admins can delete default email templates without ResearchGroupId.");
		}

		currentUserProvider().assertCanAccessResearchGroup(emailTemplate.getResearchGroup());

		emailTemplateRepository.deleteById(emailTemplateId);
	}

	private void validateTemplateCase(String templateCase) {
		if (!VALID_TEMPLATE_CASES.contains(templateCase)) {
			throw new IllegalArgumentException("Invalid template case: " + templateCase);
		}
	}

	/**
	 * Returns template variables available for a given email template.
	 *
	 * @param emailTemplateId the email template ID
	 * @return available mail variable descriptors
	 */
	public List<MailVariableDto> getVariablesForTemplate(UUID emailTemplateId) {
		EmailTemplate emailTemplate = findById(emailTemplateId);

		MailVariablesBuilder mailVariablesBuilder = new MailVariablesBuilder();
		return mailVariablesBuilder.getMailVariables(emailTemplate.getTemplateCase());
	}
}
