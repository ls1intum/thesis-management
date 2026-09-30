package de.tum.cit.aet.thesis.core.group.controller.payload;


import java.util.UUID;

public record CreateResearchGroupPayload(
	String headUsername,
	String name,
	String abbreviation,
	String campus,
	String description,
	String websiteUrl,
	UUID schoolId,
	UUID departmentId
) {
	public CreateResearchGroupPayload(
		String headUsername,
		String name,
		String abbreviation,
		String campus,
		String description,
		String websiteUrl
	) {
		this(headUsername, name, abbreviation, campus, description, websiteUrl, null, null);
	}

}
