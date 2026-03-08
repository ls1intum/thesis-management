package de.tum.cit.aet.thesis.mock;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.jayway.jsonpath.JsonPath;
import de.tum.cit.aet.thesis.controller.payload.CreateApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateThesisPayload;
import de.tum.cit.aet.thesis.controller.payload.ReplaceTopicPayload;
import de.tum.cit.aet.thesis.repository.ApplicationRepository;
import de.tum.cit.aet.thesis.repository.ApplicationReviewerRepository;
import de.tum.cit.aet.thesis.repository.DataExportRepository;
import de.tum.cit.aet.thesis.repository.EmailTemplateRepository;
import de.tum.cit.aet.thesis.repository.InterviewProcessRepository;
import de.tum.cit.aet.thesis.repository.IntervieweeRepository;
import de.tum.cit.aet.thesis.repository.NotificationSettingRepository;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.repository.ResearchGroupSettingsRepository;
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
import de.tum.cit.aet.thesis.service.AccessManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import jakarta.mail.internet.MimeMessage;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TestSecurityConfig.class)
public abstract class BaseIntegrationTest {

	@Autowired
	private ApplicationRepository applicationRepository;

	@Autowired
	private ApplicationReviewerRepository applicationReviewerRepository;

	@Autowired
	private DataExportRepository dataExportRepository;

	@Autowired
	private IntervieweeRepository intervieweeRepository;

	@Autowired
	private InterviewProcessRepository interviewProcessRepository;

	@Autowired
	private EmailTemplateRepository emailTemplateRepository;

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
	private ResearchGroupRepository researchGroupRepository;

	@Autowired
	private ResearchGroupSettingsRepository researchGroupSettingsRepository;

	@Autowired
	private UserGroupRepository userGroupRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	private AccessManagementService accessManagementService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	protected static PostgreSQLContainer dbContainer = new PostgreSQLContainer("postgres:18.2-alpine")
			.withCommand("postgres", "-c", "max_connections=200");

	protected static GreenMail greenMail;

	protected static WireMockServer wireMockServer;

	private static final String EMPTY_ICAL = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//Test//Test//EN\r\nCALSCALE:GREGORIAN\r\nEND:VCALENDAR\r\n";

	protected static void configureProperties(DynamicPropertyRegistry registry) {
		dbContainer.start();

		registry.add("spring.datasource.url", dbContainer::getJdbcUrl);
		registry.add("spring.datasource.username", dbContainer::getUsername);
		registry.add("spring.datasource.password", dbContainer::getPassword);

		if (greenMail == null) {
			greenMail = new GreenMail(new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP));
			greenMail.start();
		}

		registry.add("spring.mail.host", () -> "127.0.0.1");
		registry.add("spring.mail.port", () -> greenMail.getSmtp().getPort());

		if (wireMockServer == null) {
			wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
			wireMockServer.start();

			wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/"))
					.willReturn(WireMock.aResponse()
							.withStatus(200)
							.withHeader("Content-Type", "text/calendar")
							.withBody(EMPTY_ICAL)));

			wireMockServer.stubFor(WireMock.put(WireMock.urlEqualTo("/"))
					.willReturn(WireMock.aResponse()
							.withStatus(204)));
		}

		registry.add("thesis-management.calendar.url", () -> wireMockServer.baseUrl());
	}

	// Deletion order matters: child tables with foreign keys must be deleted before parent tables.
	@BeforeEach
	void deleteDatabase() {
		dataExportRepository.deleteAll();
		emailTemplateRepository.deleteAll();
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

		// Interview tables must be cleaned before topics/applications due to FK constraints
		intervieweeRepository.deleteAll();
		interviewProcessRepository.deleteAll();

		thesisRepository.deleteAll();
		applicationRepository.deleteAll();
		topicRepository.deleteAll();

		// Break circular FK between User and ResearchGroup before deletion
		jdbcTemplate.execute("UPDATE users SET research_group_id = NULL");
		jdbcTemplate.execute("UPDATE research_groups SET head_user_id = NULL, created_by = NULL, updated_by = NULL");

		researchGroupSettingsRepository.deleteAll();
		userGroupRepository.deleteAll();
		researchGroupRepository.deleteAll();
		userRepository.deleteAll();

		clearEmails();
	}

	protected MimeMessage[] getReceivedEmails() {
		return greenMail.getReceivedMessages();
	}

	protected void clearEmails() {
		if (greenMail != null) {
			try {
				greenMail.purgeEmailFromAllMailboxes();
			} catch (com.icegreen.greenmail.store.FolderException e) {
				throw new RuntimeException(e);
			}
		}
	}

	protected String createRandomAuthentication(String role) throws Exception {
		String universityId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

		createTestUser(universityId, List.of(role));

		return generateTestAuthenticationHeader(universityId, List.of(role));
	}

	protected String createRandomAdminAuthentication() throws Exception {
		return createRandomAuthentication("admin");
	}

	protected String generateTestAuthenticationHeader(String universityId, List<String> roles) {
		return "Bearer " + JWT.create()
				.withSubject(universityId)
				.withIssuedAt(Instant.now())
				.withExpiresAt(Instant.now().plusSeconds(3600))
				.withClaim("roles", roles)
				.withClaim("given_name", universityId)
				.withClaim("family_name", universityId)
				.withClaim("email", universityId + "@example.com")
				.sign(Algorithm.HMAC256("test-secret-key-for-jwt-tokens"));
	}

	public record TestUser(UUID userId, String universityId) {}
	protected TestUser createTestUser(String universityId, List<String> roles) throws Exception {
		String response = mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
						.header("Authorization", generateTestAuthenticationHeader(universityId, roles))
				)
				.andReturn()
				.getResponse()
				.getContentAsString();

		UUID userId = UUID.fromString(JsonPath.parse(response).read("$.userId", String.class));
		String actualUniversityId = JsonPath.parse(response).read("$.universityId", String.class);

		return new TestUser(userId, actualUniversityId);
	}

	protected TestUser createRandomTestUser(List<String> roles) throws Exception {
		String universityId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		return createTestUser(universityId, roles);
	}

	protected UUID createTestResearchGroup(String name, String headUniversityId) throws Exception {
		Map<String, Object> payload = new HashMap<>();
		payload.put("name", name + " " + UUID.randomUUID());
		payload.put("headUsername", headUniversityId);
		payload.put("abbreviation", UUID.randomUUID().toString());

		String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/research-groups")
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andReturn()
				.getResponse()
				.getContentAsString();

		return UUID.fromString(JsonPath.parse(response).read("$.id", String.class));
	}

	protected UUID createTestApplication(String authorization, String title) throws Exception {
		createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
		createTestEmailTemplate("APPLICATION_CREATED_STUDENT");
		CreateApplicationPayload payload = new CreateApplicationPayload(
				null,
				title,
				"BACHELOR",
				Instant.now(),
				"Test motivation",
				createDefaultResearchGroup(),
				true);

		String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
						.header("Authorization", authorization)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andReturn()
				.getResponse()
				.getContentAsString();

		return UUID.fromString(JsonPath.parse(response).read("$.applicationId", String.class));
	}

	protected UUID createDefaultResearchGroup() throws Exception {
		TestUser headUser = createRandomTestUser(List.of("supervisor"));
		return createTestResearchGroup("Default Research Group", headUser.universityId());
	}

	protected UUID createTestTopic(String title) throws Exception {
		TestUser staffMember = createRandomTestUser(List.of("supervisor", "advisor"));
		UUID researchGroupId = createTestResearchGroup("Test Research Group " + UUID.randomUUID(), staffMember.universityId());

		ReplaceTopicPayload payload = new ReplaceTopicPayload(
				title,
				Set.of("MASTER"),
				"Test Problem Statement",
				"Test Requirements",
				"Test Goals",
				"Test References",
				List.of(staffMember.userId()),
				List.of(staffMember.userId()),
				researchGroupId,
				null,
				null,
				false
		);

		String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/topics")
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andReturn()
				.getResponse()
				.getContentAsString();

		return objectMapper.readTree(response).get("topicId").asString().transform(UUID::fromString);
	}

	protected UUID createTestThesis(String title) throws Exception {
		TestUser staffMember = createRandomTestUser(List.of("supervisor", "advisor"));
		UUID researchGroupId = createTestResearchGroup("Test Research Group", staffMember.universityId());
		createTestEmailTemplate("THESIS_CREATED");

		CreateThesisPayload payload = new CreateThesisPayload(
				title,
				"MASTER",
				"ENGLISH",
				List.of(staffMember.userId()),
				List.of(staffMember.userId()),
				List.of(staffMember.userId()),
				researchGroupId
		);

		String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andReturn()
				.getResponse()
				.getContentAsString();

		return objectMapper.readTree(response).get("thesisId").asString().transform(UUID::fromString);
	}

	protected void createTestEmailTemplate(String templateCase) throws Exception {
		if (emailTemplateRepository.findByResearchGroupIdAndTemplateCaseAndLanguage(
				null, templateCase, "en").isPresent()) {
			return;
		}

		Map<String, Object> payload = new HashMap<>();
		payload.put("researchGroupId", null);
		payload.put("templateCase", templateCase);
		payload.put("description", "Test description");
		payload.put("subject", "Test Subject");
		payload.put("bodyHtml", "<p>Test Body</p>");
		payload.put("language", "en");

		mockMvc.perform(MockMvcRequestBuilders.post("/v2/email-templates")
						.header("Authorization", createRandomAdminAuthentication())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andReturn()
				.getResponse()
				.getContentAsString();
	}

}
