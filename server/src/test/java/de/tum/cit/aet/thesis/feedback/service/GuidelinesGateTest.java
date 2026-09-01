package de.tum.cit.aet.thesis.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.core.exception.request.AccessDeniedException;
import de.tum.cit.aet.thesis.core.group.entity.ResearchGroup;
import de.tum.cit.aet.thesis.feedback.entity.GuidelinesStatus;
import de.tum.cit.aet.thesis.feedback.entity.ResearchGroupGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.CategoryGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.repository.ResearchGroupGuidelinesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class GuidelinesGateTest {

	@Mock
	private ResearchGroupGuidelinesRepository guidelinesRepository;

	private GuidelinesGate gate;

	private final UUID researchGroupId = UUID.randomUUID();
	private ResearchGroup researchGroup;

	@BeforeEach
	void setUp() {
		gate = new GuidelinesGate(guidelinesRepository);
		researchGroup = new ResearchGroup();
		researchGroup.setId(researchGroupId);
	}

	@Test
	void rejectsAThesisWithoutAResearchGroup() {
		assertThatThrownBy(() -> gate.requireReady(null))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessageContaining("not assigned to a research group");
	}

	@Test
	void rejectsAGroupThatHasNoGuidelines() {
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> gate.requireReady(researchGroup))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessageContaining("not set up for your research group yet")
				.hasMessageContaining("research group lead");
	}

	@Test
	void rejectsGuidelinesThatPreprocessingRejected() {
		ResearchGroupGuidelines failed = new ResearchGroupGuidelines();
		failed.setResearchGroupId(researchGroupId);
		failed.setStatus(GuidelinesStatus.FAILED);
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(failed));

		assertThatThrownBy(() -> gate.requireReady(researchGroup))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessageContaining("could not be turned into review rules")
				.hasMessageContaining("research group lead");
	}

	@Test
	void rejectsAReadyRecordWithoutStructuredRules() {
		// Status and payload are separate columns, so a half-written row must not open the gate.
		ResearchGroupGuidelines ready = new ResearchGroupGuidelines();
		ready.setResearchGroupId(researchGroupId);
		ready.setStatus(GuidelinesStatus.READY);
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(ready));

		assertThatThrownBy(() -> gate.requireReady(researchGroup))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessageContaining("could not be turned into review rules");
	}

	@Test
	void returnsTheStructuredGuidelinesWhenReady() {
		StructuredGuidelines structured = new StructuredGuidelines(
				"Overview.", List.of(new CategoryGuidelines("bibliography", List.of("Cite at least 6 sources."))));
		ResearchGroupGuidelines ready = new ResearchGroupGuidelines();
		ready.setResearchGroupId(researchGroupId);
		ready.setStatus(GuidelinesStatus.READY);
		ready.setStructuredGuidelines(structured);
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(ready));

		assertThat(gate.requireReady(researchGroup)).isSameAs(structured);
	}
}
