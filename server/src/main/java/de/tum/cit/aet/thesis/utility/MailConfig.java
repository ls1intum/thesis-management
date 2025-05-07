package de.tum.cit.aet.thesis.utility;

import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.repository.UserRepository;
import jakarta.mail.internet.InternetAddress;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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

    @Autowired
    public MailConfig(
            @Value("${thesis-management.mail.enabled}") boolean enabled,
            @Value("${thesis-management.mail.sender}") InternetAddress sender,
            @Value("${thesis-management.mail.bcc-recipients}") String bccRecipientsList,
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

    public boolean isEnabled() {
        return enabled;
    }

    public List<User> getChairMembers(UUID researchGroupId) {
        return userRepository.getRoleMembers(Set.of("admin", "supervisor", "advisor"), researchGroupId);
    }

    public List<User> getChairStudents(UUID researchGroupId) {
        return userRepository.getRoleMembers(Set.of("student"), researchGroupId);
    }

    public record MailConfigDto(
            String signature,
            String workspaceUrl,
            String clientHost
    ) {}

    public MailConfigDto getConfigDto() {
        return new MailConfigDto(
                Objects.requireNonNullElse(signature, ""),
                Objects.requireNonNullElse(workspaceUrl, ""),
                Objects.requireNonNullElse(getClientHost(), "")
        );
    }
}
