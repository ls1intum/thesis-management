package de.tum.cit.aet.thesis.feedback.eval;

import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import de.tum.cit.aet.thesis.feedback.review.ReviewRequest;
import de.tum.cit.aet.thesis.feedback.review.ThesisReviewer;
import de.tum.cit.aet.thesis.feedback.service.PdfService;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the review pipeline over a fixed corpus in several server configurations and writes what
 * each one said.
 *
 * <p>A sweep is the cross product of models and review strategies ({@link EvalApproach}). Each cell
 * boots its own application context with that configuration — the same
 * {@code thesis-management.ai.reviewer}, {@code thesis-management.ai.verify-findings} and
 * {@code spring.ai.openai.chat.model} properties the real server reads — runs every document for
 * every seed, then shuts the context down and moves to the next cell. Booting per cell rather than
 * swapping beans in one context is what makes the thing under measurement the configured pipeline
 * rather than a hand-assembled lookalike.
 *
 * <p>Deliberately a separate entry point, not a profile on the main application. The server's own
 * startup brings up a datasource, Liquibase, Keycloak and a web server; a review needs none of
 * them, and an evaluation that required a database would be one nobody reruns.
 *
 * <p>Run it with:
 * <pre>
 * ./gradlew evalReview -Pmodels=openai/gpt-oss-120b,google/gemma-4-26B-A4B-it \
 *   -Pcorpus=/path/to/corpus -Pseeds=3
 * </pre>
 * or invoke the class directly with the same {@code --key=value} arguments
 * ({@link EvalPlan#parse}). {@code OPENAI_BASE_URL} and {@code OPENAI_API_KEY} come from the
 * environment unless passed as {@code --base-url} / {@code --api-key}.
 */
public final class EvalApplication {

	/**
	 * The agent's turn budget, set explicitly rather than inherited so the manifest can state it and
	 * the analysis can tell a run that hit it from one that finished. Matches the deployed default.
	 */
	static final int AGENT_MAX_TURNS = 40;

	private static final DateTimeFormatter RUN_ID =
			DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss'Z'").withZone(ZoneOffset.UTC);

	private EvalApplication() {
	}

	/**
	 * Entry point.
	 *
	 * @param args {@code --key=value} arguments; see {@link EvalPlan#parse}
	 */
	public static void main(String[] args) {
		EvalPlan plan = EvalPlan.parse(args);
		EvalCorpus corpus = EvalCorpus.discover(plan.corpus(), plan.anonymize());
		Path runRoot = plan.outputRoot().resolve(RUN_ID.format(Instant.now()));

		int total = plan.totalRuns(corpus.documents().size());
		System.out.printf("Evaluating %d model(s) x %d approach(es) x %d document(s) x %d seed(s) = %d runs%n",
				plan.models().size(), plan.approaches().size(), corpus.documents().size(), plan.seeds(), total);

		try (EvalWriter writer = new EvalWriter(runRoot)) {
			writer.writeManifest(plan, corpus);
			writer.writeJudgingMaterial(EvalGuidelines.build(), pageTexts(corpus));
			System.out.println("Writing to " + writer.runRoot().toAbsolutePath());

			int done = 0;
			for (String model : plan.models()) {
				for (String approach : plan.approaches()) {
					// The seed is part of the configuration, not of a request: Spring AI takes it
					// from spring.ai.openai.chat.seed, so a repetition means another context. They
					// cost well under a second each, which is the price of not threading an
					// evaluation-only parameter through the production reviewer interface.
					for (int seed = 1; seed <= plan.seeds(); seed++) {
						done = runConfiguration(plan, corpus, writer, model, approach, seed, done, total);
					}
				}
			}
			System.out.printf("%nDone: %d runs -> %s%n", done, writer.runRoot().toAbsolutePath());
		}
	}

	/**
	 * Every document's page text, extracted the way every reviewer extracts it. PdfService needs no
	 * context, so this runs once up front rather than inside a cell.
	 */
	private static Map<String, List<String>> pageTexts(EvalCorpus corpus) {
		PdfService pdfService = new PdfService();
		Map<String, List<String>> texts = new LinkedHashMap<>();
		for (EvalCorpus.EvalDocument document : corpus.documents()) {
			texts.put(document.id(), pdfService.extractTextFromPdf(new FileSystemResource(document.file())));
		}
		return texts;
	}

	/** Boots one server configuration and runs the whole corpus through it. */
	private static int runConfiguration(EvalPlan plan, EvalCorpus corpus, EvalWriter writer, String model,
			String approach, int seed, int startedWith, int total) {
		System.out.printf("%n=== %s / %s / seed %d ===%n", model, approach, seed);
		int done = startedWith;

		try (ConfigurableApplicationContext context = boot(plan, model, approach, seed)) {
			ThesisReviewer reviewer = context.getBean(ThesisReviewer.class);
			EvalContext.CollectingMetricsRecorder metrics =
					context.getBean(EvalContext.CollectingMetricsRecorder.class);

			for (EvalCorpus.EvalDocument document : corpus.documents()) {
				done++;
				EvalRun run = new EvalRun(model, approach, document.id(), document.type().name(), seed);
				runOne(writer, reviewer, metrics, run, document, seed, done, total);
			}
		}
		return done;
	}

	/** Runs one document once and writes the outcome, whichever way it goes. */
	private static void runOne(EvalWriter writer, ThesisReviewer reviewer,
			EvalContext.CollectingMetricsRecorder metrics, EvalRun run, EvalCorpus.EvalDocument document,
			int seed, int done, int total) {
		metrics.reset();
		Instant started = Instant.now();
		try {
			ReviewResult result = reviewer.review(new ReviewRequest(
					document.type(), EvalGuidelines.build(), new FileSystemResource(document.file())));
			writer.writeRun(run, result, metrics.last());
			writer.writeTrace(run, metrics.candidates(), metrics.categoryFailures());
			System.out.printf("  [%d/%d] %s seed=%d -> %d findings, %ds%n",
					done, total, document.id(), seed, result.findings().size(), elapsed(started));
		} catch (RuntimeException e) {
			// One model refusing one document must not cost the other 60 runs of the sweep.
			writer.writeFailure(run, e);
			writer.writeTrace(run, metrics.candidates(), metrics.categoryFailures());
			System.out.printf("  [%d/%d] %s seed=%d -> FAILED after %ds: %s%n",
					done, total, document.id(), seed, elapsed(started), e);
		}
	}

	/**
	 * Boots one cell's context. The model, temperature and seed are set as ordinary Spring
	 * properties, which is all it takes: Spring AI binds {@code spring.ai.openai.chat.*} into the
	 * chat model's default options, so every call the pipeline makes inherits them without either
	 * reviewer knowing an evaluation is running.
	 *
	 * <p>They are passed as command-line arguments, not through
	 * {@code SpringApplicationBuilder.properties}, because that method contributes
	 * <em>default</em> properties — the lowest-precedence source there is. The application's own
	 * {@code application.yml} is on this classpath and pins {@code thesis-management.ai.enabled} to
	 * false, so defaults would lose to it and the context would come up with no reviewer at all.
	 * Command-line arguments outrank every file.
	 *
	 * <p>Whether the seed actually pins sampling is the endpoint's business — plenty of
	 * OpenAI-compatible servers accept the field and ignore it. It is sent regardless, and the
	 * repetitions measure run-to-run variance either way; they just measure less of it when the
	 * endpoint honours it.
	 */
	/** Per-request timeout, the same as the server's {@code spring.ai.openai.timeout}. */
	static final String REQUEST_TIMEOUT = "10m";

	private static ConfigurableApplicationContext boot(EvalPlan plan, String model, String approach, int seed) {
		EvalApproach arm = EvalApproach.fromId(approach);
		List<String> args = new ArrayList<>(List.of(
						"--" + AIFeaturesEnabled.PROPERTY + "=true",
						"--thesis-management.ai.reviewer=" + arm.reviewer(),
						// Set for every arm rather than left to its default: the two fan-out arms
						// differ in this property alone.
						"--thesis-management.ai.verify-findings=" + arm.verifyFindings(),
						"--thesis-management.ai.agent-max-turns=" + AGENT_MAX_TURNS,
						// Text only, for both arms. The fan-out otherwise auto-detects vision from the
						// model name and attaches every rendered page to all nine concurrent calls,
						// while the agent is text-only by construction — so leaving this to
						// auto-detection would compare modalities, not orchestrations, and only for
						// the models whose names happen to match VisionModels. It is also what makes
						// the fan-out trip a 1M-token-per-minute budget on a 30-page document.
						"--thesis-management.ai.review-include-images=false",
						"--spring.ai.openai.base-url=" + plan.baseUrl(),
						"--spring.ai.openai.api-key=" + plan.apiKey(),
						// Set explicitly: the sweep runs on the test classpath, whose application.yml
						// shadows the main one, so the server's 10m would otherwise fall back to Spring
						// AI's 60s and a reasoning model's long answer would be cancelled and retried.
						"--spring.ai.openai.timeout=" + REQUEST_TIMEOUT,
						"--spring.ai.openai.chat.model=" + model,
						"--spring.ai.openai.chat.temperature=" + plan.temperature(),
						"--spring.ai.openai.chat.seed=" + seed,
						// A sweep boots one context per cell, and the runner prints its own progress.
						// Spring's startup chatter and the PDF reader's per-page lines would bury it.
						"--logging.level.root=WARN",
						"--logging.level.de.tum.cit.aet.thesis=INFO"));
		if (plan.thinkingModels().contains(model)) {
			// Brackets keep the underscores: relaxed binding strips them from unbracketed map keys.
			// Spring AI sends extra-body verbatim as additional fields of every request.
			args.add("--spring.ai.openai.chat.extra-body[chat_template_kwargs][enable_thinking]=true");
		}
		return new SpringApplicationBuilder(EvalContext.class)
				.web(WebApplicationType.NONE)
				.bannerMode(org.springframework.boot.Banner.Mode.OFF)
				.run(args.toArray(String[]::new));
	}

	private static long elapsed(Instant started) {
		return Duration.between(started, Instant.now()).toSeconds();
	}
}
