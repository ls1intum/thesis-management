package de.tum.cit.aet.thesis.core.organization.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/** A school (called faculty at some universities) that groups departments and study programs. */
@Getter
@Setter
@Entity
@Table(name = "schools")
@BatchSize(size = 50)
public class School {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "school_id", nullable = false)
	private UUID id;

	@NotBlank
	@Column(name = "name", nullable = false)
	private String name;

	@NotBlank
	@Column(name = "abbreviation", nullable = false)
	private String abbreviation;

	@Column(name = "website_url")
	private String websiteUrl;

	@Column(name = "thesis_portal_url")
	private String thesisPortalUrl;

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
