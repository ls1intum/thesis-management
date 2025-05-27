package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUniversityId(String universityId);

    List<User> findAllByUniversityIdIn(List<String> universityIds);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.researchGroup WHERE u.universityId = :universityId")
    Optional<User> findByUniversityIdWithResearchGroup(@Param("universityId") String universityId);

    @Query("""
            SELECT DISTINCT u
            FROM User u
            LEFT JOIN UserGroup g ON (u.id = g.id.userId)
            WHERE (:researchGroupId IS NULL
                   OR NOT ('advisor' IN :groups
                           OR 'supervisor' IN :groups)
                   OR u.researchGroup.id = :researchGroupId)
              AND ((:groups IS NULL)
                     OR ('student' IN :groups AND (g.id.group IN :groups OR g.id.group IS NULL))
                     OR g.id.group IN :groups)
              AND (:searchQuery IS NULL
                   OR LOWER(u.firstName) || ' ' || LOWER(u.lastName) LIKE %:searchQuery%
                   OR LOWER(u.email) LIKE %:searchQuery%
                   OR LOWER(u.matriculationNumber) LIKE %:searchQuery%
                   OR LOWER(u.universityId) LIKE %:searchQuery%)
            """)
    Page<User> searchUsers(@Param("researchGroupId") UUID researchGroupId,
                           @Param("searchQuery") String searchQuery, @Param("groups") Set<String> groups, Pageable page);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN UserGroup g ON (u.id = g.id.userId) WHERE g"
            + ".id.group IN :roles AND (:researchGroupId IS NULL OR u.researchGroup.id = "
            + ":researchGroupId)")
    List<User> getRoleMembers(@Param("roles") Set<String> roles,
                              @Param("researchGroupId") UUID researchGroupId);

    @Query("""
                SELECT DISTINCT u FROM User u
                WHERE u.id IN (
                    SELECT tr.user.id FROM ThesisRole tr
                    JOIN tr.thesis t
                    WHERE t.researchGroup.id = :researchGroupId
                      AND t.state NOT IN ('FINISHED', 'DROPPED_OUT')
                      AND tr.id.role = 'STUDENT'
                )
            """)
    List<User> findStudentsWithActiveThesesByResearchGroupId(@Param("researchGroupId") UUID researchGroupId);
}
