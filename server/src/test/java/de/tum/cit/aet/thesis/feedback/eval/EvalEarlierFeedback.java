package de.tum.cit.aet.thesis.feedback.eval;

import de.tum.cit.aet.thesis.feedback.model.EarlierFeedback;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * The earlier feedback a corpus document is compared against, with the ground truth of what the
 * comparison should find — the material the benchmark of the comparison across iterations needs.
 *
 * <p>Lives next to the document it belongs to: {@code proposal-x.pdf} is the <em>new</em> version,
 * {@code proposal-x.earlier.json} the entries raised on the earlier one. A document without that
 * file is reviewed as a first upload, exactly as before. The file reads:
 * <pre>
 * {
 *   "entries": [
 *     { "text": "Subject-verb agreement in the abstract.", "category": "WRITING", "severity": "MINOR",
 *       "completed": true, "expected": "PERSISTS" },
 *     { "text": "Number every figure.", "completed": true, "expected": "RESOLVED" },
 *     { "text": "Add a related work section.", "completed": false }
 *   ]
 * }
 * </pre>
 * {@code expected} is what a correct comparison concludes for a completed entry: the defect still
 * occurs in the new version ({@code PERSISTS}) or it is gone ({@code RESOLVED}). Open entries carry
 * no expectation — the reviewer must leave them alone.
 *
 * <p>The cheapest source of such pairs is synthetic: take a conforming document, inject defects for
 * the earlier version, fix some of them in the new one, and mark some unfixed ones as completed.
 * The ground truth is then known by construction, without new student data.
 *
 * @param entries the earlier entries, in the order they were raised
 */
public record EvalEarlierFeedback(List<Entry> entries) {

	private static final ObjectMapper JSON = JsonMapper.builder().build();

	/** When the synthetic earlier entries count as completed; only its ordering matters. */
	private static final Instant COMPLETED_AT = Instant.parse("2026-01-01T00:00:00Z");

	/** Suffix of the sidecar file, replacing the document's {@code .pdf}. */
	static final String SUFFIX = ".earlier.json";

	/** Normalizes the entry list. */
	public EvalEarlierFeedback {
		entries = entries == null ? List.of() : List.copyOf(entries);
	}

	/**
	 * One earlier entry and its ground truth.
	 *
	 * @param text      the entry's text
	 * @param category  the category token, or {@code null}
	 * @param severity  the severity token, or {@code null}
	 * @param completed whether the student marked it resolved
	 * @param expected  for a completed entry: {@code PERSISTS} or {@code RESOLVED}
	 */
	public record Entry(String text, String category, String severity, boolean completed, String expected) {
	}

	/**
	 * Reads the sidecar of a corpus document, if it has one.
	 *
	 * @param pdf the document
	 * @return the earlier feedback, or empty for a first upload
	 * @throws IllegalArgumentException if the sidecar exists but cannot be read
	 */
	public static Optional<EvalEarlierFeedback> sidecarFor(Path pdf) {
		String name = pdf.getFileName().toString();
		String base = name.toLowerCase(Locale.ROOT).endsWith(".pdf") ? name.substring(0, name.length() - 4) : name;
		Path sidecar = pdf.resolveSibling(base + SUFFIX);
		if (!Files.isRegularFile(sidecar)) {
			return Optional.empty();
		}
		try {
			EvalEarlierFeedback parsed = JSON.readValue(Files.readString(sidecar, StandardCharsets.UTF_8),
					EvalEarlierFeedback.class);
			for (Entry entry : parsed.entries()) {
				if (entry.text() == null || entry.text().isBlank()) {
					throw new IllegalArgumentException("An entry in " + sidecar + " has no text");
				}
				if (entry.completed() && !"PERSISTS".equals(entry.expected()) && !"RESOLVED".equals(entry.expected())) {
					throw new IllegalArgumentException("Completed entry '" + entry.text() + "' in " + sidecar
							+ " needs \"expected\": \"PERSISTS\" or \"RESOLVED\"");
				}
			}
			return Optional.of(parsed);
		} catch (java.io.IOException | tools.jackson.core.JacksonException e) {
			throw new IllegalArgumentException("Cannot read " + sidecar, e);
		}
	}

	/**
	 * The entries as a reviewer receives them, with refs {@code E1}, {@code E2}, ... in file order.
	 * Ids are derived from the refs, so two runs of the same case are directly comparable.
	 *
	 * @return the earlier feedback for the review request
	 */
	public List<EarlierFeedback> toEarlierFeedback() {
		List<EarlierFeedback> earlier = new ArrayList<>();
		for (int i = 0; i < entries.size(); i++) {
			Entry entry = entries.get(i);
			String ref = refOf(i);
			earlier.add(new EarlierFeedback(ref, UUID.nameUUIDFromBytes(ref.getBytes(StandardCharsets.UTF_8)),
					entry.text(), entry.category(), entry.severity(), entry.completed() ? COMPLETED_AT : null, 0,
					false));
		}
		return List.copyOf(earlier);
	}

	/**
	 * The ref an entry gets.
	 *
	 * @param index the entry's 0-based position
	 * @return its ref
	 */
	static String refOf(int index) {
		return "E" + (index + 1);
	}
}
