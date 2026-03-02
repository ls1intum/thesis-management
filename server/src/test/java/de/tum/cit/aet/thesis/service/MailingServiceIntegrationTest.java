package de.tum.cit.aet.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.constants.ApplicationRejectReason;
import de.tum.cit.aet.thesis.controller.payload.AcceptApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.RejectApplicationPayload;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Testcontainers
class MailingServiceIntegrationTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	private int countAttachments(MimeMessage message) throws Exception {
		if (message.getContent() instanceof Multipart multipart) {
			int count = 0;
			for (int i = 0; i < multipart.getCount(); i++) {
				String disposition = multipart.getBodyPart(i).getDisposition();
				if (disposition != null && disposition.equalsIgnoreCase("attachment")) {
					count++;
				}
			}
			return count;
		}
		return 0;
	}

	private String getEmailBodyText(MimeMessage message) throws Exception {
		Object content = message.getContent();
		if (content instanceof String) {
			return (String) content;
		}
		if (content instanceof Multipart multipart) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < multipart.getCount(); i++) {
				BodyPart bodyPart = multipart.getBodyPart(i);
				if (bodyPart.isMimeType("text/html") || bodyPart.isMimeType("text/plain")) {
					sb.append(bodyPart.getContent().toString());
				}
			}
			return sb.toString();
		}
		return content.toString();
	}

	private List<String> getAllRecipientAddresses(MimeMessage[] emails) {
		return Stream.of(emails)
				.flatMap(email -> {
					try {
						return Arrays.stream(email.getAllRecipients());
					} catch (Exception e) {
						return Stream.empty();
					}
				})
				.map(Address::toString)
				.toList();
	}

	private MimeMessage findEmailByRecipient(MimeMessage[] emails, String universityId) throws Exception {
		for (MimeMessage email : emails) {
			List<String> recipients = Arrays.stream(email.getAllRecipients())
					.map(Address::toString)
					.toList();
			if (recipients.stream().anyMatch(addr -> addr.contains(universityId))) {
				return email;
			}
		}
		return null;
	}

	@Nested
	class ApplicationEmails {
		@Test
		void createApplication_SendsEmailToChairMembersAndStudent() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID researchGroupId = createTestResearchGroup("Email App Group", head.universityId());

			clearEmails();

			TestUser student = createRandomTestUser(List.of("student"));
			String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

			CreateApplicationPayload payload = new CreateApplicationPayload(
					null, "Email Test Thesis", "BACHELOR", Instant.now(), "Test motivation", researchGroupId
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			MimeMessage[] emails = getReceivedEmails();
			assertThat(emails.length).as("At least one email should be sent on application creation")
					.isGreaterThanOrEqualTo(1);

			List<String> recipients = getAllRecipientAddresses(emails);
			assertThat(recipients).as("Student should receive an email")
					.anyMatch(addr -> addr.contains(student.universityId()));
		}

		@Test
		void createApplication_DefaultSetting_SendsMinimalEmailToChair() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID researchGroupId = createTestResearchGroup("Minimal Email Group", head.universityId());

			// Default setting is false (includeApplicationDataInEmail = false)
			// Do NOT enable the setting — we want to test the default behavior

			clearEmails();

			TestUser student = createRandomTestUser(List.of("student"));
			String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

			CreateApplicationPayload payload = new CreateApplicationPayload(
					null, "Minimal Email Thesis", "BACHELOR", Instant.now(), "My secret motivation text", researchGroupId
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			MimeMessage[] emails = getReceivedEmails();
			assertThat(emails.length).as("At least two emails should be sent (chair + student)")
					.isGreaterThanOrEqualTo(2);

			// Find the chair email
			MimeMessage chairEmail = findEmailByRecipient(emails, head.universityId());
			assertThat(chairEmail).as("Chair member (head) should receive an email").isNotNull();

			// Verify minimal email subject
			assertThat(chairEmail.getSubject())
					.as("Minimal chair email should have the hardcoded subject 'New Thesis Application'")
					.isEqualTo("New Thesis Application");

			// Verify minimal email has no file attachments
			int attachmentCount = countAttachments(chairEmail);
			assertThat(attachmentCount)
					.as("Minimal chair email should have no file attachments")
					.isEqualTo(0);

			// Verify minimal email body contains applicant name and application link
			String chairBody = getEmailBodyText(chairEmail);
			assertThat(chairBody)
					.as("Minimal chair email body should contain the applicant's name (university ID used as name in tests)")
					.contains(student.universityId());
			assertThat(chairBody)
					.as("Minimal chair email body should contain the thesis title")
					.contains("Minimal Email Thesis");
			assertThat(chairBody)
					.as("Minimal chair email body should contain a link to the application")
					.contains("/applications/");

			// Verify minimal email does NOT contain personal details
			assertThat(chairBody)
					.as("Minimal chair email should NOT contain the motivation text")
					.doesNotContain("My secret motivation text");

			// Verify that the student still receives their full email
			MimeMessage studentEmail = findEmailByRecipient(emails, student.universityId());
			assertThat(studentEmail).as("Student should still receive their email").isNotNull();
		}

		@Test
		void createApplication_SettingDisabled_DoesNotUseCustomTemplate() throws Exception {
			// Create a custom template with a distinctive subject
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID researchGroupId = createTestResearchGroup("No Template Group", head.universityId());

			// Default setting is false — minimal email should be sent, NOT the template

			clearEmails();

			TestUser student = createRandomTestUser(List.of("student"));
			String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

			CreateApplicationPayload payload = new CreateApplicationPayload(
					null, "Template Override Test", "MASTER", Instant.now(), "Some motivation", researchGroupId
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			MimeMessage[] emails = getReceivedEmails();

			MimeMessage chairEmail = findEmailByRecipient(emails, head.universityId());
			assertThat(chairEmail).as("Chair member should receive an email").isNotNull();

			// The test template has subject "Test Subject". Minimal email uses "New Thesis Application".
			assertThat(chairEmail.getSubject())
					.as("When setting is disabled, chair email should NOT use the custom template subject")
					.isNotEqualTo("Test Subject");
			assertThat(chairEmail.getSubject())
					.as("When setting is disabled, chair email should use the minimal hardcoded subject")
					.isEqualTo("New Thesis Application");

			// The test template body is "<p>Test Body</p>". Minimal email should not contain this.
			String chairBody = getEmailBodyText(chairEmail);
			assertThat(chairBody)
					.as("When setting is disabled, chair email should NOT use the custom template body")
					.doesNotContain("Test Body");
		}

		@Test
		void createApplication_WithSettingEnabled_UsesCustomTemplate() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID researchGroupId = createTestResearchGroup("Full Email Group", head.universityId());

			// Enable includeApplicationDataInEmail
			String settingsPayload = objectMapper.writeValueAsString(Map.of(
					"applicationEmailSettings", Map.of("includeApplicationDataInEmail", true)
			));

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", researchGroupId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(settingsPayload))
					.andExpect(status().isOk());

			clearEmails();

			TestUser student = createRandomTestUser(List.of("student"));
			String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

			CreateApplicationPayload payload = new CreateApplicationPayload(
					null, "Full Email Thesis", "BACHELOR", Instant.now(), "Enabled motivation", researchGroupId
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			MimeMessage[] emails = getReceivedEmails();
			assertThat(emails.length).as("At least two emails should be sent (chair + student)")
					.isGreaterThanOrEqualTo(2);

			// Chair email should use the custom template
			MimeMessage chairEmail = findEmailByRecipient(emails, head.universityId());
			assertThat(chairEmail).as("Chair member should receive an email when setting is enabled").isNotNull();

			// With setting enabled, the template subject should be used (test template: "Test Subject")
			assertThat(chairEmail.getSubject())
					.as("When setting is enabled, chair email should use the custom template subject")
					.isEqualTo("Test Subject");

			// Student email should always be sent regardless of the setting
			List<String> allRecipients = getAllRecipientAddresses(emails);
			assertThat(allRecipients).as("Student should receive an email")
					.anyMatch(addr -> addr.contains(student.universityId()));
		}

		@Test
		void createApplication_ToggleSettingOnThenOff_RespectsCurrentSetting() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID researchGroupId = createTestResearchGroup("Toggle Group", head.universityId());
			String adminAuth = createRandomAdminAuthentication();

			// First, enable the setting
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", researchGroupId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(Map.of(
									"applicationEmailSettings", Map.of("includeApplicationDataInEmail", true)
							))))
					.andExpect(status().isOk());

			// Then, disable the setting again
			mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-group-settings/{id}", researchGroupId)
							.header("Authorization", adminAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(Map.of(
									"applicationEmailSettings", Map.of("includeApplicationDataInEmail", false)
							))))
					.andExpect(status().isOk());

			clearEmails();

			TestUser student = createRandomTestUser(List.of("student"));
			String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

			CreateApplicationPayload payload = new CreateApplicationPayload(
					null, "Toggle Test Thesis", "BACHELOR", Instant.now(), "Toggle motivation", researchGroupId
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			MimeMessage[] emails = getReceivedEmails();
			MimeMessage chairEmail = findEmailByRecipient(emails, head.universityId());
			assertThat(chairEmail).as("Chair member should receive an email").isNotNull();

			// After toggling back to disabled, should use minimal email again
			assertThat(chairEmail.getSubject())
					.as("After disabling setting, chair email should use minimal subject again")
					.isEqualTo("New Thesis Application");

			String chairBody = getEmailBodyText(chairEmail);
			assertThat(chairBody)
					.as("After disabling setting, chair email should NOT contain motivation")
					.doesNotContain("Toggle motivation");
		}

		@Test
		void createApplication_MinimalEmail_ContainsThesisTitleWhenProvided() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID researchGroupId = createTestResearchGroup("Title Check Group", head.universityId());

			clearEmails();

			TestUser student = createRandomTestUser(List.of("student"));
			String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

			String specificTitle = "Exploring Neural Network Architectures";
			CreateApplicationPayload payload = new CreateApplicationPayload(
					null, specificTitle, "MASTER", Instant.now(), "Some motivation", researchGroupId
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			MimeMessage[] emails = getReceivedEmails();
			MimeMessage chairEmail = findEmailByRecipient(emails, head.universityId());
			assertThat(chairEmail).as("Chair email should be sent").isNotNull();

			String chairBody = getEmailBodyText(chairEmail);
			assertThat(chairBody)
					.as("Minimal chair email should include the thesis title")
					.contains(specificTitle);
		}

		@Test
		void createApplication_MinimalEmail_StudentEmailUnaffectedBySetting() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID researchGroupId = createTestResearchGroup("Student Unaffected Group", head.universityId());

			// Setting is disabled by default — only chair email should be minimal

			clearEmails();

			TestUser student = createRandomTestUser(List.of("student"));
			String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

			CreateApplicationPayload payload = new CreateApplicationPayload(
					null, "Student Email Test", "BACHELOR", Instant.now(), "Student motivation", researchGroupId
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			MimeMessage[] emails = getReceivedEmails();
			MimeMessage studentEmail = findEmailByRecipient(emails, student.universityId());
			assertThat(studentEmail).as("Student should receive their email regardless of the setting").isNotNull();

			// Student email should still use the template (not the minimal one)
			assertThat(studentEmail.getSubject())
					.as("Student email subject should be the template subject, not the minimal one")
					.isEqualTo("Test Subject");
		}

		@Test
		void createApplication_MinimalEmail_EscapesHtmlInUserInput() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID researchGroupId = createTestResearchGroup("XSS Test Group", head.universityId());

			clearEmails();

			TestUser student = createRandomTestUser(List.of("student"));
			String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

			// Use a thesis title containing HTML/script injection
			String maliciousTitle = "<script>alert('xss')</script>";
			CreateApplicationPayload payload = new CreateApplicationPayload(
					null, maliciousTitle, "BACHELOR", Instant.now(), "Some motivation", researchGroupId
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			MimeMessage[] emails = getReceivedEmails();
			MimeMessage chairEmail = findEmailByRecipient(emails, head.universityId());
			assertThat(chairEmail).as("Chair email should be sent").isNotNull();

			String chairBody = getEmailBodyText(chairEmail);
			assertThat(chairBody)
					.as("Minimal email body must NOT contain raw <script> tags (XSS prevention)")
					.doesNotContain("<script>");
			assertThat(chairBody)
					.as("Minimal email body should contain HTML-escaped version of the title")
					.contains("&lt;script&gt;");
		}

		@Test
		void acceptApplication_SendsAcceptanceEmail() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");
			createTestEmailTemplate("APPLICATION_ACCEPTED");
			createTestEmailTemplate("APPLICATION_ACCEPTED_NO_ADVISOR");
			createTestEmailTemplate("THESIS_CREATED");

			TestUser student = createRandomTestUser(List.of("student"));
			String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

			TestUser supervisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Accept Email Group", supervisor.universityId());

			CreateApplicationPayload appPayload = new CreateApplicationPayload(
					null, "Accept Email Thesis", "MASTER", Instant.now(), "Motivation", researchGroupId
			);

			String appResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(appPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			UUID applicationId = UUID.fromString(objectMapper.readTree(appResponse).get("applicationId").asString());

			clearEmails();

			AcceptApplicationPayload acceptPayload = new AcceptApplicationPayload(
					"Accept Email Thesis", "MASTER", "ENGLISH",
					List.of(supervisor.userId()),
					List.of(supervisor.userId()),
					true, false
			);

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{id}/accept", applicationId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(acceptPayload)))
					.andExpect(status().isOk());

			MimeMessage[] emails = getReceivedEmails();
			assertThat(emails.length).as("At least one email should be sent on acceptance")
					.isGreaterThanOrEqualTo(1);

			List<String> recipients = getAllRecipientAddresses(emails);
			assertThat(recipients).as("Student should receive an acceptance email")
					.anyMatch(addr -> addr.contains(student.universityId()));
		}

		@Test
		void rejectApplication_SendsRejectionEmail() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");
			createTestEmailTemplate("APPLICATION_REJECTED");

			TestUser student = createRandomTestUser(List.of("student"));
			String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID researchGroupId = createTestResearchGroup("Reject Email Group", head.universityId());

			CreateApplicationPayload appPayload = new CreateApplicationPayload(
					null, "Reject Email Thesis", "MASTER", Instant.now(), "Motivation", researchGroupId
			);

			String appResponse = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(appPayload)))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			UUID applicationId = UUID.fromString(objectMapper.readTree(appResponse).get("applicationId").asString());

			clearEmails();

			RejectApplicationPayload rejectPayload = new RejectApplicationPayload(
					ApplicationRejectReason.GENERAL, true
			);

			mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/{id}/reject", applicationId)
							.header("Authorization", createRandomAdminAuthentication())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(rejectPayload)))
					.andExpect(status().isOk());

			MimeMessage[] emails = getReceivedEmails();
			assertThat(emails.length).as("At least one email should be sent on rejection")
					.isGreaterThanOrEqualTo(1);

			List<String> recipients = getAllRecipientAddresses(emails);
			assertThat(recipients).as("Student should receive a rejection email")
					.anyMatch(addr -> addr.contains(student.universityId()));
		}
	}

	@Nested
	class ThesisEmails {
		@Test
		void createThesis_SendsCreatedEmail() throws Exception {
			createTestEmailTemplate("THESIS_CREATED");

			clearEmails();

			createTestThesis("Thesis Email Test");

			MimeMessage[] emails = getReceivedEmails();
			assertThat(emails.length).as("At least one email should be sent on thesis creation")
					.isGreaterThanOrEqualTo(1);

			List<String> recipients = getAllRecipientAddresses(emails);
			assertThat(recipients).as("At least one recipient should receive the thesis creation email")
					.isNotEmpty();

			// Verify all emails have a subject
			for (MimeMessage email : emails) {
				assertThat(email.getSubject()).as("Each email should have a non-null subject").isNotNull();
			}
		}
	}
}
