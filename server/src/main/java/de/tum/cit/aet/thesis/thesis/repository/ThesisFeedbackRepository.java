package de.tum.cit.aet.thesis.thesis.repository;

import de.tum.cit.aet.thesis.thesis.entity.ThesisFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;


@Repository
public interface ThesisFeedbackRepository extends JpaRepository<ThesisFeedback, UUID> {
	List<ThesisFeedback> findAllByThesisIdInOrderByRequestedAtAsc(List<UUID> thesisIds);

	/**
	 * Which of the given entries already have a follow-up, read from the database rather than from a
	 * thesis loaded before a (possibly minute-long) review started.
	 *
	 * @param ids candidate parent entries
	 * @return the ids among them that some entry already follows up
	 */
	@Query("SELECT f.followsUpId FROM ThesisFeedback f WHERE f.followsUpId IN :ids")
	List<UUID> findFollowedUpIds(@Param("ids") Collection<UUID> ids);

	@Modifying
	@Transactional
	void deleteAllByThesisId(UUID thesisId);
}
