package de.tum.cit.aet.thesis.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Creates dummy PDF files in the upload directory for dev seed data.
 * Only active when the "dev" profile is enabled.
 */
@Component
@Profile("dev")
public class DevSeedFileInitializer implements ApplicationRunner {
	private static final Logger logger = LoggerFactory.getLogger(DevSeedFileInitializer.class);

	private static final byte[] MINIMAL_PDF = (
			"%PDF-1.4\n" +
			"1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n" +
			"2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n" +
			"3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Resources<<>>>>endobj\n" +
			"xref\n0 4\n" +
			"0000000000 65535 f \n" +
			"0000000009 00000 n \n" +
			"0000000058 00000 n \n" +
			"0000000115 00000 n \n" +
			"trailer<</Size 4/Root 1 0 R>>\n" +
			"startxref\n206\n%%EOF"
	).getBytes();

	private static final List<String> SEED_FILES = List.of(
			"proposal_llm_code_review_v1.pdf",
			"proposal_ci_optimization_draft.pdf",
			"proposal_anomaly_detection_final.pdf",
			"proposal_microservices_migration.pdf",
			"proposal_federated_learning.pdf",
			"thesis_llm_code_review_draft_v2.pdf",
			"thesis_anomaly_detection_final.pdf",
			"thesis_microservices_migration_final.pdf",
			"slides_anomaly_detection.pdf",
			"slides_microservices_final.pdf",
			"chapter3_review_notes.pdf"
	);

	private final Path uploadLocation;

	public DevSeedFileInitializer(@Value("${thesis-management.storage.upload-location}") String uploadLocation) {
		this.uploadLocation = Path.of(uploadLocation);
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		Files.createDirectories(uploadLocation);

		int created = 0;
		for (String filename : SEED_FILES) {
			Path filePath = uploadLocation.resolve(filename);
			if (!Files.exists(filePath)) {
				try {
					Files.write(filePath, MINIMAL_PDF);
					created++;
				} catch (IOException e) {
					logger.warn("Failed to create seed file: {}", filename, e);
				}
			}
		}

		if (created > 0) {
			logger.info("Created {} seed PDF files in upload directory", created);
		}
	}
}
