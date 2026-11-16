package de.tum.cit.aet.thesis.feedback.review;

import de.tum.cit.aet.thesis.feedback.config.LenientOutputConverter;
import de.tum.cit.aet.thesis.feedback.model.Finding;
import de.tum.cit.aet.thesis.feedback.model.Location;
import de.tum.cit.aet.thesis.feedback.model.ReviewCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Drops the findings one category pass invented, before the merge ever sees them.
 *
 * <p>The fan-out gives every {@link ReviewCategory} its own call and its own slot in the output,
 * and that structure is itself a prompt: a category examined in isolation, with nothing to report,
 * tends to report something anyway. Observed in practice as findings that enforce a rule the
 * research group never wrote — "avoid the first person", "headings must be capitalised this way" —
 * attached to a real quote from a compliant document. The merger cannot catch these: it sees nine
 * lists of plausible findings and no guideline text to check them against, so a confidently stated
 * invention survives consolidation and reaches the student as a correction.
 *
 * <p>Verification happens per category rather than over the merged result for exactly that reason.
 * The question "does this finding rest on a rule that exists?" can only be asked while the
 * category's own rules are still attached to it, which is true before the merge and never after.
 *
 * <p>Two tiers, cheapest first:
 * <ol>
 * <li>{@link Grounding} drops findings whose quote is not in the document. Deterministic, free, and
 * no model involved — the same check the agentic strategy applies when a finding is recorded.</li>
 * <li>One LLM call per category checks what code cannot: whether the requirement each surviving
 * finding enforces is actually written in that category's rules.</li>
 * </ol>
 *
 * <p>The call deliberately does <em>not</em> carry the document. The defect being removed is an
 * invented rule, which is settled by the rules and the finding alone; whether the quote is real is
 * already settled in code. Re-sending every page to nine verifiers would roughly double the run's
 * token cost to re-check something that was never in question. The one exception is a quote the
 * extracted text cannot settle — from a scanned page or a figure in an image-based review — which
 * travels with the image of just the page it cites.
 *
 * <p>Fails open. A verification call that errors leaves its category's findings untouched and logs
 * why — losing real findings because a verifier call timed out is a worse outcome than keeping a
 * few invented ones, which is the situation that held before this stage existed.
 */
public class FindingVerifier {
	private static final Logger log = LoggerFactory.getLogger(FindingVerifier.class);

	/** Fence tag wrapping the JSON-serialized candidate findings in the verifier user message. */
	static final String FINDINGS_FENCE_TAG = "candidate-findings";

	/** Introduces the category's task prompt as rules the verifier must accept, not as its own task. */
	private static final String TASK_RULES_HEADING = "# Category task rules\n"
			+ "The check group was given the task below. Every rule in it is one of this category's rules.\n\n";

	private final ChatClient chatClient;
	private final ObjectMapper objectMapper;
	private final ReviewMeter meter;

	/**
	 * Creates a verifier.
	 *
	 * @param chatClient   Spring AI chat client used to call the LLM
	 * @param objectMapper Jackson mapper used to serialize candidate findings as untrusted JSON
	 * @param meter        the run's meter; the verification call is recorded against it
	 */
	public FindingVerifier(ChatClient chatClient, ObjectMapper objectMapper, ReviewMeter meter) {
		this.chatClient = chatClient;
		this.objectMapper = objectMapper;
		this.meter = meter;
	}

	/**
	 * Verifies one category's findings.
	 *
	 * @param category         the category these findings came from, for logging
	 * @param reviewType       whether the document is a proposal or a thesis
	 * @param guidelinesPrompt the rendered rules this category was reviewed against — the only
	 *                         rules a finding from it may rest on
	 * @param findings         the candidate findings
	 * @param pages            the document's per-page text, for the grounding check
	 * @param images           the per-page images the category was reviewed with, in page order;
	 *                         empty for a text-only review
	 * @return the findings that survived; possibly empty, which is a valid result
	 */
	public CategoryFindings verify(ReviewCategory category, ReviewType reviewType, String guidelinesPrompt,
			CategoryFindings findings, List<String> pages, List<Media> images) {
		return new CategoryFindings(check(category, reviewType, guidelinesPrompt, findings, pages, images).kept());
	}

	/**
	 * Verifies one category's findings and reports which tier dropped what. Same decisions as
	 * {@link #verify}; this view is what the evaluation measures the stage by.
	 *
	 * @param category         the category these findings came from
	 * @param reviewType       whether the document is a proposal or a thesis
	 * @param guidelinesPrompt the rendered rules this category was reviewed against
	 * @param findings         the candidate findings
	 * @param pages            the document's per-page text, for the grounding check
	 * @param images           the per-page images the category was reviewed with, in page order;
	 *                         empty for a text-only review
	 * @return the candidates, split into kept, ungrounded, and rejected
	 */
	public CategoryCandidates check(ReviewCategory category, ReviewType reviewType, String guidelinesPrompt,
			CategoryFindings findings, List<String> pages, List<Media> images) {
		List<Finding> generated = findings.findings();
		List<Finding> ungrounded = new ArrayList<>();
		List<Candidate> candidates = groundedOnly(category, generated, pages, images, ungrounded);
		List<Finding> candidateFindings = candidates.stream().map(Candidate::finding).toList();
		if (candidates.isEmpty()) {
			return new CategoryCandidates(category, generated, ungrounded, List.of(), List.of(), false);
		}

		List<CategoryCandidates.RejectedFinding> rejected = new ArrayList<>();
		try {
			List<Finding> kept = keepVerified(category, reviewType, guidelinesPrompt, candidates, images, rejected);
			return new CategoryCandidates(category, generated, ungrounded, rejected, kept, false);
		} catch (RuntimeException e) {
			log.warn("Verification failed for category {} ({}); keeping its {} findings unverified",
					category.getSlug(), reviewType, candidates.size(), e);
			return new CategoryCandidates(category, generated, ungrounded, List.of(), candidateFindings, true);
		}
	}

	/**
	 * A finding that reaches the model check, with the pages whose image has to show its quote.
	 *
	 * @param finding    the finding
	 * @param imagePages 1-based pages whose quote is not in the extracted text and is checked against
	 *                   the page image instead; empty when every quote is in the extracted text
	 */
	record Candidate(Finding finding, Set<Integer> imagePages) {
	}

	/**
	 * Tier one: a finding whose quote is not in the document is dropped without a model call.
	 *
	 * <p>Extracted text is not the whole document when the review also saw page images: a scanned
	 * page or the text inside a figure is visible to the reviewer but missing from the text. A quote
	 * that is not in the text but cites a page with an image therefore goes on to the model check
	 * with that image attached, rather than being dropped here. Without images, or without a usable
	 * page number, the text is all there is, and such a quote is dropped.
	 *
	 * @param dropped receives the findings this tier drops
	 */
	private static List<Candidate> groundedOnly(ReviewCategory category, List<Finding> findings, List<String> pages,
			List<Media> images, List<Finding> dropped) {
		List<Candidate> candidates = new ArrayList<>();
		for (Finding finding : findings) {
			Set<Integer> imagePages = new TreeSet<>();
			boolean unverifiable = false;
			for (Location location : finding.locations()) {
				String quote = location.quote();
				if (quote == null || quote.isBlank() || Grounding.isGrounded(pages, quote)) {
					// A finding with no quote is kept: the prompts allow an empty location for
					// something that is absent, and there is nothing to ground.
					continue;
				}
				Integer page = location.page();
				if (page == null || page < 1 || page > images.size()) {
					unverifiable = true;
					break;
				}
				imagePages.add(page);
			}
			if (unverifiable) {
				log.debug("Dropped an ungrounded finding from category {}: {}", category.getSlug(), finding.title());
				dropped.add(finding);
				continue;
			}
			candidates.add(new Candidate(finding, imagePages));
		}
		if (candidates.size() < findings.size()) {
			log.info("Category {} lost {} of {} findings to the grounding check",
					category.getSlug(), findings.size() - candidates.size(), findings.size());
		}
		return candidates;
	}

	/**
	 * Tier two: one model call deciding which findings rest on a rule that actually exists, and
	 * whether a quote missing from the extracted text is visible on its page image.
	 *
	 * @param rejected receives the findings this tier drops, with the verifier's reason
	 */
	private List<Finding> keepVerified(ReviewCategory category, ReviewType reviewType, String guidelinesPrompt,
			List<Candidate> candidates, List<Media> images, List<CategoryCandidates.RejectedFinding> rejected) {
		// The check group was reviewed against its category's task rules as well as the group's own,
		// and a group that leaves a category empty is told to apply only the task rules. Without them
		// here, every finding resting on a built-in rule (a missing required section, say) would look
		// invented and be dropped.
		String taskRules = TASK_RULES_HEADING + Prompts.taskPromptFor(category, reviewType);
		String systemPrompt = String.join("\n\n", Prompts.VERIFIER.getPrompt(reviewType), taskRules, guidelinesPrompt);
		// Only the pages a quote has to be found on, so a text-grounded category costs no images.
		Media[] pageImages = candidates.stream()
				.flatMap(candidate -> candidate.imagePages().stream())
				.distinct()
				.sorted()
				.map(page -> images.get(page - 1))
				.toArray(Media[]::new);

		ResponseEntity<ChatResponse, FindingVerdicts> response = chatClient.prompt()
				.advisors(new MeteringAdvisor(meter))
				.system(systemMessage -> systemMessage.text(systemPrompt))
				.user(userMessage -> {
					userMessage.text(buildVerificationPrompt(candidates));
					if (pageImages.length > 0) {
						userMessage.media(pageImages);
					}
				})
				.call()
				.responseEntity(LenientOutputConverter.forType(FindingVerdicts.class));
		// Number -> reason of every dropped finding. The first verdict for a number wins, so a model
		// that repeats one cannot drop a finding twice.
		Map<Integer, String> dropped = new java.util.HashMap<>();
		for (FindingVerdicts.Verdict verdict : response.getEntity().verdicts()) {
			if (verdict.drops() && verdict.finding() != null) {
				dropped.putIfAbsent(verdict.finding(), verdict.reason());
			}
		}

		List<Finding> kept = new ArrayList<>();
		for (int index = 0; index < candidates.size(); index++) {
			Finding finding = candidates.get(index).finding();
			if (dropped.containsKey(index + 1)) {
				rejected.add(new CategoryCandidates.RejectedFinding(finding, dropped.get(index + 1)));
				continue;
			}
			kept.add(finding);
		}

		if (kept.size() < candidates.size()) {
			log.info("Category {} lost {} of {} findings to verification",
					category.getSlug(), candidates.size() - kept.size(), candidates.size());
		}
		return kept;
	}

	/**
	 * Serializes the candidates as numbered JSON inside a fenced tag, so the verifier treats every
	 * string value as untrusted data rather than as prompt text. The VERIFIER prompt repeats this
	 * instruction explicitly.
	 *
	 * @param candidates the findings to verify
	 * @return the fenced user message
	 */
	String buildVerificationPrompt(List<Candidate> candidates) {
		List<Map<String, Object>> numbered = new ArrayList<>();
		for (int index = 0; index < candidates.size(); index++) {
			Candidate candidate = candidates.get(index);
			numbered.add(candidate.imagePages().isEmpty()
					? Map.of("finding", index + 1, "value", candidate.finding())
					: Map.of("finding", index + 1, "value", candidate.finding(),
							"quoteOnlyInPageImage", List.copyOf(candidate.imagePages())));
		}
		String json = objectMapper.writeValueAsString(numbered);
		return "<" + FINDINGS_FENCE_TAG + ">\n" + json + "\n</" + FINDINGS_FENCE_TAG + ">\n";
	}
}
