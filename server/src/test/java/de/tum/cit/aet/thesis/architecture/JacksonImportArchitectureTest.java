package de.tum.cit.aet.thesis.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Architecture test that ensures only Jackson 3.x (tools.jackson) is used in production code.
 *
 * <p>Jackson 3.x reuses annotation classes from {@code com.fasterxml.jackson.annotation},
 * so those imports are allowed. However, imports from {@code com.fasterxml.jackson.databind}
 * and {@code com.fasterxml.jackson.core} indicate accidental use of Jackson 2.x and must
 * be replaced with their {@code tools.jackson} equivalents.</p>
 */
class JacksonImportArchitectureTest {

	private static final Path MAIN_SRC = Paths.get("src/main/java");

	/**
	 * Pattern matching forbidden Jackson 2.x imports.
	 * Matches {@code com.fasterxml.jackson.core} and {@code com.fasterxml.jackson.databind}
	 * but NOT {@code com.fasterxml.jackson.annotation} (shared with Jackson 3.x).
	 */
	private static final Pattern FORBIDDEN_IMPORT = Pattern.compile(
			"^import\\s+(?:static\\s+)?com\\.fasterxml\\.jackson\\.(core|databind)\\b"
	);

	@Test
	void noJackson2DatabindOrCoreImportsInProductionCode() throws IOException {
		List<String> violations = new ArrayList<>();

		try (Stream<Path> files = Files.walk(MAIN_SRC)) {
			files.filter(p -> p.toString().endsWith(".java")).forEach(file -> {
				try {
					List<String> lines = Files.readAllLines(file);
					for (int i = 0; i < lines.size(); i++) {
						if (FORBIDDEN_IMPORT.matcher(lines.get(i).trim()).find()) {
							String relative = MAIN_SRC.relativize(file).toString();
							violations.add(relative + ":" + (i + 1) + " → " + lines.get(i).trim());
						}
					}
				} catch (IOException e) {
					throw new RuntimeException("Failed to read " + file, e);
				}
			});
		}

		assertThat(violations)
				.as("Found Jackson 2.x imports (com.fasterxml.jackson.databind or .core) in production code. "
						+ "Use tools.jackson equivalents instead:\n  - " + String.join("\n  - ", violations))
				.isEmpty();
	}
}
