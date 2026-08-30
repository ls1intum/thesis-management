package de.tum.cit.aet.thesis.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.core.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.core.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.core.group.entity.ResearchGroup;
import de.tum.cit.aet.thesis.core.group.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.core.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.feedback.dto.GuidelinesPreprocessingResult;
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
import org.springframework.beans.factory.ObjectProvider;

import java.util.Arrays;
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
	void updateGuidelines_storesFailedWhenSpecificButNoCategories() {
		stubSaveReturnsArgument();
		when(currentUserProvider.getUser()).thenReturn(new User());
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.empty());
		when(preprocessor.preprocess(any(String.class))).thenReturn(new GuidelinesPreprocessingResult(
				true, null, "Overview.", List.of()));

		ResearchGroupGuidelines saved = service.updateGuidelines(researchGroupId, "Looks specific but empty.");

		assertThat(saved.getStatus()).isEqualTo(GuidelinesStatus.FAILED);
		assertThat(saved.getStructuredGuidelines()).isNull();
		assertThat(saved.getFailureReason()).isNotBlank();
		assertThat(saved.getProcessedAt()).isNull();
		assertThat(saved.isReady()).isFalse();
	}

	@Test
	void updateGuidelines_storesFailedWhenSpecificButOnlyUnknownSlugsOrBlankRules() {
		stubSaveReturnsArgument();
		when(currentUserProvider.getUser()).thenReturn(new User());
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.empty());
		when(preprocessor.preprocess(any(String.class))).thenReturn(new GuidelinesPreprocessingResult(
				true, null, "Overview.",
				List.of(
						new CategoryGuidelines("not-a-real-category", List.of("Some rule.")),
						new CategoryGuidelines("bibliography", List.of("   ")))));

		ResearchGroupGuidelines saved = service.updateGuidelines(researchGroupId, "Unusable output.");

		assertThat(saved.getStatus()).isEqualTo(GuidelinesStatus.FAILED);
		assertThat(saved.getStructuredGuidelines()).isNull();
		assertThat(saved.isReady()).isFalse();
	}

	@Test
	void updateGuidelines_storesReadyWhenAtLeastOneKnownCategoryHasNonblankRule() {
		stubSaveReturnsArgument();
		when(currentUserProvider.getUser()).thenReturn(new User());
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.empty());
		when(preprocessor.preprocess(any(String.class))).thenReturn(new GuidelinesPreprocessingResult(
				true, null, "Overview.",
				List.of(
						new CategoryGuidelines("not-a-real-category", List.of("Ignored.")),
						new CategoryGuidelines("structure", List.of("Include an Abstract.")))));

		ResearchGroupGuidelines saved = service.updateGuidelines(researchGroupId, "Mostly usable.");

		assertThat(saved.getStatus()).isEqualTo(GuidelinesStatus.READY);
		assertThat(saved.getStructuredGuidelines().rulesForCategory("structure"))
				.containsExactly("Include an Abstract.");
		assertThat(saved.isReady()).isTrue();
	}

	@Test
	void updateStructuredGuidelines_persistsSanitizedRulesAndKeepsReady() {
		stubSaveReturnsArgument();
		ResearchGroupGuidelines existing = new ResearchGroupGuidelines();
		existing.setResearchGroupId(researchGroupId);
		existing.setRawGuidelines("Original raw text.");
		existing.setStatus(GuidelinesStatus.READY);
		when(currentUserProvider.getUser()).thenReturn(new User());
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(existing));

		ResearchGroupGuidelines saved = service.updateStructuredGuidelines(
				researchGroupId,
				"  Edited overview.  ",
				List.of(
						new CategoryGuidelines("structure", List.of("  Include an Abstract.  ", "   ")),
						new CategoryGuidelines("not-a-real-category", List.of("Dropped.")),
						new CategoryGuidelines("figures", List.of())));

		verify(currentUserProvider).assertCanAccessResearchGroup(researchGroup);
		assertThat(saved.getStatus()).isEqualTo(GuidelinesStatus.READY);
		assertThat(saved.getRawGuidelines()).isEqualTo("Original raw text.");
		assertThat(saved.getStructuredGuidelines().overview()).isEqualTo("Edited overview.");
		// Blank rule stripped, unknown slug dropped, empty category dropped.
		assertThat(saved.getStructuredGuidelines().rulesForCategory("structure"))
				.containsExactly("Include an Abstract.");
		assertThat(saved.getStructuredGuidelines().rulesForCategory("not-a-real-category")).isEmpty();
		assertThat(saved.getStructuredGuidelines().rulesForCategory("figures")).isEmpty();
		assertThat(saved.getFailureReason()).isNull();
		assertThat(saved.getProcessedAt()).isNotNull();
	}

	@Test
	void updateStructuredGuidelines_promotesFailedRecordToReadyWhenRulesUsable() {
		stubSaveReturnsArgument();
		ResearchGroupGuidelines existing = new ResearchGroupGuidelines();
		existing.setResearchGroupId(researchGroupId);
		existing.setStatus(GuidelinesStatus.FAILED);
		existing.setFailureReason("was too vague");
		when(currentUserProvider.getUser()).thenReturn(new User());
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(existing));

		ResearchGroupGuidelines saved = service.updateStructuredGuidelines(
				researchGroupId, null,
				List.of(new CategoryGuidelines("bibliography", List.of("Cite at least 6 sources."))));

		assertThat(saved.getStatus()).isEqualTo(GuidelinesStatus.READY);
		assertThat(saved.getFailureReason()).isNull();
		assertThat(saved.isReady()).isTrue();
	}

	@Test
	void updateStructuredGuidelines_rejectsEditThatLeavesNoUsableRule() {
		ResearchGroupGuidelines existing = new ResearchGroupGuidelines();
		existing.setResearchGroupId(researchGroupId);
		existing.setStatus(GuidelinesStatus.READY);
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> service.updateStructuredGuidelines(
				researchGroupId, "Overview.",
				List.of(
						new CategoryGuidelines("structure", List.of("   ")),
						new CategoryGuidelines("not-a-real-category", List.of("Dropped.")))))
				.isInstanceOf(ResourceInvalidParametersException.class);

		verify(currentUserProvider).assertCanAccessResearchGroup(researchGroup);
		verify(guidelinesRepository, never()).save(any(ResearchGroupGuidelines.class));
	}

	@Test
	void updateStructuredGuidelines_throwsWhenNoGuidelinesExist() {
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.updateStructuredGuidelines(
				researchGroupId, "Overview.",
				List.of(new CategoryGuidelines("structure", List.of("A rule.")))))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(guidelinesRepository, never()).save(any(ResearchGroupGuidelines.class));
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

	@Test
	void updateGuidelines_canonicalizesUnnormalizedPreprocessorOutput() {
		stubSaveReturnsArgument();
		when(currentUserProvider.getUser()).thenReturn(new User());
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.empty());
		// Nothing constrains the model to well-formed output: it can emit a null entry, a null or
		// unknown slug, blank rules, several entries for one category and a repeated rule.
		when(preprocessor.preprocess(eq("Specific."))).thenReturn(new GuidelinesPreprocessingResult(
				true, null, "Overview.",
				Arrays.asList(
						null,
						new CategoryGuidelines(null, List.of("Null slug.")),
						new CategoryGuidelines("not-a-real-category", List.of("Unknown slug.")),
						new CategoryGuidelines("structure", List.of("  Include an Abstract.  ", "   ")),
						new CategoryGuidelines("structure", List.of("Include an Abstract.", "Add a Conclusion.")))));

		ResearchGroupGuidelines saved = service.updateGuidelines(researchGroupId, "Specific.");

		assertThat(saved.getStatus()).isEqualTo(GuidelinesStatus.READY);
		StructuredGuidelines structured = saved.getStructuredGuidelines();
		// One merged entry per recognized slug, so rulesForCategory reaches every rule instead of
		// resolving to the first of two entries and dropping the rest.
		assertThat(structured.categories()).hasSize(1);
		assertThat(structured.rulesForCategory("structure"))
				.containsExactly("Include an Abstract.", "Add a Conclusion.");
		assertThat(structured.rulesForCategory("not-a-real-category")).isEmpty();
	}

	@Test
	void updateStructuredGuidelines_mergesRepeatedCategoryEntries() {
		stubSaveReturnsArgument();
		ResearchGroupGuidelines existing = new ResearchGroupGuidelines();
		existing.setResearchGroupId(researchGroupId);
		existing.setStatus(GuidelinesStatus.READY);
		when(currentUserProvider.getUser()).thenReturn(new User());
		when(guidelinesRepository.findById(researchGroupId)).thenReturn(Optional.of(existing));

		ResearchGroupGuidelines saved = service.updateStructuredGuidelines(
				researchGroupId, null,
				List.of(
						new CategoryGuidelines("bibliography", List.of("Cite at least 6 sources.")),
						new CategoryGuidelines("bibliography", List.of("Prefer peer-reviewed venues."))));

		assertThat(saved.getStructuredGuidelines().categories()).hasSize(1);
		assertThat(saved.getStructuredGuidelines().rulesForCategory("bibliography"))
				.containsExactly("Cite at least 6 sources.", "Prefer peer-reviewed venues.");
	}
}
