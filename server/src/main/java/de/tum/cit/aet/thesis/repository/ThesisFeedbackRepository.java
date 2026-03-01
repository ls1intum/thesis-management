package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.ThesisFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


@Repository
public interface ThesisFeedbackRepository extends JpaRepository<ThesisFeedback, UUID> {
	List<ThesisFeedback> findAllByThesisIdInOrderByRequestedAtAsc(List<UUID> thesisIds);

	@Modifying
	@Transactional
	void deleteAllByThesisId(UUID thesisId);
}
