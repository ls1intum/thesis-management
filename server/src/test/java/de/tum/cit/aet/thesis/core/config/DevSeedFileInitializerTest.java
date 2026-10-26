package de.tum.cit.aet.thesis.core.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class DevSeedFileInitializerTest {

	@Test
	void run_createsSeedPdfsInUploadDirectory(@TempDir Path uploadDir) throws Exception {
		DevSeedFileInitializer initializer = new DevSeedFileInitializer(uploadDir.toString());
		initializer.run(new DefaultApplicationArguments());

		// Expected seed files (a known representative subset)
		Path expected = uploadDir.resolve("thesis_anomaly_detection_final.pdf");
		assertTrue(Files.exists(expected), "Expected seed PDF was not created");
		// File content starts with %PDF marker
		String head = new String(Files.readAllBytes(expected), java.nio.charset.StandardCharsets.UTF_8).substring(0, 5);
		assertTrue(head.startsWith("%PDF"), "Expected %PDF header in: " + head);
	}

	@Test
	void run_skipsExistingFiles(@TempDir Path uploadDir) throws Exception {
		Path target = uploadDir.resolve("thesis_anomaly_detection_final.pdf");
		Files.write(target, "PRE-EXISTING".getBytes());

		DevSeedFileInitializer initializer = new DevSeedFileInitializer(uploadDir.toString());
		initializer.run(new DefaultApplicationArguments());

		// File should be untouched
		String content = Files.readString(target);
		assertTrue(content.equals("PRE-EXISTING"), "Existing file was unexpectedly overwritten: " + content);
	}

	@Test
	void run_createsUploadDirectoryIfMissing(@TempDir Path parentDir) throws Exception {
		Path newDir = parentDir.resolve("not-yet-existing");
		DevSeedFileInitializer initializer = new DevSeedFileInitializer(newDir.toString());

		initializer.run(new DefaultApplicationArguments());

		assertTrue(Files.exists(newDir));
	}

	@Test
	void run_idempotentOnSubsequentCalls(@TempDir Path uploadDir) throws Exception {
		DevSeedFileInitializer initializer = new DevSeedFileInitializer(uploadDir.toString());
		initializer.run(new DefaultApplicationArguments());
		long firstCount = countPdfs(uploadDir);

		initializer.run(new DefaultApplicationArguments());
		long secondCount = countPdfs(uploadDir);

		assertTrue(firstCount > 0 && firstCount == secondCount,
				"PDF count should be unchanged across calls: " + firstCount + " vs " + secondCount);
	}

	private long countPdfs(Path dir) throws IOException {
		try (var stream = Files.list(dir)) {
			return stream.filter(p -> p.toString().endsWith(".pdf")).count();
		}
	}
}
