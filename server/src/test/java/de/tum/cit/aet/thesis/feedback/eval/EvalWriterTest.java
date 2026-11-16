package de.tum.cit.aet.thesis.feedback.eval;

import static org.assertj.core.api.Assertions.assertThat;

import de.tum.cit.aet.thesis.feedback.model.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.model.Finding;
import de.tum.cit.aet.thesis.feedback.model.Location;
import de.tum.cit.aet.thesis.feedback.model.ReviewCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import de.tum.cit.aet.thesis.feedback.review.CategoryCandidates;
import de.tum.cit.aet.thesis.feedback.review.ReviewRunMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

class EvalWriterTest {

	private static final EvalRun RUN =
			new EvalRun("openai/gpt-oss-120b", "agentic", "proposal-01", "PROPOSAL", 2);

	private static final ReviewResult RESULT = new ReviewResult(
			AssessmentCategory.NEEDS_WORK, 55, "Promising but unfinished.",
			List.of(new Finding("MAJOR", "WRITING", "Subject-verb disagreement", "Use does not.",
					List.of(new Location(1, "Abstract", "The system **don't** handle edge cases.")))));

	private static final ReviewRunMetrics METRICS =
			new ReviewRunMetrics("AgenticReviewer", 12, 24, 12, 48000, 3000, 9, 9, 91000);

	@Test
	void oneRunLandsInAPathMadeOfModelApproachDocumentAndSeed(@TempDir Path root) throws IOException {
		try (EvalWriter writer = new EvalWriter(root)) {
			writer.writeRun(RUN, RESULT, METRICS);
		}

		// Comparing two strategies means diffing two paths that differ in exactly one segment.
		Path directory = root.resolve("openai_gpt-oss-120b/agentic/proposal-01/seed-2");
		assertThat(directory.resolve("feedback.md")).exists();
		assertThat(directory.resolve("result.json")).exists();
		assertThat(directory.resolve("metrics.json")).exists();
		assertThat(Files.readString(directory.resolve("result.json"))).contains("Subject-verb disagreement");
	}

	@Test
	void theRenderedFeedbackCarriesTheVerdictTheFindingsAndWhatTheRunCost(@TempDir Path root) throws IOException {
		try (EvalWriter writer = new EvalWriter(root)) {
			writer.writeRun(RUN, RESULT, METRICS);
		}

		String feedback = Files.readString(root.resolve("openai_gpt-oss-120b/agentic/proposal-01/seed-2/feedback.md"));

		assertThat(feedback)
				.contains("# proposal-01 — agentic")
				.contains("**Model:** openai/gpt-oss-120b")
				.contains("**Assessment:** NEEDS_WORK")
				.contains("**Score:** 55")
				.contains("Promising but unfinished.")
				.contains("### 1. Subject-verb disagreement")
				.contains("`MAJOR` · `WRITING`")
				.contains("Page 1, Abstract")
				.contains("12 calls, 24 tool calls")
				.contains("9/9 categories");
	}

	@Test
	void everyRunAlsoAppendsOneFlatLineForAnalysis(@TempDir Path root) throws IOException {
		try (EvalWriter writer = new EvalWriter(root)) {
			writer.writeRun(RUN, RESULT, METRICS);
			writer.writeRun(new EvalRun("m", "category-fan-out", "proposal-01", "PROPOSAL", 1),
					new ReviewResult(null, null, "Partial.", List.of()), null);
		}

		List<String> lines = Files.readAllLines(root.resolve("runs.jsonl"));

		assertThat(lines).hasSize(2);
		assertThat(lines.get(0)).contains("\"approach\":\"agentic\"").contains("\"findingCount\":1");
		assertThat(lines.get(1)).contains("\"approach\":\"category-fan-out\"").contains("\"findingCount\":0");
	}

	@Test
	void aFailedRunIsRecordedRatherThanLeavingAGapInTheGrid(@TempDir Path root) throws IOException {
		try (EvalWriter writer = new EvalWriter(root)) {
			writer.writeFailure(RUN, new IllegalStateException("502 Bad Gateway"));
		}

		// A missing directory is indistinguishable from a sweep that never got that far.
		assertThat(Files.readString(root.resolve("openai_gpt-oss-120b/agentic/proposal-01/seed-2/error.txt")))
				.contains("502 Bad Gateway");
		assertThat(Files.readString(root.resolve("runs.jsonl")))
				.contains("\"status\":\"failed\"")
				.contains("502 Bad Gateway");
	}

	@Test
	void onlyTheManifestMapsAnIdBackToItsFilename(@TempDir Path root, @TempDir Path corpusDir) throws IOException {
		Files.createDirectories(corpusDir.resolve("proposal"));
		Files.writeString(corpusDir.resolve("proposal/Max_Mustermann.pdf"), "%PDF-1.4");
		EvalPlan plan = EvalPlan.parse(new String[]{
				"--models=m", "--corpus=" + corpusDir, "--base-url=http://e", "--api-key=k",
				"--anonymize=true"});
		EvalCorpus corpus = EvalCorpus.discover(corpusDir, plan.anonymize());

		try (EvalWriter writer = new EvalWriter(root)) {
			writer.writeManifest(plan, corpus);
			writer.writeRun(new EvalRun("m", "agentic", "proposal-01", "PROPOSAL", 1), RESULT, METRICS);
		}

		assertThat(Files.readString(root.resolve("manifest.json"))).contains("Max_Mustermann.pdf");
		assertThat(Files.readString(root.resolve("runs.jsonl"))).doesNotContain("Max_Mustermann");
	}

	@Test
	void theTraceKeepsEveryCategorysCandidatesWhatVerificationDroppedAndWhy(@TempDir Path root) throws IOException {
		Finding kept = RESULT.findings().getFirst();
		Finding invented = new Finding("MINOR", "WRITING", "Avoid the first person", "Rewrite it.", List.of());
		CategoryCandidates candidates = new CategoryCandidates(ReviewCategory.WRITING_STYLE, List.of(kept, invented),
				List.of(), List.of(new CategoryCandidates.RejectedFinding(invented, "No such rule.")), List.of(kept),
				false);

		try (EvalWriter writer = new EvalWriter(root)) {
			writer.writeTrace(RUN, List.of(candidates), Map.of("schedule", "java.net.SocketTimeoutException: timeout"));
		}

		String trace = Files.readString(root.resolve("openai_gpt-oss-120b/agentic/proposal-01/seed-2/trace.json"));
		assertThat(trace)
				.contains("\"WRITING_STYLE\"")
				.contains("Avoid the first person")
				.contains("No such rule.")
				.contains("SocketTimeoutException");
	}

	@Test
	void theJudgingMaterialNumbersTheRulesAndKeepsThePageTextAsTheReviewersSawIt(@TempDir Path root)
			throws IOException {
		try (EvalWriter writer = new EvalWriter(root)) {
			writer.writeJudgingMaterial(EvalGuidelines.build(),
					Map.of("proposal-p3", List.of("Title page.", "Abstract text.")));
		}

		String guidelines = Files.readString(root.resolve("guidelines.json"));
		assertThat(guidelines).contains("\"id\" : \"R1\"").contains("\"id\" : \"R20\"")
				.doesNotContain("\"R21\"").contains("\"category\" : \"schedule\"");
		assertThat(Files.readString(root.resolve("documents/proposal-p3.txt")))
				.isEqualTo("=== PAGE 1 ===\nTitle page.\n\n=== PAGE 2 ===\nAbstract text.\n\n");
	}
}
