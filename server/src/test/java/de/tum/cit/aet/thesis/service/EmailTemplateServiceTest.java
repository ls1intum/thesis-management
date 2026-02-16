package de.tum.cit.aet.thesis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import de.tum.cit.aet.thesis.dto.MailVariableDto;
import de.tum.cit.aet.thesis.entity.EmailTemplate;
import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.exception.request.AccessDeniedException;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.EmailTemplateRepository;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class EmailTemplateServiceTest {

	@Mock
	private EmailTemplateRepository emailTemplateRepository;
	@Mock
	private ObjectProvider<CurrentUserProvider> currentUserProviderProvider;
	@Mock
	private CurrentUserProvider currentUserProvider;
	@Mock
	private ResearchGroupRepository researchGroupRepository;

	private EmailTemplateService emailTemplateService;

	@BeforeEach
	void setUp() {
		emailTemplateService = new EmailTemplateService(
				emailTemplateRepository,
				currentUserProviderProvider,
				researchGroupRepository
		);
		lenient().when(currentUserProviderProvider.getObject()).thenReturn(currentUserProvider);
	}

	@Test
	void getAll_WhenNonAdminAccessesDifferentResearchGroup_ThrowsAccessDenied() {
		UUID ownGroupId = UUID.randomUUID();
		UUID otherGroupId = UUID.randomUUID();
		ResearchGroup ownGroup = new ResearchGroup();
		ownGroup.setId(ownGroupId);

		when(currentUserProvider.isAdmin()).thenReturn(false);
		when(currentUserProvider.getResearchGroupOrThrow()).thenReturn(ownGroup);

		assertThrows(AccessDeniedException.class, () ->
				emailTemplateService.getAll(
						new String[]{"INTERVIEW_INVITATION"},
						new String[]{"en"},
						"search",
						0,
						10,
						"templateCase",
						"asc",
						otherGroupId
				)
		);
		verify(emailTemplateRepository, never()).searchEmailTemplate(any(), any(), any(), any(), any());
	}

	@Test
	void getAll_WhenAdminAndLimitMinusOne_UsesLowercaseSearchAndMaxPageSize() {
		UUID researchGroupId = UUID.randomUUID();
		when(currentUserProvider.isAdmin()).thenReturn(true);
		when(emailTemplateRepository.searchEmailTemplate(any(), any(), any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of()));

		Page<EmailTemplate> result = emailTemplateService.getAll(
				new String[]{},
				new String[]{},
				"HeLLo",
				0,
				-1,
				"templateCase",
				"asc",
				researchGroupId
		);

		assertNotNull(result);
		ArgumentCaptor<UUID> rgCaptor = ArgumentCaptor.forClass(UUID.class);
		ArgumentCaptor<String[]> caseCaptor = ArgumentCaptor.forClass(String[].class);
		ArgumentCaptor<String[]> langCaptor = ArgumentCaptor.forClass(String[].class);
		ArgumentCaptor<String> searchCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(emailTemplateRepository).searchEmailTemplate(
				rgCaptor.capture(),
				caseCaptor.capture(),
				langCaptor.capture(),
				searchCaptor.capture(),
				pageableCaptor.capture()
		);

		assertEquals(researchGroupId, rgCaptor.getValue());
		assertEquals(null, caseCaptor.getValue());
		assertEquals(null, langCaptor.getValue());
		assertEquals("hello", searchCaptor.getValue());
		assertEquals(Integer.MAX_VALUE, pageableCaptor.getValue().getPageSize());
	}

	@Test
	void findById_WhenMissing_ThrowsResourceNotFound() {
		UUID id = UUID.randomUUID();
		when(emailTemplateRepository.findById(id)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> emailTemplateService.findById(id));
	}

	@Test
	void createEmailTemplate_WhenNonAdminWithoutResearchGroup_ThrowsAccessDenied() {
		when(currentUserProvider.isAdmin()).thenReturn(false);

		assertThrows(AccessDeniedException.class, () ->
				emailTemplateService.createEmailTemplate(
						null,
						"INTERVIEW_INVITATION",
						"desc",
						"subject",
						"body",
						"en"
				)
		);
	}

	@Test
	void createEmailTemplate_WhenInvalidTemplateCase_ThrowsIllegalArgumentException() {
		when(currentUserProvider.isAdmin()).thenReturn(true);

		assertThrows(IllegalArgumentException.class, () ->
				emailTemplateService.createEmailTemplate(
						null,
						"NOT_VALID_CASE",
						"desc",
						"subject",
						"body",
						"en"
				)
		);
	}

	@Test
	void createEmailTemplate_WhenExistingTemplate_UpdatesInPlace() {
		User updater = new User();
		updater.setId(UUID.randomUUID());
		Instant createdAt = Instant.parse("2025-01-01T00:00:00Z");

		EmailTemplate existing = new EmailTemplate();
		existing.setId(UUID.randomUUID());
		existing.setCreatedAt(createdAt);

		when(currentUserProvider.isAdmin()).thenReturn(true);
		when(currentUserProvider.getUser()).thenReturn(updater);
		when(emailTemplateRepository.findByResearchGroupAndTemplateCase(null, "INTERVIEW_INVITATION"))
				.thenReturn(Optional.of(existing));
		when(emailTemplateRepository.save(any(EmailTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

		EmailTemplate result = emailTemplateService.createEmailTemplate(
				null,
				"INTERVIEW_INVITATION",
				"description",
				"subject",
				"body",
				"en"
		);

		assertEquals(existing.getId(), result.getId());
		assertEquals(createdAt, result.getCreatedAt());
		assertEquals("subject", result.getSubject());
		assertEquals("body", result.getBodyHtml());
		assertEquals("en", result.getLanguage());
		assertEquals(updater, result.getUpdatedBy());
	}

	@Test
	void updateEmailTemplate_WhenDefaultTemplateAndNonAdmin_ThrowsAccessDenied() {
		EmailTemplate emailTemplate = new EmailTemplate();
		emailTemplate.setResearchGroup(null);

		when(currentUserProvider.isAdmin()).thenReturn(false);

		assertThrows(AccessDeniedException.class, () ->
				emailTemplateService.updateEmailTemplate(
						emailTemplate,
						"INTERVIEW_INVITATION",
						"description",
						"subject",
						"body",
						"en"
				)
		);
	}

	@Test
	void deleteEmailTemplate_WhenDefaultTemplateAndNonAdmin_ThrowsAccessDenied() {
		UUID id = UUID.randomUUID();
		EmailTemplate emailTemplate = new EmailTemplate();
		emailTemplate.setResearchGroup(null);
		when(emailTemplateRepository.findById(id)).thenReturn(Optional.of(emailTemplate));
		when(currentUserProvider.isAdmin()).thenReturn(false);

		assertThrows(AccessDeniedException.class, () -> emailTemplateService.deleteEmailTemplate(id));
	}

	@Test
	void getVariablesForTemplate_WithInterviewSlotCase_ReturnsRecipientAndInterviewVariables() {
		UUID id = UUID.randomUUID();
		EmailTemplate emailTemplate = new EmailTemplate();
		emailTemplate.setTemplateCase("INTERVIEW_SLOT_BOOKED_CONFORMATION");
		when(emailTemplateRepository.findById(id)).thenReturn(Optional.of(emailTemplate));

		List<MailVariableDto> result = emailTemplateService.getVariablesForTemplate(id);

		assertFalse(result.isEmpty());
		assertTrueContainsVariable(result, "[[${recipient.firstName}]]");
		assertTrueContainsVariable(result, "[[${application.thesisTitle}]]");
		assertTrueContainsVariable(result, "[[${inviteUrl}]]");
		assertTrueContainsVariable(result, "[[${slot.streamUrl}]]");
	}

	private void assertTrueContainsVariable(List<MailVariableDto> variables, String templateVariable) {
		boolean found = variables.stream().anyMatch(v -> templateVariable.equals(v.templateVariable()));
		assertTrue(found, "Missing variable: " + templateVariable);
	}
}
