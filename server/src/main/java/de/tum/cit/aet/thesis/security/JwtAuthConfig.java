package de.tum.cit.aet.thesis.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "thesis-management.keycloak")
public class JwtAuthConfig {
	@NotBlank
	private String clientId;
}
