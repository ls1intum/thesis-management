package de.tum.cit.aet.thesis.feedback.eval;

import de.tum.cit.aet.thesis.feedback.model.FollowUp;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * How well one review compared the new version against its earlier feedback, scored against the
 * ground truth of {@link EvalEarlierFeedback}.
 *
 * <p>The two errors differ in cost, which is why they are reported apart rather than folded into
 * one accuracy. A missed recurrence ({@code missedRecurrences}) leaves the student where a review
 * without comparison would have left them. Acknowledging a defect that persists
 * ({@code falsePraise}) tells them a fix worked when it did not — the error the comparison is
 * designed hardest against.
 *
 * @param expectedPersists        completed entries whose defect the new version still shows
 * @param expectedResolved        completed entries the new version fixed
 * @param followedUp              refs the review followed up
 * @param acknowledged            refs the review reported resolved
 * @param followUpPrecision       share of follow-ups that target a persisting defect; {@code null}
 *                                when there were none
 * @param followUpRecall          share of persisting defects that were followed up; {@code null}
 *                                when none persisted
 * @param acknowledgementPrecision share of acknowledgements that target a resolved defect;
 *                                {@code null} when there were none
 * @param acknowledgementRecall   share of resolved defects that were acknowledged; {@code null}
 *                                when none were resolved
 * @param falsePraise             refs acknowledged although their defect persists
 * @param missedRecurrences       refs whose defect persists but that were not followed up
 * @param openTouched             refs of open entries the review followed up or acknowledged, which
 *                                it must never do
 */
public record EvalComparisonScore(
		List<String> expectedPersists,
		List<String> expectedResolved,
		List<String> followedUp,
		List<String> acknowledged,
		Double followUpPrecision,
		Double followUpRecall,
		Double acknowledgementPrecision,
		Double acknowledgementRecall,
		List<String> falsePraise,
		List<String> missedRecurrences,
		List<String> openTouched
) {

	/**
	 * Scores one review.
	 *
	 * @param truth  the case's earlier entries and their ground truth
	 * @param result the review of the new version
	 * @return the score
	 */
	public static EvalComparisonScore score(EvalEarlierFeedback truth, ReviewResult result) {
		Set<String> persists = new LinkedHashSet<>();
		Set<String> resolved = new LinkedHashSet<>();
		Set<String> open = new LinkedHashSet<>();
		for (int i = 0; i < truth.entries().size(); i++) {
			EvalEarlierFeedback.Entry entry = truth.entries().get(i);
			String ref = EvalEarlierFeedback.refOf(i);
			if (!entry.completed()) {
				open.add(ref);
			} else if ("PERSISTS".equals(entry.expected())) {
				persists.add(ref);
			} else {
				resolved.add(ref);
			}
		}

		Set<String> followedUp = new LinkedHashSet<>(result.followUps().stream().map(FollowUp::ref).toList());
		Set<String> acknowledged = new LinkedHashSet<>(result.resolvedRefs());

		return new EvalComparisonScore(
				List.copyOf(persists),
				List.copyOf(resolved),
				List.copyOf(followedUp),
				List.copyOf(acknowledged),
				ratio(intersection(followedUp, persists).size(), followedUp.size()),
				ratio(intersection(followedUp, persists).size(), persists.size()),
				ratio(intersection(acknowledged, resolved).size(), acknowledged.size()),
				ratio(intersection(acknowledged, resolved).size(), resolved.size()),
				intersection(acknowledged, persists),
				difference(persists, followedUp),
				union(intersection(followedUp, open), intersection(acknowledged, open)));
	}

	private static Double ratio(int numerator, int denominator) {
		return denominator == 0 ? null : (double) numerator / denominator;
	}

	private static List<String> intersection(Set<String> a, Set<String> b) {
		return a.stream().filter(b::contains).toList();
	}

	private static List<String> difference(Set<String> a, Set<String> b) {
		return a.stream().filter(ref -> !b.contains(ref)).toList();
	}

	private static List<String> union(List<String> a, List<String> b) {
		List<String> all = new ArrayList<>(a);
		b.stream().filter(ref -> !all.contains(ref)).forEach(all::add);
		return List.copyOf(all);
	}
}
