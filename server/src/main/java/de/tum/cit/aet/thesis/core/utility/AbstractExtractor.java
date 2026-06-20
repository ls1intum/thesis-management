package de.tum.cit.aet.thesis.core.utility;

import com.itextpdf.kernel.geom.Vector;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Deterministically extracts the abstract section from a thesis or proposal PDF.
 *
 * <p>The extractor reads the PDF text together with position and font-size information,
 * locates an English abstract heading, bounds it at the next section, and rebuilds the
 * paragraphs faithfully (joining line-end hyphenation back into whole words). It only
 * reports {@link Confidence#CONFIDENT} when a clear heading and boundary were found and
 * the result is a sane length; otherwise the caller treats the result as a suggestion.
 */
public final class AbstractExtractor {

	/** Only the first few pages are scanned — abstracts always appear at the front. */
	private static final int MAX_PAGES = 5;
	/** Baselines within this many points are considered the same text line. */
	private static final float LINE_Y_TOLERANCE = 3f;
	/** A new paragraph starts when the vertical gap exceeds this multiple of the median line gap. */
	private static final float PARAGRAPH_GAP_FACTOR = 1.5f;
	/** Minimum plausible abstract length (characters) for a confident result. */
	private static final int MIN_CONFIDENT_LENGTH = 50;
	/** Maximum plausible abstract length (characters) for a confident result. */
	private static final int MAX_CONFIDENT_LENGTH = 2000;
	/** Section headings (other than the abstract) that mark the end of the abstract. */
	private static final Set<String> STOP_HEADINGS = Set.of(
			"introduction", "contents", "table of contents", "acknowledgements",
			"acknowledgments", "keywords", "index terms");
	/** English abstract heading words. */
	private static final Set<String> ABSTRACT_HEADINGS = Set.of("abstract", "summary");
	private static final Pattern NUMBERED_HEADING = Pattern.compile("^\\d+(\\.\\d+)*\\.?\\s+\\S.*");
	private static final Pattern CHAPTER_HEADING = Pattern.compile("^chapter\\s+\\d.*");
	private static final Pattern TRAILING_PUNCTUATION = Pattern.compile("[\\s:.]+$");
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private AbstractExtractor() {
	}

	/** How confident the extractor is that it located a correct abstract. */
	public enum Confidence {
		/** A clear abstract heading and end boundary were found and the result looks sane. */
		CONFIDENT,
		/** An abstract heading was found but the boundary or length is questionable. */
		UNCERTAIN,
		/** No plausible abstract heading was found. */
		NONE
	}

	/**
	 * The outcome of an extraction attempt.
	 *
	 * @param confidence how confident the extractor is in the result
	 * @param html the extracted abstract as paragraph HTML, or empty when nothing was found
	 */
	public record Result(Confidence confidence, String html) {
	}

	private record Chunk(String text, float startX, float endX, float y, float fontSize, int page) {
	}

	private record Line(String text, int page, float y, float fontSize) {
	}

	/**
	 * Extracts the abstract from the given PDF.
	 *
	 * @param pdfBytes the raw PDF content
	 * @return the extraction result
	 */
	public static Result extract(byte[] pdfBytes) {
		List<Line> lines = readLines(pdfBytes);
		if (lines.isEmpty()) {
			return new Result(Confidence.NONE, "");
		}

		float medianFontSize = median(lines.stream().map(Line::fontSize).sorted().toList());

		int headingIndex = findHeadingIndex(lines);
		if (headingIndex < 0) {
			return new Result(Confidence.NONE, "");
		}

		Line heading = lines.get(headingIndex);
		int boundaryIndex = findBoundaryIndex(lines, headingIndex, medianFontSize);
		boolean boundaryFound = boundaryIndex >= 0;
		int end = boundaryFound ? boundaryIndex : lines.size();

		List<Line> body = lines.subList(headingIndex + 1, end);
		List<String> paragraphs = buildParagraphs(body);
		String html = toHtml(paragraphs);
		int plainLength = String.join(" ", paragraphs).length();

		boolean headingStrong = heading.fontSize() >= medianFontSize;
		boolean confident = headingStrong && boundaryFound
				&& plainLength >= MIN_CONFIDENT_LENGTH && plainLength <= MAX_CONFIDENT_LENGTH;

		return new Result(confident ? Confidence.CONFIDENT : Confidence.UNCERTAIN, html);
	}

	private static List<Line> readLines(byte[] pdfBytes) {
		List<Chunk> chunks = new ArrayList<>();

		try (PdfDocument pdf = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfBytes)))) {
			int pages = Math.min(pdf.getNumberOfPages(), MAX_PAGES);
			ChunkCollector collector = new ChunkCollector(chunks);
			PdfCanvasProcessor processor = new PdfCanvasProcessor(collector);

			for (int page = 1; page <= pages; page++) {
				collector.page = page;
				processor.processPageContent(pdf.getPage(page));
				processor.reset();
			}
		} catch (Exception e) {
			// Unreadable / encrypted / image-only PDF — nothing to extract.
			return List.of();
		}

		return groupIntoLines(chunks);
	}

	private static List<Line> groupIntoLines(List<Chunk> chunks) {
		List<Line> lines = new ArrayList<>();
		List<Chunk> sorted = new ArrayList<>(chunks);
		sorted.sort((a, b) -> {
			if (a.page() != b.page()) {
				return Integer.compare(a.page(), b.page());
			}
			if (Math.abs(a.y() - b.y()) > LINE_Y_TOLERANCE) {
				return Float.compare(b.y(), a.y());
			}
			return Float.compare(a.startX(), b.startX());
		});

		List<Chunk> currentParts = new ArrayList<>();
		for (Chunk chunk : sorted) {
			if (chunk.text().isBlank()) {
				continue;
			}
			boolean sameLine = !currentParts.isEmpty()
					&& currentParts.getLast().page() == chunk.page()
					&& Math.abs(currentParts.getLast().y() - chunk.y()) <= LINE_Y_TOLERANCE;
			if (!sameLine && !currentParts.isEmpty()) {
				lines.add(toLine(currentParts));
				currentParts = new ArrayList<>();
			}
			currentParts.add(chunk);
		}
		if (!currentParts.isEmpty()) {
			lines.add(toLine(currentParts));
		}

		return lines;
	}

	private static Line toLine(List<Chunk> parts) {
		parts.sort((a, b) -> Float.compare(a.startX(), b.startX()));
		StringBuilder text = new StringBuilder(parts.getFirst().text());
		float maxFontSize = parts.getFirst().fontSize();

		for (int i = 1; i < parts.size(); i++) {
			Chunk prev = parts.get(i - 1);
			Chunk part = parts.get(i);
			if (part.startX() - prev.endX() > 0.15f * part.fontSize()) {
				text.append(' ');
			}
			text.append(part.text());
			maxFontSize = Math.max(maxFontSize, part.fontSize());
		}

		String collapsed = WHITESPACE.matcher(text.toString()).replaceAll(" ").trim();
		return new Line(collapsed, parts.getFirst().page(), parts.getFirst().y(), maxFontSize);
	}

	private static int findHeadingIndex(List<Line> lines) {
		for (int i = 0; i < lines.size(); i++) {
			if (ABSTRACT_HEADINGS.contains(normalize(lines.get(i).text()))) {
				return i;
			}
		}
		return -1;
	}

	private static int findBoundaryIndex(List<Line> lines, int headingIndex, float medianFontSize) {
		for (int i = headingIndex + 1; i < lines.size(); i++) {
			if (isBoundary(lines.get(i), medianFontSize)) {
				return i;
			}
		}
		return -1;
	}

	private static boolean isBoundary(Line line, float medianFontSize) {
		String normalized = normalize(line.text());
		int words = WHITESPACE.split(line.text().trim()).length;

		if (STOP_HEADINGS.contains(normalized)) {
			return true;
		}
		if ((NUMBERED_HEADING.matcher(normalized).matches() || CHAPTER_HEADING.matcher(normalized).matches())
				&& words <= 5) {
			return true;
		}
		return line.fontSize() > medianFontSize && words <= 6;
	}

	private static List<String> buildParagraphs(List<Line> body) {
		List<String> paragraphs = new ArrayList<>();
		if (body.isEmpty()) {
			return paragraphs;
		}

		float medianGap = medianGap(body);
		List<Line> current = new ArrayList<>();
		current.add(body.getFirst());

		for (int i = 1; i < body.size(); i++) {
			Line prev = body.get(i - 1);
			Line line = body.get(i);
			boolean newParagraph = prev.page() != line.page()
					|| (prev.y() - line.y()) > PARAGRAPH_GAP_FACTOR * medianGap;
			if (newParagraph) {
				paragraphs.add(joinLines(current));
				current = new ArrayList<>();
			}
			current.add(line);
		}
		paragraphs.add(joinLines(current));

		paragraphs.removeIf(String::isBlank);
		return paragraphs;
	}

	private static float medianGap(List<Line> body) {
		List<Float> gaps = new ArrayList<>();
		for (int i = 1; i < body.size(); i++) {
			float gap = body.get(i - 1).y() - body.get(i).y();
			if (gap > 0 && body.get(i - 1).page() == body.get(i).page()) {
				gaps.add(gap);
			}
		}
		gaps.sort(Float::compare);
		return median(gaps);
	}

	private static String joinLines(List<Line> lines) {
		StringBuilder result = new StringBuilder();
		for (Line line : lines) {
			String text = line.text().trim();
			if (text.isEmpty()) {
				continue;
			}
			if (result.isEmpty()) {
				result.append(text);
				continue;
			}
			int last = result.length() - 1;
			boolean hyphenBreak = result.charAt(last) == '-'
					&& last > 0 && Character.isLetter(result.charAt(last - 1));
			if (hyphenBreak && Character.isLowerCase(text.charAt(0))) {
				// Soft line-break hyphen: rejoin into a single word.
				result.deleteCharAt(last).append(text);
			} else if (hyphenBreak) {
				// Hyphen before an uppercase letter or digit: likely a real compound, keep it.
				result.append(text);
			} else {
				result.append(' ').append(text);
			}
		}
		return WHITESPACE.matcher(result.toString()).replaceAll(" ").trim();
	}

	private static String toHtml(List<String> paragraphs) {
		StringBuilder html = new StringBuilder();
		for (String paragraph : paragraphs) {
			html.append("<p>").append(escapeHtml(paragraph)).append("</p>");
		}
		return html.toString();
	}

	private static String escapeHtml(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String normalize(String text) {
		String trimmed = TRAILING_PUNCTUATION.matcher(text.trim()).replaceAll("");
		return trimmed.toLowerCase(Locale.ROOT);
	}

	private static float median(List<Float> sortedValues) {
		if (sortedValues.isEmpty()) {
			return 0f;
		}
		int size = sortedValues.size();
		if (size % 2 == 1) {
			return sortedValues.get(size / 2);
		}
		return (sortedValues.get(size / 2 - 1) + sortedValues.get(size / 2)) / 2f;
	}

	private static final class ChunkCollector implements IEventListener {
		private final List<Chunk> chunks;
		private int page;

		private ChunkCollector(List<Chunk> chunks) {
			this.chunks = chunks;
		}

		@Override
		public void eventOccurred(IEventData data, EventType type) {
			if (type != EventType.RENDER_TEXT || !(data instanceof TextRenderInfo info)) {
				return;
			}
			String text = info.getText();
			if (text == null || text.isEmpty()) {
				return;
			}
			Vector start = info.getBaseline().getStartPoint();
			Vector end = info.getBaseline().getEndPoint();
			chunks.add(new Chunk(
					text,
					start.get(Vector.I1),
					end.get(Vector.I1),
					start.get(Vector.I2),
					info.getFontSize(),
					page));
		}

		@Override
		public Set<EventType> getSupportedEvents() {
			return Set.of(EventType.RENDER_TEXT);
		}
	}
}
