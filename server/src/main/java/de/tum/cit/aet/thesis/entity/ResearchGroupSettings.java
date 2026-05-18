package de.tum.cit.aet.thesis.entity;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

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

	@Column(name = "reject_duration", nullable = false)
	private int rejectDuration;

	@Column(name = "presentation_slot_duration")
	private Integer presentationSlotDuration;

	@Column(name = "proposal_phase_active", nullable = false)
	private boolean proposalPhaseActive = true;

	@Column(name = "application_notification_email")
	private String applicationNotificationEmail;

	@Column(name = "scientific_writing_guide_link")
	private String scientificWritingGuideLink;

	@Column(name = "include_application_data_in_email", nullable = false)
	private boolean includeApplicationDataInEmail = false;

	@Column(name = "email_signature")
	private String emailSignature;
}
