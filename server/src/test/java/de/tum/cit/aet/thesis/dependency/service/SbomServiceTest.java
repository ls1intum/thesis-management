package de.tum.cit.aet.thesis.dependency.service;

import static org.assertj.core.api.Assertions.assertThat;

import de.tum.cit.aet.thesis.dependency.dto.CombinedSbomDTO;
import de.tum.cit.aet.thesis.dependency.dto.SbomComponentDTO;
import de.tum.cit.aet.thesis.dependency.dto.SbomDTO;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class SbomServiceTest {

	private static final String SERVER_FIXTURE = "sbom-fixtures/sample-server-sbom.json";
	private static final String CLIENT_FIXTURE = "sbom-fixtures/sample-client-sbom.json";
	private static final String MISSING_FIXTURE = "sbom-fixtures/does-not-exist.json";

	private final ObjectMapper objectMapper = JsonMapper.builder().build();

	@Test
	void parsesServerSbomFromClasspath() {
		SbomService service = new SbomService(objectMapper, SERVER_FIXTURE, MISSING_FIXTURE);

		SbomDTO sbom = service.getServerSbom();

		assertThat(sbom).isNotNull();
		assertThat(sbom.bomFormat()).isEqualTo("CycloneDX");
		assertThat(sbom.specVersion()).isEqualTo("1.6");
		assertThat(sbom.version()).isEqualTo(1);
		assertThat(sbom.metadata()).isNotNull();
		assertThat(sbom.metadata().componentName()).isEqualTo("Thesis Management");
		assertThat(sbom.metadata().version()).isEqualTo("4.11.0");
		assertThat(sbom.components()).hasSize(3);

		SbomComponentDTO springCore = sbom.components().stream()
				.filter(c -> "spring-core".equals(c.name())).findFirst().orElseThrow();
		assertThat(springCore.group()).isEqualTo("org.springframework");
		assertThat(springCore.version()).isEqualTo("6.0.0");
		assertThat(springCore.purl()).isEqualTo("pkg:maven/org.springframework/spring-core@6.0.0");
		assertThat(springCore.licenses()).containsExactly("Apache-2.0");
	}

	@Test
	void parsesClientSbomFromClasspath() {
		SbomService service = new SbomService(objectMapper, MISSING_FIXTURE, CLIENT_FIXTURE);

		SbomDTO sbom = service.getClientSbom();

		assertThat(sbom).isNotNull();
		assertThat(sbom.components()).hasSize(2);
		assertThat(sbom.components().get(0).name()).isEqualTo("react");
		assertThat(sbom.components().get(0).purl()).isEqualTo("pkg:npm/react@19.0.0");
	}

	@Test
	void licenseParsingFallsBackToNameWhenIdMissing() {
		SbomService service = new SbomService(objectMapper, SERVER_FIXTURE, MISSING_FIXTURE);

		SbomComponentDTO jackson = service.getServerSbom().components().stream()
				.filter(c -> "jackson-databind".equals(c.name())).findFirst().orElseThrow();

		assertThat(jackson.licenses()).containsExactly("Apache License, Version 2.0");
	}

	@Test
	void licenseExpressionIsExtracted() {
		SbomService service = new SbomService(objectMapper, SERVER_FIXTURE, MISSING_FIXTURE);

		SbomComponentDTO noPurl = service.getServerSbom().components().stream()
				.filter(c -> "no-purl-lib".equals(c.name())).findFirst().orElseThrow();

		assertThat(noPurl.licenses()).containsExactly("MIT OR Apache-2.0");
	}

	@Test
	void getCombinedSbomMergesBothSides() {
		SbomService service = new SbomService(objectMapper, SERVER_FIXTURE, CLIENT_FIXTURE);

		CombinedSbomDTO combined = service.getCombinedSbom();

		assertThat(combined.server()).isNotNull();
		assertThat(combined.client()).isNotNull();
		assertThat(combined.server().components()).hasSize(3);
		assertThat(combined.client().components()).hasSize(2);
	}

	@Test
	void missingSbomReturnsNullDto() {
		SbomService service = new SbomService(objectMapper, MISSING_FIXTURE, MISSING_FIXTURE);

		assertThat(service.getServerSbom()).isNull();
		assertThat(service.getClientSbom()).isNull();
		assertThat(service.isSbomAvailable()).isFalse();
	}

	@Test
	void isSbomAvailableTrueWhenAtLeastOneSidePresent() {
		SbomService onlyServer = new SbomService(objectMapper, SERVER_FIXTURE, MISSING_FIXTURE);
		SbomService onlyClient = new SbomService(objectMapper, MISSING_FIXTURE, CLIENT_FIXTURE);

		assertThat(onlyServer.isSbomAvailable()).isTrue();
		assertThat(onlyClient.isSbomAvailable()).isTrue();
	}

	@Test
	void parsedSbomsAreCachedAcrossCalls() {
		SbomService service = new SbomService(objectMapper, SERVER_FIXTURE, CLIENT_FIXTURE);

		SbomDTO first = service.getServerSbom();
		SbomDTO second = service.getServerSbom();

		// Same record instance — the parser ran only once
		assertThat(second).isSameAs(first);
	}
}
