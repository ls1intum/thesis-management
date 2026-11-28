package de.tum.cit.aet.thesis.feedback.review;

import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.model.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.model.EarlierFeedback;
import de.tum.cit.aet.thesis.feedback.model.Finding;
import de.tum.cit.aet.thesis.feedback.model.FollowUp;
import de.tum.cit.aet.thesis.feedback.model.Location;
import de.tum.cit.aet.thesis.feedback.model.ReviewCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import de.tum.cit.aet.thesis.feedback.progress.ProgressReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The tools one {@link AgenticReviewer} run exposes to the model, and the state those tools build
 * up: the findings recorded so far, which review categories were actually checked, and the
 * agent's closing verdict.
 *
 * <p>One instance per run — it <em>is</em> the run's state, and the result is read off it once the
 * agent stops. That is also why a run that is cut short still yields something: whatever the agent
 * recorded before it ran out of turns or hit an error is already here.
 *
 * <p>Progress is reported from here rather than from the reviewer because this is the only place
 * that learns, as it happens, which category the agent is working on. Every category is announced
 * up front — exactly as {@link CategoryFanOutReviewer} announces its fan-out — and is completed
 * when the agent fetches its rules; whatever is left when the run ends is reported as a check that
 * did not happen.
 *
 * <p>Tool methods are safe to call concurrently, since nothing in the tool-execution contract
 * promises that the model's parallel tool calls are run one at a time.
 */
public class ReviewToolbox {
	private static final Logger log = LoggerFactory.getLogger(ReviewToolbox.class);

	/** Progress step id for assembling the final result, reported once the agent is done. */
	static final String FINALIZE_STEP_ID = "finalize";

	/** What an untouched category reports to the client; mirrors the fan-out's failure wording. */
	static final String NOT_CHECKED_MESSAGE = "This check was not performed";

	/** Fence tag wrapping uploaded page text wherever a tool hands it back to the model. */
	static final String PAGE_TEXT_TAG = "student-upload-page-text";

	/** Progress step id for the comparison against earlier feedback. */
	static final String EARLIER_FEEDBACK_STEP_ID = "earlier-feedback";

	/** Most search hits one call returns, so a common word cannot flood the context window. */
	private static final int MAX_SEARCH_HITS = 12;

	/** Characters of context shown before and after a search hit. */
	private static final int SEARCH_CONTEXT_BEFORE = 80;
	private static final int SEARCH_CONTEXT_AFTER = 160;

	/**
	 * Hands every tool result back as the plain text it already is. Spring AI's default converter
	 * JSON-encodes it, which turns a page of prose into one escaped string — exactly the text the
	 * agent is then asked to copy a quote out of verbatim, and every {@code \n} and {@code \"} in
	 * it is a chance to copy something the grounding check will not recognize.
	 */
	private static final ToolCallResultConverter PLAIN_TEXT = (result, type) -> result == null ? "" : result.toString();

	private final List<String> pages;
	private final StructuredGuidelines guidelines;
	private final ReviewType reviewType;
	private final ProgressReporter progress;
	private final ReviewMeter meter;
	private final List<EarlierFeedback> earlierFeedback;
	private final boolean compares;
	private final int totalSteps;

	private final List<Finding> findings = Collections.synchronizedList(new ArrayList<>());
	private final Set<ReviewCategory> covered = Collections.synchronizedSet(EnumSet.noneOf(ReviewCategory.class));
	private final Map<String, FollowUp> followUps = Collections.synchronizedMap(new LinkedHashMap<>());
	private final Set<String> resolved = Collections.synchronizedSet(new LinkedHashSet<>());
	private volatile boolean earlierFeedbackChecked;

	private volatile AssessmentCategory assessment;
	private volatile Integer score;
	private volatile String summary;
	private volatile boolean finished;

	/**
	 * Opens a toolbox for one run and announces every review category as a pending step.
	 *
	 * @param pages      the document's per-page extracted text, in document order
	 * @param guidelines the research group's structured guidelines
	 * @param reviewType whether the document is reviewed as a proposal or as a final thesis
	 * @param progress   sink for the per-category progress events
	 * @param meter      the run's meter; every tool call is recorded against it
	 */
	public ReviewToolbox(List<String> pages, StructuredGuidelines guidelines, ReviewType reviewType,
			ProgressReporter progress, ReviewMeter meter) {
		this(pages, guidelines, reviewType, List.of(), progress, meter);
	}

	/**
	 * Opens a toolbox for one run that also compares the document against earlier feedback. With
	 * no earlier entries it is the plain toolbox: the comparison tools are not offered at all.
	 *
	 * @param pages           the document's per-page extracted text, in document order
	 * @param guidelines      the research group's structured guidelines
	 * @param reviewType      whether the document is reviewed as a proposal or as a final thesis
	 * @param earlierFeedback the entries raised on earlier versions; may be empty
	 * @param progress        sink for the per-category progress events
	 * @param meter           the run's meter; every tool call is recorded against it
	 */
	public ReviewToolbox(List<String> pages, StructuredGuidelines guidelines, ReviewType reviewType,
			List<EarlierFeedback> earlierFeedback, ProgressReporter progress, ReviewMeter meter) {
		this.pages = List.copyOf(pages);
		this.guidelines = guidelines;
		this.reviewType = reviewType;
		this.earlierFeedback = List.copyOf(earlierFeedback);
		this.compares = !this.earlierFeedback.isEmpty();
		this.progress = progress;
		this.meter = meter;
		// Categories, the comparison when there is one, and the finalize step.
		this.totalSteps = ReviewCategory.values().length + (compares ? 1 : 0) + 1;

		for (ReviewCategory category : ReviewCategory.values()) {
			progress.stepStarted(category.getSlug(), category.getDisplayName(), stepIndex(category), totalSteps);
		}
		if (compares) {
			progress.stepStarted(EARLIER_FEEDBACK_STEP_ID, "Checking earlier feedback", totalSteps - 1, totalSteps);
		}
	}

	/**
	 * The tools handed to the model for this run.
	 *
	 * @return the tool callbacks, in the order the system prompt introduces them
	 */
	public List<ToolCallback> callbacks() {
		List<ToolCallback> tools = new ArrayList<>(reviewTools());
		if (compares) {
			tools.addAll(comparisonTools());
		}
		return List.copyOf(tools);
	}

	/**
	 * Whether this run compares against earlier feedback, i.e. whether the comparison tools are
	 * offered.
	 *
	 * @return {@code true} when earlier entries exist
	 */
	public boolean compares() {
		return compares;
	}

	/**
	 * Whether the agent skipped the comparison: earlier entries existed, but it never fetched them.
	 *
	 * @return {@code true} when the comparison was offered and not done
	 */
	public boolean earlierFeedbackUnchecked() {
		return compares && !earlierFeedbackChecked;
	}

	private List<ToolCallback> comparisonTools() {
		return List.of(
				FunctionToolCallback.builder("get_earlier_feedback", (Supplier<String>) this::earlierFeedback)
						.description("List the feedback entries raised on earlier versions of this document, each with "
								+ "its ref and whether the student marked it resolved (completed) or not (open).")
						.toolCallResultConverter(PLAIN_TEXT)
						.build(),
				FunctionToolCallback.builder("record_follow_up", (Function<FollowUpArg, String>) this::recordFollowUp)
						.description("For a COMPLETED earlier entry whose defect still occurs: record ONE follow-up that "
								+ "explains the defect more explicitly, with an example of a conforming passage. The "
								+ "quote must be copied verbatim from the new version.")
						.inputType(FollowUpArg.class)
						.toolCallResultConverter(PLAIN_TEXT)
						.build(),
				FunctionToolCallback.builder("mark_resolved", (Function<RefArg, String>) this::markResolved)
						.description("For a COMPLETED earlier entry whose defect the new version no longer shows. Only "
								+ "for an entry you actually checked.")
						.inputType(RefArg.class)
						.toolCallResultConverter(PLAIN_TEXT)
						.build());
	}

	private List<ToolCallback> reviewTools() {
		return List.of(
				FunctionToolCallback.builder("list_review_categories", (Supplier<String>) this::listCategories)
						.description("List the review dimensions this review must cover, with the id to pass to get_category_rules.")
						.toolCallResultConverter(PLAIN_TEXT)
						.build(),
				FunctionToolCallback.builder("get_category_rules", (Function<CategoryArg, String>) this::categoryRules)
						.description("Get the research group's rules for one review dimension, and what to check for it. "
								+ "You must call this before judging that dimension; it is the only place those rules exist.")
						.inputType(CategoryArg.class)
						.toolCallResultConverter(PLAIN_TEXT)
						.build(),
				FunctionToolCallback.builder("get_page_text", (Function<PageArg, String>) this::pageText)
						.description("Return the exact extracted text of one page, to read a section or verify a quote.")
						.inputType(PageArg.class)
						.toolCallResultConverter(PLAIN_TEXT)
						.build(),
				FunctionToolCallback.builder("search_document", (Function<QueryArg, String>) this::search)
						.description("Find where a term or phrase occurs in the document. Returns the matching pages with context.")
						.inputType(QueryArg.class)
						.toolCallResultConverter(PLAIN_TEXT)
						.build(),
				FunctionToolCallback.builder("record_finding", (Function<FindingArg, String>) this::recordFinding)
						.description("Record ONE grounded, actionable issue. The quote must be copied verbatim from the document.")
						.inputType(FindingArg.class)
						.toolCallResultConverter(PLAIN_TEXT)
						.build(),
				FunctionToolCallback.builder("finish_review", (Function<FinishArg, String>) this::finishReview)
						.description("End the review with a short summary, an integer score from 0 to 100, and an "
								+ "overall assessment that is exactly one of: good, acceptable, needs-work.")
						.inputType(FinishArg.class)
						// Not returnDirect: Spring AI ANDs that flag across a turn's tool calls, so it
						// would not stop a turn that also records findings. AgenticReviewer stops the
						// loop on any turn that calls finish_review instead.
						.toolCallResultConverter(PLAIN_TEXT)
						.build());
	}

	/**
	 * Whether the agent has called {@code finish_review}. The reviewer stops the loop on this
	 * rather than waiting for the model to fall silent.
	 *
	 * @return {@code true} once the verdict is in
	 */
	public boolean finished() {
		return finished;
	}

	/**
	 * The review categories the agent never fetched rules for, in declaration order.
	 *
	 * @return the unchecked categories; empty when the run covered everything
	 */
	public List<ReviewCategory> uncovered() {
		List<ReviewCategory> missing = new ArrayList<>();
		for (ReviewCategory category : ReviewCategory.values()) {
			if (!covered.contains(category)) {
				missing.add(category);
			}
		}
		return List.copyOf(missing);
	}

	/**
	 * Number of categories the agent actually checked.
	 *
	 * @return the covered category count
	 */
	public int coveredCount() {
		return covered.size();
	}

	/**
	 * Closes the run: reports every unchecked category as a check that did not happen, and returns
	 * what the agent produced.
	 *
	 * <p>Call exactly once, after the agent has stopped for whatever reason. The result is
	 * unqualified — {@link IncompleteCoverage} decides what an incomplete run may still claim.
	 *
	 * @return the agent's verdict and findings
	 */
	public ReviewResult close() {
		for (ReviewCategory category : uncovered()) {
			progress.stepFailed(category.getSlug(), stepIndex(category), totalSteps, NOT_CHECKED_MESSAGE);
		}
		progress.stepStarted(FINALIZE_STEP_ID, "Consolidating findings", totalSteps, totalSteps);
		if (compares) {
			if (earlierFeedbackChecked) {
				progress.stepCompleted(EARLIER_FEEDBACK_STEP_ID, totalSteps - 1, totalSteps);
			} else {
				progress.stepFailed(EARLIER_FEEDBACK_STEP_ID, totalSteps - 1, totalSteps, NOT_CHECKED_MESSAGE);
			}
		}
		List<String> resolvedRefs;
		synchronized (resolved) {
			// A ref that was both marked resolved and followed up is contradictory; the follow-up,
			// which carries a grounded quote, wins.
			resolvedRefs = resolved.stream().filter(ref -> !followUps.containsKey(ref)).toList();
		}
		List<FollowUp> recordedFollowUps;
		synchronized (followUps) {
			recordedFollowUps = List.copyOf(followUps.values());
		}
		ReviewResult result = new ReviewResult(assessment, score, summary, List.copyOf(findings), recordedFollowUps,
				resolvedRefs);
		progress.stepCompleted(FINALIZE_STEP_ID, totalSteps, totalSteps);
		return result;
	}

	// --- Tool argument shapes; the JSON schema the model sees is generated from these ----------

	/**
	 * Argument of {@code get_category_rules}.
	 *
	 * @param category the review dimension's id
	 */
	public record CategoryArg(String category) {
	}

	/**
	 * Argument of {@code get_page_text}.
	 *
	 * @param page the 1-based page number
	 */
	public record PageArg(Integer page) {
	}

	/**
	 * Argument of {@code search_document}.
	 *
	 * @param query the term or phrase to look for
	 */
	public record QueryArg(String query) {
	}

	/**
	 * Argument of {@code record_finding}. Flat rather than nested because a nested location object
	 * is the first thing a mid-size model gets wrong in a generated tool schema.
	 *
	 * @param severity    CRITICAL, MAJOR, MINOR, or SUGGESTION
	 * @param category    the finding's category token (STRUCTURE, WRITING, CITATION, ...)
	 * @param title       a one-line summary of the issue
	 * @param description what is wrong and what to do about it
	 * @param section     the section or chapter the issue is in; may be omitted
	 * @param quote       a verbatim excerpt evidencing the issue; omitted only for an absence
	 * @param page        the 1-based page the issue is on; may be omitted
	 */
	public record FindingArg(String severity, String category, String title, String description,
			String section, String quote, Integer page) {
	}

	/**
	 * Argument of {@code finish_review}.
	 *
	 * @param assessment the overall verdict; exactly one of {@code good}, {@code acceptable} or
	 *                   {@code needs-work}. Anything else is read as "no verdict" rather than
	 *                   failing the run, so the vocabulary is stated in the tool description and in
	 *                   the system prompt — a model left to invent a token costs the review its
	 *                   assessment silently.
	 * @param score      a 0-100 quality score
	 * @param summary    a two-to-three sentence summary of strengths and weaknesses
	 */
	public record FinishArg(String assessment, Integer score, String summary) {
	}

	/**
	 * Argument of {@code record_follow_up}. Flat for the same reason as {@link FindingArg}.
	 *
	 * @param ref         the earlier entry's ref, e.g. {@code E3}
	 * @param title       a one-line restatement of the defect
	 * @param description a more explicit explanation, with an example of a conforming passage
	 * @param section     the section of the new version the defect is in; may be omitted
	 * @param quote       a verbatim excerpt of the new version; omitted only for an absence
	 * @param page        the 1-based page of the new version; may be omitted
	 */
	public record FollowUpArg(String ref, String title, String description, String section, String quote,
			Integer page) {
	}

	/**
	 * Argument of {@code mark_resolved}.
	 *
	 * @param ref the earlier entry's ref, e.g. {@code E3}
	 */
	public record RefArg(String ref) {
	}

	// --- Tool implementations ------------------------------------------------------------------

	private String listCategories() {
		meter.recordToolCall();
		StringBuilder sb = new StringBuilder("Review dimensions to cover (call get_category_rules with the id):\n");
		for (ReviewCategory category : ReviewCategory.values()) {
			sb.append("- ").append(category.getSlug()).append(": ").append(category.getDisplayName())
					.append(" — ").append(category.getDescription()).append("\n");
		}
		return sb.toString();
	}

	private String categoryRules(CategoryArg arg) {
		meter.recordToolCall();
		ReviewCategory category = resolve(arg.category());
		if (category == null) {
			return "Unknown dimension '" + arg.category() + "'. Call list_review_categories for the valid ids.";
		}
		if (covered.add(category)) {
			progress.stepCompleted(category.getSlug(), stepIndex(category), totalSteps);
		}
		return Prompts.taskPromptFor(category, reviewType) + "\n\n"
				+ GuidelinesPrompt.forCategory(guidelines, category);
	}

	private String pageText(PageArg arg) {
		meter.recordToolCall();
		int page = arg.page() == null ? -1 : arg.page();
		if (page < 1 || page > pages.size()) {
			return "Page " + arg.page() + " does not exist; the document has pages 1-" + pages.size() + ".";
		}
		return fence("=== PAGE " + page + " ===\n" + pages.get(page - 1));
	}

	private String search(QueryArg arg) {
		meter.recordToolCall();
		String query = arg.query() == null ? "" : arg.query().strip();
		if (query.isEmpty()) {
			return "Provide a non-empty query.";
		}

		// Both sides of the search run through Grounding.normalize, and so do the snippets handed
		// back: a quote the agent copies out of a snippet is then exactly what record_finding's
		// grounding check will look for, instead of a near-miss the check would reject.
		String needle = Grounding.normalize(query);
		StringBuilder sb = new StringBuilder();
		int hits = 0;
		for (int i = 0; i < pages.size() && hits < MAX_SEARCH_HITS; i++) {
			String page = Grounding.normalize(pages.get(i));
			int at = page.indexOf(needle);
			if (at < 0) {
				continue;
			}
			int from = Math.max(0, at - SEARCH_CONTEXT_BEFORE);
			int to = Math.min(page.length(), at + needle.length() + SEARCH_CONTEXT_AFTER);
			sb.append("page ").append(i + 1).append(": …").append(page, from, to).append("…\n");
			hits++;
		}
		if (hits == 0) {
			return "'" + query + "' does not occur in the document.";
		}
		return fence(sb.toString());
	}

	private String recordFinding(FindingArg arg) {
		meter.recordToolCall();
		String title = strip(arg.title());
		if (title == null) {
			return "Rejected: a finding needs a title. Nothing was recorded.";
		}

		String quote = strip(arg.quote());
		if (quote != null && !Grounding.isGrounded(pages, quote)) {
			// Checked in code rather than trusted: a fabricated quote is the dominant failure mode
			// of this kind of run, and it is the one thing about a finding that can be verified
			// without another model call.
			log.debug("Rejected an ungrounded finding: {}", title);
			return "Rejected: that quote does not appear in the document. Use get_page_text or search_document to "
					+ "find the exact wording, then record the finding again. Nothing was recorded.";
		}

		Integer page = arg.page() != null && arg.page() >= 1 && arg.page() <= pages.size() ? arg.page() : null;
		String section = strip(arg.section());
		List<Location> locations = page == null && section == null && quote == null
				? List.of()
				: List.of(new Location(page, section, quote));

		findings.add(new Finding(upper(arg.severity()), upper(arg.category()), title, strip(arg.description()),
				locations));
		return "Recorded finding #" + findings.size() + ".";
	}

	private String earlierFeedback() {
		meter.recordToolCall();
		earlierFeedbackChecked = true;
		return EarlierFeedbackPrompt.fenced(earlierFeedback, entry -> entry.completed() ? "completed" : "open");
	}

	private String recordFollowUp(FollowUpArg arg) {
		meter.recordToolCall();
		EarlierFeedback entry = earlierEntry(arg.ref());
		if (entry == null) {
			return "Rejected: there is no earlier entry '" + arg.ref() + "'. Call get_earlier_feedback for the refs.";
		}
		if (!entry.completed()) {
			return "Rejected: " + entry.ref() + " is still open — the student still sees it, so do not repeat it. "
					+ "Nothing was recorded.";
		}
		String title = strip(arg.title());
		if (title == null) {
			return "Rejected: a follow-up needs a title. Nothing was recorded.";
		}
		String quote = strip(arg.quote());
		if (quote != null && !Grounding.isGrounded(pages, quote)) {
			return "Rejected: that quote does not appear in the new version. Use get_page_text or search_document to "
					+ "find the exact wording, then record the follow-up again. Nothing was recorded.";
		}
		// The page is taken from where the quote actually is, so the follow-up never points at a page
		// the student cannot find its quote on.
		Integer page = Grounding.pageOf(pages, quote, arg.page());
		String section = strip(arg.section());
		List<Location> locations = page == null && section == null && quote == null
				? List.of()
				: List.of(new Location(page, section, quote));
		// Same labels as the original entry: it is the same defect.
		Finding finding = new Finding(entry.severity(), entry.category(), title, strip(arg.description()), locations);
		if (followUps.putIfAbsent(entry.ref(), new FollowUp(entry.ref(), finding)) != null) {
			return "Rejected: " + entry.ref() + " already has a follow-up. Nothing was recorded.";
		}
		return "Recorded a follow-up to " + entry.ref() + ".";
	}

	private String markResolved(RefArg arg) {
		meter.recordToolCall();
		EarlierFeedback entry = earlierEntry(arg.ref());
		if (entry == null) {
			return "Rejected: there is no earlier entry '" + arg.ref() + "'. Call get_earlier_feedback for the refs.";
		}
		if (!entry.completed()) {
			return "Ignored: " + entry.ref() + " is still open; only the student can mark it resolved.";
		}
		resolved.add(entry.ref());
		return "Marked " + entry.ref() + " as resolved.";
	}

	private EarlierFeedback earlierEntry(String ref) {
		if (ref == null) {
			return null;
		}
		String wanted = ref.strip();
		return earlierFeedback.stream().filter(entry -> entry.ref().equalsIgnoreCase(wanted)).findFirst().orElse(null);
	}

	private String finishReview(FinishArg arg) {
		meter.recordToolCall();
		assessment = AssessmentCategory.fromJson(arg.assessment());
		score = arg.score();
		summary = strip(arg.summary());
		finished = true;
		return "Review closed.";
	}

	// --- Helpers --------------------------------------------------------------------------------

	private static int stepIndex(ReviewCategory category) {
		return category.ordinal() + 1;
	}

	private static ReviewCategory resolve(String slug) {
		if (slug == null) {
			return null;
		}
		String normalized = slug.strip().replace('_', '-');
		for (ReviewCategory category : ReviewCategory.values()) {
			if (category.getSlug().equalsIgnoreCase(normalized) || category.name().equalsIgnoreCase(slug.strip())) {
				return category;
			}
		}
		return null;
	}

	private static String fence(String body) {
		return "<" + PAGE_TEXT_TAG + ">\n" + body.strip() + "\n</" + PAGE_TEXT_TAG + ">";
	}

	private static String strip(String value) {
		if (value == null) {
			return null;
		}
		String stripped = value.strip();
		return stripped.isEmpty() ? null : stripped;
	}

	private static String upper(String value) {
		String stripped = strip(value);
		return stripped == null ? null : stripped.toUpperCase(Locale.ROOT);
	}
}
