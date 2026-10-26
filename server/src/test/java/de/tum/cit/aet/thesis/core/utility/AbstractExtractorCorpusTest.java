package de.tum.cit.aet.thesis.core.utility;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Manual evaluation harness (not a unit test): runs the real {@link AbstractExtractor} over a local
 * corpus of real PDFs and writes a markdown report next to the corpus. It auto-skips when no corpus
 * directory is present, so it stays green in CI.
 *
 * <p>Run against a corpus with:
 * <pre>
 *   ABSTRACT_CORPUS_DIR=/path/to/corpus ./gradlew test --tests "*AbstractExtractorCorpusTest"
 * </pre>
 * The harness self-skips when {@code ABSTRACT_CORPUS_DIR} is unset, so it never runs in CI. The
 * PDFs are real student documents and must never be committed; keep the corpus outside the repo.
 */
class AbstractExtractorCorpusTest {

	@Test
	void evaluateCorpus() throws IOException {
		String dirEnv = System.getenv("ABSTRACT_CORPUS_DIR");
		Assumptions.assumeTrue(dirEnv != null && !dirEnv.isBlank(),
				"Set ABSTRACT_CORPUS_DIR to a folder of real PDFs to run this evaluation harness");
		Path dir = Path.of(dirEnv);

		Assumptions.assumeTrue(Files.isDirectory(dir), "No corpus directory at " + dir);

		List<Path> pdfs;
		try (Stream<Path> walk = Files.walk(dir)) {
			pdfs = walk.filter(Files::isRegularFile)
					.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".pdf"))
					.sorted()
					.toList();
		}
		Assumptions.assumeFalse(pdfs.isEmpty(), "No PDFs found in " + dir);

		List<FileResult> results = new ArrayList<>();
		for (Path pdf : pdfs) {
			AbstractExtractor.Result result;
			String error = null;
			try {
				result = AbstractExtractor.extract(Files.readAllBytes(pdf));
			} catch (Exception e) {
				result = new AbstractExtractor.Result(AbstractExtractor.Confidence.NONE, "");
				error = e.getClass().getSimpleName() + ": " + e.getMessage();
			}
			results.add(new FileResult(dir.relativize(pdf).toString(), result, error));
		}

		String report = buildReport(dir, results);
		Path reportPath = dir.resolve("extraction-report.md");
		Files.writeString(reportPath, report);

		System.out.println(summaryLine(results));
		System.out.println("Wrote report: " + reportPath);
	}

	private static String summaryLine(List<FileResult> results) {
		Map<AbstractExtractor.Confidence, Long> counts = countByConfidence(results);
		return "Corpus: %d PDFs | CONFIDENT=%d UNCERTAIN=%d NONE=%d".formatted(
				results.size(),
				counts.getOrDefault(AbstractExtractor.Confidence.CONFIDENT, 0L),
				counts.getOrDefault(AbstractExtractor.Confidence.UNCERTAIN, 0L),
				counts.getOrDefault(AbstractExtractor.Confidence.NONE, 0L));
	}

	private static Map<AbstractExtractor.Confidence, Long> countByConfidence(List<FileResult> results) {
		Map<AbstractExtractor.Confidence, Long> counts = new TreeMap<>();
		for (FileResult r : results) {
			counts.merge(r.result().confidence(), 1L, Long::sum);
		}
		return counts;
	}

	private static String buildReport(Path dir, List<FileResult> results) {
		// Per top-level subfolder (e.g. proposals / theses) breakdown plus overall.
		Map<String, List<FileResult>> byGroup = new TreeMap<>();
		for (FileResult r : results) {
			int slash = r.relativePath().indexOf('/');
			String group = slash > 0 ? r.relativePath().substring(0, slash) : "(root)";
			byGroup.computeIfAbsent(group, k -> new ArrayList<>()).add(r);
		}

		StringBuilder sb = new StringBuilder();
		sb.append("# Abstract extraction corpus report\n\n");
		sb.append("Corpus: `").append(dir).append("`\n\n");
		sb.append("## Summary\n\n");
		sb.append("| Group | Files | CONFIDENT | UNCERTAIN | NONE |\n");
		sb.append("|-------|------:|----------:|----------:|-----:|\n");
		for (var entry : byGroup.entrySet()) {
			appendCountRow(sb, entry.getKey(), entry.getValue());
		}
		appendCountRow(sb, "**overall**", results);
		sb.append("\n");

		sb.append("## Per-file results\n\n");
		sb.append("| File | Confidence | Chars | Note |\n");
		sb.append("|------|------------|------:|------|\n");
		for (FileResult r : results) {
			sb.append("| `").append(r.relativePath()).append("` | ")
					.append(r.result().confidence()).append(" | ")
					.append(r.result().html() == null ? 0 : r.result().html().length()).append(" | ")
					.append(r.error() == null ? "" : "ERROR " + r.error()).append(" |\n");
		}
		sb.append("\n");

		sb.append("## Extracted text per file\n\n");
		for (FileResult r : results) {
			sb.append("### `").append(r.relativePath()).append("`\n\n");
			sb.append("- confidence: **").append(r.result().confidence()).append("**\n");
			if (r.error() != null) {
				sb.append("- error: ").append(r.error()).append("\n");
			}
			sb.append("\n```html\n");
			sb.append(r.result().html() == null ? "" : r.result().html());
			sb.append("\n```\n\n");
		}
		return sb.toString();
	}

	private static void appendCountRow(StringBuilder sb, String label, List<FileResult> results) {
		Map<AbstractExtractor.Confidence, Long> counts = countByConfidence(results);
		sb.append("| ").append(label).append(" | ").append(results.size()).append(" | ")
				.append(counts.getOrDefault(AbstractExtractor.Confidence.CONFIDENT, 0L)).append(" | ")
				.append(counts.getOrDefault(AbstractExtractor.Confidence.UNCERTAIN, 0L)).append(" | ")
				.append(counts.getOrDefault(AbstractExtractor.Confidence.NONE, 0L)).append(" |\n");
	}

	private record FileResult(String relativePath, AbstractExtractor.Result result, String error) {
	}
}
