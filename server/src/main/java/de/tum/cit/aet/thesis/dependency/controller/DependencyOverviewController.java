package de.tum.cit.aet.thesis.dependency.controller;

import de.tum.cit.aet.thesis.dependency.dto.CombinedSbomDTO;
import de.tum.cit.aet.thesis.dependency.dto.ComponentVulnerabilitiesDTO;
import de.tum.cit.aet.thesis.dependency.dto.SbomDTO;
import de.tum.cit.aet.thesis.dependency.dto.ThesisManagementVersionDTO;
import de.tum.cit.aet.thesis.dependency.service.SbomService;
import de.tum.cit.aet.thesis.dependency.service.ThesisManagementVersionService;
import de.tum.cit.aet.thesis.dependency.service.VulnerabilityScanScheduleService;
import de.tum.cit.aet.thesis.dependency.service.VulnerabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for the admin dependency overview page. Exposes the bundled SBOMs,
 * OSV-derived vulnerability information, version-update status, and a manual trigger
 * for the weekly vulnerability email.
 */
@RestController
@RequestMapping("/v2/admin/dependencies")
@PreAuthorize("hasRole('admin')")
public class DependencyOverviewController {

	private static final Logger log = LoggerFactory.getLogger(DependencyOverviewController.class);

	private final SbomService sbomService;

	private final VulnerabilityService vulnerabilityService;

	private final ThesisManagementVersionService versionService;

	private final VulnerabilityScanScheduleService scanScheduleService;

	/**
	 * Creates the dependency overview controller.
	 *
	 * @param sbomService          service that exposes the bundled SBOMs
	 * @param vulnerabilityService service that returns OSV vulnerability information
	 * @param versionService       service that compares current vs. latest GitHub release
	 * @param scanScheduleService  service used to manually trigger the weekly email
	 */
	public DependencyOverviewController(
			SbomService sbomService,
			VulnerabilityService vulnerabilityService,
			ThesisManagementVersionService versionService,
			VulnerabilityScanScheduleService scanScheduleService
	) {
		this.sbomService = sbomService;
		this.vulnerabilityService = vulnerabilityService;
		this.versionService = versionService;
		this.scanScheduleService = scanScheduleService;
	}

	/**
	 * GET /v2/admin/dependencies — combined server + client SBOM.
	 *
	 * @return 200 with the combined SBOM, or 404 if no SBOM is bundled in this build
	 */
	@GetMapping
	public ResponseEntity<CombinedSbomDTO> getCombinedSbom() {
		log.debug("REST request to get combined SBOM");
		if (!sbomService.isSbomAvailable()) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(sbomService.getCombinedSbom());
	}

	/**
	 * GET /v2/admin/dependencies/server — server-side SBOM (Java/Gradle).
	 *
	 * @return 200 with the server SBOM, or 404 if not bundled
	 */
	@GetMapping("/server")
	public ResponseEntity<SbomDTO> getServerSbom() {
		log.debug("REST request to get server SBOM");
		SbomDTO sbom = sbomService.getServerSbom();
		return sbom == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(sbom);
	}

	/**
	 * GET /v2/admin/dependencies/client — client-side SBOM (npm).
	 *
	 * @return 200 with the client SBOM, or 404 if not bundled
	 */
	@GetMapping("/client")
	public ResponseEntity<SbomDTO> getClientSbom() {
		log.debug("REST request to get client SBOM");
		SbomDTO sbom = sbomService.getClientSbom();
		return sbom == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(sbom);
	}

	/**
	 * GET /v2/admin/dependencies/vulnerabilities — cached vulnerability data (24h TTL).
	 *
	 * @return 200 with vulnerability data, or 404 if no SBOM is bundled
	 */
	@GetMapping("/vulnerabilities")
	public ResponseEntity<ComponentVulnerabilitiesDTO> getVulnerabilities() {
		log.debug("REST request to get SBOM vulnerabilities");
		if (!sbomService.isSbomAvailable()) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(vulnerabilityService.getVulnerabilities());
	}

	/**
	 * POST /v2/admin/dependencies/vulnerabilities/refresh — bypasses cache, fetches fresh data
	 * from OSV. Modeled as POST because it mutates the in-memory cache and should not be
	 * subject to GET caching/prefetching semantics.
	 *
	 * @return 200 with the freshly fetched vulnerability data, or 404 if no SBOM is bundled
	 */
	@PostMapping("/vulnerabilities/refresh")
	public ResponseEntity<ComponentVulnerabilitiesDTO> refreshVulnerabilities() {
		log.info("REST request to refresh SBOM vulnerabilities");
		if (!sbomService.isSbomAvailable()) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(vulnerabilityService.refreshVulnerabilities());
	}

	/**
	 * GET /v2/admin/dependencies/version — current vs. latest GitHub release.
	 *
	 * @return 200 with the version comparison
	 */
	@GetMapping("/version")
	public ResponseEntity<ThesisManagementVersionDTO> getVersionInfo() {
		log.debug("REST request to get version info");
		return ResponseEntity.ok(versionService.getVersionInfo());
	}

	/**
	 * POST /v2/admin/dependencies/vulnerabilities/send-email — manual trigger for the weekly email.
	 *
	 * @return 200 with {@code {"sent": true}} on success, or 400 with {@code {"sent": false}} when
	 *         the run was skipped (no SBOM, no admin email, etc.). A JSON body is always returned
	 *         so the client's {@code doRequest} can parse it.
	 */
	@PostMapping("/vulnerabilities/send-email")
	public ResponseEntity<Map<String, Boolean>> sendVulnerabilityEmail() {
		log.info("REST request to send vulnerability scan email");
		boolean sent = scanScheduleService.sendVulnerabilityScanEmail();
		Map<String, Boolean> body = Map.of("sent", sent);
		return sent ? ResponseEntity.ok(body) : ResponseEntity.badRequest().body(body);
	}
}
