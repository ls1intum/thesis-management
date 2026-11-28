package de.tum.cit.aet.thesis.feedback.service;

import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import de.tum.cit.aet.thesis.feedback.dto.AIFeedbackDraftDTO;
import de.tum.cit.aet.thesis.feedback.model.EarlierFeedback;
import de.tum.cit.aet.thesis.feedback.model.FollowUp;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import de.tum.cit.aet.thesis.feedback.repository.AIReviewSummaryRepository;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackType;
import de.tum.cit.aet.thesis.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.thesis.entity.ThesisFeedback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The application side of the comparison across iterations: which earlier entries a review is
 * compared against, and what the comparison a reviewer returns turns into.
 *
 * <p>The reviewer only judges the document — whether the defect of an earlier entry recurs or is
 * gone. What follows from that is policy, decided here where the feedback history is known:
 * <ul>
 * <li>A recurrence of an <b>open</b> entry produces nothing: the student still sees that entry.</li>
 * <li>A recurrence of a <b>completed</b> entry produces a follow-up linked to it, until the defect
 * has been followed up {@code thesis-management.ai.max-follow-ups} times. The next recurrence is
 * deferred to the supervisor instead, because restating it once more evidently does not help.</li>
 * <li>A recurrence of an entry that already <b>defers</b> to the supervisor produces no further
 * entry; the summary names it instead.</li>
 * <li>A completed entry the new version <b>resolved</b> is acknowledged — once, by the first review
 * after the student completed it, not by every review after that.</li>
 * </ul>
 */
@Service
@Conditional(AIFeaturesEnabled.class)
public class IterationComparison {
	private static final Logger log = LoggerFactory.getLogger(IterationComparison.class);

	/**
	 * Most earlier entries one review compares against. Every open entry and every completed chain
	 * head would otherwise grow the prompt with each iteration; the newest entries are the ones a
	 * student is still working on.
	 */
	static final int MAX_ENTRIES = 60;

	/** Characters of the original entry quoted in a follow-up's text. */
	private static final int ORIGINAL_EXCERPT_LENGTH = 120;

	private final AIReviewSummaryRepository summaryRepository;
	private final int maxFollowUps;

	/**
	 * Creates the comparison policy.
	 *
	 * @param summaryRepository used to find when the previous auto review ran
	 * @param maxFollowUps      how many follow-ups a defect gets before it is deferred
	 */
	public IterationComparison(AIReviewSummaryRepository summaryRepository,
			@Value("${thesis-management.ai.max-follow-ups:1}") int maxFollowUps) {
		this.summaryRepository = summaryRepository;
		this.maxFollowUps = Math.max(0, maxFollowUps);
	}

	/**
	 * The earlier entries a review is compared against, and when the previous review ran.
	 *
	 * @param entries      the entries, with refs assigned
	 * @param lastReviewAt when the previous auto review of this type captured its baseline;
	 *                     {@code null} if there was none
	 * @param capturedAt   when this baseline was read; recorded with the summary so the next review
	 *                     knows which completions this one could not have seen
	 */
	public record Baseline(List<EarlierFeedback> entries, Instant lastReviewAt, Instant capturedAt) {
		/**
		 * Normalizes the entry list.
		 *
		 * @param entries      the entries, with refs assigned
		 * @param lastReviewAt when the previous auto review captured its baseline
		 * @param capturedAt   when this baseline was read
		 */
		public Baseline {
			entries = entries == null ? List.of() : List.copyOf(entries);
		}
	}

	/**
	 * What a review amounts to once compared: the result with the summary extended, the drafts to
	 * show or persist, and the entries to acknowledge.
	 *
	 * @param result      the result, its summary extended with acknowledgement and deferral notes
	 * @param drafts      new findings followed by follow-ups and deferrals
	 * @param resolvedIds the completed entries newly found resolved
	 */
	public record Compared(ReviewResult result, List<AIFeedbackDraftDTO> drafts, List<UUID> resolvedIds) {
	}

	/**
	 * Collects the earlier entries of a thesis for one review type.
	 *
	 * <p>Only the head of each follow-up chain is included: once a defect has been followed up, the
	 * newer entry carries it, and comparing against the older one too would follow up the same
	 * defect twice.
	 *
	 * @param thesis the reviewed thesis, with its feedback loaded
	 * @param type   whether the proposal or the thesis is reviewed
	 * @return the baseline; empty entries for a first review
	 */
	public Baseline load(Thesis thesis, ReviewType type) {
		Instant capturedAt = Instant.now();
		ThesisFeedbackType feedbackType = switch (type) {
			case PROPOSAL -> ThesisFeedbackType.PROPOSAL;
			case THESIS -> ThesisFeedbackType.THESIS;
		};
		List<ThesisFeedback> ofType = thesis.getFeedback() == null ? List.of()
				: thesis.getFeedback().stream().filter(feedback -> feedback.getType() == feedbackType).toList();
		// The cutoff is when the previous review read its feedback, not when its summary was saved:
		// an entry completed while that review was running was open in its baseline, so it was never
		// checked as completed and must still be acknowledgeable now.
		Instant lastReviewAt = summaryRepository.findByThesisIdAndType(thesis.getId(), type)
				.map(summary -> summary.getBaselineCapturedAt() != null
						? summary.getBaselineCapturedAt()
						: summary.getUpdatedAt())
				.orElse(null);
		return new Baseline(selectEntries(ofType), lastReviewAt, capturedAt);
	}

	/**
	 * Picks and labels the entries to compare against: open entries and the head of every
	 * follow-up chain, newest first, capped at {@value #MAX_ENTRIES}.
	 *
	 * @param feedback the feedback of one thesis and document type
	 * @return the entries, with refs assigned in the order they were raised
	 */
	public static List<EarlierFeedback> selectEntries(List<ThesisFeedback> feedback) {
		Map<UUID, ThesisFeedback> byId = feedback.stream()
				.filter(entry -> entry.getId() != null)
				.collect(Collectors.toMap(ThesisFeedback::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
		Set<UUID> followedUp = feedback.stream()
				.map(ThesisFeedback::getFollowsUpId)
				.filter(id -> id != null && byId.containsKey(id))
				.collect(Collectors.toSet());

		// Open entries first — they are cheap to include and keep the review from repeating what the
		// student is still working on — then the most recently completed ones.
		Comparator<ThesisFeedback> priority = Comparator
				.comparing((ThesisFeedback entry) -> entry.getCompletedAt() != null)
				.thenComparing(entry -> entry.getCompletedAt() != null ? entry.getCompletedAt() : entry.getRequestedAt(),
						Comparator.nullsLast(Comparator.reverseOrder()));
		List<ThesisFeedback> selected = byId.values().stream()
				.filter(entry -> !followedUp.contains(entry.getId()))
				.filter(entry -> entry.getFeedback() != null && !entry.getFeedback().isBlank())
				.sorted(priority)
				.limit(MAX_ENTRIES)
				// Refs follow the order the entries were raised in, which reads naturally in a prompt.
				.sorted(Comparator.comparing(ThesisFeedback::getRequestedAt, Comparator.nullsLast(Comparator.naturalOrder())))
				.toList();

		List<EarlierFeedback> entries = new ArrayList<>();
		for (ThesisFeedback entry : selected) {
			entries.add(new EarlierFeedback(
					"E" + (entries.size() + 1),
					entry.getId(),
					entry.getFeedback(),
					entry.getCategory() == null ? null : entry.getCategory().name(),
					entry.getSeverity() == null ? null : entry.getSeverity().name(),
					entry.getCompletedAt(),
					depthOf(entry, byId),
					entry.isDeferredToSupervisor()));
		}
		return List.copyOf(entries);
	}

	private static int depthOf(ThesisFeedback entry, Map<UUID, ThesisFeedback> byId) {
		// Walked to the end however long the chain is, so any configured cap is honoured; the visited
		// set only stops a cycle, which the schema cannot rule out for hand-edited data.
		int depth = 0;
		Set<UUID> visited = new HashSet<>();
		visited.add(entry.getId());
		ThesisFeedback current = entry;
		while (current.getFollowsUpId() != null) {
			ThesisFeedback parent = byId.get(current.getFollowsUpId());
			if (parent == null || !visited.add(parent.getId())) {
				break;
			}
			depth++;
			current = parent;
		}
		return depth;
	}

	/**
	 * Turns a reviewer's result into what the student or supervisor receives.
	 *
	 * @param result   the reviewer's result
	 * @param baseline the entries the review was compared against
	 * @return the compared review
	 */
	public Compared apply(ReviewResult result, Baseline baseline) {
		Map<String, EarlierFeedback> byRef = baseline.entries().stream()
				.collect(Collectors.toMap(EarlierFeedback::ref, Function.identity()));

		List<AIFeedbackDraftDTO> drafts = new ArrayList<>(result.findings().stream().map(FeedbackMapper::toDraft).toList());
		Set<String> handled = new HashSet<>();
		List<EarlierFeedback> deferredNow = new ArrayList<>();
		List<EarlierFeedback> stillDeferred = new ArrayList<>();

		for (FollowUp followUp : result.followUps()) {
			EarlierFeedback original = byRef.get(followUp.ref());
			if (original == null) {
				// Reviewers validate refs already; a stray one still describes a real, grounded defect.
				log.debug("Follow-up for unknown ref {} kept as a new finding", followUp.ref());
				drafts.add(FeedbackMapper.toDraft(followUp.finding()));
				continue;
			}
			if (!handled.add(original.ref()) || !original.completed()) {
				continue;
			}
			if (original.deferred()) {
				stillDeferred.add(original);
			} else if (original.followUpDepth() >= maxFollowUps) {
				deferredNow.add(original);
				drafts.add(FeedbackMapper.toFollowUpDraft(followUp.finding(), original, true));
			} else {
				drafts.add(FeedbackMapper.toFollowUpDraft(followUp.finding(), original, false));
			}
		}

		List<UUID> resolvedIds = result.resolvedRefs().stream()
				.distinct()
				.map(byRef::get)
				.filter(entry -> entry != null && entry.completed() && !handled.contains(entry.ref()))
				// Acknowledged by the first review after the student completed it; later reviews still
				// check it (a recurrence is a regression) but do not congratulate them again.
				.filter(entry -> baseline.lastReviewAt() == null || entry.completedAt().isAfter(baseline.lastReviewAt()))
				.map(EarlierFeedback::id)
				.toList();

		List<EarlierFeedback> deferred = new ArrayList<>(deferredNow);
		deferred.addAll(stillDeferred);
		String summary = appendNotes(result.summary(), resolvedIds.size(), deferred);
		return new Compared(result.withSummary(summary), List.copyOf(drafts), resolvedIds);
	}

	private static String appendNotes(String summary, int resolved, List<EarlierFeedback> deferred) {
		List<String> notes = new ArrayList<>();
		if (summary != null && !summary.isBlank()) {
			notes.add(summary.strip());
		}
		if (resolved == 1) {
			notes.add("Well done: one issue from earlier feedback is resolved in this version.");
		} else if (resolved > 1) {
			notes.add("Well done: " + resolved + " issues from earlier feedback are resolved in this version.");
		}
		// Named, not just counted: an already-deferred defect raises no new entry, so the summary is the
		// only place this review mentions it.
		String named = deferred.stream()
				.map(entry -> "\"" + excerpt(entry.text()) + "\"")
				.collect(Collectors.joining("; "));
		if (deferred.size() == 1) {
			notes.add("One issue keeps recurring despite earlier feedback. Please discuss it with your supervisor: "
					+ named);
		} else if (deferred.size() > 1) {
			notes.add(deferred.size() + " issues keep recurring despite earlier feedback. Please discuss them with your "
					+ "supervisor: " + named);
		}
		return notes.isEmpty() ? summary : String.join("\n\n", notes);
	}

	/**
	 * A short excerpt of an entry's text, for the follow-up that names it.
	 *
	 * @param text the entry's text
	 * @return the excerpt
	 */
	static String excerpt(String text) {
		String flat = text.strip().replaceAll("\\s+", " ");
		return flat.length() <= ORIGINAL_EXCERPT_LENGTH ? flat : flat.substring(0, ORIGINAL_EXCERPT_LENGTH).strip() + "…";
	}
}
