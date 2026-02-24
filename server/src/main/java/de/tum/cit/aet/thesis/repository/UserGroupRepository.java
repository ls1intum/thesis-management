package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.UserGroup;
import de.tum.cit.aet.thesis.entity.key.UserGroupId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, UserGroupId> {
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Transactional
	@Query("DELETE FROM UserGroup ug WHERE ug.id.userId = :userId")
	void deleteByUserId(UUID userId);
}
