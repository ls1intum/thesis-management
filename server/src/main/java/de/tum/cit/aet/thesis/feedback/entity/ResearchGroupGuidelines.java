package de.tum.cit.aet.thesis.feedback.entity;

import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A research group's custom AI review guidelines. There is at most one record per research group
 * (the primary key is the research group id). The lead uploads {@link #rawGuidelines} as free
 * text; the system preprocesses them into {@link #structuredGuidelines} organized by the fixed
 * review categories.
 *
 * <p>The AI review features are gated on the existence of a {@link GuidelinesStatus#READY} record:
 * members of a group whose lead has not (successfully) uploaded guidelines cannot run reviews.
 */
@Getter
@Setter
@Entity
@Table(name = "research_group_guidelines")
public class ResearchGroupGuidelines {
	@Id
	@Column(name = "research_group_id", nullable = false)
	private UUID researchGroupId;

	@Column(name = "raw_guidelines", nullable = false, columnDefinition = "text")
	private String rawGuidelines;

	@Column(name = "structured_guidelines", columnDefinition = "jsonb")
	@JdbcTypeCode(SqlTypes.JSON)
	private StructuredGuidelines structuredGuidelines;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private GuidelinesStatus status;

	/** Reason the guidelines were rejected; only set when {@link #status} is {@link GuidelinesStatus#FAILED}. */
	@Column(name = "failure_reason", columnDefinition = "text")
	private String failureReason;

	/** When the guidelines were last successfully preprocessed; null while {@link GuidelinesStatus#FAILED}. */
	@Column(name = "processed_at")
	private Instant processedAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "updated_by")
	private User updatedBy;

	public boolean isReady() {
		return status == GuidelinesStatus.READY;
	}
}
