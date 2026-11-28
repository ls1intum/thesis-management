package de.tum.cit.aet.thesis.feedback.review;

import de.tum.cit.aet.thesis.feedback.config.LenientOutputConverter;
import de.tum.cit.aet.thesis.feedback.model.EarlierFeedback;
import de.tum.cit.aet.thesis.feedback.model.Finding;
import de.tum.cit.aet.thesis.feedback.model.FollowUp;
import de.tum.cit.aet.thesis.feedback.model.Location;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The fan-out's comparison across iterations: one model call that checks every completed earlier
 * feedback entry against the new version and says, per entry, whether its defect is resolved,
 * persists, or cannot be judged.
 *
 * <p>One dedicated call rather than one slice per category, because the categories of a stored
 * entry (its display label) and the categories of the fan-out (guideline sections) are different
 * vocabularies with no mapping between them. It runs concurrently with the category reviews, so it
 * adds a call but no latency.
 *
 * <p>An explicit verdict per entry, rather than reading "no category re-reported it" as resolved,
 * because a category that misses a defect would then congratulate the student on a fix they did
 * not make. The same distrust applies to the verdicts themselves: a persisting defect needs a
 * grounded quote, and a verdict for a ref that was not asked about is dropped.
 */
public class EarlierFeedbackChecker {
	private static final Logger log = LoggerFactory.getLogger(EarlierFeedbackChecker.class);

	private final ChatClient chatClient;
	private final ReviewMeter meter;

	/**
	 * Creates the checker for one run.
	 *
	 * @param chatClient the chat client the call goes through
	 * @param meter      the run's meter; the call is recorded against it
	 */
	public EarlierFeedbackChecker(ChatClient chatClient, ReviewMeter meter) {
		this.chatClient = chatClient;
		this.meter = meter;
	}

	/**
	 * What the comparison found.
	 *
	 * @param followUps    restated defects of the entries that persist
	 * @param persistRefs  refs of the entries that persist, including any whose follow-up was dropped
	 * @param resolvedRefs refs of the entries the new version resolved
	 */
	public record Outcome(List<FollowUp> followUps, List<String> persistRefs, List<String> resolvedRefs) {
		/** Nothing compared: no completed entries, or a failed call. */
		public static final Outcome EMPTY = new Outcome(List.of(), List.of(), List.of());
	}

	/**
	 * Checks the completed entries against the new version.
	 *
	 * @param type    whether a proposal or a thesis is reviewed
	 * @param entries the completed earlier entries; open ones are not the call's concern
	 * @param pages   the new version's per-page text
	 * @param images  the new version's page images, possibly empty
	 * @return the verdicts that survived validation
	 */
	public Outcome check(ReviewType type, List<EarlierFeedback> entries, List<String> pages, List<Media> images) {
		if (entries.isEmpty()) {
			return Outcome.EMPTY;
		}
		ResponseEntity<ChatResponse, EarlierFeedbackVerdicts> response = chatClient.prompt()
				.advisors(new MeteringAdvisor(meter))
				.system(systemMessage -> systemMessage.text(EarlierFeedbackPrompt.checkSystemPrompt(type)))
				.user(userMessage -> userMessage
						.text(EarlierFeedbackPrompt.fenced(entries, entry -> "completed") + "\n\n"
								+ CategoryReviewer.fencedPages(pages))
						.media(images.toArray(new Media[0])))
				.call()
				.responseEntity(LenientOutputConverter.forType(EarlierFeedbackVerdicts.class));
		EarlierFeedbackVerdicts verdicts = response.getEntity();
		return interpret(verdicts == null ? List.of() : verdicts.verdicts(), entries, pages);
	}

	/**
	 * Validates the model's verdicts against the entries that were asked about and the document.
	 * Package-private so the validation can be tested without a model.
	 */
	static Outcome interpret(List<EarlierFeedbackVerdicts.Verdict> verdicts, List<EarlierFeedback> entries,
			List<String> pages) {
		Map<String, EarlierFeedback> byRef = entries.stream()
				.collect(Collectors.toMap(entry -> normalizeRef(entry.ref()), Function.identity()));
		Set<String> seen = new HashSet<>();
		List<FollowUp> followUps = new ArrayList<>();
		List<String> persists = new ArrayList<>();
		List<String> resolved = new ArrayList<>();

		for (EarlierFeedbackVerdicts.Verdict verdict : verdicts) {
			if (verdict == null) {
				continue;
			}
			EarlierFeedback entry = byRef.get(normalizeRef(verdict.ref()));
			if (entry == null) {
				log.debug("Dropping a comparison verdict for unknown ref '{}'", verdict.ref());
				continue;
			}
			// One verdict per entry; a model that answers twice gets its first answer taken.
			if (!seen.add(entry.ref())) {
				continue;
			}
			String status = verdict.status() == null ? "" : verdict.status().strip().toUpperCase(Locale.ROOT);
			switch (status) {
				case "RESOLVED" -> resolved.add(entry.ref());
				case "PERSISTS" -> {
					String quote = strip(verdict.quote());
					if (quote != null && !Grounding.isGrounded(pages, quote)) {
						// A recurrence the model cannot point at is not one we can show the student;
						// neither does it prove the defect gone, so the entry is left undecided.
						log.debug("Dropping a persisting verdict for {} with an ungrounded quote", entry.ref());
						continue;
					}
					persists.add(entry.ref());
					followUps.add(new FollowUp(entry.ref(), toFinding(entry, verdict, quote, pages.size())));
				}
				default -> {
					// UNCLEAR or an unknown token: no claim either way.
				}
			}
		}
		return new Outcome(List.copyOf(followUps), List.copyOf(persists), List.copyOf(resolved));
	}

	private static Finding toFinding(EarlierFeedback entry, EarlierFeedbackVerdicts.Verdict verdict, String quote,
			int pageCount) {
		Integer page = verdict.page() != null && verdict.page() >= 1 && verdict.page() <= pageCount
				? verdict.page()
				: null;
		String section = strip(verdict.section());
		List<Location> locations = page == null && section == null && quote == null
				? List.of()
				: List.of(new Location(page, section, quote));
		String title = Objects.requireNonNullElse(strip(verdict.title()), "This issue is still present");
		// The follow-up keeps the original entry's labels: it is the same defect, and a recategorised
		// follow-up would read as a different issue in the student's list.
		return new Finding(entry.severity(), entry.category(), title, strip(verdict.explanation()), locations);
	}

	private static String normalizeRef(String ref) {
		return ref == null ? "" : ref.strip().toUpperCase(Locale.ROOT);
	}

	private static String strip(String value) {
		if (value == null) {
			return null;
		}
		String stripped = value.strip();
		return stripped.isEmpty() ? null : stripped;
	}
}
