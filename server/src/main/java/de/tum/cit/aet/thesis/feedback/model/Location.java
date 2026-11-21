package de.tum.cit.aet.thesis.feedback.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Where in the reviewed document a finding applies.
 *
 * @param page    1-based page number, or {@code null} when the model did not pin one down
 * @param section the section or chapter heading the finding refers to
 * @param quote   verbatim text from the document that evidences the finding
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Location(Integer page, String section, String quote) {

	/**
	 * Renders this location as a short human-readable hint such as {@code "Page 3, Introduction"}.
	 * The quote is left out: it is often a full sentence and would dominate the feedback text.
	 *
	 * @return the hint, or an empty string when neither a page nor a section is known
	 */
	public String describe() {
		StringBuilder sb = new StringBuilder();
		if (page != null) {
			sb.append("Page ").append(page);
		}
		if (section != null && !section.isBlank()) {
			if (!sb.isEmpty()) {
				sb.append(", ");
			}
			sb.append(section.strip());
		}
		return sb.toString();
	}
}
