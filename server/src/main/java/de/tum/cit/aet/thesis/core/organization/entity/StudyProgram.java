package de.tum.cit.aet.thesis.core.organization.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/** A study program a student is enrolled in. It may be associated with a school. */
@Getter
@Setter
@Entity
@Table(name = "study_programs")
@BatchSize(size = 50)
public class StudyProgram {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "study_program_id", nullable = false)
	private UUID id;

	/** Stable identifier, e.g. {@code COMPUTER_SCIENCE}. Kept from the times study programs were configured by key. */
	@NotBlank
	@Column(name = "key", nullable = false)
	private String key;

	@NotBlank
	@Column(name = "name", nullable = false)
	private String name;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "school_id")
	private School school;

	@NotNull
	@Column(name = "active", nullable = false)
	private boolean active = true;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
