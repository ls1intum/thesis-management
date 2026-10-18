package de.tum.cit.aet.thesis.feedback.review;

/**
 * The measured cost of one review run, so the configured strategies can be compared on numbers
 * taken from real executions rather than on estimates. Recorded by every {@link ThesisReviewer}
 * implementation and logged when the run ends.
 *
 * <p>{@code callDepth} is the longest <em>sequential</em> chain of model calls and is the
 * interesting figure next to {@code llmCalls}: {@link CategoryFanOutReviewer} makes many calls but
 * keeps depth at a constant two stages, whereas {@link AgenticReviewer} makes one call at a time
 * and its depth grows with how hard the agent works. Depth is the latency proxy; {@code llmCalls}
 * and the token counts are the cost.
 *
 * @param strategy           the {@link ThesisReviewer#strategy()} that produced the run
 * @param llmCalls           number of model round-trips the run made
 * @param toolCalls          number of tool invocations; always 0 for a strategy without tools
 * @param callDepth          longest sequential chain of model calls
 * @param promptTokens       input tokens billed across every call, as reported by the provider
 * @param completionTokens   output tokens billed across every call, as reported by the provider
 * @param categoriesCovered  review categories the run actually checked
 * @param categoriesTotal    review categories the run was meant to check
 * @param wallMillis         wall-clock duration of the run
 */
public record ReviewRunMetrics(
		String strategy,
		int llmCalls,
		int toolCalls,
		int callDepth,
		long promptTokens,
		long completionTokens,
		int categoriesCovered,
		int categoriesTotal,
		long wallMillis
) {
}
