package de.tum.cit.aet.thesis.core.session.repository;

import de.tum.cit.aet.thesis.core.session.entity.UserSession;
import de.tum.cit.aet.thesis.core.session.entity.UserSessionEndReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Session queries. The bulk updates deliberately do not clear the persistence context: they are called from
 * the middle of other flows (role changes, account deletion) whose managed entities must stay attached.
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
	Optional<UserSession> findByTokenIdentifier(String tokenIdentifier);

	Optional<UserSession> findByIdAndUserId(UUID id, UUID userId);

	List<UserSession> findByUserIdAndEndedAtIsNullOrderByCreatedAtAsc(UUID userId);

	List<UserSession> findByEndedAtIsNullAndExpiresAtBefore(Instant now);

	List<UserSession> findByEndedAtIsNullAndLastUsedAtBefore(Instant cutoff);

	/** Reads from the database (not from an entity that may have been loaded before a rotation finished). */
	@Query("SELECT s.id FROM UserSession s WHERE s.rotatedFrom = :id")
	List<UUID> findIdsByRotatedFrom(@Param("id") UUID id);

	@Modifying
	@Transactional
	@Query("UPDATE UserSession s SET s.rotationRequired = TRUE WHERE s.userId = :userId AND s.endedAt IS NULL")
	int markRotationRequiredForUser(@Param("userId") UUID userId);

	@Modifying
	@Transactional
	@Query("UPDATE UserSession s SET s.endedAt = :endedAt, s.endReason = :reason WHERE s.userId = :userId AND s.endedAt IS NULL")
	int endAllForUser(@Param("userId") UUID userId, @Param("endedAt") Instant endedAt, @Param("reason") UserSessionEndReason reason);

	/**
	 * Atomically takes over the pending rotation of a session. Returns 1 for exactly one caller, so
	 * parallel requests do not each create a replacement session.
	 */
	@Modifying
	@Transactional
	@Query("UPDATE UserSession s SET s.rotationRequired = FALSE WHERE s.id = :id AND s.rotationRequired = TRUE AND s.endedAt IS NULL")
	int claimRotation(@Param("id") UUID id);

	@Modifying
	@Transactional
	@Query("UPDATE UserSession s SET s.endedAt = :endedAt, s.endReason = :reason WHERE s.id = :id AND s.endedAt IS NULL")
	int endById(@Param("id") UUID id, @Param("endedAt") Instant endedAt, @Param("reason") UserSessionEndReason reason);

	@Modifying
	@Transactional
	@Query("UPDATE UserSession s SET s.endedAt = :endedAt, s.endReason = de.tum.cit.aet.thesis.core.session.entity.UserSessionEndReason.ROTATED, "
			+ "s.supersededBy = :supersededBy, s.supersededUntil = :supersededUntil WHERE s.id = :id AND s.endedAt IS NULL")
	int markRotated(@Param("id") UUID id, @Param("endedAt") Instant endedAt, @Param("supersededBy") UUID supersededBy,
					@Param("supersededUntil") Instant supersededUntil);

	/**
	 * Ends the user's active sessions except the kept ones and the sessions that replaced a kept one. A single
	 * statement, so a replacement inserted by a concurrent rotation is spared as well: it records its predecessor
	 * when it is inserted.
	 */
	@Modifying
	@Transactional
	@Query("UPDATE UserSession s SET s.endedAt = :endedAt, s.endReason = :reason WHERE s.userId = :userId AND s.endedAt IS NULL "
			+ "AND s.id NOT IN :keep AND (s.rotatedFrom IS NULL OR s.rotatedFrom NOT IN :keep)")
	int endAllExcept(@Param("userId") UUID userId, @Param("keep") java.util.Collection<UUID> keep,
					@Param("endedAt") Instant endedAt, @Param("reason") UserSessionEndReason reason);

	@Modifying
	@Transactional
	@Query("UPDATE UserSession s SET s.lastUsedAt = :at, s.lastIp = :ip WHERE s.id = :id")
	int touchLastUsed(@Param("id") UUID id, @Param("at") Instant at, @Param("ip") String ip);

	@Modifying
	@Transactional
	@Query("DELETE FROM UserSession s WHERE s.endedAt IS NOT NULL AND s.endedAt < :cutoff")
	int deleteEndedBefore(@Param("cutoff") Instant cutoff);
}
