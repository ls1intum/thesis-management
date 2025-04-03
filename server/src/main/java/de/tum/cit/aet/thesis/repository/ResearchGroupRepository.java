package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.ResearchGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ResearchGroupRepository extends JpaRepository<ResearchGroup, UUID> {
}