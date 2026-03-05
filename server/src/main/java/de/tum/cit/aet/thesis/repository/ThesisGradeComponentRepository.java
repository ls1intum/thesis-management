package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.ThesisGradeComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface ThesisGradeComponentRepository extends JpaRepository<ThesisGradeComponent, UUID> {
	@Modifying
	@Transactional
	void deleteAllByAssessmentId(UUID assessmentId);
}
