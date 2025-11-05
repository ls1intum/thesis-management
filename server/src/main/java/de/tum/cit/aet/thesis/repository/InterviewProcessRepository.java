package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.InterviewProcess;
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
}
