package de.tum.cit.aet.thesis.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.feedback.dto.AIFeedbackDraftDTO;
import de.tum.cit.aet.thesis.feedback.entity.AIReviewSummary;
import de.tum.cit.aet.thesis.feedback.model.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.model.EarlierFeedback;
import de.tum.cit.aet.thesis.feedback.model.Finding;
import de.tum.cit.aet.thesis.feedback.model.FollowUp;
import de.tum.cit.aet.thesis.feedback.model.Location;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import de.tum.cit.aet.thesis.feedback.repository.AIReviewSummaryRepository;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackCategory;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSeverity;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackType;
import de.tum.cit.aet.thesis.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.thesis.entity.ThesisFeedback;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class IterationComparisonTest {

	private static final Instant T0 = Instant.parse("2026-10-01T10:00:00Z");

	@Mock
	private AIReviewSummaryRepository summaryRepository;

	private static ThesisFeedback feedback(String text, Instant requestedAt, Instant completedAt, UUID followsUp) {
		ThesisFeedback entry = new ThesisFeedback();
		entry.setId(UUID.randomUUID());
		entry.setType(ThesisFeedbackType.PROPOSAL);
		entry.setFeedback(text);
		entry.setRequestedAt(requestedAt);
		entry.setCompletedAt(completedAt);
		entry.setCategory(ThesisFeedbackCategory.WRITING);
		entry.setSeverity(ThesisFeedbackSeverity.MINOR);
		entry.setFollowsUpId(followsUp);
		return entry;
	}

	private static EarlierFeedback earlier(String ref, Instant completedAt, int depth, boolean deferred) {
		return new EarlierFeedback(ref, UUID.randomUUID(), "Original entry " + ref + ".", "WRITING", "MINOR",
				completedAt, depth, deferred);
	}

	private static FollowUp followUp(String ref) {
		return new FollowUp(ref, new Finding("MINOR", "WRITING", "Still wrong", "Explained again.",
				List.of(new Location(2, "Introduction", null))));
	}

	private static ReviewResult result(List<Finding> findings, List<FollowUp> followUps, List<String> resolved) {
		return new ReviewResult(AssessmentCategory.ACCEPTABLE, 70, "Summary.", findings, followUps, resolved);
	}

	@Test
	void onlyTheHeadOfAFollowUpChainIsComparedAndItKnowsItsDepth() {
		ThesisFeedback original = feedback("Original", T0, T0.plusSeconds(10), null);
		ThesisFeedback followUp = feedback("Follow-up", T0.plusSeconds(20), T0.plusSeconds(30), original.getId());
		ThesisFeedback unrelated = feedback("Unrelated", T0.plusSeconds(40), null, null);

		List<EarlierFeedback> entries = IterationComparison.selectEntries(List.of(original, followUp, unrelated));

		assertThat(entries).extracting(EarlierFeedback::text).containsExactly("Follow-up", "Unrelated");
		assertThat(entries).extracting(EarlierFeedback::ref).containsExactly("E1", "E2");
		assertThat(entries.getFirst().followUpDepth()).isEqualTo(1);
		assertThat(entries.getFirst().id()).isEqualTo(followUp.getId());
		assertThat(entries.get(1).completed()).isFalse();
	}

	@Test
	void aChainIsWalkedToItsEndHoweverLongAndACycleStillTerminates() {
		List<ThesisFeedback> chain = new ArrayList<>();
		chain.add(feedback("Entry 0", T0, T0, null));
		for (int i = 1; i <= 25; i++) {
			chain.add(feedback("Entry " + i, T0.plusSeconds(i), T0.plusSeconds(i), chain.get(i - 1).getId()));
		}
		assertThat(IterationComparison.selectEntries(chain)).singleElement()
				.satisfies(head -> assertThat(head.followUpDepth()).isEqualTo(25));

		ThesisFeedback a = feedback("A", T0, T0, null);
		ThesisFeedback b = feedback("B", T0, T0, a.getId());
		ThesisFeedback c = feedback("C", T0, T0, b.getId());
		a.setFollowsUpId(c.getId());
		ThesisFeedback head = feedback("Head", T0.plusSeconds(1), T0.plusSeconds(1), c.getId());
		// a -> c -> b -> a is a cycle and Head follows up C: only Head is a chain head, and the walk stops.
		assertThat(IterationComparison.selectEntries(List.of(a, b, c, head))).singleElement()
				.satisfies(entry -> assertThat(entry.followUpDepth()).isEqualTo(3));
	}

	@Test
	void theCapKeepsEveryOpenEntryAndTheMostRecentlyCompletedOnes() {
		List<ThesisFeedback> feedback = new ArrayList<>();
		feedback.add(feedback("Open", T0, null, null));
		IntStream.range(0, IterationComparison.MAX_ENTRIES + 5).forEach(i ->
				feedback.add(feedback("Completed " + i, T0.plusSeconds(i), T0.plusSeconds(1000 + i), null)));

		List<EarlierFeedback> entries = IterationComparison.selectEntries(feedback);

		assertThat(entries).hasSize(IterationComparison.MAX_ENTRIES);
		assertThat(entries).extracting(EarlierFeedback::text).contains("Open").doesNotContain("Completed 0");
	}

	@Test
	void loadOnlyComparesFeedbackOfTheReviewedDocumentType() {
		ThesisFeedback proposal = feedback("Proposal entry", T0, null, null);
		ThesisFeedback thesisEntry = feedback("Thesis entry", T0, null, null);
		thesisEntry.setType(ThesisFeedbackType.THESIS);
		Thesis thesis = new Thesis();
		thesis.setId(UUID.randomUUID());
		thesis.setFeedback(new ArrayList<>(List.of(proposal, thesisEntry)));

		IterationComparison.Baseline baseline = new IterationComparison(summaryRepository, 1).load(thesis, ReviewType.THESIS);

		assertThat(baseline.entries()).extracting(EarlierFeedback::text).containsExactly("Thesis entry");
		assertThat(baseline.lastReviewAt()).isNull();
	}

	@Test
	void theCutoffIsWhenThePreviousReviewReadItsBaselineNotWhenItsSummaryWasSaved() {
		Instant baselineAt = T0.plusSeconds(100);
		AIReviewSummary previous = new AIReviewSummary();
		previous.setBaselineCapturedAt(baselineAt);
		previous.setUpdatedAt(T0.plusSeconds(500));
		Thesis thesis = new Thesis();
		thesis.setId(UUID.randomUUID());
		thesis.setFeedback(new ArrayList<>());
		when(summaryRepository.findByThesisIdAndType(thesis.getId(), ReviewType.PROPOSAL))
				.thenReturn(Optional.of(previous));

		IterationComparison.Baseline baseline = new IterationComparison(summaryRepository, 1)
				.load(thesis, ReviewType.PROPOSAL);

		assertThat(baseline.lastReviewAt()).isEqualTo(baselineAt);
		assertThat(baseline.capturedAt()).isNotNull();

		// Completed while the previous review was still running: open in its baseline, so this
		// review is the first to check it as completed and must acknowledge it.
		EarlierFeedback completedMidReview = earlier("E1", T0.plusSeconds(300), 0, false);
		IterationComparison.Compared compared = new IterationComparison(summaryRepository, 1)
				.apply(result(List.of(), List.of(), List.of("E1")),
						new IterationComparison.Baseline(List.of(completedMidReview), baseline.lastReviewAt(), T0));
		assertThat(compared.resolvedIds()).containsExactly(completedMidReview.id());
	}

	@Test
	void aRecurringCompletedDefectIsFollowedUpAndLinkedToItsEntry() {
		EarlierFeedback completed = earlier("E1", T0, 0, false);
		IterationComparison.Compared compared = new IterationComparison(summaryRepository, 1)
				.apply(result(List.of(), List.of(followUp("E1")), List.of()),
						new IterationComparison.Baseline(List.of(completed), null, T0));

		assertThat(compared.drafts()).singleElement().satisfies(draft -> {
			assertThat(draft.followsUpId()).isEqualTo(completed.id());
			assertThat(draft.deferredToSupervisor()).isFalse();
			assertThat(draft.feedback()).startsWith("Follow-up to \"Original entry E1.\": Still wrong — Explained again.");
			assertThat(draft.category()).isEqualTo(ThesisFeedbackCategory.WRITING);
		});
		assertThat(compared.result().summary()).isEqualTo("Summary.");
	}

	@Test
	void aDefectThatRecursAfterItsFollowUpIsDeferredToTheSupervisor() {
		EarlierFeedback followedUpOnce = earlier("E1", T0, 1, false);
		IterationComparison.Compared compared = new IterationComparison(summaryRepository, 1)
				.apply(result(List.of(), List.of(followUp("E1")), List.of()),
						new IterationComparison.Baseline(List.of(followedUpOnce), null, T0));

		AIFeedbackDraftDTO draft = compared.drafts().getFirst();
		assertThat(draft.deferredToSupervisor()).isTrue();
		assertThat(draft.followsUpId()).isEqualTo(followedUpOnce.id());
		assertThat(draft.feedback()).startsWith("Discuss with your supervisor:");
		assertThat(compared.result().summary()).contains("Please discuss it with your supervisor");
	}

	@Test
	void anAlreadyDeferredDefectRaisesNoFurtherEntryButIsNamedInTheSummary() {
		IterationComparison.Compared compared = new IterationComparison(summaryRepository, 1)
				.apply(result(List.of(), List.of(followUp("E1")), List.of()),
						new IterationComparison.Baseline(List.of(earlier("E1", T0, 2, true)), null, T0));

		assertThat(compared.drafts()).isEmpty();
		// No new entry is raised, so the summary has to say which issue it is.
		assertThat(compared.result().summary()).contains("keeps recurring").contains("\"Original entry E1.\"");
	}

	@Test
	void aRecurrenceOfAnOpenEntryIsDroppedAndAnUnknownRefIsKeptAsANewFinding() {
		IterationComparison.Compared compared = new IterationComparison(summaryRepository, 1)
				.apply(result(List.of(), List.of(followUp("E1"), followUp("E9")), List.of()),
						new IterationComparison.Baseline(List.of(earlier("E1", null, 0, false)), null, T0));

		assertThat(compared.drafts()).singleElement().satisfies(draft -> {
			assertThat(draft.followsUpId()).isNull();
			assertThat(draft.feedback()).startsWith("Still wrong");
		});
	}

	@Test
	void onlyEntriesCompletedSinceTheLastReviewAreAcknowledged() {
		Instant lastReview = T0.plusSeconds(100);
		EarlierFeedback before = earlier("E1", T0, 0, false);
		EarlierFeedback after = earlier("E2", T0.plusSeconds(200), 0, false);
		EarlierFeedback alsoAfterButRecurring = earlier("E3", T0.plusSeconds(300), 0, false);
		EarlierFeedback open = earlier("E4", null, 0, false);

		IterationComparison.Compared compared = new IterationComparison(summaryRepository, 1)
				.apply(result(List.of(), List.of(followUp("E3")), List.of("E1", "E2", "E3", "E4", "E2")),
						new IterationComparison.Baseline(List.of(before, after, alsoAfterButRecurring, open), lastReview, T0));

		assertThat(compared.resolvedIds()).containsExactly(after.id());
		assertThat(compared.result().summary())
				.isEqualTo("Summary.\n\nWell done: one issue from earlier feedback is resolved in this version.");
	}
}
