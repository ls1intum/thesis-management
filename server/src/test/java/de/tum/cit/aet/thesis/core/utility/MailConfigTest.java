package de.tum.cit.aet.thesis.core.utility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.core.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import java.util.List;
import java.util.Set;
import java.util.UUID;

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

	@Test
	void getChairMembers_delegatesToUserRepository() throws Exception {
		UserRepository repo = mock(UserRepository.class);
		MailConfig config = new MailConfig(
				true,
				new InternetAddress("noreply@example.com"),
				"https://x",
				repo,
				mock(TemplateEngine.class)
		);
		UUID rg = UUID.randomUUID();
		List<User> expected = List.of(new User());
		when(repo.getRoleMembers(eq(Set.of("admin", "supervisor", "advisor")), eq(rg))).thenReturn(expected);

		assertThat(config.getChairMembers(rg)).isSameAs(expected);
	}

	@Test
	void getChairStudents_delegatesToUserRepository() throws Exception {
		UserRepository repo = mock(UserRepository.class);
		MailConfig config = new MailConfig(
				false,
				new InternetAddress("noreply@example.com"),
				"https://x",
				repo,
				mock(TemplateEngine.class)
		);
		UUID rg = UUID.randomUUID();
		List<User> expected = List.of(new User(), new User());
		when(repo.getRoleMembers(eq(Set.of("student")), eq(rg))).thenReturn(expected);

		assertThat(config.getChairStudents(rg)).isSameAs(expected);
	}

	@Test
	void isEnabled_returnsConfiguredFlag() throws Exception {
		assertThat(newConfig("https://x").isEnabled()).isFalse();
		MailConfig enabled = new MailConfig(true, new InternetAddress("a@b"), "h", mock(UserRepository.class), mock(TemplateEngine.class));
		assertThat(enabled.isEnabled()).isTrue();
	}

	@Test
	void getConfigDto_usesClientHost_orEmptyWhenNull() throws Exception {
		assertThat(newConfig("https://thesis.example.com/").getConfigDto().clientHost())
				.isEqualTo("https://thesis.example.com");
		assertThat(newConfig(null).getConfigDto().clientHost()).isEqualTo("");
	}

	@Test
	void senderAndTemplateEngine_areExposed() throws Exception {
		TemplateEngine engine = mock(TemplateEngine.class);
		MailConfig config = new MailConfig(
				false,
				new InternetAddress("noreply@example.com"),
				"https://x",
				mock(UserRepository.class),
				engine
		);
		assertThat(config.getSender().getAddress()).isEqualTo("noreply@example.com");
		assertThat(config.getTemplateEngine()).isSameAs(engine);
	}
}
