package de.tum.cit.aet.thesis.thesis.service;

import de.tum.cit.aet.thesis.core.utility.AbstractExtractor;
import de.tum.cit.aet.thesis.thesis.constants.ThesisAbstractSource;
import de.tum.cit.aet.thesis.thesis.entity.Thesis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Applies an extracted abstract to a thesis, auto-filling it when confident and otherwise
 * recording an editable suggestion, without ever overwriting a human-edited abstract.
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
			apply(thesis, AbstractExtractor.extract(file.getBytes()));
		} catch (Exception e) {
			log.warn("Abstract extraction failed for thesis {}: {}", thesis.getId(), e.getMessage());
		}
	}

	/**
	 * Applies an extraction result to the thesis according to the fill / suggest / overwrite rules.
	 *
	 * @param thesis the thesis to update (mutated in place)
	 * @param result the extraction result
	 */
	public void apply(Thesis thesis, AbstractExtractor.Result result) {
		switch (result.confidence()) {
			case CONFIDENT -> {
				boolean canAutoFill = isBlank(thesis.getAbstractField())
						|| thesis.getAbstractSource() == ThesisAbstractSource.EXTRACTED;
				if (canAutoFill) {
					thesis.setAbstractField(result.html());
					thesis.setAbstractSource(ThesisAbstractSource.EXTRACTED);
					thesis.setAbstractSuggestion(null);
				} else {
					// A human-edited abstract is present: never overwrite, only suggest.
					thesis.setAbstractSuggestion(result.html());
				}
			}
			case UNCERTAIN -> thesis.setAbstractSuggestion(result.html());
			default -> {
				// NONE (or any future value): nothing plausible found — leave the thesis untouched.
			}
		}
	}

	private static boolean isBlank(String html) {
		if (html == null) {
			return true;
		}
		return html.replaceAll("<[^>]*>", "").replace("&nbsp;", " ").isBlank();
	}
}
