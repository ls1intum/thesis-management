package de.tum.cit.aet.thesis.entity;

import de.tum.cit.aet.thesis.constants.ThesisCommentType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "thesis_comments")
public class ThesisComment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "comment_id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thesis_id", nullable = false)
    private Thesis thesis;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JoinColumn(name = "type", nullable = false)
    private ThesisCommentType type;

    @NotNull
    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "filename")
    private String filename;

    @Column(name = "upload_name")
    private String uploadName;

    @CreationTimestamp
    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    public boolean hasManagementAccess(User user) {
        return user.hasAnyGroup("admin") || createdBy.getId().equals(user.getId());
    }

    public ResearchGroup getResearchGroup() {
        return thesis.getResearchGroup();
    }
}
