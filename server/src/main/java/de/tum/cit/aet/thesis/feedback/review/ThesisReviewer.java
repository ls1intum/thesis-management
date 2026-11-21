package de.tum.cit.aet.thesis.feedback.review;

import de.tum.cit.aet.thesis.feedback.model.ReviewResult;

/**
 * The AI review strategy: turns an uploaded proposal or thesis PDF into a consolidated set of
 * findings. This is the single seam between <em>how</em> a document is reviewed and everything
 * that happens around it — gating on guidelines, persisting feedback, previewing drafts — all of
 * which live in {@code feedback.service} and know nothing beyond this interface.
 *
 * <p>Swapping the strategy therefore means adding one implementation and selecting it by
 * configuration; no caller changes. The bundled {@link CategoryFanOutReviewer} runs a fixed
 * fan-out — one LLM call per {@link de.tum.cit.aet.thesis.feedback.model.ReviewCategory}, merged by
 * a final call — but an implementation is free to use a different pipeline entirely (a single
 * pass, an agent driving its own tool calls, a verification loop, a human-in-the-loop step), as
 * long as it returns a {@link ReviewResult}.
 *
 * <p>Implementations must be safe to call concurrently: several reviews can run at once, and a
 * single run may itself fan out across threads.
 */
public interface ThesisReviewer {

	/**
	 * Reviews one document.
	 *
	 * @param request the document, its review type, and the guidelines to judge it against
	 * @return the overall assessment plus the consolidated findings; never {@code null}
	 */
	ReviewResult review(ReviewRequest request);

	/**
	 * Short identifier of this strategy, used in log lines that would otherwise not say which
	 * pipeline produced a result.
	 *
	 * @return the strategy name
	 */
	default String strategy() {
		return getClass().getSimpleName();
	}
}
