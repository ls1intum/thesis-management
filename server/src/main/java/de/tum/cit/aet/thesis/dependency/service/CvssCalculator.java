package de.tum.cit.aet.thesis.dependency.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Computes the CVSS base score from a CVSS v3.x or v4.0 vector string.
 * <p>
 * OSV populates {@code severity[].score} with the full vector
 * (e.g. {@code "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"}), not a number,
 * so we need this conversion to drive {@code mapCvssScoreToSeverity}. Falls
 * back to {@code null} on malformed input.
 * <p>
 * Implements the official formulas from
 * <a href="https://www.first.org/cvss/v3.1/specification-document">CVSS v3.1
 * specification §7.1</a>. For CVSS 4.0 it returns the worst-case base score
 * derived from the impact and exploitability sub-scores (a faithful 4.0
 * implementation requires the macro-vector lookup tables and is significantly
 * more code; the approximation is sufficient for the severity bucketing the
 * admin page does).
 */
final class CvssCalculator {

	private CvssCalculator() {
	}

	/**
	 * Parses a CVSS vector and returns its base score, or {@code null} if the input is not a
	 * valid CVSS 2.0/3.x/4.0 vector.
	 *
	 * @param vector e.g. {@code "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"}
	 * @return the base score in {@code [0.0, 10.0]}, or {@code null} on parse failure
	 */
	static Double scoreFromVector(String vector) {
		if (vector == null) {
			return null;
		}
		String trimmed = vector.trim();
		if (!trimmed.startsWith("CVSS:")) {
			return null;
		}
		Map<String, String> metrics = parseMetrics(trimmed);
		if (metrics.isEmpty()) {
			return null;
		}

		if (trimmed.startsWith("CVSS:3")) {
			return scoreCvss3(metrics);
		}
		if (trimmed.startsWith("CVSS:4")) {
			return scoreCvss4Approximation(metrics);
		}
		// CVSS 2.0 and older variants aren't worth a separate code path here — the rough
		// heuristic in VulnerabilityService.estimateSeverityFromCvssVector still applies.
		return null;
	}

	private static Map<String, String> parseMetrics(String vector) {
		Map<String, String> metrics = new HashMap<>();
		String[] parts = vector.split("/");
		// First token is the version prefix (e.g. CVSS:3.1) — skip it.
		for (int i = 1; i < parts.length; i++) {
			String[] kv = parts[i].split(":", 2);
			if (kv.length == 2) {
				metrics.put(kv[0], kv[1]);
			}
		}
		return metrics;
	}

	// --- CVSS 3.x ---

	private static Double scoreCvss3(Map<String, String> m) {
		Double av = exploitMetric("AV", m, "N", 0.85, "A", 0.62, "L", 0.55, "P", 0.2);
		Double ac = exploitMetric("AC", m, "L", 0.77, "H", 0.44);
		Double ui = exploitMetric("UI", m, "N", 0.85, "R", 0.62);
		Double c = impactMetric("C", m);
		Double i = impactMetric("I", m);
		Double a = impactMetric("A", m);
		String scope = m.get("S");
		Double pr = privilegesRequired(m.get("PR"), scope);
		if (av == null || ac == null || ui == null || c == null || i == null || a == null || pr == null || scope == null) {
			return null;
		}

		double iscBase = 1 - ((1 - c) * (1 - i) * (1 - a));
		double isc;
		if ("U".equals(scope)) {
			isc = 6.42 * iscBase;
		} else if ("C".equals(scope)) {
			isc = 7.52 * (iscBase - 0.029) - 3.25 * Math.pow(iscBase - 0.02, 15);
		} else {
			return null;
		}

		if (isc <= 0) {
			return 0.0;
		}

		double exploitability = 8.22 * av * ac * pr * ui;
		double raw = "U".equals(scope)
				? Math.min(isc + exploitability, 10)
				: Math.min(1.08 * (isc + exploitability), 10);
		return roundUp(raw);
	}

	private static Double exploitMetric(String key, Map<String, String> m, Object... codeValuePairs) {
		String code = m.get(key);
		if (code == null) {
			return null;
		}
		for (int i = 0; i < codeValuePairs.length; i += 2) {
			if (code.equals(codeValuePairs[i])) {
				return (Double) codeValuePairs[i + 1];
			}
		}
		return null;
	}

	private static Double impactMetric(String key, Map<String, String> m) {
		String code = m.get(key);
		if (code == null) {
			return null;
		}
		return switch (code) {
			case "H" -> 0.56;
			case "L" -> 0.22;
			case "N" -> 0.0;
			default -> null;
		};
	}

	private static Double privilegesRequired(String code, String scope) {
		if (code == null || scope == null) {
			return null;
		}
		boolean changed = "C".equals(scope);
		return switch (code) {
			case "N" -> 0.85;
			case "L" -> changed ? 0.68 : 0.62;
			case "H" -> changed ? 0.50 : 0.27;
			default -> null;
		};
	}

	/**
	 * CVSS 3.1 §7.1 roundup: round to one decimal, but always round up if there is any
	 * non-zero second-decimal remainder.
	 */
	private static double roundUp(double value) {
		long scaled = Math.round(value * 100_000);
		if (scaled % 10_000 == 0) {
			return scaled / 100_000.0;
		}
		return (Math.floorDiv(scaled, 10_000) + 1) / 10.0;
	}

	// --- CVSS 4.0 (approximation) ---

	/**
	 * Approximate CVSS 4.0 base score by deriving severity buckets from impact + attack
	 * vector metrics. CVSS 4.0's official scoring uses a macro-vector lookup table that
	 * would require hundreds of lines to embed; for the admin page's purpose (mapping to
	 * CRITICAL/HIGH/MEDIUM/LOW buckets) a heuristic that returns a representative score
	 * per bucket is adequate.
	 */
	private static Double scoreCvss4Approximation(Map<String, String> m) {
		int highImpact = 0;
		if ("H".equals(m.get("VC"))) {
			highImpact++;
		}
		if ("H".equals(m.get("VI"))) {
			highImpact++;
		}
		if ("H".equals(m.get("VA"))) {
			highImpact++;
		}
		int lowImpact = 0;
		if ("L".equals(m.get("VC"))) {
			lowImpact++;
		}
		if ("L".equals(m.get("VI"))) {
			lowImpact++;
		}
		if ("L".equals(m.get("VA"))) {
			lowImpact++;
		}
		boolean network = "N".equals(m.get("AV"));
		boolean noPrivileges = "N".equals(m.get("PR"));
		boolean noInteraction = "N".equals(m.get("UI"));

		if (highImpact == 3 && network && noPrivileges && noInteraction) {
			return 9.8;
		}
		if (highImpact == 3 && network && noPrivileges) {
			return 9.1;
		}
		if (highImpact >= 2 && network) {
			return 7.5;
		}
		if (highImpact >= 1 && network) {
			return noInteraction ? 7.5 : 6.5;
		}
		if (highImpact >= 1) {
			return 5.5;
		}
		if (lowImpact >= 1) {
			return network ? 5.3 : 3.7;
		}
		return null;
	}
}
