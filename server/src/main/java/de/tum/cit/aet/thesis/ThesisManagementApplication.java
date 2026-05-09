package de.tum.cit.aet.thesis;

import de.tum.cit.aet.thesis.utility.TimeLogUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** Main Spring Boot application class for the Thesis Management system. */
@SpringBootApplication
@EnableScheduling
public class ThesisManagementApplication {
	private static final Logger log = LoggerFactory.getLogger(ThesisManagementApplication.class);

	public static final long APP_START = System.nanoTime();

	/**
	 * Starts the Spring Boot application.
	 *
	 * @param args the command-line arguments
	 */
	static void main(String[] args) {
		SpringApplication app = new SpringApplication(ThesisManagementApplication.class);
		var context = app.run(args);
		Environment env = context.getEnvironment();
		var buildProperties = context.getBean(BuildProperties.class);
		var gitProperties = context.getBeanProvider(GitProperties.class).getIfAvailable();
		logApplicationStartup(env, buildProperties, gitProperties);
	}

	/**
	 * Logs application startup information, including access URLs, active profiles, version, and git commit details.
	 *
	 * @param env the Spring environment containing configuration properties
	 * @param buildProperties the build properties containing version information
	 * @param gitProperties the git properties containing commit and branch information, or null if unavailable
	 */
	private static void logApplicationStartup(Environment env, BuildProperties buildProperties, GitProperties gitProperties) {
		String protocol = "http";
		if (env.getProperty("server.ssl.key-store") != null) {
			protocol = "https";
		}
		String serverPort = env.getProperty("server.port");
		String version = buildProperties.getVersion();
		String gitCommitId = gitProperties != null ? gitProperties.getShortCommitId() : "unknown";
		String gitBranch = gitProperties != null ? gitProperties.getBranch() : "unknown";
		String contextPath = env.getProperty("server.servlet.context-path");
		if (StringUtils.isBlank(contextPath)) {
			contextPath = "/";
		}
		String hostAddress = "localhost";
		try {
			hostAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			log.warn("The host name could not be determined, using `localhost` as fallback");
		}
		log.info("""

				----------------------------------------------------------
				\t'{}' is running! Access URLs:
				\tLocal:        {}://localhost:{}{}
				\tExternal:     {}://{}:{}{}
				\tProfiles:     {}
				\tVersion:      {}
				\tGit Commit:   {}
				\tGit Branch:   {}
				\tFull startup: {}
				----------------------------------------------------------

				""", env.getProperty("spring.application.name"), protocol, serverPort, contextPath, protocol, hostAddress, serverPort, contextPath,
				env.getActiveProfiles().length == 0 ? env.getDefaultProfiles() : env.getActiveProfiles(), version, gitCommitId, gitBranch,
				TimeLogUtil.formatDurationFrom(APP_START));
	}
}
