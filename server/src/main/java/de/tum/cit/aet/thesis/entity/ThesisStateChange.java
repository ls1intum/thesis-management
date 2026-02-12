package de.tum.cit.aet.thesis.entity;

import de.tum.cit.aet.thesis.entity.key.ThesisStateChangeId;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
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
@Table(name = "thesis_state_changes")
public class ThesisStateChange {
	@EmbeddedId
	private ThesisStateChangeId id;

	@MapsId("thesisId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "thesis_id", nullable = false)
	private Thesis thesis;

	@NotNull
	@Column(name = "changed_at", nullable = false)
	private Instant changedAt;

}
