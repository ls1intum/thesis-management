package de.tum.cit.aet.thesis.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.controller.payload.CreateEmailTemplatePayload;
import de.tum.cit.aet.thesis.dto.MailVariableDto;
import de.tum.cit.aet.thesis.dto.PaginationDto;
import de.tum.cit.aet.thesis.entity.EmailTemplate;
import de.tum.cit.aet.thesis.service.EmailTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class EmailTemplateControllerTest {

	@Mock
	private EmailTemplateService emailTemplateService;

	private EmailTemplateController emailTemplateController;

	@BeforeEach
	void setUp() {
		emailTemplateController = new EmailTemplateController(emailTemplateService);
	}

	@Test
	void getEmailTemplates_ReturnsPaginationDto() {
		EmailTemplate entity = createEmailTemplate();
		Page<EmailTemplate> page = new PageImpl<>(List.of(entity));
		when(emailTemplateService.getAll(
				new String[]{"INTERVIEW_INVITATION"},
				new String[]{"en"},
				"interview",
				0,
				20,
				"templateCase",
				"asc",
				null
		)).thenReturn(page);

		ResponseEntity<PaginationDto<de.tum.cit.aet.thesis.dto.EmailTemplateDto>> response =
				emailTemplateController.getEmailTemplates(
					"interview",
					new String[]{"INTERVIEW_INVITATION"},
					new String[]{"en"},
					0,
					20,
					"templateCase",
					"asc",
					null
				);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(1, response.getBody().content().size());
		verify(emailTemplateService).getAll(
				new String[]{"INTERVIEW_INVITATION"},
				new String[]{"en"},
				"interview",
				0,
				20,
				"templateCase",
				"asc",
				null
		);
	}

	@Test
	void createEmailTemplate_CallsServiceAndReturnsDto() {
		EmailTemplate created = createEmailTemplate();
		when(emailTemplateService.createEmailTemplate(
				null,
				"INTERVIEW_INVITATION",
				"Interview invitation",
				"Subject",
				"<p>Body</p>",
				"en"
		)).thenReturn(created);

		ResponseEntity<?> response = emailTemplateController.createEmailTemplate(
				new CreateEmailTemplatePayload(
						null,
						"INTERVIEW_INVITATION",
						"Interview invitation",
						"Subject",
						"<p>Body</p>",
						"en"
				)
		);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		verify(emailTemplateService).createEmailTemplate(
				null,
				"INTERVIEW_INVITATION",
				"Interview invitation",
				"Subject",
				"<p>Body</p>",
				"en"
		);
	}

	@Test
	void getEmailTemplateVariables_ReturnsVariables() {
		UUID id = UUID.randomUUID();
		List<MailVariableDto> variables = List.of(
				new MailVariableDto("Label", "[[${application.thesisTitle}]]", "Example", "Application")
		);
		when(emailTemplateService.getVariablesForTemplate(id)).thenReturn(variables);

		ResponseEntity<List<MailVariableDto>> response = emailTemplateController.getEmailTemplateVariables(id);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(1, response.getBody().size());
		assertEquals("[[${application.thesisTitle}]]", response.getBody().getFirst().templateVariable());
		verify(emailTemplateService).getVariablesForTemplate(id);
	}

	private EmailTemplate createEmailTemplate() {
		EmailTemplate template = new EmailTemplate();
		template.setId(UUID.randomUUID());
		template.setTemplateCase("INTERVIEW_INVITATION");
		template.setDescription("Interview invitation");
		template.setSubject("Subject");
		template.setBodyHtml("<p>Body</p>");
		template.setLanguage("en");
		return template;
	}
}
