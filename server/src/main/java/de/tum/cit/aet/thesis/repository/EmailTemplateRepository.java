package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {
    @Query("""
            SELECT e FROM EmailTemplate e
            WHERE (:researchGroupId IS NULL AND e.researchGroup.id IS NULL
                   OR e.researchGroup.id = :researchGroupId)
            AND e.templateCase = :templateCase
            AND e.language = :language
            """)
    Optional<EmailTemplate> findByResearchGroupIdAndTemplateCaseAndLanguage(
            @Param("researchGroupId") UUID researchGroupId,
            @Param("templateCase") String templateCase,
            @Param("language") String language
    );

    default Optional<EmailTemplate> findTemplateWithFallback(UUID researchGroupId, String templateCase, String language) {
        Optional<EmailTemplate> specific = findByResearchGroupIdAndTemplateCaseAndLanguage(researchGroupId, templateCase, language);
        if (specific.isPresent()) return specific;

        return findByResearchGroupIdAndTemplateCaseAndLanguage(null, templateCase, "en");
    }
}
