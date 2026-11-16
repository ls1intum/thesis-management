package de.tum.cit.aet.thesis.feedback.eval;

import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import de.tum.cit.aet.thesis.feedback.model.Finding;
import de.tum.cit.aet.thesis.feedback.model.Location;
import de.tum.cit.aet.thesis.feedback.model.ReviewCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import de.tum.cit.aet.thesis.feedback.review.CategoryCandidates;
import de.tum.cit.aet.thesis.feedback.review.ReviewRunMetrics;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes an evaluation sweep's results as a tree one can read and a file one can analyse.
 *
 * <pre>
 * &lt;out&gt;/&lt;runId&gt;/
 *   manifest.json                                  the sweep's configuration and id-to-filename map
 *   runs.jsonl                                     one line per review, for plotting and statistics
 *   guidelines.json                                the rules every arm judged by, numbered R1..Rn
 *   documents/&lt;document&gt;.txt                       the page text every reviewer was given
 *   &lt;model&gt;/&lt;approach&gt;/&lt;document&gt;/seed-&lt;n&gt;/
 *     feedback.md                                  the review as a person would read it
 *     result.json                                  the raw ReviewResult
 *     metrics.json                                 what the run cost
 *     trace.json                                   per-category findings before the merge, and failures
 *     error.txt                                    only when the run failed
 * </pre>
 *
 * <p>Both shapes on purpose. The tree is what you actually diff when asking "what did the agent say
 * about this proposal that the fan-out did not" — two paths differing in one segment. The JSONL is
 * what a script reads, and it is flushed after every run, so a sweep that dies on run 40 keeps the
 * first 39.
 *
 * <p>Document ids are anonymous ({@link EvalCorpus}); only {@code manifest.json} maps them back to
 * filenames. Keep that file out of anything you publish if the corpus is confidential.
 */
public class EvalWriter implements AutoCloseable {

	private static final ObjectMapper JSON = JsonMapper.builder()
			.enable(SerializationFeature.INDENT_OUTPUT)
			.build();

	private static final ObjectMapper COMPACT = JsonMapper.builder().build();

	private final Path runRoot;
	private final Writer jsonl;

	/**
	 * Opens a results tree.
	 *
	 * @param runRoot the directory this sweep's results go under
	 */
	public EvalWriter(Path runRoot) {
		this.runRoot = runRoot;
		try {
			Files.createDirectories(runRoot);
			this.jsonl = Files.newBufferedWriter(runRoot.resolve("runs.jsonl"), StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot open the results directory " + runRoot, e);
		}
	}

	/**
	 * The directory this sweep writes to.
	 *
	 * @return the run root
	 */
	public Path runRoot() {
		return runRoot;
	}

	/**
	 * Writes the sweep's configuration and the document id mapping.
	 *
	 * @param plan   the sweep being run
	 * @param corpus the documents it runs over
	 */
	public void writeManifest(EvalPlan plan, EvalCorpus corpus) {
		Map<String, Object> manifest = new LinkedHashMap<>();
		manifest.put("startedAt", java.time.Instant.now().toString());
		manifest.put("models", plan.models());
		manifest.put("approaches", plan.approaches());
		manifest.put("seeds", plan.seeds());
		manifest.put("temperature", plan.temperature());
		manifest.put("baseUrl", plan.baseUrl());
		manifest.put("corpus", plan.corpus().toAbsolutePath().toString());
		manifest.put("anonymized", plan.anonymize());
		manifest.put("thinkingModels", plan.thinkingModels());
		manifest.put("requestTimeout", EvalApplication.REQUEST_TIMEOUT);
		manifest.put("agentMaxTurns", EvalApplication.AGENT_MAX_TURNS);

		Map<String, Object> documents = new LinkedHashMap<>();
		for (EvalCorpus.EvalDocument document : corpus.documents()) {
			documents.put(document.id(), Map.of(
					"type", document.type().name(),
					"file", document.file().toAbsolutePath().toString()));
		}
		manifest.put("documents", documents);

		write(runRoot.resolve("manifest.json"), JSON.writeValueAsString(manifest) + "\n");
	}

	/**
	 * Writes what the judging step needs besides the reviews: the rules, numbered in
	 * {@link ReviewCategory} order so a judgement can cite one by id, and each document's page text
	 * exactly as the reviewers received it, so a quote is checked against the same text it was
	 * taken from.
	 *
	 * @param guidelines the rules every arm of the sweep judged by
	 * @param pageTexts  document id to its per-page text, in document order
	 */
	public void writeJudgingMaterial(StructuredGuidelines guidelines, Map<String, List<String>> pageTexts) {
		List<Map<String, Object>> rules = new java.util.ArrayList<>();
		for (ReviewCategory category : ReviewCategory.values()) {
			for (String rule : guidelines.rulesForCategory(category.getSlug())) {
				Map<String, Object> entry = new LinkedHashMap<>();
				entry.put("id", "R" + (rules.size() + 1));
				entry.put("category", category.getSlug());
				entry.put("categoryName", category.getDisplayName());
				entry.put("text", rule);
				rules.add(entry);
			}
		}
		Map<String, Object> document = new LinkedHashMap<>();
		document.put("overview", guidelines.overview());
		document.put("rules", rules);
		write(runRoot.resolve("guidelines.json"), JSON.writeValueAsString(document) + "\n");

		Path documents = runRoot.resolve("documents");
		try {
			Files.createDirectories(documents);
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot create " + documents, e);
		}
		pageTexts.forEach((id, pages) -> {
			StringBuilder text = new StringBuilder();
			for (int index = 0; index < pages.size(); index++) {
				text.append("=== PAGE ").append(index + 1).append(" ===\n").append(pages.get(index)).append("\n\n");
			}
			write(documents.resolve(id + ".txt"), text.toString());
		});
	}

	/**
	 * Writes what happened inside a run before its result: every category's findings before the
	 * merge, what verification dropped and why, and which categories failed. Written for failed runs
	 * too, where the failures are usually the interesting part.
	 *
	 * @param run              which model, approach, document and seed produced it
	 * @param candidates       the per-category trace; empty for a strategy without categories
	 * @param categoryFailures category slug to the root cause of its failure
	 */
	public void writeTrace(EvalRun run, List<CategoryCandidates> candidates, Map<String, String> categoryFailures) {
		Map<String, Object> trace = new LinkedHashMap<>();
		trace.put("categories", candidates);
		trace.put("categoryFailures", categoryFailures);
		write(directoryFor(run).resolve("trace.json"), JSON.writeValueAsString(trace) + "\n");
	}

	/**
	 * Writes one completed review.
	 *
	 * @param run     which model, approach, document and seed produced it
	 * @param result  the review
	 * @param metrics what the run cost, or {@code null} when the strategy reported none
	 */
	public void writeRun(EvalRun run, ReviewResult result, ReviewRunMetrics metrics) {
		Path directory = directoryFor(run);
		write(directory.resolve("result.json"), JSON.writeValueAsString(result) + "\n");
		write(directory.resolve("feedback.md"), renderFeedback(run, result, metrics));
		if (metrics != null) {
			write(directory.resolve("metrics.json"), JSON.writeValueAsString(metrics) + "\n");
		}

		Map<String, Object> record = baseRecord(run);
		record.put("status", "ok");
		record.put("findingCount", result.findings().size());
		record.put("assessment", result.assessment() == null ? null : result.assessment().name());
		record.put("score", result.normalizedScore());
		record.put("summary", result.summary());
		record.put("findings", result.findings());
		record.put("metrics", metrics);
		appendJsonl(record);
	}

	/**
	 * Writes a review that failed outright.
	 *
	 * @param run     which model, approach, document and seed was being run
	 * @param failure what went wrong
	 */
	public void writeFailure(EvalRun run, Throwable failure) {
		Path directory = directoryFor(run);
		// The whole cause chain, not just the top frame. A review strategy wraps what actually went
		// wrong ("Category review failed for every category") around the provider error that caused
		// it, and the wrapper alone says nothing about whether it was a rate limit, a timeout, or a
		// malformed request — which is exactly what you need when a sweep comes back empty.
		StringBuilder detail = new StringBuilder();
		Throwable current = failure;
		String prefix = "";
		while (current != null) {
			detail.append(prefix).append(current).append("\n");
			for (StackTraceElement element : current.getStackTrace()) {
				detail.append("\tat ").append(element).append("\n");
			}
			current = current.getCause() == current ? null : current.getCause();
			prefix = "\nCaused by: ";
		}
		write(directory.resolve("error.txt"), detail.toString());

		Map<String, Object> record = baseRecord(run);
		record.put("status", "failed");
		record.put("error", failure.toString());
		record.put("rootCause", rootCauseOf(failure));
		appendJsonl(record);
	}

	/**
	 * The innermost cause's description, which is the one that names the actual provider error.
	 *
	 * @param failure the failure
	 * @return the root cause's {@code toString()}
	 */
	static String rootCauseOf(Throwable failure) {
		Throwable current = failure;
		while (current.getCause() != null && current.getCause() != current) {
			current = current.getCause();
		}
		return current.toString();
	}

	@Override
	public void close() {
		try {
			jsonl.close();
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot close " + runRoot.resolve("runs.jsonl"), e);
		}
	}

	private Map<String, Object> baseRecord(EvalRun run) {
		Map<String, Object> record = new LinkedHashMap<>();
		record.put("model", run.model());
		record.put("approach", run.approach());
		record.put("document", run.documentId());
		record.put("documentType", run.documentType());
		record.put("seed", run.seed());
		return record;
	}

	private Path directoryFor(EvalRun run) {
		Path directory = runRoot
				.resolve(EvalPlan.slug(run.model()))
				.resolve(run.approach())
				.resolve(run.documentId())
				.resolve("seed-" + run.seed());
		try {
			Files.createDirectories(directory);
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot create " + directory, e);
		}
		return directory;
	}

	private void appendJsonl(Map<String, Object> record) {
		try {
			jsonl.write(COMPACT.writeValueAsString(record));
			jsonl.write("\n");
			// Flushed per run: a sweep is hours long and a crash must not cost the runs before it.
			jsonl.flush();
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot append to runs.jsonl", e);
		}
	}

	private static void write(Path file, String content) {
		try {
			Files.writeString(file, content, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot write " + file, e);
		}
	}

	/** Renders a review the way a person compares two of them: verdict first, then the findings. */
	private static String renderFeedback(EvalRun run, ReviewResult result, ReviewRunMetrics metrics) {
		StringBuilder md = new StringBuilder();
		md.append("# ").append(run.documentId()).append(" — ").append(run.approach()).append("\n\n");
		md.append("- **Model:** ").append(run.model()).append("\n");
		md.append("- **Document type:** ").append(run.documentType()).append("\n");
		md.append("- **Seed:** ").append(run.seed()).append("\n");
		md.append("- **Assessment:** ").append(result.assessment() == null ? "—" : result.assessment().name())
				.append("\n");
		md.append("- **Score:** ").append(result.normalizedScore() == null ? "—" : result.normalizedScore())
				.append("\n");
		md.append("- **Findings:** ").append(result.findings().size()).append("\n");
		if (metrics != null) {
			md.append("- **Cost:** ").append(metrics.llmCalls()).append(" calls, ")
					.append(metrics.toolCalls()).append(" tool calls, depth ").append(metrics.callDepth())
					.append(", ").append(metrics.promptTokens() + metrics.completionTokens()).append(" tokens, ")
					.append(metrics.wallMillis() / 1000).append("s\n");
			md.append("- **Coverage:** ").append(metrics.categoriesCovered()).append("/")
					.append(metrics.categoriesTotal()).append(" categories\n");
		}

		md.append("\n## Summary\n\n").append(result.summary() == null ? "_none_" : result.summary()).append("\n");

		md.append("\n## Findings\n");
		if (result.findings().isEmpty()) {
			md.append("\n_No findings._\n");
			return md.toString();
		}
		int index = 0;
		for (Finding finding : result.findings()) {
			index++;
			md.append("\n### ").append(index).append(". ").append(blankTo(finding.title(), "(untitled)")).append("\n\n");
			md.append("`").append(blankTo(finding.severity(), "?")).append("` · `")
					.append(blankTo(finding.category(), "?")).append("`\n\n");
			md.append(blankTo(finding.description(), "_no description_")).append("\n");
			appendLocations(md, finding.locations());
		}
		return md.toString();
	}

	private static void appendLocations(StringBuilder md, List<Location> locations) {
		if (locations.isEmpty()) {
			return;
		}
		md.append("\n");
		for (Location location : locations) {
			String where = location.describe();
			md.append("- ").append(where.isEmpty() ? "(no location)" : where);
			if (location.quote() != null && !location.quote().isBlank()) {
				md.append(": “").append(location.quote().strip()).append("”");
			}
			md.append("\n");
		}
	}

	private static String blankTo(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value.strip();
	}
}
