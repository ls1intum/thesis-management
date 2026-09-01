package de.tum.cit.aet.thesis.feedback.repository;

import de.tum.cit.aet.thesis.feedback.entity.AIReviewSummary;
import de.tum.cit.aet.thesis.feedback.service.reviewer.ReviewType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AIReviewSummaryRepository extends JpaRepository<AIReviewSummary, UUID> {
	Optional<AIReviewSummary> findByThesisIdAndType(UUID thesisId, ReviewType type);
}
