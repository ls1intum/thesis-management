package de.tum.cit.aet.thesis.proposal.repository;

import de.tum.cit.aet.thesis.proposal.entity.ThesisProposal;
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
public interface ThesisProposalRepository extends JpaRepository<ThesisProposal, UUID> {
	@Modifying
	@Transactional
	void deleteAllByThesisId(UUID thesisId);

	@Query("SELECT p.proposalFilename FROM ThesisProposal p WHERE p.thesis.id = :thesisId AND p.proposalFilename IS NOT NULL")
	List<String> findFilenamesByThesisId(@Param("thesisId") UUID thesisId);

	/**
	 * The thesis's newest proposal, read straight from the database. Mirrors the
	 * {@code createdAt DESC} ordering of {@code Thesis.proposals}, but without going through that
	 * lazy collection — callers use this precisely when the in-memory snapshot may be outdated.
	 */
	Optional<ThesisProposal> findFirstByThesisIdOrderByCreatedAtDesc(UUID thesisId);
}
