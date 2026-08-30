package de.tum.cit.aet.thesis.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.core.exception.request.AccessDeniedException;
import de.tum.cit.aet.thesis.core.group.entity.ResearchGroup;
import de.tum.cit.aet.thesis.feedback.dto.AIPreviewResponseDTO;
import de.tum.cit.aet.thesis.feedback.dto.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.dto.ReviewResultDTO;
import de.tum.cit.aet.thesis.feedback.entity.GuidelinesStatus;
import de.tum.cit.aet.thesis.feedback.entity.ResearchGroupGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.CategoryGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.repository.ResearchGroupGuidelinesRepository;
import de.tum.cit.aet.thesis.feedback.service.reviewer.ReviewType;
import de.tum.cit.aet.thesis.proposal.entity.ThesisProposal;
import de.tum.cit.aet.thesis.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.thesis.service.ThesisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class AIFeedbackServiceTest {

	@Mock
	private ReviewService reviewService;

	@Mock
	private ThesisService thesisService;

	@Mock
	private ResearchGroupGuidelinesRepository guidelinesRepository;

	@Mock
	private Thesis thesis;

	private AIFeedbackService service;

	private final UUID researchGroupId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new AIFeedbackService(reviewService, thesisService, guidelinesRepository);

		ResearchGroup researchGroup = new ResearchGroup();
		researchGroup.setId(researchGroupId);
		when(thesis.getResearchGroup()).thenReturn(researchGroup);
	}

	private ResearchGroupGuidelines readyGuidelines() {
		ResearchGroupGuidelines guidelines = new ResearchGroupGuidelines();
		guidelines.setResearchGroupId(researchGroupId);
		guidelines.setStatus(GuidelinesStatus.READY);
		guidelines.setStructuredGuidelines(new StructuredGuidelines(
				"Overview.",
				List.of(new CategoryGuidelines("bibliography", List.of("Cite at least 6 sources.")))));
		return guidelines;
	}

	@Test
	void previewReview_throwsWhenGroupHasNoGuidelines() {
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.previewReview(thesis, ReviewType.PROPOSAL))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessageContaining("not set up for your research group yet")
				.hasMessageContaining("research group lead");

		verify(reviewService, never()).review(any(), any(), any());
	}

	@Test
	void autoReviewAndSave_throwsWhenGuidelinesNotReady() {
		ResearchGroupGuidelines failed = new ResearchGroupGuidelines();
		failed.setResearchGroupId(researchGroupId);
		failed.setStatus(GuidelinesStatus.FAILED);
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(failed));

		assertThatThrownBy(() -> service.autoReviewAndSave(thesis, ReviewType.PROPOSAL))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessageContaining("could not be turned into review rules")
				.hasMessageContaining("research group lead");

		verify(reviewService, never()).review(any(), any(), any());
	}

	@Test
	void previewReview_passesGroupGuidelinesToReviewWhenReady() {
		ResearchGroupGuidelines guidelines = readyGuidelines();
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(guidelines));

		ThesisProposal proposal = org.mockito.Mockito.mock(ThesisProposal.class);
		when(thesis.getProposals()).thenReturn(List.of(proposal));
		Resource pdfResource = new ByteArrayResource("pdf".getBytes());
		when(thesisService.getProposalFile(proposal)).thenReturn(pdfResource);

		when(reviewService.review(eq(pdfResource), eq(ReviewType.PROPOSAL), eq(guidelines.getStructuredGuidelines())))
				.thenReturn(new ReviewResultDTO(AssessmentCategory.GOOD, "All good.", List.of()));

		AIPreviewResponseDTO response = service.previewReview(thesis, ReviewType.PROPOSAL);

		assertThat(response.summary()).isEqualTo("All good.");
		verify(reviewService).review(pdfResource, ReviewType.PROPOSAL, guidelines.getStructuredGuidelines());
	}
}
