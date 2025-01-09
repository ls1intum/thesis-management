package de.tum.cit.aet.thesis.entity.key;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Embeddable
public class ApplicationReviewerId implements java.io.Serializable {
    @NotNull
    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        ApplicationReviewerId entity = (ApplicationReviewerId) o;
        return Objects.equals(this.applicationId, entity.applicationId) &&
                Objects.equals(this.userId, entity.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationId, userId);
    }

}