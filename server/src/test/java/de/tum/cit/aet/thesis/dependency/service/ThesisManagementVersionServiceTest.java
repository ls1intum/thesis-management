package de.tum.cit.aet.thesis.dependency.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import de.tum.cit.aet.thesis.dependency.dto.ThesisManagementVersionDTO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.info.BuildProperties;

import java.util.Properties;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ThesisManagementVersionServiceTest {

	private static final String GITHUB_PATH = "/repos/ls1intum/thesis-management/releases/latest";

	private WireMockServer gitHubMock;

	@BeforeAll
	void startWireMock() {
		gitHubMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
		gitHubMock.start();
	}

	@AfterAll
	void stopWireMock() {
		gitHubMock.stop();
	}

	@BeforeEach
	void resetStubs() {
		gitHubMock.resetAll();
	}

	private ThesisManagementVersionService newService(String currentVersion) {
		Properties props = new Properties();
		props.setProperty("version", currentVersion);
		BuildProperties buildProperties = new BuildProperties(props);
		return new ThesisManagementVersionService(buildProperties, "ls1intum/thesis-management", gitHubMock.baseUrl());
	}

	@Test
	void reportsUpdateAvailableWhenLatestIsNewer() {
		gitHubMock.stubFor(get(urlPathEqualTo(GITHUB_PATH))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("""
								{
								"tag_name": "v5.0.0",
								"html_url": "https://example.com/release/5.0.0",
								"body": "Notes for 5.0.0"
								}""")));

		ThesisManagementVersionDTO version = newService("4.11.0").getVersionInfo();

		assertThat(version.currentVersion()).isEqualTo("4.11.0");
		assertThat(version.latestVersion()).isEqualTo("5.0.0");
		assertThat(version.updateAvailable()).isTrue();
		assertThat(version.releaseUrl()).isEqualTo("https://example.com/release/5.0.0");
		assertThat(version.releaseNotes()).isEqualTo("Notes for 5.0.0");
		assertThat(version.lastChecked()).isNotBlank();
	}

	@Test
	void noUpdateWhenLatestEqualsCurrent() {
		gitHubMock.stubFor(get(urlPathEqualTo(GITHUB_PATH))
				.willReturn(jsonOk("""
						{ "tag_name": "v4.11.0", "html_url": "https://example.com/release/4.11.0" }""")));

		ThesisManagementVersionDTO version = newService("4.11.0").getVersionInfo();

		assertThat(version.updateAvailable()).isFalse();
		assertThat(version.latestVersion()).isEqualTo("4.11.0");
	}

	@Test
	void noUpdateWhenLatestIsOlder() {
		gitHubMock.stubFor(get(urlPathEqualTo(GITHUB_PATH))
				.willReturn(jsonOk("""
						{ "tag_name": "v3.0.0", "html_url": "https://example.com/release/3.0.0" }""")));

		ThesisManagementVersionDTO version = newService("4.11.0").getVersionInfo();

		assertThat(version.updateAvailable()).isFalse();
	}

	@Test
	void stripsLeadingVFromTagName() {
		gitHubMock.stubFor(get(urlPathEqualTo(GITHUB_PATH))
				.willReturn(jsonOk("""
						{ "tag_name": "V5.0.0" }""")));

		ThesisManagementVersionDTO version = newService("4.11.0").getVersionInfo();

		assertThat(version.latestVersion()).isEqualTo("5.0.0");
		assertThat(version.updateAvailable()).isTrue();
	}

	@Test
	void cacheServesSecondCallWithoutHittingGitHub() {
		gitHubMock.stubFor(get(urlPathEqualTo(GITHUB_PATH))
				.willReturn(jsonOk("""
						{ "tag_name": "v4.11.0" }""")));

		ThesisManagementVersionService service = newService("4.11.0");
		service.getVersionInfo();
		service.getVersionInfo();
		service.getVersionInfo();

		gitHubMock.verify(1, getRequestedFor(urlPathEqualTo(GITHUB_PATH)));
	}

	@Test
	void gitHubErrorYieldsNoUpdateWithCurrentVersionOnly() {
		gitHubMock.stubFor(get(urlPathEqualTo(GITHUB_PATH))
				.willReturn(aResponse().withStatus(503)));

		ThesisManagementVersionDTO version = newService("4.11.0").getVersionInfo();

		assertThat(version.currentVersion()).isEqualTo("4.11.0");
		assertThat(version.latestVersion()).isNull();
		assertThat(version.updateAvailable()).isFalse();
	}

	@Test
	void missingTagNameYieldsNoUpdate() {
		gitHubMock.stubFor(get(urlPathEqualTo(GITHUB_PATH))
				.willReturn(jsonOk("""
						{ "html_url": "https://example.com/r" }""")));

		ThesisManagementVersionDTO version = newService("4.11.0").getVersionInfo();

		assertThat(version.latestVersion()).isNull();
		assertThat(version.updateAvailable()).isFalse();
	}

	@Test
	void supportsTwoPartVersionString() {
		gitHubMock.stubFor(get(urlPathEqualTo(GITHUB_PATH))
				.willReturn(jsonOk("""
						{ "tag_name": "v5.0" }""")));

		// "9.2" → padded to "9.2.0" internally
		ThesisManagementVersionDTO version = newService("4.11").getVersionInfo();

		assertThat(version.latestVersion()).isEqualTo("5.0");
		assertThat(version.updateAvailable()).isTrue();
	}

	private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder jsonOk(String body) {
		return aResponse()
				.withStatus(200)
				.withHeader("Content-Type", "application/json")
				.withBody(body);
	}
}
