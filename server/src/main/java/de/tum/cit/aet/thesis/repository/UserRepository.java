package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findByUniversityId(String universityId);

	List<User> findAllByUniversityIdIn(List<String> universityIds);

	@Query("SELECT u FROM User u LEFT JOIN FETCH u.researchGroup WHERE u.universityId = :universityId")
	Optional<User> findByUniversityIdWithResearchGroup(@Param("universityId") String universityId);

	@Query("""
			SELECT DISTINCT u
			FROM User u
			LEFT JOIN UserGroup g ON (u.id = g.id.userId)
			WHERE (:researchGroupId IS NULL
				OR NOT ('advisor' IN :groups
						OR 'supervisor' IN :groups)
				OR u.researchGroup.id = :researchGroupId)
			AND ((:groups IS NULL)
					OR ('student' IN :groups AND (g.id.group IN :groups OR g.id.group IS NULL))
					OR g.id.group IN :groups)
			AND (:searchQuery IS NULL
				OR LOWER(u.firstName) || ' ' || LOWER(u.lastName) LIKE %:searchQuery%
				OR LOWER(u.email) LIKE %:searchQuery%
				OR LOWER(u.matriculationNumber) LIKE %:searchQuery%
				OR LOWER(u.universityId) LIKE %:searchQuery%)
			""")
	Page<User> searchUsers(@Param("researchGroupId") UUID researchGroupId,
						@Param("searchQuery") String searchQuery, @Param("groups") Set<String> groups, Pageable page);

	@Query("SELECT DISTINCT u FROM User u LEFT JOIN UserGroup g ON (u.id = g.id.userId) WHERE g"
			+ ".id.group IN :roles AND (:researchGroupId IS NULL OR u.researchGroup.id = "
			+ ":researchGroupId)")
	List<User> getRoleMembers(@Param("roles") Set<String> roles,
							@Param("researchGroupId") UUID researchGroupId);

	@Query("SELECT u FROM User u WHERE u.avatar IS NULL OR u.avatar = ''")
	List<User> findAllByAvatarIsNullOrAvatarIsEmpty();

	@Query("""
				SELECT DISTINCT u FROM User u
				WHERE u.id IN (
					SELECT tr.user.id FROM ThesisRole tr
					JOIN tr.thesis t
					WHERE t.researchGroup.id = :researchGroupId
					AND t.state NOT IN ('FINISHED', 'DROPPED_OUT')
					AND tr.id.role = 'STUDENT'
				)
			""")
	List<User> findStudentsWithActiveThesesByResearchGroupId(@Param("researchGroupId") UUID researchGroupId);

	List<User> findAllByDeletionRequestedAtIsNotNull();

	List<User> findAllByDeletionScheduledForIsNotNull();

	@Modifying
	@Transactional
	@Query("UPDATE User u SET u.deletionScheduledFor = NULL WHERE u.id = :userId")
	void clearDeletionScheduledFor(@Param("userId") UUID userId);

	@Query("""
			SELECT DISTINCT u FROM User u
			JOIN UserGroup g ON u.id = g.id.userId AND g.id.group = 'student'
			WHERE u.disabled = FALSE
			AND COALESCE(u.lastLoginAt, u.joinedAt) < :cutoff
			AND COALESCE(u.updatedAt, u.joinedAt) < :cutoff
			AND NOT EXISTS (
				SELECT 1 FROM UserGroup ug2
				WHERE ug2.id.userId = u.id AND ug2.id.group IN ('admin', 'supervisor', 'advisor')
			)
			AND NOT EXISTS (
				SELECT 1 FROM ThesisRole tr
				JOIN tr.thesis t
				WHERE tr.user.id = u.id
				AND (
					t.createdAt >= :cutoff
					OR t.endDate >= :cutoff
					OR EXISTS (
						SELECT 1 FROM ThesisStateChange sc
						WHERE sc.thesis = t AND sc.changedAt >= :cutoff
					)
				)
			)
			AND NOT EXISTS (
				SELECT 1 FROM Application a
				WHERE a.user.id = u.id AND a.createdAt >= :cutoff
			)
			""")
	List<User> findInactiveStudentCandidates(@Param("cutoff") Instant cutoff);
}
