package de.tum.cit.aet.thesis.feedback.service;

import de.tum.cit.aet.thesis.feedback.dto.AIFeedbackDraftDTO;
import de.tum.cit.aet.thesis.feedback.model.Finding;
import de.tum.cit.aet.thesis.feedback.model.Location;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackCategory;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

/**
 * Turns a model's {@link Finding} into the shape thesis feedback uses: one plain-text string, a
 * category, and a severity. The severity and category tokens come from an LLM, so an unexpected
 * value is mapped to a catch-all rather than failing a review that has otherwise completed.
 */
final class FeedbackMapper {
	private static final Logger log = LoggerFactory.getLogger(FeedbackMapper.class);

	private FeedbackMapper() {
	}

	/**
	 * Maps one finding to an editable draft.
	 *
	 * @param finding the finding to map
	 * @return the draft the preview flow returns and the auto flow persists
	 */
	static AIFeedbackDraftDTO toDraft(Finding finding) {
		return new AIFeedbackDraftDTO(toFeedbackText(finding), toCategory(finding.category()),
				toSeverity(finding.severity()));
	}

	/**
	 * Collapses a finding into a single feedback string: title, description, then a parenthetical
	 * hint naming the first location so the student knows where to look. Additional locations are
	 * dropped — {@code ThesisFeedback.feedback} is a plain TEXT column and we don't want to explode
	 * it into JSON just for this.
	 *
	 * <p>The text is stored and rendered verbatim (the feedback overview and the request-changes
	 * dialog both show it as plain text), so no Markdown markup is added here.
	 *
	 * @param finding the finding to render
	 * @return the feedback text; never blank
	 */
	static String toFeedbackText(Finding finding) {
		StringBuilder sb = new StringBuilder();
		append(sb, finding.title(), "");
		append(sb, finding.description(), " — ");

		List<Location> locations = finding.locations();
		if (!locations.isEmpty()) {
			String hint = locations.getFirst().describe();
			if (!hint.isEmpty()) {
				append(sb, "(" + hint + ")", " ");
			}
		}

		// Defensive fallback: shouldn't happen since the LLM contract requires a title.
		return sb.isEmpty() ? "AI feedback finding" : sb.toString();
	}

	private static void append(StringBuilder sb, String value, String separator) {
		if (value == null || value.isBlank()) {
			return;
		}
		if (!sb.isEmpty()) {
			sb.append(separator);
		}
		sb.append(value.strip());
	}

	private static ThesisFeedbackCategory toCategory(String category) {
		if (category == null || category.isBlank()) {
			return null;
		}
		try {
			return ThesisFeedbackCategory.valueOf(category.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			log.warn("Unknown AI category '{}' — mapping to OTHER", category);
			return ThesisFeedbackCategory.OTHER;
		}
	}

	private static ThesisFeedbackSeverity toSeverity(String severity) {
		if (severity == null || severity.isBlank()) {
			return null;
		}
		try {
			return ThesisFeedbackSeverity.valueOf(severity.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			log.warn("Unknown AI severity '{}' — mapping to MINOR", severity);
			return ThesisFeedbackSeverity.MINOR;
		}
	}
}
