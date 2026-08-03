package de.tum.cit.aet.thesis.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.core.group.entity.ResearchGroup;
import de.tum.cit.aet.thesis.core.group.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.core.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.feedback.dto.GuidelinesPreprocessingResult;
import de.tum.cit.aet.thesis.feedback.entity.GuidelinesStatus;
import de.tum.cit.aet.thesis.feedback.entity.ResearchGroupGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.CategoryGuidelines;
import de.tum.cit.aet.thesis.feedback.repository.ResearchGroupGuidelinesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class GuidelinesServiceTest {

	@Mock
	private ResearchGroupGuidelinesRepository guidelinesRepository;

	@Mock
	private ResearchGroupRepository researchGroupRepository;

	@Mock
	private GuidelinesPreprocessor preprocessor;

	@Mock
	private ObjectProvider<CurrentUserProvider> currentUserProviderProvider;

	@Mock
	private CurrentUserProvider currentUserProvider;

	private GuidelinesService service;

	private final UUID researchGroupId = UUID.randomUUID();
	private ResearchGroup researchGroup;

	@BeforeEach
	void setUp() {
		service = new GuidelinesService(
				guidelinesRepository, researchGroupRepository, preprocessor, currentUserProviderProvider);

		researchGroup = new ResearchGroup();
		researchGroup.setId(researchGroupId);

		when(currentUserProviderProvider.getObject()).thenReturn(currentUserProvider);
		when(researchGroupRepository.findById(researchGroupId)).thenReturn(Optional.of(researchGroup));
	}

	private void stubSaveReturnsArgument() {
		when(guidelinesRepository.save(any(ResearchGroupGuidelines.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void updateGuidelines_storesReadyWhenPreprocessingSucceeds() {
		stubSaveReturnsArgument();
		when(currentUserProvider.getUser()).thenReturn(new User());
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.empty());
		when(preprocessor.preprocess(any(String.class))).thenReturn(new GuidelinesPreprocessingResult(
				true, null, "Overview.",
				List.of(new CategoryGuidelines("bibliography", List.of("Cite at least 6 sources.")))));

		ResearchGroupGuidelines saved = service.updateGuidelines(researchGroupId, "Some specific guidelines.");

		verify(currentUserProvider).assertCanAccessResearchGroup(researchGroup);
		assertThat(saved.getStatus()).isEqualTo(GuidelinesStatus.READY);
		assertThat(saved.getRawGuidelines()).isEqualTo("Some specific guidelines.");
		assertThat(saved.getStructuredGuidelines()).isNotNull();
		assertThat(saved.getStructuredGuidelines().rulesForCategory("bibliography"))
				.containsExactly("Cite at least 6 sources.");
		assertThat(saved.getFailureReason()).isNull();
		assertThat(saved.getProcessedAt()).isNotNull();
		assertThat(saved.isReady()).isTrue();
	}

	@Test
	void updateGuidelines_storesFailedWhenPreprocessingRejectsAsVague() {
		stubSaveReturnsArgument();
		when(currentUserProvider.getUser()).thenReturn(new User());
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.empty());
		when(preprocessor.preprocess(any(String.class))).thenReturn(new GuidelinesPreprocessingResult(
				false, "Too vague to build concrete rules.", null, List.of()));

		ResearchGroupGuidelines saved = service.updateGuidelines(researchGroupId, "write well");

		assertThat(saved.getStatus()).isEqualTo(GuidelinesStatus.FAILED);
		assertThat(saved.getStructuredGuidelines()).isNull();
		assertThat(saved.getFailureReason()).isEqualTo("Too vague to build concrete rules.");
		assertThat(saved.getProcessedAt()).isNull();
		assertThat(saved.isReady()).isFalse();
	}

	@Test
	void getByResearchGroupId_assertsAccessAndReturnsRepositoryResult() {
		ResearchGroupGuidelines existing = new ResearchGroupGuidelines();
		existing.setResearchGroupId(researchGroupId);
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(existing));

		Optional<ResearchGroupGuidelines> result = service.getByResearchGroupId(researchGroupId);

		verify(currentUserProvider).assertCanAccessResearchGroup(researchGroup);
		assertThat(result).containsSame(existing);
	}

	@Test
	void updateGuidelines_overwritesExistingRecord() {
		stubSaveReturnsArgument();
		ResearchGroupGuidelines existing = new ResearchGroupGuidelines();
		existing.setResearchGroupId(researchGroupId);
		existing.setStatus(GuidelinesStatus.FAILED);
		existing.setFailureReason("old reason");
		when(currentUserProvider.getUser()).thenReturn(new User());
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(existing));
		when(preprocessor.preprocess(eq("Now specific."))).thenReturn(new GuidelinesPreprocessingResult(
				true, null, "Overview.",
				List.of(new CategoryGuidelines("structure", List.of("Include an Abstract.")))));

		ResearchGroupGuidelines saved = service.updateGuidelines(researchGroupId, "Now specific.");

		assertThat(saved).isSameAs(existing);
		assertThat(saved.getStatus()).isEqualTo(GuidelinesStatus.READY);
		assertThat(saved.getFailureReason()).isNull();
	}
}
