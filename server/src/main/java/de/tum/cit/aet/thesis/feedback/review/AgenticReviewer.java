package de.tum.cit.aet.thesis.feedback.review;

import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import de.tum.cit.aet.thesis.feedback.model.ReviewCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import de.tum.cit.aet.thesis.feedback.service.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The model-driven {@link ThesisReviewer}: one agent, a toolbox, and no fixed plan. Where
 * {@link CategoryFanOutReviewer} decides in Java that there will be one call per category followed
 * by a merge, this strategy decides nothing — which dimension to look at next, how much of the
 * document to re-read, which quote to verify, and when to stop all come out of the model's own
 * tool-use decisions. The number of calls and their order therefore vary from run to run, and the
 * review goes deeper where it finds problems instead of spending the same budget everywhere.
 *
 * <p>The trade is latency and predictability for depth. The fan-out's calls are concurrent and its
 * call depth is a constant two stages; the agent's turns are strictly sequential, so a thorough
 * run is slower in wall-clock time even when it costs fewer tokens. {@link ReviewRunMetrics},
 * logged at the end of every run of either strategy, is what makes that comparison concrete.
 *
 * <p>Four things are fixed in code rather than left to the model, because they are the parts a
 * model is bad at:
 * <ul>
 * <li><b>Coverage</b> is recorded, not claimed: the group's rules for a dimension exist nowhere but
 * in {@code get_category_rules}, so a dimension the agent never asked about is one it cannot have
 * judged, and the run is reported as incomplete for it.</li>
 * <li><b>Quotes</b> are checked against the document by {@link Grounding} before a finding is
 * accepted, so a fabricated quote is refused at the point it is made.</li>
 * <li><b>The budget</b> is bounded: the loop stops after
 * {@code thesis-management.ai.agent-max-turns} model turns however busy the agent still is.</li>
 * <li><b>A run cut short still counts.</b> Findings accumulate in {@link ReviewToolbox} as they are
 * recorded, so hitting the turn cap, losing the connection, or an agent that simply never calls
 * {@code finish_review} costs the run its verdict, not its findings.</li>
 * </ul>
 *
 * <p>Text only, on purpose: the agent re-sends its whole conversation every turn, and page images
 * carried across a dozen turns cost more than the figure checks they enable are worth. A review
 * that must judge figure rendering belongs on the fan-out, which pays for the images exactly once.
 *
 * <p>Selected by {@code thesis-management.ai.reviewer=agentic}; {@link CategoryFanOutReviewer} is
 * the default.
 */
@Service
@Conditional(AIFeaturesEnabled.class)
@ConditionalOnProperty(name = "thesis-management.ai.reviewer", havingValue = "agentic")
public class AgenticReviewer implements ThesisReviewer {
	private static final Logger log = LoggerFactory.getLogger(AgenticReviewer.class);

	/**
	 * Largest document, in characters, handed to the agent up front. Below it the whole text goes
	 * into the first user message: the agent then spends its turns reasoning rather than paging,
	 * which is much the cheaper shape for a proposal. Above it — a long thesis — the same choice
	 * inverts, because every one of those characters is re-sent on every turn, so the agent gets a
	 * page index and reads what it needs through the tools instead.
	 */
	private static final int INLINE_DOCUMENT_LIMIT = 60_000;

	/** Characters of each page shown in the index of a document too long to inline. */
	private static final int PAGE_PREVIEW_LENGTH = 200;

	private final PdfService pdfService;
	private final ChatClient chatClient;
	private final ToolCallingManager toolCallingManager;
	private final ReviewMetricsRecorder metricsRecorder;
	private final int maxTurns;

	/**
	 * Creates the reviewer.
	 *
	 * @param pdfService         service used to extract the document's per-page text
	 * @param chatClientBuilder  Spring AI builder used to construct the chat client
	 * @param toolCallingManager executes the model's tool calls; taken from the context so the
	 *                           application's configured tool-call limits and observations apply
	 * @param metricsRecorder    where each finished run's measured cost goes
	 * @param maxTurns           hard cap on the model turns one review may take
	 */
	public AgenticReviewer(PdfService pdfService, ChatClient.Builder chatClientBuilder,
			ToolCallingManager toolCallingManager, ReviewMetricsRecorder metricsRecorder,
			@Value("${thesis-management.ai.agent-max-turns:40}") int maxTurns) {
		this.pdfService = pdfService;
		this.chatClient = chatClientBuilder.build();
		this.toolCallingManager = toolCallingManager;
		this.metricsRecorder = metricsRecorder;
		this.maxTurns = maxTurns;
		log.info("AI review runs the agentic strategy (max {} model turns per review)", maxTurns);
	}

	@Override
	public ReviewResult review(ReviewRequest request) {
		List<String> pages = pdfService.extractTextFromPdf(request.document());
		ReviewMeter meter = new ReviewMeter();
		ReviewToolbox toolbox = new ReviewToolbox(pages, request.guidelines(), request.type(), request.progress(),
				meter);

		try {
			runAgent(toolbox, meter, request.type(), pages);
		} catch (RuntimeException e) {
			// Whatever went wrong — a provider error, a tool-call limit, a model that emitted a
			// malformed tool name — the agent's work up to that point is already in the toolbox and
			// is worth more than a clean failure.
			log.warn("Agentic review ended early after {} turns ({}); keeping what it recorded",
					meter.llmCalls(), request.type(), e);
		}

		ReviewResult result = toolbox.close();
		List<ReviewCategory> missing = toolbox.uncovered();

		metricsRecorder.record(meter.snapshot(strategy(), toolbox.coveredCount(),
				ReviewCategory.values().length));

		if (missing.size() == ReviewCategory.values().length && result.findings().isEmpty()) {
			// Nothing was checked and nothing was found: there is no review here to qualify, only a
			// failure to report as one.
			throw new IllegalStateException("Agentic review produced no findings and checked no category");
		}
		if (!missing.isEmpty()) {
			log.warn("Agentic review covered {} of {} categories; {} were never checked",
					toolbox.coveredCount(), ReviewCategory.values().length, slugsOf(missing));
		}
		return IncompleteCoverage.qualify(result, missing);
	}

	/**
	 * Runs the agent loop until it finishes, falls silent, or exhausts its turn budget.
	 *
	 * <p>The loop itself is Spring AI's {@link ToolCallingAdvisor}, driven here with a stateful
	 * eligibility check instead of the default one. That check is called once per model turn with
	 * that turn's response, which makes it the one place that sees every round: it meters the call,
	 * and it is also where the loop is stopped — on the verdict being in, or on the budget running
	 * out. Doing it this way keeps the conversation handling in Spring AI rather than reimplementing
	 * it to get a counter.
	 *
	 * <p>Overridable so a test can exercise the surrounding logic without a model.
	 *
	 * @param toolbox the run's tools and state
	 * @param meter   the run's meter
	 * @param type    whether the document is reviewed as a proposal or as a final thesis
	 * @param pages   the document's per-page extracted text
	 */
	protected void runAgent(ReviewToolbox toolbox, ReviewMeter meter, ReviewType type, List<String> pages) {
		chatClient.prompt()
				.system(AgentPrompt.systemPrompt(type))
				.user(openingMessage(type, pages))
				.tools(toolbox.callbacks().toArray())
				.advisors(ToolCallingAdvisor.builder()
						.toolCallingManager(toolCallingManager)
						.toolExecutionEligibilityChecker(response -> continueLoop(response, toolbox, meter))
						.build())
				.call()
				.chatResponse();
	}

	/**
	 * Decides whether the agent gets another turn, and records the turn that just happened.
	 */
	private boolean continueLoop(ChatResponse response, ReviewToolbox toolbox, ReviewMeter meter) {
		meter.recordCall(response);
		meter.addDepth(1);

		if (response == null || !response.hasToolCalls()) {
			return false;
		}
		if (toolbox.finished()) {
			return false;
		}
		if (meter.llmCalls() >= maxTurns) {
			log.warn("Agentic review hit its {}-turn budget; stopping the agent and keeping what it recorded",
					maxTurns);
			return false;
		}
		return true;
	}

	/**
	 * Builds the first user message: the brief, and either the whole document or an index of it.
	 */
	private static String openingMessage(ReviewType type, List<String> pages) {
		String document = type == ReviewType.PROPOSAL ? "proposal" : "thesis";
		int characters = pages.stream().mapToInt(String::length).sum();
		String brief = ("You are reviewing a %d-page student %s. Work through every review dimension, record grounded "
				+ "findings with verbatim quotes, then call finish_review.")
						.formatted(pages.size(), document);

		if (characters <= INLINE_DOCUMENT_LIMIT) {
			return brief + "\n\nThe full text follows; use get_page_text and search_document to verify exact wording.\n\n"
					+ fencedPages(pages);
		}
		return brief + "\n\nThis " + document + " is too long to include here in full. An index of its pages follows; "
				+ "read the pages you need with get_page_text and locate sections with search_document.\n\n"
				+ fencedPages(previewOf(pages));
	}

	/** Shortens every page to its opening characters, for the index of a long document. */
	private static List<String> previewOf(List<String> pages) {
		return pages.stream()
				.map(page -> page.length() <= PAGE_PREVIEW_LENGTH
						? page
						: page.substring(0, PAGE_PREVIEW_LENGTH) + "…")
				.toList();
	}

	private static String fencedPages(List<String> pages) {
		String body = IntStream.range(0, pages.size())
				.mapToObj(index -> "=== PAGE " + (index + 1) + " ===\n" + pages.get(index))
				.collect(Collectors.joining("\n\n"));
		return "<" + ReviewToolbox.PAGE_TEXT_TAG + ">\n" + body + "\n</" + ReviewToolbox.PAGE_TEXT_TAG + ">";
	}

	private static String slugsOf(List<ReviewCategory> categories) {
		return categories.stream().map(ReviewCategory::getSlug).collect(Collectors.joining(", "));
	}
}
