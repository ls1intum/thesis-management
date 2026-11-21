package de.tum.cit.aet.thesis.core.session.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(UserSessionProperties.class)
public class UserSessionConfig {
}
