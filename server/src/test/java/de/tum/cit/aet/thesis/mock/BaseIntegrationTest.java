package de.tum.cit.aet.thesis.mock;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import de.tum.cit.aet.thesis.controller.payload.CreateApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateThesisPayload;
import de.tum.cit.aet.thesis.controller.payload.ReplaceTopicPayload;
import de.tum.cit.aet.thesis.repository.*;
import de.tum.cit.aet.thesis.service.AccessManagementService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.Instant;
import java.util.*;

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

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private AccessManagementService accessManagementService;

    @Container
    protected static PostgreSQLContainer<?> dbContainer = new PostgreSQLContainer<>("postgres:17.5-alpine");

    protected static void configureProperties(DynamicPropertyRegistry registry) {
        dbContainer.start();

        registry.add("spring.datasource.url", dbContainer::getJdbcUrl);
        registry.add("spring.datasource.username", dbContainer::getUsername);
        registry.add("spring.datasource.password", dbContainer::getPassword);
    }

    @BeforeAll
    protected static void startDatabase() {
        dbContainer.start();
    }

    @AfterAll
    protected static void stopDatabase() {
        dbContainer.stop();
    }

    @BeforeEach
    void deleteDatabase() {
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
                createDefaultResearchGroup()
        );

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
        TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
        UUID researchGroupId = createTestResearchGroup("Test Research Group " + UUID.randomUUID(), advisor.universityId());

        ReplaceTopicPayload payload = new ReplaceTopicPayload(
                title,
                Set.of("MASTER"),
                "Test Problem Statement",
                "Test Requirements",
                "Test Goals",
                "Test References",
                List.of(advisor.userId()),
                List.of(advisor.userId()),
                researchGroupId,
                null,
                null
        );

        String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/topics")
                        .header("Authorization", createRandomAdminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("topicId").asText().transform(UUID::fromString);
    }

    protected UUID createTestThesis(String title) throws Exception {
        TestUser advisor = createTestUser("supervisor", List.of("supervisor", "advisor"));
        UUID researchGroupId = createTestResearchGroup("Test Research Group", advisor.universityId());
        createTestEmailTemplate("THESIS_CREATED");

        CreateThesisPayload payload = new CreateThesisPayload(
                title,
                "MASTER",
                "ENGLISH",
                List.of(advisor.userId()),
                List.of(advisor.userId()),
                List.of(advisor.userId()),
                researchGroupId
        );

        String response = mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
                        .header("Authorization", createRandomAdminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("thesisId").asText().transform(UUID::fromString);
    }

    protected void createTestEmailTemplate(String templateCase) throws Exception {
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
