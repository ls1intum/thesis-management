package de.tum.cit.aet.thesis.dependency.service;

import de.tum.cit.aet.thesis.dependency.dto.CombinedSbomDTO;
import de.tum.cit.aet.thesis.dependency.dto.SbomComponentDTO;
import de.tum.cit.aet.thesis.dependency.dto.SbomDTO;
import de.tum.cit.aet.thesis.dependency.dto.SbomMetadataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Reads and parses Software Bill of Materials (SBOM) files in CycloneDX format.
 * Loads both the server-side (Java/Gradle) and client-side (npm) SBOMs that are
 * bundled into the executable jar via {@code processResources}.
 */
@Service
public class SbomService {

	private static final Logger log = LoggerFactory.getLogger(SbomService.class);

	private static final String SERVER_SBOM_PATH = "sbom/server-sbom.json";

	private static final String CLIENT_SBOM_PATH = "sbom/client-sbom.json";

	private final ObjectMapper objectMapper;

	/**
	 * Parsed SBOMs cached per classpath location. Parsing ~1.5 MB of JSON per request adds
	 * meaningful latency to the admin dependency page, and the bundled files don't change at
	 * runtime, so we keep parsed copies for the lifetime of the process. A sentinel
	 * {@link #ABSENT} marks a path that was found missing on first lookup so we don't
	 * re-check the classpath repeatedly.
	 */
	private final ConcurrentMap<String, SbomDTO> parsedSboms = new ConcurrentHashMap<>();

	private static final SbomDTO ABSENT = new SbomDTO(null, null, null, 0, null, List.of());

	/**
	 * Creates the SBOM service.
	 *
	 * @param objectMapper the Spring-managed Jackson object mapper used for SBOM parsing
	 */
	public SbomService(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * Retrieves the combined SBOM containing both server and client dependencies.
	 *
	 * @return the combined SBOM DTO with server and client SBOMs
	 */
	public CombinedSbomDTO getCombinedSbom() {
		return new CombinedSbomDTO(getServerSbom(), getClientSbom());
	}

	/**
	 * Retrieves the server-side SBOM (Java/Gradle dependencies).
	 *
	 * @return the server SBOM DTO, or null if not available
	 */
	public SbomDTO getServerSbom() {
		return loadSbomCached(SERVER_SBOM_PATH);
	}

	/**
	 * Retrieves the client-side SBOM (npm dependencies).
	 *
	 * @return the client SBOM DTO, or null if not available
	 */
	public SbomDTO getClientSbom() {
		return loadSbomCached(CLIENT_SBOM_PATH);
	}

	/**
	 * @return true if at least one SBOM file is bundled in the executable
	 */
	public boolean isSbomAvailable() {
		return isResourceAvailable(SERVER_SBOM_PATH) || isResourceAvailable(CLIENT_SBOM_PATH);
	}

	private boolean isResourceAvailable(String path) {
		return new ClassPathResource(path).exists();
	}

	private SbomDTO loadSbomCached(String path) {
		SbomDTO cached = parsedSboms.computeIfAbsent(path, p -> {
			SbomDTO parsed = parseSbomFile(p);
			return parsed != null ? parsed : ABSENT;
		});
		return cached == ABSENT ? null : cached;
	}

	private SbomDTO parseSbomFile(String path) {
		Resource resource = new ClassPathResource(path);
		if (!resource.exists()) {
			log.debug("SBOM file not found: {}", path);
			return null;
		}

		try (InputStream inputStream = resource.getInputStream()) {
			JsonNode root = objectMapper.readTree(inputStream);
			return parseCycloneDxSbom(root);
		} catch (IOException | RuntimeException e) {
			log.error("Failed to read SBOM file: {}", path, e);
			return null;
		}
	}

	private SbomDTO parseCycloneDxSbom(JsonNode root) {
		String bomFormat = getTextValue(root, "bomFormat");
		String specVersion = getTextValue(root, "specVersion");
		String serialNumber = getTextValue(root, "serialNumber");
		int version = root.has("version") ? root.get("version").asInt() : 1;

		SbomMetadataDTO metadata = parseMetadata(root.get("metadata"));
		List<SbomComponentDTO> components = parseComponents(root.get("components"));

		return new SbomDTO(bomFormat, specVersion, serialNumber, version, metadata, components);
	}

	private SbomMetadataDTO parseMetadata(JsonNode metadataNode) {
		if (metadataNode == null) {
			return null;
		}

		Instant timestamp = null;
		if (metadataNode.has("timestamp")) {
			try {
				timestamp = Instant.parse(metadataNode.get("timestamp").asString());
			} catch (Exception e) {
				log.debug("Failed to parse SBOM timestamp", e);
			}
		}

		String componentName = null;
		String componentVersion = null;
		JsonNode componentNode = metadataNode.get("component");
		if (componentNode != null) {
			componentName = getTextValue(componentNode, "name");
			componentVersion = getTextValue(componentNode, "version");
		}

		return new SbomMetadataDTO(timestamp, componentName, componentVersion);
	}

	private List<SbomComponentDTO> parseComponents(JsonNode componentsNode) {
		List<SbomComponentDTO> components = new ArrayList<>();
		if (componentsNode == null || !componentsNode.isArray()) {
			return components;
		}

		for (JsonNode componentNode : componentsNode) {
			components.add(parseComponent(componentNode));
		}

		return components;
	}

	private SbomComponentDTO parseComponent(JsonNode node) {
		String group = getTextValue(node, "group");
		String name = getTextValue(node, "name");
		String version = getTextValue(node, "version");
		String type = getTextValue(node, "type");
		String purl = getTextValue(node, "purl");
		String description = getTextValue(node, "description");

		List<String> licenses = parseLicenses(node.get("licenses"));

		return new SbomComponentDTO(group, name, version, type, purl, licenses, description);
	}

	private List<String> parseLicenses(JsonNode licensesNode) {
		List<String> licenses = new ArrayList<>();
		if (licensesNode == null || !licensesNode.isArray()) {
			return licenses;
		}

		for (JsonNode licenseEntry : licensesNode) {
			JsonNode licenseNode = licenseEntry.get("license");
			if (licenseNode != null) {
				String licenseId = getTextValue(licenseNode, "id");
				if (licenseId != null) {
					licenses.add(licenseId);
				} else {
					String licenseName = getTextValue(licenseNode, "name");
					if (licenseName != null) {
						licenses.add(licenseName);
					}
				}
			}
			// Handle expression format (e.g., "MIT OR Apache-2.0")
			String expression = getTextValue(licenseEntry, "expression");
			if (expression != null) {
				licenses.add(expression);
			}
		}

		return licenses;
	}

	private String getTextValue(JsonNode node, String fieldName) {
		return Optional.ofNullable(node.get(fieldName))
				.filter(JsonNode::isString)
				.map(JsonNode::asString)
				.orElse(null);
	}
}
