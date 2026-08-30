package de.tum.cit.aet.thesis.feedback.entity;

import de.tum.cit.aet.thesis.feedback.dto.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.service.reviewer.ReviewType;
import de.tum.cit.aet.thesis.thesis.entity.Thesis;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * The AI review pipeline's latest read on a thesis's proposal or thesis document: a numeric
 * score, the {@link AssessmentCategory}, and a short summary. There is at most one record per
 * {@code (thesis, type)} pair — every review run (student auto-review or supervisor preview)
 * upserts this row, independent of whether any resulting findings are actually saved.
 */
@Getter
@Setter
@Entity
@Table(name = "ai_review_summary", uniqueConstraints = @UniqueConstraint(columnNames = {"thesis_id", "type"}))
public class AIReviewSummary {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "ai_review_summary_id", nullable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "thesis_id", nullable = false)
	private Thesis thesis;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 20)
	private ReviewType type;

	@Column(name = "score")
	private Integer score;

	@Enumerated(EnumType.STRING)
	@Column(name = "assessment", length = 20)
	private AssessmentCategory assessment;

	@Column(name = "summary", columnDefinition = "text")
	private String summary;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
