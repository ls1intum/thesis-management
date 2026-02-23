package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.ThesisRole;
import de.tum.cit.aet.thesis.entity.key.ThesisRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public interface ThesisRoleRepository extends JpaRepository<ThesisRole, ThesisRoleId> {
	List<ThesisRole> deleteByThesisId(UUID thesisId);

	@Query("SELECT tr FROM ThesisRole tr JOIN FETCH tr.thesis WHERE tr.id.userId = :userId")
	List<ThesisRole> findAllByIdUserIdWithThesis(@Param("userId") UUID userId);

	List<ThesisRole> findAllByIdUserId(UUID userId);

	void deleteAllByIdUserId(UUID userId);
}
