package de.tum.cit.aet.thesis.utility;

import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.repository.UserRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;

import jakarta.mail.internet.InternetAddress;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Provides mail configuration properties and access to chair member data for email sending. */
@Component
public class MailConfig {
	private final UserRepository userRepository;

	private final Boolean enabled;

	@Getter
	private final String clientHost;

	@Getter
	private final InternetAddress sender;

	@Getter
	private final String signature;

	@Getter
	private final String workspaceUrl;

	@Getter
	private final TemplateEngine templateEngine;

	/**
	 * Injects mail-related configuration properties, the user repository, and the template engine.
	 *
	 * @param enabled whether email sending is enabled
	 * @param sender the sender email address
	 * @param mailSignature the email signature
	 * @param workspaceUrl the workspace URL
	 * @param clientHost the client host URL
	 * @param userRepository the user repository
	 * @param templateEngine the Thymeleaf template engine
	 */
	@Autowired
	public MailConfig(
			@Value("${thesis-management.mail.enabled}") boolean enabled,
			@Value("${thesis-management.mail.sender}") InternetAddress sender,
			@Value("${thesis-management.mail.signature}") String mailSignature,
			@Value("${thesis-management.mail.workspace-url}") String workspaceUrl,
			@Value("${thesis-management.client.host}") String clientHost,
			UserRepository userRepository,
			TemplateEngine templateEngine
	) {
		this.enabled = enabled;
		this.sender = sender;
		this.workspaceUrl = workspaceUrl;
		this.signature = mailSignature;
		this.clientHost = clientHost;

		this.templateEngine = templateEngine;
		this.userRepository = userRepository;
	}

	/**
	 * Returns whether email sending is enabled.
	 *
	 * @return true if email sending is enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Returns all admin, supervisor, and advisor users belonging to the given research group.
	 *
	 * @param researchGroupId the research group ID
	 * @return the list of chair members
	 */
	public List<User> getChairMembers(UUID researchGroupId) {
		return userRepository.getRoleMembers(Set.of("admin", "supervisor", "advisor"), researchGroupId);
	}

	/**
	 * Returns all student users belonging to the given research group.
	 *
	 * @param researchGroupId the research group ID
	 * @return the list of students
	 */
	public List<User> getChairStudents(UUID researchGroupId) {
		return userRepository.getRoleMembers(Set.of("student"), researchGroupId);
	}

	/**
	 * Data transfer object holding mail configuration values for use in email templates.
	 *
	 * @param signature the email signature
	 * @param workspaceUrl the workspace URL
	 * @param clientHost the client host URL
	 */
	public record MailConfigDto(
			String signature,
			String workspaceUrl,
			String clientHost
	) {}

	/**
	 * Creates a MailConfigDto with the current configuration values, substituting empty strings for null values.
	 *
	 * @return the mail configuration DTO
	 */
	public MailConfigDto getConfigDto() {
		return new MailConfigDto(
				Objects.requireNonNullElse(signature, ""),
				Objects.requireNonNullElse(workspaceUrl, ""),
				Objects.requireNonNullElse(getClientHost(), "")
		);
	}
}
