package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.ResearchGroup;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ResearchGroupRepository extends JpaRepository<ResearchGroup, UUID> {

  @Query(value = """
          SELECT r.* FROM research_groups r
          WHERE (:searchQuery IS NULL OR r.name ILIKE CONCAT('%', :searchQuery, '%')
            OR r.abbreviation ILIKE CONCAT('%', :searchQuery, '%'))
            AND (CAST(:heads AS UUID[]) IS NULL OR r.head_user_id = ANY(CAST(:heads AS UUID[])))
            AND (CAST(:campuses AS TEXT[]) IS NULL OR r.campus IS NULL
            OR r.campus = ANY(CAST(:campuses AS TEXT[])))
            AND (:includeArchived = TRUE OR r.archived = FALSE)
          """, nativeQuery = true)
  Page<ResearchGroup> searchResearchGroup(
      @Param("heads") String[] heads,
      @Param("campuses") String[] campuses,
      @Param("includeArchived")
      boolean includeArchived,
      @Param("searchQuery") String searchQuery,
      Pageable page
  );

    @Query("SELECT r FROM ResearchGroup r WHERE r.abbreviation = :abbreviation")
  ResearchGroup findByAbbreviation(String abbreviation);
}