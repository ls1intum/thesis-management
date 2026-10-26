package de.tum.cit.aet.thesis.core.session.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserAgentSignatureTest {
	private static final String CHROME_MAC_130 = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";
	private static final String CHROME_MAC_131 = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
	private static final String FIREFOX_MAC = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:132.0) Gecko/20100101 Firefox/132.0";
	private static final String CHROME_WINDOWS = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";
	private static final String EDGE_WINDOWS = CHROME_WINDOWS + " Edg/130.0.0.0";
	private static final String SAFARI_MAC = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15";
	private static final String SAFARI_IPHONE = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1";
	private static final String CHROME_IPHONE = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/130.0.6723.37 Mobile/15E148 Safari/604.1";
	private static final String CHROME_ANDROID = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36";
	private static final String FIREFOX_LINUX = "Mozilla/5.0 (X11; Linux x86_64; rv:132.0) Gecko/20100101 Firefox/132.0";

	@Test
	void browserVersionsDoNotChangeTheSignature() {
		assertThat(UserAgentSignature.of(CHROME_MAC_130)).isEqualTo("chrome|macos");
		assertThat(UserAgentSignature.of(CHROME_MAC_131)).isEqualTo(UserAgentSignature.of(CHROME_MAC_130));
	}

	@Test
	void browserFamilyAndOperatingSystemChangeTheSignature() {
		assertThat(UserAgentSignature.of(FIREFOX_MAC)).isEqualTo("firefox|macos");
		assertThat(UserAgentSignature.of(FIREFOX_MAC)).isNotEqualTo(UserAgentSignature.of(CHROME_MAC_130));
		assertThat(UserAgentSignature.of(CHROME_WINDOWS)).isEqualTo("chrome|windows");
		assertThat(UserAgentSignature.of(CHROME_WINDOWS)).isNotEqualTo(UserAgentSignature.of(CHROME_MAC_130));
	}

	@Test
	void chromiumDerivativesAreNotMistakenForChrome() {
		assertThat(UserAgentSignature.of(EDGE_WINDOWS)).isEqualTo("edge|windows");
		assertThat(UserAgentSignature.of(SAFARI_MAC)).isEqualTo("safari|macos");
	}

	@Test
	void mobileSystemsAreRecognisedBeforeDesktopOnes() {
		assertThat(UserAgentSignature.of(SAFARI_IPHONE)).isEqualTo("safari|ios");
		assertThat(UserAgentSignature.of(CHROME_IPHONE)).isEqualTo("chrome|ios");
		assertThat(UserAgentSignature.of(CHROME_ANDROID)).isEqualTo("chrome|android");
		assertThat(UserAgentSignature.of(FIREFOX_LINUX)).isEqualTo("firefox|linux");
	}

	@Test
	void unknownAndMissingUserAgentsMapToOther() {
		assertThat(UserAgentSignature.of(null)).isEqualTo("other|other");
		assertThat(UserAgentSignature.of("")).isEqualTo("other|other");
		assertThat(UserAgentSignature.of("curl/8.4.0")).isEqualTo("other|other");
	}

	@Test
	void deviceLabelIsReadable() {
		assertThat(UserAgentSignature.label("chrome|macos")).isEqualTo("Chrome on macOS");
		assertThat(UserAgentSignature.label("safari|ios")).isEqualTo("Safari on iOS");
		assertThat(UserAgentSignature.label("other|other")).isEqualTo("Unknown device");
		assertThat(UserAgentSignature.label(null)).isEqualTo("Unknown device");
	}
}
