package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.constants.DataExportState;
import de.tum.cit.aet.thesis.entity.DataExport;
import de.tum.cit.aet.thesis.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface DataExportRepository extends JpaRepository<DataExport, UUID> {
	List<DataExport> findAllByUserOrderByCreatedAtDesc(User user);

	List<DataExport> findAllByStateIn(List<DataExportState> states);

	@Query("SELECT e FROM DataExport e JOIN FETCH e.user WHERE e.id = :id")
	DataExport findByIdWithUser(@Param("id") UUID id);

	@Modifying
	@Transactional
	@Query("UPDATE DataExport e SET e.state = 'IN_CREATION' WHERE e.id = :id AND e.state = :expectedState")
	int claimForProcessing(@Param("id") UUID id, @Param("expectedState") DataExportState expectedState);

	@Query("""
			SELECT e FROM DataExport e
			WHERE e.creationFinishedAt < :cutoff
			AND e.state IN :states
			""")
	List<DataExport> findExpiredExports(
			@Param("cutoff") Instant cutoff,
			@Param("states") List<DataExportState> states);
}
