package de.tum.cit.aet.thesis.entity;

import de.tum.cit.aet.thesis.constants.DataExportState;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "data_exports")
public class DataExport {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "data_export_id", nullable = false)
	private UUID id;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "state", nullable = false)
	private DataExportState state;

	@Column(name = "file_path")
	private String filePath;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "creation_finished_at")
	private Instant creationFinishedAt;

	@Column(name = "downloaded_at")
	private Instant downloadedAt;
}
