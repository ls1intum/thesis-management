package de.tum.cit.aet.thesis.dependency.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.SemverException;
import de.tum.cit.aet.thesis.dependency.dto.ThesisManagementVersionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Checks the GitHub releases endpoint for the latest published version of Thesis Management
 * and compares it against the version embedded in the running jar. Results are cached for
 * 24 hours to keep GitHub API calls below the unauthenticated rate limit.
 */
@Service
public class ThesisManagementVersionService {

	private static final Logger log = LoggerFactory.getLogger(ThesisManagementVersionService.class);

	private static final Duration CACHE_TTL = Duration.ofHours(24);

	private final WebClient gitHubWebClient;

	private final BuildProperties buildProperties;

	private final String gitHubRepo;

	private final String gitHubApiBaseUrl;

	private final AtomicReference<CachedVersion> cache = new AtomicReference<>();

	private final Object cacheLock = new Object();

	/**
	 * Creates the version service.
	 *
	 * @param buildProperties  the build properties used to read the currently running version
	 * @param gitHubRepo       the {@code owner/repo} slug used to look up the latest release on GitHub
	 * @param gitHubApiBaseUrl the GitHub API base URL (overridable for tests)
	 */
	public ThesisManagementVersionService(
			BuildProperties buildProperties,
			@Value("${thesis-management.dependency-scan.github-repo:ls1intum/thesis-management}") String gitHubRepo,
			@Value("${thesis-management.dependency-scan.github-api-url:https://api.github.com}") String gitHubApiBaseUrl
	) {
		this.buildProperties = buildProperties;
		this.gitHubRepo = gitHubRepo;
		this.gitHubApiBaseUrl = gitHubApiBaseUrl.replaceAll("/+$", "");
		this.gitHubWebClient = WebClient.builder()
				.defaultHeader("Accept", "application/vnd.github+json")
				.defaultHeader("User-Agent", "ThesisManagement-Version-Check")
				.build();
	}

	/**
	 * @return version info, served from the 24h cache when available
	 */
	public ThesisManagementVersionDTO getVersionInfo() {
		CachedVersion cached = cache.get();
		if (cached != null && !cached.isExpired()) {
			return cached.value();
		}
		// Double-checked synchronization: prevents a thundering herd of concurrent admins
		// (or scheduled jobs running at the same minute) from all calling GitHub when the
		// cache expires.
		synchronized (cacheLock) {
			cached = cache.get();
			if (cached != null && !cached.isExpired()) {
				return cached.value();
			}
			log.info("Fetching latest Thesis Management version from GitHub");
			ThesisManagementVersionDTO result = fetchVersionFromGitHub();
			cache.set(new CachedVersion(result, Instant.now()));
			return result;
		}
	}

	private ThesisManagementVersionDTO fetchVersionFromGitHub() {
		String currentVersion = buildProperties.getVersion();
		String url = gitHubApiBaseUrl + "/repos/" + gitHubRepo + "/releases/latest";
		try {
			GitHubReleaseResponse release = gitHubWebClient.get()
					.uri(url)
					.retrieve()
					.bodyToMono(GitHubReleaseResponse.class)
					.block(Duration.ofSeconds(30));

			if (release == null || release.tagName() == null) {
				return new ThesisManagementVersionDTO(currentVersion, null, false, null, null, Instant.now().toString());
			}

			String latestVersion = normalizeVersion(release.tagName());
			boolean updateAvailable = isNewerVersionAvailable(currentVersion, latestVersion);

			return new ThesisManagementVersionDTO(
					currentVersion,
					latestVersion,
					updateAvailable,
					release.htmlUrl(),
					truncateReleaseNotes(release.body()),
					Instant.now().toString()
			);
		} catch (RuntimeException e) {
			log.error("Failed to fetch latest version from GitHub: {}", e.getMessage());
			return new ThesisManagementVersionDTO(currentVersion, null, false, null, null, Instant.now().toString());
		}
	}

	private String normalizeVersion(String version) {
		if (version == null) {
			return "unknown";
		}
		if (version.startsWith("v") || version.startsWith("V")) {
			return version.substring(1);
		}
		return version;
	}

	private boolean isNewerVersionAvailable(String current, String latest) {
		if (current == null || latest == null || "unknown".equals(current)) {
			return false;
		}
		try {
			Semver currentSemver = parseForComparison(normalizeVersion(current));
			Semver latestSemver = parseForComparison(latest);
			return latestSemver.isGreaterThan(currentSemver);
		} catch (SemverException e) {
			log.debug("Failed to compare versions '{}' and '{}': {}", current, latest, e.getMessage());
			return false;
		}
	}

	/**
	 * Parses a version string for comparison. Two-part inputs ({@code "9.2"}) are padded to
	 * {@code x.y.0} so strict semver4j semantics hold. Anything not exactly two or three
	 * numeric components is rejected to avoid silently tolerating malformed inputs.
	 */
	private Semver parseForComparison(String version) {
		String trimmed = version.trim();
		if (trimmed.matches("\\d+\\.\\d+")) {
			return new Semver(trimmed + ".0");
		}
		if (!trimmed.matches("\\d+\\.\\d+\\.\\d+")) {
			throw new SemverException("Invalid version: " + version);
		}
		return new Semver(trimmed);
	}

	private String truncateReleaseNotes(String notes) {
		if (notes == null) {
			return null;
		}
		int firstNewline = notes.indexOf('\n');
		if (firstNewline > 0 && firstNewline < 500) {
			return notes.substring(0, firstNewline).trim();
		}
		if (notes.length() > 500) {
			return notes.substring(0, 497) + "...";
		}
		return notes.trim();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record GitHubReleaseResponse(
			@JsonProperty("tag_name") String tagName,
			@JsonProperty("html_url") String htmlUrl,
			@JsonProperty("body") String body,
			@JsonProperty("name") String name
	) {
	}

	private record CachedVersion(ThesisManagementVersionDTO value, Instant fetchedAt) {
		boolean isExpired() {
			return Duration.between(fetchedAt, Instant.now()).compareTo(CACHE_TTL) > 0;
		}
	}
}
