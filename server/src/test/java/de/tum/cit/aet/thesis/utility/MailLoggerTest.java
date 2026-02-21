package de.tum.cit.aet.thesis.utility;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.util.Properties;

class MailLoggerTest {

	private MimeMessage createMimeMessage() {
		Session session = Session.getDefaultInstance(new Properties());
		return new MimeMessage(session);
	}

	@Nested
	class GetTextFromMimeMessage {
		@Test
		void returnsFormattedString_WithAllFields() throws Exception {
			MimeMessage message = createMimeMessage();
			message.setSubject("Test Subject");
			message.setFrom(new InternetAddress("from@test.com"));
			message.setRecipient(Message.RecipientType.TO, new InternetAddress("to@test.com"));
			message.addRecipient(Message.RecipientType.CC, new InternetAddress("cc@test.com"));
			message.addRecipient(Message.RecipientType.BCC, new InternetAddress("bcc@test.com"));
			message.setText("Plain text body");

			String result = MailLogger.getTextFromMimeMessage(message);

			assertThat(result).contains("Subject: Test Subject");
			assertThat(result).contains("From: from@test.com");
			assertThat(result).contains("To: to@test.com");
			assertThat(result).contains("CC: cc@test.com");
			assertThat(result).contains("BCC: bcc@test.com");
			assertThat(result).contains("Content: Plain text body");
		}

		@Test
		void handlesNullRecipients() throws Exception {
			MimeMessage message = createMimeMessage();
			message.setSubject("No Recipients");
			message.setFrom(new InternetAddress("from@test.com"));
			message.setText("Body");

			String result = MailLogger.getTextFromMimeMessage(message);

			assertThat(result).contains("Subject: No Recipients");
			assertThat(result).contains("To: ");
			assertThat(result).contains("CC: ");
			assertThat(result).contains("BCC: ");
		}

		@Test
		void handlesMultipartContent() throws Exception {
			MimeMessage message = createMimeMessage();
			message.setSubject("Multipart");
			message.setFrom(new InternetAddress("from@test.com"));

			MimeMultipart multipart = new MimeMultipart();
			MimeBodyPart textPart = new MimeBodyPart();
			textPart.setText("Plain text content", "utf-8", "plain");
			multipart.addBodyPart(textPart);

			MimeBodyPart htmlPart = new MimeBodyPart();
			htmlPart.setText("<p>HTML content</p>", "utf-8", "html");
			multipart.addBodyPart(htmlPart);

			message.setContent(multipart);

			String result = MailLogger.getTextFromMimeMessage(message);

			assertThat(result).contains("Subject: Multipart");
			assertThat(result).contains("Content: ");
			assertThat(result).contains("Plain text content");
			assertThat(result).contains("<p>HTML content</p>");
		}

		@Test
		void handlesNestedMultipart() throws Exception {
			MimeMessage message = createMimeMessage();
			message.setSubject("Nested");
			message.setFrom(new InternetAddress("from@test.com"));

			MimeMultipart innerMultipart = new MimeMultipart();
			MimeBodyPart innerText = new MimeBodyPart();
			innerText.setText("Nested text", "utf-8", "plain");
			innerMultipart.addBodyPart(innerText);

			MimeBodyPart wrapperPart = new MimeBodyPart();
			wrapperPart.setContent(innerMultipart, innerMultipart.getContentType());

			MimeMultipart outerMultipart = new MimeMultipart();
			outerMultipart.addBodyPart(wrapperPart);

			message.setContent(outerMultipart);
			message.saveChanges();

			String result = MailLogger.getTextFromMimeMessage(message);

			assertThat(result).contains("Nested text");
		}
	}
}
