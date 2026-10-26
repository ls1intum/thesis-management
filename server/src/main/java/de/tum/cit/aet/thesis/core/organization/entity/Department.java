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

/** A department that belongs to exactly one school. */
@Getter
@Setter
@Entity
@Table(name = "departments")
@BatchSize(size = 50)
public class Department {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "department_id", nullable = false)
	private UUID id;

	@NotNull
	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "school_id", nullable = false)
	private School school;

	@NotBlank
	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "abbreviation")
	private String abbreviation;

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
