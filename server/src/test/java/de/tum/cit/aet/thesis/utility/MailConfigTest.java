package de.tum.cit.aet.thesis.utility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import de.tum.cit.aet.thesis.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

class MailConfigTest {

	private static MailConfig newConfig(String clientHost) throws AddressException {
		return new MailConfig(
				false,
				new InternetAddress("noreply@example.com"),
				clientHost,
				mock(UserRepository.class),
				mock(TemplateEngine.class)
		);
	}

	@Test
	void clientHost_TrailingSlashStripped() throws Exception {
		MailConfig config = newConfig("https://thesis.example.com/");

		assertThat(config.getClientHost()).isEqualTo("https://thesis.example.com");
	}

	@Test
	void clientHost_MultipleTrailingSlashesStripped() throws Exception {
		MailConfig config = newConfig("https://thesis.example.com///");

		assertThat(config.getClientHost()).isEqualTo("https://thesis.example.com");
	}

	@Test
	void clientHost_NoTrailingSlash_LeftUnchanged() throws Exception {
		MailConfig config = newConfig("https://thesis.example.com");

		assertThat(config.getClientHost()).isEqualTo("https://thesis.example.com");
	}

	@Test
	void clientHost_PathOnly_TrailingSlashStripped() throws Exception {
		MailConfig config = newConfig("https://example.com/app/");

		assertThat(config.getClientHost()).isEqualTo("https://example.com/app");
	}

	@Test
	void clientHost_Null_RemainsNull() throws Exception {
		MailConfig config = newConfig(null);

		assertThat(config.getClientHost()).isNull();
	}

	@Test
	void interviewInviteUrl_NoDoubleSlash() throws Exception {
		MailConfig config = newConfig("https://thesis.example.com/");

		// This is the exact pattern used in MailBuilder.fillPlaceholder("inviteUrl", ...).
		String inviteUrl = config.getClientHost() + "/interview_booking/abc";

		assertThat(inviteUrl)
				.isEqualTo("https://thesis.example.com/interview_booking/abc")
				.doesNotContain("//interview_booking");
	}
}
