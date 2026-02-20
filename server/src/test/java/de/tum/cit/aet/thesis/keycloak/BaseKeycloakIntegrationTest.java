package de.tum.cit.aet.thesis.keycloak;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.tum.cit.aet.thesis.repository.ApplicationRepository;
import de.tum.cit.aet.thesis.repository.ApplicationReviewerRepository;
import de.tum.cit.aet.thesis.repository.NotificationSettingRepository;
import de.tum.cit.aet.thesis.repository.ThesisAssessmentRepository;
import de.tum.cit.aet.thesis.repository.ThesisCommentRepository;
import de.tum.cit.aet.thesis.repository.ThesisFeedbackRepository;
import de.tum.cit.aet.thesis.repository.ThesisFileRepository;
import de.tum.cit.aet.thesis.repository.ThesisPresentationInviteRepository;
import de.tum.cit.aet.thesis.repository.ThesisPresentationRepository;
import de.tum.cit.aet.thesis.repository.ThesisProposalRepository;
import de.tum.cit.aet.thesis.repository.ThesisRepository;
import de.tum.cit.aet.thesis.repository.ThesisRoleRepository;
import de.tum.cit.aet.thesis.repository.ThesisStateChangeRepository;
import de.tum.cit.aet.thesis.repository.TopicRepository;
import de.tum.cit.aet.thesis.repository.TopicRoleRepository;
import de.tum.cit.aet.thesis.repository.UserGroupRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("keycloak-test")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseKeycloakIntegrationTest {

	static final PostgreSQLContainer DB_CONTAINER = new PostgreSQLContainer("postgres:17.8-alpine");

	static final KeycloakContainer KEYCLOAK_CONTAINER = new KeycloakContainer("quay.io/keycloak/keycloak:26.4")
			.withRealmImportFile("thesis-management-realm.json");

	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	static String serviceClientSecret;

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	private ApplicationRepository applicationRepository;

	@Autowired
	private ApplicationReviewerRepository applicationReviewerRepository;

	@Autowired
	private NotificationSettingRepository notificationSettingRepository;

	@Autowired
	private ThesisAssessmentRepository thesisAssessmentRepository;

	@Autowired
	private ThesisCommentRepository thesisCommentRepository;

	@Autowired
	private ThesisFeedbackRepository thesisFeedbackRepository;

	@Autowired
	private ThesisFileRepository thesisFileRepository;

	@Autowired
	private ThesisPresentationInviteRepository thesisPresentationInviteRepository;

	@Autowired
	private ThesisPresentationRepository thesisPresentationRepository;

	@Autowired
	private ThesisProposalRepository thesisProposalRepository;

	@Autowired
	private ThesisRepository thesisRepository;

	@Autowired
	private ThesisRoleRepository thesisRoleRepository;

	@Autowired
	private ThesisStateChangeRepository thesisStateChangeRepository;

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private TopicRoleRepository topicRoleRepository;

	@Autowired
	private UserGroupRepository userGroupRepository;

	@Autowired
	private UserRepository userRepository;

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		DB_CONTAINER.start();
		KEYCLOAK_CONTAINER.start();

		// Only generate the secret once per JVM — generating a new secret would
		// invalidate the previous one, breaking any already-initialized
		// AccessManagementService instances in cached Spring contexts.
		if (serviceClientSecret == null) {
			Keycloak adminClient = KEYCLOAK_CONTAINER.getKeycloakAdminClient();
			String serviceClientUuid = adminClient.realm("thesis-management")
					.clients().findByClientId("thesis-management-service-client").get(0).getId();
			CredentialRepresentation cred = adminClient.realm("thesis-management")
					.clients().get(serviceClientUuid).generateNewSecret();
			serviceClientSecret = cred.getValue();
		}

		registry.add("spring.datasource.url", DB_CONTAINER::getJdbcUrl);
		registry.add("spring.datasource.username", DB_CONTAINER::getUsername);
		registry.add("spring.datasource.password", DB_CONTAINER::getPassword);

		String issuerUri = KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/thesis-management";
		registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> issuerUri);
		registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
				() -> issuerUri + "/protocol/openid-connect/certs");

		registry.add("thesis-management.keycloak.host", KEYCLOAK_CONTAINER::getAuthServerUrl);
		registry.add("thesis-management.keycloak.service-client.secret", () -> serviceClientSecret);
	}

	@BeforeEach
	void cleanDatabase() {
		thesisCommentRepository.deleteAll();
		thesisFeedbackRepository.deleteAll();
		thesisFileRepository.deleteAll();
		thesisPresentationInviteRepository.deleteAll();
		thesisAssessmentRepository.deleteAll();
		notificationSettingRepository.deleteAll();
		applicationReviewerRepository.deleteAll();
		thesisProposalRepository.deleteAll();

		thesisPresentationRepository.deleteAll();
		thesisStateChangeRepository.deleteAll();
		thesisRoleRepository.deleteAll();
		topicRoleRepository.deleteAll();

		thesisRepository.deleteAll();
		applicationRepository.deleteAll();
		topicRepository.deleteAll();
		userGroupRepository.deleteAll();

		userRepository.deleteAll();
	}

	protected String obtainAccessToken(String username, String password) {
		try {
			String tokenUrl = KEYCLOAK_CONTAINER.getAuthServerUrl()
					+ "/realms/thesis-management/protocol/openid-connect/token";
			String body = "grant_type=password"
					+ "&client_id=thesis-management-app"
					+ "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
					+ "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(tokenUrl))
					.header("Content-Type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString(body))
					.build();

			HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				throw new RuntimeException("Token request failed with status " + response.statusCode()
						+ ": " + response.body());
			}

			JsonNode json = objectMapper.readTree(response.body());
			JsonNode tokenNode = json.get("access_token");
			if (tokenNode == null || tokenNode.isNull()) {
				throw new RuntimeException("No access_token in response: " + response.body());
			}
			return tokenNode.asText();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to obtain access token for user: " + username, e);
		}
	}

	/** All realm test users use their username as their password. */
	protected String authHeaderFor(String username) {
		return "Bearer " + obtainAccessToken(username, username);
	}
}
