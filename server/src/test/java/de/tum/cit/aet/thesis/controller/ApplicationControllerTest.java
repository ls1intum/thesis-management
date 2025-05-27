package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.constants.ApplicationState;
import de.tum.cit.aet.thesis.controller.payload.AcceptApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.CreateApplicationPayload;
import de.tum.cit.aet.thesis.controller.payload.UpdateApplicationCommentPayload;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.repository.EmailTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
class ApplicationControllerTest extends BaseIntegrationTest {

    @DynamicPropertySource
    static void configureDynamicProperties(DynamicPropertyRegistry registry) {
        configureProperties(registry);
    }

    @Autowired
    private EmailTemplateRepository emailTemplateRepository;

    @BeforeEach
    void cleanUpTemplates() {
        emailTemplateRepository.deleteAll();
    }

    @Test
    void createApplication_Success() throws Exception {
        CreateApplicationPayload payload = new CreateApplicationPayload(
                null,
                "Test Thesis",
                "MASTER",
                Instant.now(),
                "Test motivation",
                createDefaultResearchGroup()
        );
        createTestEmailTemplate("APPLICATION_CREATED_CHAIR");
        createTestEmailTemplate("APPLICATION_CREATED_STUDENT");

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/applications")
                        .header("Authorization", createRandomAdminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                )
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.thesisTitle").value("Test Thesis"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.thesisType").value("MASTER"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.motivation").value("Test motivation"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.state").value(ApplicationState.NOT_ASSESSED.getValue()));
    }

    @Test
    void updateApplication_Success() throws Exception {
        String authorization = createRandomAdminAuthentication();
        UUID applicationId = createTestApplication(authorization, "Application");
        CreateApplicationPayload updatePayload = new CreateApplicationPayload(
                null,
                "Updated Thesis",
                "BACHELOR",
                Instant.now(),
                "Updated motivation",
                createDefaultResearchGroup()
        );

        mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/" + applicationId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload))
                )
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.thesisTitle").value("Updated Thesis"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.thesisType").value("BACHELOR"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.motivation").value("Updated motivation"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.state").value(ApplicationState.NOT_ASSESSED.getValue()));
    }

    @Test
    void updateApplicationComment_Success() throws Exception {
        UUID applicationId = createTestApplication(createRandomAdminAuthentication(), "Application");

        UpdateApplicationCommentPayload payload = new UpdateApplicationCommentPayload("Test comment");

        mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/" + applicationId + "/comment")
                        .header("Authorization", createRandomAdminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                )
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.comment").value("Test comment"));
    }

    @Test
    void acceptApplication_Success() throws Exception {
        UUID applicationId = createTestApplication(createRandomAdminAuthentication(), "Application");
        TestUser advisor = createTestUser("advisor", List.of("advisor"));
        TestUser supervisor = createTestUser("supervisor", List.of("supervisor"));
        createTestEmailTemplate("APPLICATION_ACCEPTED");

        AcceptApplicationPayload payload = new AcceptApplicationPayload(
                "Final Thesis Title",
                "MASTER",
                "ENGLISH",
                List.of(advisor.userId()),
                List.of(supervisor.userId()),
                true,
                true
        );

        mockMvc.perform(MockMvcRequestBuilders.put("/v2/applications/" + applicationId + "/accept")
                        .header("Authorization", createRandomAdminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].state").value(ApplicationState.ACCEPTED.getValue()));
    }
}
