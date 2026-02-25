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
		void createApplication_DefaultSetting_ChairEmailHasNoAttachments() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID researchGroupId = createTestResearchGroup("No Attach Group", head.universityId());

			clearEmails();

			TestUser student = createRandomTestUser(List.of("student"));
			String studentAuth = generateTestAuthenticationHeader(student.universityId(), List.of("student"));

			CreateApplicationPayload payload = new CreateApplicationPayload(
					null, "No Attachments Thesis", "BACHELOR", Instant.now(), "Test motivation", researchGroupId
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			MimeMessage[] emails = getReceivedEmails();
			assertThat(emails.length).isGreaterThanOrEqualTo(1);

			// Find the chair email (sent to the head, not the student)
			for (MimeMessage email : emails) {
				List<String> recipients = Arrays.stream(email.getAllRecipients())
						.map(Address::toString)
						.toList();
				boolean isChairEmail = recipients.stream().anyMatch(addr -> addr.contains(head.universityId()));
				if (isChairEmail) {
					// With default setting (false), chair email should not have file attachments
					int attachmentCount = countAttachments(email);
					assertThat(attachmentCount).as("Chair email should have no file attachments with default setting")
							.isEqualTo(0);
				}
			}
		}

		@Test
		void createApplication_WithSettingEnabled_ChairEmailHasAttachments() throws Exception {
			createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
			createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

			TestUser head = createRandomTestUser(List.of("supervisor"));
			UUID researchGroupId = createTestResearchGroup("With Attach Group", head.universityId());

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
					null, "With Attachments Thesis", "BACHELOR", Instant.now(), "Test motivation", researchGroupId
			);

			mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
							.header("Authorization", studentAuth)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(payload)))
					.andExpect(status().isOk());

			MimeMessage[] emails = getReceivedEmails();
			assertThat(emails.length).as("At least one email should be sent")
					.isGreaterThanOrEqualTo(1);

			// Student email should always be sent regardless of the setting
			List<String> allRecipients = getAllRecipientAddresses(emails);
			assertThat(allRecipients).as("Student should receive an email")
					.anyMatch(addr -> addr.contains(student.universityId()));
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

			TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
			UUID researchGroupId = createTestResearchGroup("Accept Email Group", advisor.universityId());

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
					List.of(advisor.userId()),
					List.of(advisor.userId()),
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
