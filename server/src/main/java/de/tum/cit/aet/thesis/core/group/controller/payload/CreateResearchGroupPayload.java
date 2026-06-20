package de.tum.cit.aet.thesis.core.group.controller.payload;


public record CreateResearchGroupPayload(
	String headUsername,
	String name,
	String abbreviation,
	String campus,
	String description,
	String websiteUrl
) {

}
