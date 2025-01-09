package de.tum.cit.aet.thesis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import de.tum.cit.aet.thesis.constants.ApplicationState;
import de.tum.cit.aet.thesis.entity.Application;
import de.tum.cit.aet.thesis.entity.Topic;
import de.tum.cit.aet.thesis.entity.User;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    @Query(
            "SELECT DISTINCT a FROM Application a WHERE " +
            "(:userId IS NULL OR a.user.id = :userId) AND " +
            "(:states IS NULL OR a.state IN :states OR (:previousIds IS NOT NULL AND a.id IN :previousIds)) AND " +
            "(:reviewerId IS NULL OR NOT EXISTS (SELECT ar FROM ApplicationReviewer ar WHERE a.id = ar.application.id AND ar.user.id = :reviewerId AND ar.reason = 'NOT_INTERESTED') OR (:previousIds IS NOT NULL AND a.id IN :previousIds)) AND " +
            "(:includeSuggestedTopics = true OR a.topic IS NOT NULL) AND " +
            "(:topics IS NULL OR a.topic.id IN :topics OR (:includeSuggestedTopics = true AND a.topic IS NULL)) AND " +
            "(:types IS NULL OR a.thesisType IN :types) AND " +
            "(:searchQuery IS NULL OR (LOWER(a.user.firstName) || ' ' || LOWER(a.user.lastName)) LIKE %:searchQuery% OR " +
            "LOWER(a.user.email) LIKE %:searchQuery% OR " +
            "LOWER(a.user.matriculationNumber) LIKE %:searchQuery% OR " +
            "LOWER(a.user.universityId) LIKE %:searchQuery%)"
    )
    Page<Application> searchApplications(
            @Param("userId") UUID userId,
            @Param("reviewerId") UUID reviewerId,
            @Param("searchQuery") String searchQuery,
            @Param("states") Set<ApplicationState> states,
            @Param("previousIds") Set<String> previousIds,
            @Param("topics") Set<String> topics,
            @Param("types") Set<String> types,
            @Param("includeSuggestedTopics") boolean includeSuggestedTopics,
            Pageable page
    );

    @Query(
            "SELECT COUNT(DISTINCT a) FROM Application a " +
            "LEFT JOIN Topic t ON (a.topic.id = t.id) " +
            "LEFT JOIN TopicRole r ON (t.id = r.topic.id) " +
            "WHERE " +
                    "(a.topic IS NULL OR :userId IS NULL OR r.user.id = :userId) AND " +
                    "a.state = 'NOT_ASSESSED' AND " +
                    "(:userId IS NULL OR NOT EXISTS(SELECT ar FROM ApplicationReviewer ar WHERE ar.application.id = a.id AND ar.user.id = :userId))"
    )
    long countUnreviewedApplications(@Param("userId") UUID userId);

    @Query(
            "SELECT EXISTS (" +
                    "SELECT a FROM Application a " +
                    "WHERE a.user.id = :userId AND a.state = 'NOT_ASSESSED' AND " +
                    "((a.topic IS NULL AND :topicId IS NULL) OR (a.topic IS NOT NULL AND a.topic.id = :topicId))" +
            ")"
    )
    boolean existsPendingApplication(
            @Param("userId") UUID userId,
            @Param("topicId") UUID topicId
    );

    List<Application> findAllByTopic(Topic topic);
    List<Application> findAllByUser(User user);
}
