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
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
 * token cost to re-check something that was never in question.
 *
 * <p>Fails open. A verification call that errors leaves its category's findings untouched and logs
 * why — losing real findings because a verifier call timed out is a worse outcome than keeping a
 * few invented ones, which is the situation that held before this stage existed.
 */
public class FindingVerifier {
	private static final Logger log = LoggerFactory.getLogger(FindingVerifier.class);

	/** Fence tag wrapping the JSON-serialized candidate findings in the verifier user message. */
	static final String FINDINGS_FENCE_TAG = "candidate-findings";

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
	 * @return the findings that survived; possibly empty, which is a valid result
	 */
	public CategoryFindings verify(ReviewCategory category, ReviewType reviewType, String guidelinesPrompt,
			CategoryFindings findings, List<String> pages) {
		return new CategoryFindings(check(category, reviewType, guidelinesPrompt, findings, pages).kept());
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
	 * @return the candidates, split into kept, ungrounded, and rejected
	 */
	public CategoryCandidates check(ReviewCategory category, ReviewType reviewType, String guidelinesPrompt,
			CategoryFindings findings, List<String> pages) {
		List<Finding> generated = findings.findings();
		List<Finding> ungrounded = new ArrayList<>();
		List<Finding> candidates = groundedOnly(category, generated, pages, ungrounded);
		if (candidates.isEmpty()) {
			return new CategoryCandidates(category, generated, ungrounded, List.of(), List.of(), false);
		}

		List<CategoryCandidates.RejectedFinding> rejected = new ArrayList<>();
		try {
			List<Finding> kept = keepVerified(category, reviewType, guidelinesPrompt, candidates, rejected);
			return new CategoryCandidates(category, generated, ungrounded, rejected, kept, false);
		} catch (RuntimeException e) {
			log.warn("Verification failed for category {} ({}); keeping its {} findings unverified",
					category.getSlug(), reviewType, candidates.size(), e);
			return new CategoryCandidates(category, generated, ungrounded, List.of(), candidates, true);
		}
	}

	/**
	 * Tier one: a finding whose quote is not in the document is dropped without a model call.
	 *
	 * @param dropped receives the findings this tier drops
	 */
	private static List<Finding> groundedOnly(ReviewCategory category, List<Finding> findings, List<String> pages,
			List<Finding> dropped) {
		List<Finding> grounded = new ArrayList<>();
		for (Finding finding : findings) {
			if (hasUngroundedQuote(finding, pages)) {
				log.debug("Dropped an ungrounded finding from category {}: {}", category.getSlug(), finding.title());
				dropped.add(finding);
				continue;
			}
			grounded.add(finding);
		}
		if (grounded.size() < findings.size()) {
			log.info("Category {} lost {} of {} findings to the grounding check",
					category.getSlug(), findings.size() - grounded.size(), findings.size());
		}
		return grounded;
	}

	/**
	 * Whether any location on this finding cites a quote that is not in the document. A finding
	 * with no quote at all is kept: the prompts allow an empty location for something that is
	 * absent, and there is nothing to ground.
	 */
	private static boolean hasUngroundedQuote(Finding finding, List<String> pages) {
		for (Location location : finding.locations()) {
			String quote = location.quote();
			if (quote != null && !quote.isBlank() && !Grounding.isGrounded(pages, quote)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Tier two: one model call deciding which findings rest on a rule that actually exists.
	 *
	 * @param rejected receives the findings this tier drops, with the verifier's reason
	 */
	private List<Finding> keepVerified(ReviewCategory category, ReviewType reviewType, String guidelinesPrompt,
			List<Finding> candidates, List<CategoryCandidates.RejectedFinding> rejected) {
		String systemPrompt = String.join("\n\n", Prompts.VERIFIER.getPrompt(reviewType), guidelinesPrompt);

		ResponseEntity<ChatResponse, FindingVerdicts> response = chatClient.prompt()
				.system(systemMessage -> systemMessage.text(systemPrompt))
				.user(userMessage -> userMessage.text(buildVerificationPrompt(candidates)))
				.call()
				.responseEntity(LenientOutputConverter.forType(FindingVerdicts.class));
		meter.recordCall(response.getResponse());

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
			if (dropped.containsKey(index + 1)) {
				rejected.add(new CategoryCandidates.RejectedFinding(candidates.get(index), dropped.get(index + 1)));
				continue;
			}
			kept.add(candidates.get(index));
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
	String buildVerificationPrompt(List<Finding> candidates) {
		List<Map<String, Object>> numbered = new ArrayList<>();
		for (int index = 0; index < candidates.size(); index++) {
			numbered.add(Map.of("finding", index + 1, "value", candidates.get(index)));
		}
		String json = objectMapper.writeValueAsString(numbered);
		return "<" + FINDINGS_FENCE_TAG + ">\n" + json + "\n</" + FINDINGS_FENCE_TAG + ">\n";
	}
}
