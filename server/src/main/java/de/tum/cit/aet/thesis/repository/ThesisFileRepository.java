package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.ThesisFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


@Repository
public interface ThesisFileRepository extends JpaRepository<ThesisFile, UUID> {
	@Modifying
	@Transactional
	void deleteAllByThesisId(UUID thesisId);

	@Query("SELECT f.filename FROM ThesisFile f WHERE f.thesis.id = :thesisId")
	List<String> findFilenamesByThesisId(@Param("thesisId") UUID thesisId);
}
