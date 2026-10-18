package de.tum.cit.aet.thesis.feedback.review;

import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Accumulates what one review run actually cost while it runs, and hands the totals over as a
 * {@link ReviewRunMetrics} when it ends.
 *
 * <p>One instance per run, never shared between runs. Thread-safe, because a fan-out strategy
 * records from several threads at once.
 *
 * <p>Token counts come from the provider's usage metadata and are simply skipped when a response
 * carries none — an endpoint that reports no usage costs the run its token figures, not the run
 * itself.
 */
public final class ReviewMeter {

	private final AtomicInteger llmCalls = new AtomicInteger();
	private final AtomicInteger toolCalls = new AtomicInteger();
	private final AtomicInteger callDepth = new AtomicInteger();
	private final AtomicLong promptTokens = new AtomicLong();
	private final AtomicLong completionTokens = new AtomicLong();
	private final long startedAt = System.nanoTime();

	/**
	 * Records one model round-trip and the tokens the provider billed for it.
	 *
	 * @param response the response of that call; a {@code null} response still counts as a call,
	 *                 since it was made and paid for
	 */
	public void recordCall(ChatResponse response) {
		llmCalls.incrementAndGet();
		Usage usage = response == null || response.getMetadata() == null ? null : response.getMetadata().getUsage();
		if (usage == null) {
			return;
		}
		Integer in = usage.getPromptTokens();
		Integer out = usage.getCompletionTokens();
		if (in != null) {
			promptTokens.addAndGet(in);
		}
		if (out != null) {
			completionTokens.addAndGet(out);
		}
	}

	/** Records one tool invocation. */
	public void recordToolCall() {
		toolCalls.incrementAndGet();
	}

	/**
	 * Records that the run advanced by one or more sequential stages. A concurrent fan-out is one
	 * stage however many calls it makes; an agent turn is one stage each.
	 *
	 * @param stages how many sequential stages to add
	 */
	public void addDepth(int stages) {
		callDepth.addAndGet(stages);
	}

	/**
	 * Number of model round-trips recorded so far. Read while the run is still going to enforce a
	 * turn budget.
	 *
	 * @return the current call count
	 */
	public int llmCalls() {
		return llmCalls.get();
	}

	/**
	 * Freezes the totals, measuring the elapsed time from when this meter was created.
	 *
	 * @param strategy          the strategy that produced the run
	 * @param categoriesCovered review categories the run actually checked
	 * @param categoriesTotal   review categories the run was meant to check
	 * @return the metrics of this run
	 */
	public ReviewRunMetrics snapshot(String strategy, int categoriesCovered, int categoriesTotal) {
		return new ReviewRunMetrics(strategy, llmCalls.get(), toolCalls.get(), callDepth.get(),
				promptTokens.get(), completionTokens.get(), categoriesCovered, categoriesTotal,
				(System.nanoTime() - startedAt) / 1_000_000);
	}
}
