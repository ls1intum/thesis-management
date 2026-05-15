package de.tum.cit.aet.thesis.controller.payload;

import java.util.List;
import java.util.UUID;

public record CreateThesisPayload(
		String thesisTitle,
		String thesisType,
		String language,
		List<UUID> studentIds,
		List<String> additionalStudentUsernames,
		List<UUID> supervisorIds,
		List<UUID> examinerIds,
		UUID researchGroupId
) {
	public CreateThesisPayload {
		additionalStudentUsernames = additionalStudentUsernames == null
				? List.of()
				: List.copyOf(additionalStudentUsernames);
	}

	public CreateThesisPayload(
			String thesisTitle,
			String thesisType,
			String language,
			List<UUID> studentIds,
			List<UUID> supervisorIds,
			List<UUID> examinerIds,
			UUID researchGroupId
	) {
		this(thesisTitle, thesisType, language, studentIds, List.of(), supervisorIds, examinerIds, researchGroupId);
	}
}
