package de.tum.cit.aet.thesis.thesis.repository;

import de.tum.cit.aet.thesis.thesis.entity.ThesisFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface ThesisFileRepository extends JpaRepository<ThesisFile, UUID> {
	@Modifying
	@Transactional
	void deleteAllByThesisId(UUID thesisId);

	@Query("SELECT f.filename FROM ThesisFile f WHERE f.thesis.id = :thesisId")
	List<String> findFilenamesByThesisId(@Param("thesisId") UUID thesisId);

	/**
	 * The thesis's newest file of the given type, read straight from the database. Mirrors what
	 * {@code Thesis.getLatestFile} picks out of the {@code uploadedAt DESC} collection, but
	 * without going through it — callers use this precisely when that snapshot may be outdated.
	 */
	Optional<ThesisFile> findFirstByThesisIdAndTypeOrderByUploadedAtDesc(UUID thesisId, String type);
}
