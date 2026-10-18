package de.tum.cit.aet.thesis.feedback.eval;

import de.tum.cit.aet.thesis.feedback.model.ReviewType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The fixed set of documents an evaluation sweep runs over.
 *
 * <p>Every artifact the sweep writes is keyed by a document id. By default that id is the
 * filename — {@code proposal-federated-learning} — because the committed corpus is sample material
 * and a readable id is what makes two results trees worth diffing. Point the sweep at real student
 * submissions and that stops being true, so {@code anonymize} replaces the ids with
 * {@code proposal-01}, {@code thesis-02} in sorted filename order; the mapping is then written only
 * to the run manifest, which stays with the operator.
 *
 * <p>Review type comes from the layout: {@code <corpus>/proposal/*.pdf} and
 * {@code <corpus>/thesis/*.pdf}. A flat directory of PDFs is read as proposals, which is the
 * common case and saves setting up folders to try something out.
 *
 * @param documents the documents to review, in a stable order
 */
public record EvalCorpus(List<EvalDocument> documents) {

	/** Longest filename-derived id, before the review-type prefix. */
	private static final int MAX_ID_LENGTH = 48;

	/**
	 * One document in the corpus.
	 *
	 * @param id   the anonymous, stable identifier used in every output path
	 * @param type whether it is reviewed as a proposal or as a thesis
	 * @param file the PDF on disk
	 */
	public record EvalDocument(String id, ReviewType type, Path file) {
	}

	/**
	 * Discovers the corpus under a directory, keeping the filenames as ids.
	 *
	 * @param root the corpus directory
	 * @return the discovered documents
	 * @throws IllegalArgumentException if the directory does not exist or holds no PDF
	 */
	public static EvalCorpus discover(Path root) {
		return discover(root, false);
	}

	/**
	 * Discovers the corpus under a directory.
	 *
	 * @param root      the corpus directory
	 * @param anonymize whether to replace filenames with numbered ids in every output path
	 * @return the discovered documents
	 * @throws IllegalArgumentException if the directory does not exist or holds no PDF
	 */
	public static EvalCorpus discover(Path root, boolean anonymize) {
		if (!Files.isDirectory(root)) {
			throw new IllegalArgumentException("Corpus directory does not exist: " + root);
		}

		List<EvalDocument> documents = new ArrayList<>();
		Path proposals = root.resolve("proposal");
		Path theses = root.resolve("thesis");
		boolean structured = Files.isDirectory(proposals) || Files.isDirectory(theses);

		if (structured) {
			collect(proposals, ReviewType.PROPOSAL, anonymize, documents);
			collect(theses, ReviewType.THESIS, anonymize, documents);
		} else {
			collect(root, ReviewType.PROPOSAL, anonymize, documents);
		}

		if (documents.isEmpty()) {
			throw new IllegalArgumentException("No PDF found under " + root
					+ " (expected <corpus>/proposal/*.pdf and <corpus>/thesis/*.pdf, or a flat directory of proposals)");
		}
		return new EvalCorpus(List.copyOf(documents));
	}

	private static void collect(Path directory, ReviewType type, boolean anonymize, List<EvalDocument> into) {
		if (!Files.isDirectory(directory)) {
			return;
		}
		List<Path> pdfs = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.pdf")) {
			stream.forEach(pdfs::add);
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot read corpus directory " + directory, e);
		}
		pdfs.sort((a, b) -> a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString()));

		String prefix = type.name().toLowerCase(Locale.ROOT);
		Set<String> taken = new HashSet<>();
		int index = 0;
		for (Path pdf : pdfs) {
			index++;
			String id = anonymize ? "%s-%02d".formatted(prefix, index) : prefix + "-" + stemOf(pdf);
			into.add(new EvalDocument(unique(id, taken), type, pdf));
		}
	}

	/**
	 * A filesystem-safe form of a filename, so an id is always a single path segment.
	 *
	 * <p>Truncated at a word boundary, because a thesis PDF is routinely named after its full title
	 * and an id that long makes every path in the results tree unreadable. Truncation can collide,
	 * which {@link #unique} then resolves.
	 */
	private static String stemOf(Path pdf) {
		String name = pdf.getFileName().toString();
		int dot = name.lastIndexOf('.');
		String stem = dot > 0 ? name.substring(0, dot) : name;
		String sanitized = stem.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");

		if (sanitized.length() > MAX_ID_LENGTH) {
			int lastDash = sanitized.lastIndexOf('-', MAX_ID_LENGTH);
			sanitized = sanitized.substring(0, lastDash > MAX_ID_LENGTH / 2 ? lastDash : MAX_ID_LENGTH);
		}
		return sanitized.isEmpty() ? "document" : sanitized;
	}

	/** Two filenames can sanitize to the same id; a sweep must still produce distinct directories. */
	private static String unique(String id, Set<String> taken) {
		if (taken.add(id)) {
			return id;
		}
		int suffix = 2;
		while (!taken.add(id + "-" + suffix)) {
			suffix++;
		}
		return id + "-" + suffix;
	}
}
