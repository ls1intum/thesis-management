package de.tum.cit.aet.thesis.entity;

import de.tum.cit.aet.thesis.constants.ApplicationReviewReason;
import de.tum.cit.aet.thesis.entity.key.ApplicationReviewerId;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "application_reviewers")
public class ApplicationReviewer {
    @EmbeddedId
    private ApplicationReviewerId id;

    @MapsId("applicationId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private ApplicationReviewReason reason;

    @NotNull
    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

}
