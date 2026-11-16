package de.tum.cit.aet.thesis.feedback.eval;

import de.tum.cit.aet.thesis.feedback.config.ChatClientTimeoutConfig;
import de.tum.cit.aet.thesis.feedback.model.ReviewCategory;
import de.tum.cit.aet.thesis.feedback.review.AgenticReviewer;
import de.tum.cit.aet.thesis.feedback.review.CategoryCandidates;
import de.tum.cit.aet.thesis.feedback.review.CategoryFanOutReviewer;
import de.tum.cit.aet.thesis.feedback.review.ReviewMetricsRecorder;
import de.tum.cit.aet.thesis.feedback.review.ReviewRunMetrics;
import de.tum.cit.aet.thesis.feedback.review.SinglePromptReviewer;
import de.tum.cit.aet.thesis.feedback.service.PdfService;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The server configuration one cell of the sweep runs under: the real review pipeline and nothing
 * else.
 *
 * <p>Only Spring AI's chat beans are auto-configured, plus the HTTP, Jackson, retry and tool-calling
 * beans they need. No datasource, JPA, Liquibase, Keycloak, or web server — a review needs none of
 * them, and leaving them out means a configuration boots in under a second, which matters when the
 * sweep boots one per model-and-strategy combination.
 *
 * <p>What is deliberately <em>not</em> stubbed is the selection itself. {@link SinglePromptReviewer},
 * {@link CategoryFanOutReviewer} and {@link AgenticReviewer} are imported with their conditions
 * intact, so each cell gets its reviewer the same way production does — from
 * {@code thesis-management.ai.reviewer}. An evaluation that hand-constructed the strategy it is measuring would not be measuring the shipped one.
 *
 * <p>{@link ChatClientTimeoutConfig} comes along because without it every call reverts to Spring
 * AI's 60-second default and a full review is cut off mid-generation.
 */
@Configuration(proxyBeanMethods = false)
@ImportAutoConfiguration({
		RestClientAutoConfiguration.class,
		WebClientAutoConfiguration.class,
		JacksonAutoConfiguration.class,
		SpringAiRetryAutoConfiguration.class,
		ToolCallingAutoConfiguration.class,
		OpenAiChatAutoConfiguration.class,
		ChatClientAutoConfiguration.class,
})
@Import({ChatClientTimeoutConfig.class, PdfService.class, SinglePromptReviewer.class, CategoryFanOutReviewer.class,
		AgenticReviewer.class})
public class EvalContext {

	/**
	 * Keeps the last finished run's metrics instead of logging them, so the sweep can write them
	 * beside the findings they belong to.
	 *
	 * @return the collecting recorder
	 */
	@Bean
	CollectingMetricsRecorder collectingMetricsRecorder() {
		return new CollectingMetricsRecorder();
	}

	/**
	 * A {@link ReviewMetricsRecorder} that holds what it is given. One review runs at a time in a
	 * sweep, so the last value recorded always belongs to the review that just finished; it is
	 * {@code volatile} only because the fan-out strategy finishes on a different thread than the
	 * one that started it. The per-category trace arrives concurrently from the fan-out's tasks,
	 * hence the synchronized collections.
	 */
	public static class CollectingMetricsRecorder implements ReviewMetricsRecorder {
		private volatile ReviewRunMetrics last;
		private final List<CategoryCandidates> candidates = Collections.synchronizedList(new ArrayList<>());
		private final Map<String, String> categoryFailures = Collections.synchronizedMap(new LinkedHashMap<>());

		@Override
		public void record(ReviewRunMetrics metrics) {
			this.last = metrics;
		}

		@Override
		public void recordCandidates(CategoryCandidates categoryCandidates) {
			candidates.add(categoryCandidates);
		}

		@Override
		public void recordCategoryFailure(ReviewCategory category, Throwable cause) {
			categoryFailures.put(category.getSlug(), EvalWriter.rootCauseOf(cause));
		}

		/** Clears everything held before a run, so a failed run cannot report the previous one's. */
		public void reset() {
			this.last = null;
			candidates.clear();
			categoryFailures.clear();
		}

		/**
		 * What each category of the run that just finished produced before the merge, in
		 * {@link ReviewCategory} order. Empty for a strategy without a category stage.
		 *
		 * @return the per-category trace
		 */
		public List<CategoryCandidates> candidates() {
			synchronized (candidates) {
				return candidates.stream()
						.sorted(java.util.Comparator.comparing(CategoryCandidates::category))
						.toList();
			}
		}

		/**
		 * The categories of the run that just finished whose pass failed, mapped to the root cause.
		 *
		 * @return category slug to root cause
		 */
		public Map<String, String> categoryFailures() {
			synchronized (categoryFailures) {
				return Map.copyOf(categoryFailures);
			}
		}

		/**
		 * The metrics of the run that just finished.
		 *
		 * @return the metrics, or {@code null} if the run never reported any
		 */
		public ReviewRunMetrics last() {
			return last;
		}
	}
}
