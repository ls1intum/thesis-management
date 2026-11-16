package de.tum.cit.aet.thesis.feedback.eval;

import static org.assertj.core.api.Assertions.assertThat;

import de.tum.cit.aet.thesis.feedback.review.AgenticReviewer;
import de.tum.cit.aet.thesis.feedback.review.CategoryFanOutReviewer;
import de.tum.cit.aet.thesis.feedback.review.ReviewMetricsRecorder;
import de.tum.cit.aet.thesis.feedback.review.ReviewRunMetrics;
import de.tum.cit.aet.thesis.feedback.review.SinglePromptReviewer;
import de.tum.cit.aet.thesis.feedback.review.ThesisReviewer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Boots the evaluation's server configuration for real. No endpoint is contacted — a context comes
 * up without making a model call — but everything up to that point is exercised: the autoconfigured
 * Spring AI beans, the conditions on both reviewers, and the property that picks between them.
 *
 * <p>Worth a test because the alternative is discovering a context that will not start, or that
 * starts with the wrong strategy, an hour into a sweep.
 */
class EvalContextTest {

	private static ConfigurableApplicationContext boot(String approach) {
		EvalApproach arm = EvalApproach.fromId(approach);
		return new SpringApplicationBuilder(EvalContext.class)
				.web(WebApplicationType.NONE)
				.bannerMode(org.springframework.boot.Banner.Mode.OFF)
				.properties(
						"thesis-management.ai.enabled=true",
						"thesis-management.ai.reviewer=" + arm.reviewer(),
						"thesis-management.ai.verify-findings=" + arm.verifyFindings(),
						"spring.ai.openai.base-url=http://unreachable",
						"spring.ai.openai.api-key=test-placeholder",
						"spring.ai.openai.chat.model=test/model",
						"spring.ai.openai.chat.temperature=0.4",
						"spring.ai.openai.chat.seed=1")
				.run();
	}

	@Test
	void theFanOutConfigurationComesUpWithTheFanOutStrategy() {
		try (ConfigurableApplicationContext context = boot("category-fan-out-verified")) {
			assertThat(context.getBean(ThesisReviewer.class)).isInstanceOf(CategoryFanOutReviewer.class);
		}
		try (ConfigurableApplicationContext context = boot("category-fan-out-unverified")) {
			assertThat(context.getBean(ThesisReviewer.class)).isInstanceOf(CategoryFanOutReviewer.class);
		}
	}

	@Test
	void theSinglePromptConfigurationComesUpWithTheSinglePrompt() {
		try (ConfigurableApplicationContext context = boot("single-prompt")) {
			assertThat(context.getBean(ThesisReviewer.class)).isInstanceOf(SinglePromptReviewer.class);
		}
	}

	@Test
	void theAgenticConfigurationComesUpWithTheAgent() {
		// The sweep measures whatever thesis-management.ai.reviewer selects, exactly as the server
		// would — not a hand-constructed reviewer that merely resembles it.
		try (ConfigurableApplicationContext context = boot("agentic")) {
			assertThat(context.getBean(ThesisReviewer.class)).isInstanceOf(AgenticReviewer.class);
		}
	}

	@Test
	void theCollectingRecorderReplacesTheLoggingDefaultSoMetricsCanBeWrittenToDisk() {
		try (ConfigurableApplicationContext context = boot("agentic")) {
			ReviewMetricsRecorder recorder = context.getBean(ReviewMetricsRecorder.class);
			assertThat(recorder).isInstanceOf(EvalContext.CollectingMetricsRecorder.class);

			EvalContext.CollectingMetricsRecorder collecting = (EvalContext.CollectingMetricsRecorder) recorder;
			assertThat(collecting.last()).isNull();
			ReviewRunMetrics metrics = new ReviewRunMetrics("AgenticReviewer", 1, 2, 1, 10, 20, 9, 9, 100);
			collecting.record(metrics);
			assertThat(collecting.last()).isSameAs(metrics);
			collecting.reset();
			assertThat(collecting.last()).isNull();
		}
	}

	@Test
	void noDatasourceOrWebServerIsBroughtUp() {
		// A review needs neither, and requiring Postgres and Keycloak is how an evaluation becomes
		// one nobody reruns.
		try (ConfigurableApplicationContext context = boot("agentic")) {
			assertThat(context.getBeanNamesForType(javax.sql.DataSource.class)).isEmpty();
		}
	}
}
