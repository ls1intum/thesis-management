package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.ApplicationReviewer;
import de.tum.cit.aet.thesis.entity.key.ApplicationReviewerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ApplicationReviewerRepository extends JpaRepository<ApplicationReviewer, ApplicationReviewerId> {
}
