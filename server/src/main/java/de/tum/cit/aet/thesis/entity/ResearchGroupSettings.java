package de.tum.cit.aet.thesis.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "research_group_settings")
public class ResearchGroupSettings {
    @Id
    @Column(name = "research_group_id")
    private UUID researchGroupId;

    @Column(name = "automatic_reject_enabled", nullable = false)
    private boolean automaticRejectEnabled;

    @Column(name = "reject_duration")
    private int rejectDuration;
}

