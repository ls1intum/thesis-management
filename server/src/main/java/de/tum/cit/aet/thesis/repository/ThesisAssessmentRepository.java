package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.ThesisAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ThesisAssessmentRepository extends JpaRepository<ThesisAssessment, UUID> {
	List<ThesisAssessment> findAllByThesisIdInOrderByCreatedAtDesc(List<UUID> thesisIds);
}
