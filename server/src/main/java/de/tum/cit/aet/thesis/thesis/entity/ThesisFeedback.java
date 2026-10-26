package de.tum.cit.aet.thesis.thesis.entity;

import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackCategory;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSeverity;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSource;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackType;
import lombok.Getter;
import lombok.Setter;

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
@Table(name = "thesis_feedback")
public class ThesisFeedback {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "feedback_id", nullable = false)
	private UUID id;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "thesis_id", nullable = false)
	private Thesis thesis;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private ThesisFeedbackType type;

	@NotNull
	@Column(name = "feedback", nullable = false)
	private String feedback;

	@Column(name = "completed_at")
	private Instant completedAt;

	@NotNull
	@Column(name = "requested_at", nullable = false)
	private Instant requestedAt;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "requested_by", nullable = false)
	private User requestedBy;

	// Nullable: legacy rows created before the classification columns existed have no category
	// or severity. The UI renders them as "Uncategorized".
	@Enumerated(EnumType.STRING)
	@Column(name = "category")
	private ThesisFeedbackCategory category;

	@Enumerated(EnumType.STRING)
	@Column(name = "severity")
	private ThesisFeedbackSeverity severity;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "generation_source", nullable = false)
	private ThesisFeedbackSource generationSource = ThesisFeedbackSource.HUMAN;

	// Points at either a thesis_proposals row (for PROPOSAL feedback) or a thesis_files row (for
	// THESIS feedback); the {@link ThesisFeedbackType} disambiguates which table. Populated
	// automatically at write time with whatever revision was current when the feedback was
	// created, so later uploads don't reassign old comments to the wrong version.
	@Column(name = "document_version_id")
	private UUID documentVersionId;
}
