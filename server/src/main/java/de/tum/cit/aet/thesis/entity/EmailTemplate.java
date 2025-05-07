package de.tum.cit.aet.thesis.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "email_templates", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"research_group_id", "template_case", "language"})
})
public class EmailTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "email_template_id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "research_group_id")
    private ResearchGroup researchGroup;

    @NotBlank
    @Column(name = "template_case", nullable = false)
    private String templateCase;

    @Column(name = "description")
    private String description;

    @NotBlank
    @Column(name = "subject", nullable = false)
    private String subject;

    @NotNull
    @Column(name = "body_html", nullable = false)
    private String bodyHtml;

    @NotBlank
    @Column(nullable = false)
    private String language;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "updated_by", nullable = false)
    private User updatedBy;

    @UpdateTimestamp
    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}