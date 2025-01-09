package de.tum.cit.aet.thesis.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "thesis_assessments")
public class ThesisAssessment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "assessment_id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thesis_id", nullable = false)
    private Thesis thesis;

    @NotNull
    @Column(name = "summary", nullable = false)
    private String summary;

    @NotNull
    @Column(name = "positives", nullable = false)
    private String positives;

    @NotNull
    @Column(name = "negatives", nullable = false)
    private String negatives;

    @NotNull
    @Column(name = "grade_suggestion", nullable = false)
    private String gradeSuggestion;

    @CreationTimestamp
    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

}