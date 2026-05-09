package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.UserGroup;
import de.tum.cit.aet.thesis.entity.key.UserGroupId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, UserGroupId> {
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Transactional
	@Query("DELETE FROM UserGroup ug WHERE ug.id.userId = :userId")
	void deleteByUserId(UUID userId);

	@Modifying(flushAutomatically = true)
	@Transactional
	@Query("DELETE FROM UserGroup ug WHERE ug.id.userId = :userId AND ug.id.group = :group")
	void deleteByUserIdAndGroup(UUID userId, String group);

	@Modifying
	@Transactional
	@Query(value = "INSERT INTO user_groups (user_id, \"group\") VALUES (:userId, :group) ON CONFLICT DO NOTHING",
			nativeQuery = true)
	void insertIfNotExists(@Param("userId") UUID userId, @Param("group") String group);

	@Query("SELECT ug.id.group FROM UserGroup ug WHERE ug.user.universityId = :universityId")
	List<String> findGroupNamesByUniversityId(@Param("universityId") String universityId);
}
