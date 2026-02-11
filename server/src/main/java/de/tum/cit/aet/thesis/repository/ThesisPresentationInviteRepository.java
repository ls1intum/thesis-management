package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.ThesisPresentationInvite;
import de.tum.cit.aet.thesis.entity.key.ThesisPresentationInviteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Repository
public interface ThesisPresentationInviteRepository extends JpaRepository<ThesisPresentationInvite, ThesisPresentationInviteId> {
	void deleteByPresentationId(UUID id);
}
