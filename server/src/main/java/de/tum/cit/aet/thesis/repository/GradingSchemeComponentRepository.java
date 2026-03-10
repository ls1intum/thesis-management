package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.GradingSchemeComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GradingSchemeComponentRepository extends JpaRepository<GradingSchemeComponent, UUID> {
	List<GradingSchemeComponent> findByResearchGroupIdOrderByPositionAsc(UUID researchGroupId);
}
