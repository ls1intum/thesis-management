package de.tum.cit.aet.thesis.thesis.service;

import de.tum.cit.aet.thesis.core.utility.AbstractExtractor;
import de.tum.cit.aet.thesis.thesis.constants.ThesisAbstractSource;
import de.tum.cit.aet.thesis.thesis.entity.Thesis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Applies an extracted abstract to a thesis. A confident extraction into an empty abstract is
 * filled silently; anything that would replace existing abstract text is instead staged as a
 * suggestion for the student to confirm or deny. An extraction matching the current abstract is
 * ignored so the student is never prompted about a no-op change.
 */
@Service
public class AbstractAutoFillService {

	private static final Logger log = LoggerFactory.getLogger(AbstractAutoFillService.class);

	/**
	 * Extracts the abstract from the uploaded PDF and applies it to the thesis. Any failure
	 * is swallowed so an upload is never broken by extraction.
	 *
	 * @param thesis the thesis to update (mutated in place; the caller persists it)
	 * @param file the uploaded proposal or thesis PDF
	 */
	public void process(Thesis thesis, MultipartFile file) {
		try {
			process(thesis, file.getBytes());
		} catch (Exception e) {
			log.warn("Abstract extraction failed for thesis {}: {}", thesis.getId(), e.getMessage());
		}
	}

	/**
	 * Extracts the abstract from a PDF loaded via a Spring {@link Resource} (e.g. an already-
	 * stored thesis or proposal file) and applies it to the thesis. Mirrors
	 * {@link #process(Thesis, MultipartFile)} so callers that only hold a stored-file resource
	 * (like the AI review pipeline) can trigger abstract auto-fill against the same file.
	 *
	 * @param thesis the thesis to update (mutated in place; the caller persists it)
	 * @param resource the stored proposal or thesis PDF resource
	 */
	public void process(Thesis thesis, Resource resource) {
		try (InputStream in = resource.getInputStream()) {
			process(thesis, in.readAllBytes());
		} catch (Exception e) {
			log.warn("Abstract extraction failed for thesis {}: {}", thesis.getId(), e.getMessage());
		}
	}

	private void process(Thesis thesis, byte[] pdfBytes) {
		apply(thesis, AbstractExtractor.extract(pdfBytes));
	}

	/**
	 * Applies an extraction result to the thesis.
	 *
	 * <ul>
	 *   <li>Nothing plausible found, or the extraction equals the current abstract: the abstract
	 *       is left untouched and any earlier pending suggestion is cleared.</li>
	 *   <li>Confident extraction into an empty abstract: filled silently.</li>
	 *   <li>Anything that would replace existing abstract text, or any uncertain extraction:
	 *       staged as a suggestion for the student to confirm or deny.</li>
	 * </ul>
	 *
	 * @param thesis the thesis to update (mutated in place)
	 * @param result the extraction result
	 */
	public void apply(Thesis thesis, AbstractExtractor.Result result) {
		if (result.confidence() == AbstractExtractor.Confidence.NONE) {
			// Nothing found — drop any earlier suggestion so a stale modal can't resurface.
			thesis.setAbstractSuggestion(null);
			return;
		}

		String extracted = result.html();
		if (isBlank(extracted) || sameText(extracted, thesis.getAbstractField())) {
			// Nothing found, or no change from the current abstract — don't prompt the student,
			// and clear any earlier suggestion so the state matches this upload's outcome.
			thesis.setAbstractSuggestion(null);
			return;
		}

		boolean confidentFillOfEmpty = result.confidence() == AbstractExtractor.Confidence.CONFIDENT
				&& isBlank(thesis.getAbstractField());
		if (confidentFillOfEmpty) {
			thesis.setAbstractField(extracted);
			thesis.setAbstractSource(ThesisAbstractSource.EXTRACTED);
			thesis.setAbstractSuggestion(null);
		} else {
			// Replacing existing text or an uncertain result: let the student confirm or deny.
			thesis.setAbstractSuggestion(extracted);
		}
	}

	private static boolean isBlank(String html) {
		return normalizeText(html).isEmpty();
	}

	private static boolean sameText(String first, String second) {
		return normalizeText(first).equals(normalizeText(second));
	}

	private static String normalizeText(String html) {
		if (html == null) {
			return "";
		}
		return html.replaceAll("<[^>]*>", "").replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
	}
}
