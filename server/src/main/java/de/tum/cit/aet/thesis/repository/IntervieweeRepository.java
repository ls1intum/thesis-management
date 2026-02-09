package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.InterviewProcess;
import de.tum.cit.aet.thesis.entity.Interviewee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IntervieweeRepository extends JpaRepository<Interviewee, UUID> {
    @Query("""
        SELECT i FROM Interviewee i
        WHERE i.interviewProcess.id = :interviewProcessId
          AND (
            :state IS NULL
            OR (
                (:state = 'UNCONTACTED' AND i.lastInvited IS NULL and i.score IS NULL)
             OR (:state = 'SCHEDULED'   AND SIZE(i.slots) > 0 AND i.score IS NULL)
             OR (:state = 'COMPLETED'   AND i.score IS NOT NULL)
             OR (:state = 'INVITED'     AND i.lastInvited IS NOT NULL AND SIZE(i.slots) = 0 AND i.score IS NULL)
            )
          )
          AND (
            :searchQuery IS NULL
            OR (
              LOWER(i.application.user.firstName) LIKE :searchQuery
              OR LOWER(i.application.user.lastName) LIKE :searchQuery
              OR LOWER(i.application.user.email) LIKE :searchQuery
            )
          )
        """)
    Page<Interviewee> findAllInterviewees(
            @Param("interviewProcessId") UUID interviewProcessId,
            @Param("searchQuery") String searchQuery,
            @Param("state") String state,
            Pageable pageable
    );
}
