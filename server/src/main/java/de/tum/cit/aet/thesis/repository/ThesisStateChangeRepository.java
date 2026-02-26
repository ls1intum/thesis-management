package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.ThesisStateChange;
import de.tum.cit.aet.thesis.entity.key.ThesisStateChangeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Repository
public interface ThesisStateChangeRepository extends JpaRepository<ThesisStateChange, ThesisStateChangeId> {
	@Modifying
	@Transactional
	@Query("DELETE FROM ThesisStateChange s WHERE s.id.thesisId = :thesisId")
	void deleteAllByThesisId(@Param("thesisId") UUID thesisId);
}
