package de.tum.cit.aet.thesis.utility;

import de.tum.cit.aet.thesis.exception.request.ResourceInvalidParametersException;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates email template content to prevent Server-Side Template Injection (SSTI)
 * through Thymeleaf expression evaluation.
 *
 * <p>Uses a two-layer approach:
 * <ol>
 *   <li>Structural patterns that are dangerous in any context (Thymeleaf preprocessing, SpEL expressions)</li>
 *   <li>Patterns only checked inside ${...} expression blocks to avoid false positives on plain text</li>
 * </ol>
 */
public final class TemplateValidator {

	/**
	 * Patterns that are dangerous anywhere in template content — these are Thymeleaf/SpEL
	 * structural constructs that have no legitimate use in email body text.
	 */
	private static final List<Pattern> STRUCTURAL_PATTERNS = List.of(
			// Thymeleaf preprocessing expressions — enables expression composition attacks
			Pattern.compile("__\\$\\{", Pattern.CASE_INSENSITIVE),
			// SpEL type operator — T(java.lang.Runtime) etc.
			Pattern.compile("T\\s*\\(", Pattern.CASE_INSENSITIVE),
			// SpEL constructor — ${new java.io.File(...)}
			Pattern.compile("\\$\\{new\\s", Pattern.CASE_INSENSITIVE),
			// SpEL bean references — #{@beanName}
			Pattern.compile("#\\{", Pattern.CASE_INSENSITIVE),
			// SpEL utility objects — ${#ctx}, ${#request}, etc.
			Pattern.compile("\\$\\{#", Pattern.CASE_INSENSITIVE)
	);

	/**
	 * Keywords checked only inside ${...} expression blocks. These would cause false positives
	 * in plain text (e.g., "the university System" or "email thread") but are dangerous inside expressions.
	 */
	private static final List<Pattern> EXPRESSION_KEYWORDS = List.of(
			Pattern.compile("getClass\\s*\\(", Pattern.CASE_INSENSITIVE),
			Pattern.compile("Class\\.forName", Pattern.CASE_INSENSITIVE),
			Pattern.compile("\\.forName\\s*\\(", Pattern.CASE_INSENSITIVE),
			Pattern.compile("Runtime", Pattern.CASE_INSENSITIVE),
			Pattern.compile("ProcessBuilder", Pattern.CASE_INSENSITIVE),
			Pattern.compile("ProcessHandle", Pattern.CASE_INSENSITIVE),
			Pattern.compile("exec\\s*\\(", Pattern.CASE_INSENSITIVE),
			Pattern.compile("\\.class[.\\s]", Pattern.CASE_INSENSITIVE),
			Pattern.compile("java\\.lang\\.", Pattern.CASE_INSENSITIVE),
			Pattern.compile("java\\.io\\.", Pattern.CASE_INSENSITIVE),
			Pattern.compile("java\\.net\\.", Pattern.CASE_INSENSITIVE),
			Pattern.compile("java\\.util\\.", Pattern.CASE_INSENSITIVE),
			Pattern.compile("javax\\.", Pattern.CASE_INSENSITIVE),
			Pattern.compile("org\\.springframework", Pattern.CASE_INSENSITIVE),
			Pattern.compile("com\\.sun\\.", Pattern.CASE_INSENSITIVE),
			Pattern.compile("jdk\\.", Pattern.CASE_INSENSITIVE),
			Pattern.compile("\\.invoke\\s*\\(", Pattern.CASE_INSENSITIVE),
			Pattern.compile("getMethod\\s*\\(", Pattern.CASE_INSENSITIVE),
			Pattern.compile("getDeclaredMethod\\s*\\(", Pattern.CASE_INSENSITIVE),
			Pattern.compile("ClassLoader", Pattern.CASE_INSENSITIVE),
			Pattern.compile("URLClassLoader", Pattern.CASE_INSENSITIVE),
			Pattern.compile("ScriptEngine", Pattern.CASE_INSENSITIVE),
			Pattern.compile("MethodHandle", Pattern.CASE_INSENSITIVE),
			Pattern.compile("java\\.lang\\.reflect", Pattern.CASE_INSENSITIVE),
			Pattern.compile("Thread\\s*[\\.\\(]", Pattern.CASE_INSENSITIVE),
			Pattern.compile("System\\s*\\.", Pattern.CASE_INSENSITIVE),
			Pattern.compile("Unsafe", Pattern.CASE_INSENSITIVE)
	);

	/** Matches ${...} expression blocks, including nested braces. */
	private static final Pattern EXPRESSION_BLOCK = Pattern.compile("\\$\\{[^}]+}");

	private TemplateValidator() {
	}

	/**
	 * Validates that the given template content does not contain dangerous Thymeleaf expressions
	 * that could lead to server-side template injection.
	 *
	 * @param content the template content to validate (bodyHtml or subject)
	 * @throws ResourceInvalidParametersException if dangerous expressions are detected
	 */
	public static void validateTemplateContent(String content) {
		if (content == null || content.isEmpty()) {
			return;
		}

		// Check structural patterns anywhere in the content
		for (Pattern pattern : STRUCTURAL_PATTERNS) {
			if (pattern.matcher(content).find()) {
				throw new ResourceInvalidParametersException(
						"Template content contains a forbidden expression pattern and cannot be saved.");
			}
		}

		// Check dangerous keywords only inside ${...} expression blocks
		var matcher = EXPRESSION_BLOCK.matcher(content);
		while (matcher.find()) {
			String expression = matcher.group();
			for (Pattern keyword : EXPRESSION_KEYWORDS) {
				if (keyword.matcher(expression).find()) {
					throw new ResourceInvalidParametersException(
							"Template content contains a forbidden expression pattern and cannot be saved.");
				}
			}
		}
	}
}
