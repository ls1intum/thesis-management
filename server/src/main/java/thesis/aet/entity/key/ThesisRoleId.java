package de.tum.cit.aet.thesis.entity.key;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;
import de.tum.cit.aet.thesis.constants.ThesisRoleName;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Embeddable
public class ThesisRoleId implements Serializable {
    @NotNull
    @Column(name = "thesis_id", nullable = false)
    private UUID thesisId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private ThesisRoleName role;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        ThesisRoleId entity = (ThesisRoleId) o;
        return Objects.equals(this.thesisId, entity.thesisId) &&
                Objects.equals(this.userId, entity.userId) &&
                Objects.equals(this.role, entity.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash("thesis-roles", thesisId, userId);
    }

}