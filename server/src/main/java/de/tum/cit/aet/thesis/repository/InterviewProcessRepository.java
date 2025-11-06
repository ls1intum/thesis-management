package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.InterviewProcess;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InterviewProcessRepository extends JpaRepository<InterviewProcess, UUID> {
    @Query("SELECT CASE WHEN COUNT(ip) > 0 THEN true ELSE false END " +
            "FROM InterviewProcess ip " +
            "WHERE ip.topic.id = :topicId")
    boolean existsByTopicId(UUID topicId);

    @Query("SELECT DISTINCT ip FROM InterviewProcess ip " +
            "JOIN ip.topic t " +
            "WHERE (:searchQuery IS NULL OR LOWER(t.title) LIKE :searchQuery) " +
            "AND ( :userId IS NULL " +
            "      OR ( :excludeSupervised = true AND EXISTS (SELECT 1 FROM TopicRole r WHERE r.topic = t AND r.id.userId = :userId AND r.id.role = 'ADVISOR')) " +
            "      OR ( :excludeSupervised = false AND EXISTS (SELECT 1 FROM TopicRole r WHERE r.topic = t AND r.id.userId = :userId AND (r.id.role = 'ADVISOR' OR r.id.role = 'SUPERVISOR')) )" +
            "    )")
    Page<InterviewProcess> searchMyInterviewProcesses(UUID userId, String searchQuery, boolean excludeSupervised, Pageable pageable);

}
