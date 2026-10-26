package de.tum.cit.aet.thesis.core.session.service;

import java.util.Locale;

/**
 * Reduces a User-Agent header to browser family and operating system family. The full header changes with
 * every browser update, so it cannot be used to recognise a browser during a session; this coarse signature
 * stays the same across updates and changes only when a different browser or system presents the cookie.
 */
public final class UserAgentSignature {
	private static final String OTHER = "other";

	private UserAgentSignature() {
	}

	/**
	 * Computes the signature.
	 *
	 * @param userAgent the User-Agent header, may be null
	 * @return {@code browser|os}, for example {@code chrome|macos}
	 */
	public static String of(String userAgent) {
		if (userAgent == null || userAgent.isBlank()) {
			return OTHER + "|" + OTHER;
		}

		return browser(userAgent) + "|" + operatingSystem(userAgent);
	}

	/**
	 * Turns a signature into a short label for the active sessions list.
	 *
	 * @param signature a value produced by {@link #of(String)}, may be null
	 * @return for example "Chrome on macOS", or "Unknown device"
	 */
	public static String label(String signature) {
		if (signature == null || !signature.contains("|")) {
			return "Unknown device";
		}

		String[] parts = signature.split("\\|", 2);
		String browser = browserName(parts[0]);
		String system = systemName(parts[1]);

		if (browser == null && system == null) {
			return "Unknown device";
		}

		if (system == null) {
			return browser;
		}

		return (browser == null ? "Unknown browser" : browser) + " on " + system;
	}

	private static String browser(String ua) {
		if (ua.contains("Edg/") || ua.contains("EdgA/") || ua.contains("EdgiOS/")) {
			return "edge";
		}

		if (ua.contains("OPR/") || ua.contains("Opera")) {
			return "opera";
		}

		if (ua.contains("Firefox/") || ua.contains("FxiOS/")) {
			return "firefox";
		}

		if (ua.contains("Chrome/") || ua.contains("CriOS/")) {
			return "chrome";
		}

		if (ua.contains("Safari/") && ua.contains("Version/")) {
			return "safari";
		}

		return OTHER;
	}

	private static String operatingSystem(String ua) {
		if (ua.contains("iPhone") || ua.contains("iPad") || ua.contains("iPod")) {
			return "ios";
		}

		if (ua.contains("Android")) {
			return "android";
		}

		if (ua.contains("Windows")) {
			return "windows";
		}

		if (ua.contains("CrOS")) {
			return "chromeos";
		}

		if (ua.contains("Macintosh") || ua.contains("Mac OS X")) {
			return "macos";
		}

		if (ua.contains("Linux") || ua.contains("X11")) {
			return "linux";
		}

		return OTHER;
	}

	private static String browserName(String browser) {
		return switch (browser) {
			case "chrome" -> "Chrome";
			case "firefox" -> "Firefox";
			case "safari" -> "Safari";
			case "edge" -> "Edge";
			case "opera" -> "Opera";
			default -> null;
		};
	}

	private static String systemName(String system) {
		return switch (system.toLowerCase(Locale.ROOT)) {
			case "macos" -> "macOS";
			case "windows" -> "Windows";
			case "ios" -> "iOS";
			case "android" -> "Android";
			case "linux" -> "Linux";
			case "chromeos" -> "ChromeOS";
			default -> null;
		};
	}
}
