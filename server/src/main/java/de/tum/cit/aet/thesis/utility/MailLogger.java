package de.tum.cit.aet.thesis.utility;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/** Utility class for extracting human-readable text representations from MIME email messages. */
public class MailLogger {
	/**
	 * Extracts the subject, sender, recipients, and body content from a MimeMessage as a formatted string.
	 *
	 * @param message the MIME message to extract text from
	 * @return the formatted string representation of the message
	 */
	public static String getTextFromMimeMessage(MimeMessage message) {
		StringBuilder builder = new StringBuilder();

		try {
			builder.append("Subject: ").append(message.getSubject()).append("\n");
			builder.append("From: ").append(String.join(",", Arrays.stream(message.getFrom()).map(Address::toString).toList())).append("\n");
			builder.append("To: ").append(String.join(",", Arrays.stream(
					Objects.requireNonNullElse(message.getRecipients(Message.RecipientType.TO), new InternetAddress[0])
			).map(Address::toString).toList())).append("\n");
			builder.append("CC: ").append(String.join(",", Arrays.stream(
					Objects.requireNonNullElse(message.getRecipients(Message.RecipientType.CC), new InternetAddress[0])
			).map(Address::toString).toList())).append("\n");
			builder.append("BCC: ").append(String.join(",", Arrays.stream(
					Objects.requireNonNullElse(message.getRecipients(Message.RecipientType.BCC), new InternetAddress[0])
			).map(Address::toString).toList())).append("\n");

			Object content = message.getContent();
			if (content instanceof String) {
				builder.append("Content: ").append(content).append("\n");
			} else if (content instanceof MimeMultipart mimeMultipart) {
				builder.append("Content: ").append(getTextFromMimeMultipart(mimeMultipart)).append("\n");
			}
		} catch (MessagingException | IOException ignored) { }

		return builder.toString();
	}

	private static String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
		StringBuilder result = new StringBuilder();
		int count = mimeMultipart.getCount();

		for (int i = 0; i < count; i++) {
			var bodyPart = mimeMultipart.getBodyPart(i);

			if (bodyPart.isMimeType("text/plain")) {
				result.append(bodyPart.getContent());
			} else if (bodyPart.isMimeType("text/html")) {
				result.append(bodyPart.getContent());
			} else if (bodyPart.getContent() instanceof MimeMultipart) {
				result.append(getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent()));
			}
		}

		return result.toString();
	}
}
