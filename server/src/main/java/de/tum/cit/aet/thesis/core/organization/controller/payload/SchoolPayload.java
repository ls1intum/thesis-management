package de.tum.cit.aet.thesis.core.organization.controller.payload;

public record SchoolPayload(
		String name,
		String abbreviation,
		String websiteUrl,
		String thesisPortalUrl,
		Boolean active
) {
}
