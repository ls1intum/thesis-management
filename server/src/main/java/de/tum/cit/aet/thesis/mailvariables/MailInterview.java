package de.tum.cit.aet.thesis.mailvariables;

import de.tum.cit.aet.thesis.dto.MailVariableDto;

import java.util.List;

/** Mail placeholder model for interview invitation variables. */
public record MailInterview(
		String inviteUrl
) {
	/**
	 * Returns all selectable template variables for interviews.
	 *
	 * @return interview variable descriptors
	 */
	public static List<MailVariableDto> templateVariables() {
		return List.of(
				new MailVariableDto("Interview Invite URL", "[[${inviteUrl}]]", "https://thesis-management.com/interviews/invite/abc123", "Interview")
		);
	}
}
