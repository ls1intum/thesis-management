package de.tum.cit.aet.thesis.feedback.repository;

import de.tum.cit.aet.thesis.feedback.entity.ResearchGroupGuidelines;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ResearchGroupGuidelinesRepository extends JpaRepository<ResearchGroupGuidelines, UUID> {
}
