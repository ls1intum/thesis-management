package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {
    Optional<EmailTemplate> findByResearchGroupIdAndTemplateCaseAndLanguage(UUID researchGroupId, String templateCase, String language);

    default Optional<EmailTemplate> findTemplateWithFallback(UUID researchGroupId, String templateCase, String language) {
        Optional<EmailTemplate> specific = findByResearchGroupIdAndTemplateCaseAndLanguage(researchGroupId, templateCase, language);
        if (specific.isPresent()) return specific;

        return findByResearchGroupIdAndTemplateCaseAndLanguage(null, templateCase, "en");
    }
}
