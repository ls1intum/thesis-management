package de.tum.cit.aet.thesis.core.application.repository;

import de.tum.cit.aet.thesis.core.application.entity.ApplicationReviewer;
import de.tum.cit.aet.thesis.core.application.entity.key.ApplicationReviewerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Repository
public interface ApplicationReviewerRepository extends JpaRepository<ApplicationReviewer, ApplicationReviewerId> {
	@Modifying
	@Transactional
	@Query("DELETE FROM ApplicationReviewer ar WHERE ar.application.id = :applicationId")
	void deleteByApplicationId(UUID applicationId);
}
