package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.EmailTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {

    @Query(value = """
            SELECT et.* FROM email_templates et
            WHERE (:researchGroupId IS NULL OR et.research_group_id = :researchGroupId)
              AND (:searchQuery IS NULL OR et.description ILIKE CONCAT('%', :searchQuery, '%')
                OR et.subject ILIKE CONCAT('%', :searchQuery, '%')
                OR et.body_html ILIKE CONCAT('%', :searchQuery, '%'))
              AND (CAST(:templateCases AS TEXT[]) IS NULL OR et.template_case = ANY(CAST(:templateCases AS TEXT[])))
              AND (CAST(:languages AS TEXT[]) IS NULL OR et.language = ANY(CAST(:languages AS TEXT[])))
            """, nativeQuery = true)
    Page<EmailTemplate> searchEmailTemplate(
            @Param("researchGroupId") UUID researchGroupId,
            @Param("templateCases") String[] templateCases,
            @Param("languages") String[] languages,
            @Param("searchQuery") String searchQuery,
            Pageable page
    );

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
