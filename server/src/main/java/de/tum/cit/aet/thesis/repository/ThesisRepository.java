package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.constants.ThesisRoleName;
import de.tum.cit.aet.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.constants.ThesisVisibility;
import de.tum.cit.aet.thesis.entity.Thesis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ThesisRepository extends JpaRepository<Thesis, UUID> {
    @Query("""
            SELECT DISTINCT t FROM Thesis t
             LEFT JOIN ThesisRole r ON t.id = r.thesis.id
             WHERE (
                 :visibilities IS NULL
                 OR (
                     t.visibility IN :visibilities
                     AND (:researchGroupIds IS NULL OR t.researchGroup.id IN :researchGroupIds)
                 )
                 OR (:userId IS NOT NULL AND r.user.id = :userId )
             )
             AND (:states IS NULL OR t.state IN :states)
             AND (:types IS NULL OR t.type IN :types)
             AND (
                 :searchQuery IS NULL OR (
                     LOWER(t.title) LIKE %:searchQuery%
                     OR LOWER(r.user.firstName || ' ' || r.user.lastName) LIKE %:searchQuery%
                     OR LOWER(r.user.email) LIKE %:searchQuery%
                     OR LOWER(r.user.matriculationNumber) LIKE %:searchQuery%
                     OR LOWER(r.user.universityId) LIKE %:searchQuery%
                 )
             )
            """)
    Page<Thesis> searchTheses(
            @Param("researchGroupIds") Set<UUID> researchGroupIds,
            @Param("userId") UUID userId,
            @Param("visibilities") Set<ThesisVisibility> visibilities,
            @Param("searchQuery") String searchQuery,
            @Param("states") Set<ThesisState> states,
            @Param("types") Set<String> types,
            Pageable page
    );

    @Query("""
            SELECT DISTINCT t FROM Thesis t LEFT JOIN ThesisRole r ON (t.id = r.thesis.id) WHERE
            (:userId IS NULL OR r.user.id = :userId) AND
            (:researchGroupId IS NULL OR t.researchGroup.id = :researchGroupId) AND
            (t.state != 'FINISHED' AND t.state != 'DROPPED_OUT') AND
            (:roleNames IS NULL OR r.id.role IN :roleNames) AND
            (:states IS NULL OR t.state IN :states)
            """)
    List<Thesis> findActiveThesesForRole(
            @Param("userId") UUID userId,
            @Param("researchGroupId") UUID researchGroupId,
            @Param("roleNames") Set<ThesisRoleName> roleNames,
            @Param("states") Set<ThesisState> states
    );
}
