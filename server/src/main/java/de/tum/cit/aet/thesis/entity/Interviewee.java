package de.tum.cit.aet.thesis.entity;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "interviewees")
public class Interviewee {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "interviewee_id", nullable = false)
	private UUID intervieweeId;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "interview_process_id", nullable = false)
	private InterviewProcess interviewProcess;

	@Column(name = "last_invited")
	private Instant lastInvited;

	@Column(name = "score")
	private Integer score;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "application_id", nullable = false)
	private Application application;

	@OneToMany(mappedBy = "interviewee", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<InterviewAssessment> assessments = new ArrayList<>();

	@OneToMany(mappedBy = "interviewee", fetch = FetchType.LAZY)
	private List<InterviewSlot> slots = new ArrayList<>();

	public InterviewSlot getNextSlot() {
		return slots.stream()
				.min((slot1, slot2) -> slot1.getStartDate().compareTo(slot2.getStartDate()))
				.orElse(null);
	}
}
