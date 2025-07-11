package de.tum.cit.aet.thesis.controller;

import de.tum.cit.aet.thesis.constants.ThesisCommentType;
import de.tum.cit.aet.thesis.constants.ThesisPresentationType;
import de.tum.cit.aet.thesis.constants.ThesisPresentationVisibility;
import de.tum.cit.aet.thesis.constants.ThesisVisibility;
import de.tum.cit.aet.thesis.controller.payload.*;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import de.tum.cit.aet.thesis.repository.EmailTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
class ThesisControllerTest extends BaseIntegrationTest {

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

    @Nested
    class ThesisBasicOperations {
        @Test
        void getTheses_Success() throws Exception {
            createTestThesis("Test Thesis");

            mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses")
                            .header("Authorization", createRandomAdminAuthentication())
                            .param("fetchAll", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", isA(List.class)))
                    .andExpect(jsonPath("$.content", hasSize(equalTo(1))))
                    .andExpect(jsonPath("$.totalElements", isA(Number.class)));
        }

        @Test
        void createThesis_Success() throws Exception {
            TestUser advisor = createTestUser("supervisor", List.of("supervisor", "advisor"));
            UUID researchGroupId = createTestResearchGroup("Test Research Group", advisor.universityId());
            createTestEmailTemplate("THESIS_CREATED");

            CreateThesisPayload payload = new CreateThesisPayload(
                    "Test Thesis",
                    "MASTER",
                    "ENGLISH",
                    List.of(advisor.userId()),
                    List.of(advisor.userId()),
                    List.of(advisor.userId()),
                    researchGroupId
            );

            mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses")
                            .header("Authorization", createRandomAdminAuthentication())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Test Thesis"))
                    .andExpect(jsonPath("$.type").value("MASTER"));
        }

        @Test
        void updateThesis_Success() throws Exception {
            TestUser advisor = createRandomTestUser(List.of("supervisor", "advisor"));
            UUID researchGroupId = createTestResearchGroup("Test Research Group", advisor.universityId());
            UUID thesisId = createTestThesis("Test Thesis");

            UpdateThesisPayload payload = new UpdateThesisPayload(
                    "Updated Thesis",
                    "MASTER",
                    "ENGLISH",
                    ThesisVisibility.PUBLIC,
                    Set.of("keyword1", "keyword2"),
                    Instant.now(),
                    Instant.now().plusSeconds(3600),
                    List.of(advisor.userId()),
                    List.of(advisor.userId()),
                    List.of(advisor.userId()),
                    List.of(),
                    researchGroupId
            );

            mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}", thesisId)
                            .header("Authorization", createRandomAdminAuthentication())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Thesis"))
                    .andExpect(jsonPath("$.type").value("MASTER"));
        }
    }

    @Nested
    class ThesisProposalOperations {
        @Test
        void uploadProposal_Success() throws Exception {
            UUID thesisId = createTestThesis("Test Thesis");
            MockMultipartFile proposalFile = new MockMultipartFile(
                    "proposal",
                    "test.pdf",
                    MediaType.APPLICATION_PDF_VALUE,
                    "test content".getBytes()
            );
            createTestEmailTemplate("THESIS_PROPOSAL_UPLOADED");

            mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/proposal", thesisId)
                            .file(proposalFile)
                            .header("Authorization", createRandomAdminAuthentication()))
                    .andExpect(status().isOk());
        }

        @Test
        void acceptProposal_Success() throws Exception {
            String authorization = createRandomAdminAuthentication();
            UUID thesisId = createTestThesis("Test Thesis");
            createTestEmailTemplate("THESIS_PROPOSAL_UPLOADED");
            createTestEmailTemplate("THESIS_PROPOSAL_ACCEPTED");

            MockMultipartFile proposalFile = new MockMultipartFile(
                    "proposal",
                    "test.pdf",
                    MediaType.APPLICATION_PDF_VALUE,
                    "test content".getBytes()
            );

            mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/proposal", thesisId)
                            .file(proposalFile)
                            .header("Authorization", authorization))
                    .andExpect(status().isOk());

            mockMvc.perform(MockMvcRequestBuilders.put("/v2/theses/{thesisId}/proposal/accept", thesisId)
                            .header("Authorization", authorization))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class ThesisCommentOperations {
        @Test
        void getComments_Success() throws Exception {
            UUID thesisId = createTestThesis("Test Thesis");

            mockMvc.perform(MockMvcRequestBuilders.get("/v2/theses/{thesisId}/comments", thesisId)
                            .header("Authorization", createRandomAdminAuthentication())
                            .param("commentType", ThesisCommentType.THESIS.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", isA(List.class)));
        }

        @Test
        void createComment_WithFile_Success() throws Exception {
            UUID thesisId = createTestThesis("Test Thesis");
            createTestEmailTemplate("THESIS_COMMENT_POSTED");

            PostThesisCommentPayload payload = new PostThesisCommentPayload(
                    "Test comment",
                    ThesisCommentType.THESIS
            );

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.pdf",
                    MediaType.APPLICATION_PDF_VALUE,
                    "test content".getBytes()
            );

            MockMultipartFile jsonFile = new MockMultipartFile(
                    "data",
                    "",
                    MediaType.APPLICATION_JSON_VALUE,
                    objectMapper.writeValueAsString(payload).getBytes()
            );

            mockMvc.perform(MockMvcRequestBuilders.multipart("/v2/theses/{thesisId}/comments", thesisId)
                            .file(file)
                            .file(jsonFile)
                            .header("Authorization", createRandomAdminAuthentication()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class ThesisAssessmentAndGrading {
        @Test
        void createAssessment_Success() throws Exception {
            UUID thesisId = createTestThesis("Test Thesis");
            createTestEmailTemplate("THESIS_ASSESSMENT_ADDED");

            CreateAssessmentPayload payload = new CreateAssessmentPayload(
                    "Test summary",
                    "Test positives",
                    "Test negatives",
                    "1.0"
            );

            mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/assessment", thesisId)
                            .header("Authorization", createRandomAdminAuthentication())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());
        }

        @Test
        void addGrade_Success() throws Exception {
            UUID thesisId = createTestThesis("Test Thesis");
            createTestEmailTemplate("THESIS_FINAL_GRADE");

            AddThesisGradePayload payload = new AddThesisGradePayload(
                    "1.0",
                    "Great work",
                    ThesisVisibility.PUBLIC
            );

            mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/grade", thesisId)
                            .header("Authorization", createRandomAdminAuthentication())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class ThesisPresentation {
        @Test
        void createPresentation_Success() throws Exception {
            UUID thesisId = createTestThesis("Test Thesis");

            ReplacePresentationPayload payload = new ReplacePresentationPayload(
                    ThesisPresentationType.INTERMEDIATE,
                    ThesisPresentationVisibility.PUBLIC,
                    "Room 101",
                    "http://stream.url",
                    "English",
                    Instant.now().plusSeconds(3600)
            );

            mockMvc.perform(MockMvcRequestBuilders.post("/v2/theses/{thesisId}/presentations", thesisId)
                            .header("Authorization", createRandomAdminAuthentication())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());
        }
    }
}
