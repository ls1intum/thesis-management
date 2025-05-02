package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {
    @Query(value = """
            SELECT t.* FROM topics t WHERE (:researchGroupId IS NULL OR t.research_group_id = :researchGroupId) AND
                    (:searchQuery IS NULL OR t.title ILIKE CONCAT('%', :searchQuery, '%')) AND
                    (t.thesis_types IS NULL OR CAST(:types AS TEXT[]) IS NULL OR t.thesis_types && CAST(:types AS TEXT[])) AND
                    (:includeClosed = TRUE OR t.closed_at IS NULL)
            """, nativeQuery = true)
    Page<Topic> searchTopics(
            @Param("researchGroupId") UUID researchGroupId,
            @Param("types") String[] types,
            @Param("includeClosed") boolean includeClosed,
            @Param("searchQuery") String searchQuery,
            Pageable page
    );

    @Query("""
                SELECT COUNT(*)
                FROM Topic t
                WHERE t.closedAt IS NULL
                AND (:researchGroupId IS NULL OR t.researchGroup.id = :researchGroupId)
            """)
    long countOpenTopics(@Param("researchGroupId") UUID researchGroupId);
}
