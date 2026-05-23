package de.tum.cit.aet.thesis.dependency.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * DTO containing Thesis Management version information including update status.
 *
 * @param currentVersion  the currently running version
 * @param latestVersion   the latest available version from GitHub releases
 * @param updateAvailable true if a newer version is available
 * @param releaseUrl      URL to the latest release on GitHub
 * @param releaseNotes    brief description of the latest release
 * @param lastChecked     timestamp when the version was last checked (ISO 8601)
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ThesisManagementVersionDTO(
		String currentVersion,
		String latestVersion,
		boolean updateAvailable,
		String releaseUrl,
		String releaseNotes,
		String lastChecked
) implements Serializable {
}
