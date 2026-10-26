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

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
	Optional<UserSession> findByTokenIdentifier(String tokenIdentifier);

	Optional<UserSession> findByIdAndUserId(UUID id, UUID userId);

	List<UserSession> findByUserIdAndEndedAtIsNullOrderByCreatedAtAsc(UUID userId);

	List<UserSession> findByEndedAtIsNullAndExpiresAtBefore(Instant now);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Transactional
	@Query("UPDATE UserSession s SET s.rotationRequired = TRUE WHERE s.userId = :userId AND s.endedAt IS NULL")
	int markRotationRequiredForUser(@Param("userId") UUID userId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Transactional
	@Query("UPDATE UserSession s SET s.endedAt = :endedAt, s.endReason = :reason WHERE s.userId = :userId AND s.endedAt IS NULL")
	int endAllForUser(@Param("userId") UUID userId, @Param("endedAt") Instant endedAt, @Param("reason") UserSessionEndReason reason);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Transactional
	@Query("UPDATE UserSession s SET s.lastUsedAt = :at, s.lastIp = :ip WHERE s.id = :id")
	int touchLastUsed(@Param("id") UUID id, @Param("at") Instant at, @Param("ip") String ip);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Transactional
	@Query("DELETE FROM UserSession s WHERE s.endedAt IS NOT NULL AND s.endedAt < :cutoff")
	int deleteEndedBefore(@Param("cutoff") Instant cutoff);
}
