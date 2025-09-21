package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.ResearchGroupSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResearchGroupSettingsRepository extends JpaRepository<ResearchGroupSettings, UUID> {
    @Query("""
            SELECT rs FROM ResearchGroupSettings rs WHERE
            rs.automaticRejectEnabled = true
            """)
    public List<ResearchGroupSettings> findAllByAutomaticRejectEnabled();
}

