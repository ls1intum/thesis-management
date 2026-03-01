package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.ThesisProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface ThesisProposalRepository extends JpaRepository<ThesisProposal, UUID> {
	@Modifying
	@Transactional
	void deleteAllByThesisId(UUID thesisId);

	@Query("SELECT p.proposalFilename FROM ThesisProposal p WHERE p.thesis.id = :thesisId AND p.proposalFilename IS NOT NULL")
	List<String> findFilenamesByThesisId(@Param("thesisId") UUID thesisId);
}
