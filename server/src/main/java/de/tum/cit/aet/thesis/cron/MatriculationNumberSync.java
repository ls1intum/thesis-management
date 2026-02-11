package de.tum.cit.aet.thesis.cron;

import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.repository.UserRepository;
import de.tum.cit.aet.thesis.service.AccessManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Scheduled task that backfills missing matriculation numbers from Keycloak.
 *
 * <p><strong>This is a temporary backfill task.</strong> It exists only to populate matriculation
 * numbers for users who were created before the JWT-based sync (in {@code AuthenticationService})
 * was implemented. Once all affected users have been backfilled, this task can be disabled (e.g. via a
 * configuration flag). The code is kept in place in case a similar backfill is needed in the future.</p>
 *
 * <p>The primary/permanent mechanism for syncing matriculation numbers is the JWT {@code matrikelnr}
 * claim extraction in {@link de.tum.cit.aet.thesis.service.AuthenticationService#updateAuthenticatedUser}.
 * This task complements it by covering users who may not log in frequently enough.</p>
 *
 * <p>The task runs 10 minutes after server start and then every 7 days. User IDs are loaded once,
 * then each user is processed and committed individually so that a single Keycloak failure does not
 * roll back progress and no long-running transaction is held open during blocking HTTP calls.</p>
 */
@Component
public class MatriculationNumberSync {
	private static final Logger log = LoggerFactory.getLogger(MatriculationNumberSync.class);

	private final UserRepository userRepository;
	private final AccessManagementService accessManagementService;

	public MatriculationNumberSync(UserRepository userRepository, AccessManagementService accessManagementService) {
		this.userRepository = userRepository;
		this.accessManagementService = accessManagementService;
	}

	/**
	 * Loads IDs of all users without a matriculation number, then fetches each user's
	 * {@code matrikelnr} attribute from the Keycloak admin API. Each user is saved in its own
	 * transaction so that progress is preserved even if individual lookups fail (e.g. for
	 * professors who do not have a matriculation number in Keycloak).
	 */
	@Scheduled(initialDelay = 10 * 60 * 1000, fixedRate = 7 * 24 * 60 * 60 * 1000)
	public void syncMatriculationNumbers() {
		List<UUID> userIds = userRepository.findIdsWithoutMatriculationNumber();
		log.info("Starting matriculation number sync for {} users", userIds.size());

		int updated = 0;
		for (UUID userId : userIds) {
			try {
				if (syncSingleUser(userId)) {
					updated++;
				}
			} catch (RuntimeException e) {
				log.warn("Could not sync matriculation number for user id {}", userId, e);
			}
		}

		log.info("Matriculation number sync completed: {}/{} users updated", updated, userIds.size());
	}

	/**
	 * Fetches the matriculation number from Keycloak for a single user and persists it.
	 *
	 * @param userId the database ID of the user to sync
	 * @return {@code true} if the matriculation number was updated, {@code false} otherwise
	 */
	public boolean syncSingleUser(UUID userId) {
		User user = userRepository.findById(userId).orElse(null);
		if (user == null || user.getMatriculationNumber() != null) {
			return false;
		}

		AccessManagementService.KeycloakUserInformation keycloakUser =
				accessManagementService.getUserByUsername(user.getUniversityId());
		String matriculationNumber = keycloakUser.getMatriculationNumber();

		if (matriculationNumber != null && !matriculationNumber.isEmpty()) {
			user.setMatriculationNumber(matriculationNumber);
			userRepository.save(user);
			return true;
		}

		return false;
	}
}
