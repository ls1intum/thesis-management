package de.tum.cit.aet.thesis.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import de.tum.cit.aet.thesis.constants.ApplicationReviewReason;
import de.tum.cit.aet.thesis.entity.key.ApplicationReviewerId;

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