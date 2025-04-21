package de.tum.cit.aet.thesis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import de.tum.cit.aet.thesis.entity.EmailTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {
    Optional<EmailTemplate> findByResearchGroupIdAndTemplateCaseAndLanguage(UUID researchGroupId, String templateCase, String language);

    List<EmailTemplate> findAllByResearchGroupId(UUID researchGroupId);
}
