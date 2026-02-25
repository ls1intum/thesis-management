package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.TopicRole;
import de.tum.cit.aet.thesis.entity.key.TopicRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


@Repository
public interface TopicRoleRepository extends JpaRepository<TopicRole, TopicRoleId> {
	List<TopicRole> deleteByTopicId(UUID topicId);

	@Modifying
	@Transactional
	@Query("DELETE FROM TopicRole tr WHERE tr.id.userId = :userId")
	void deleteAllByIdUserId(UUID userId);
}
