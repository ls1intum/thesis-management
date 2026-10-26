package de.tum.cit.aet.thesis.feedback.entity;

import de.tum.cit.aet.thesis.feedback.model.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
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
 * score, the {@link AssessmentCategory}, a short summary, and the document revision it was
 * produced from. There is at most one record per {@code (thesis, type)} pair — each auto-review
 * run upserts this row, independent of whether any resulting findings are actually saved.
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

	/**
	 * The revision this summary was produced from — a {@code thesis_proposals} id for
	 * {@link ReviewType#PROPOSAL}, a {@code thesis_files} id for {@link ReviewType#THESIS}. Lets
	 * the UI drop the score once a newer document is uploaded rather than showing a stale one as
	 * current. Null for rows written before the column existed, or when the reviewed document
	 * could not be resolved.
	 */
	@Column(name = "document_version_id")
	private UUID documentVersionId;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
