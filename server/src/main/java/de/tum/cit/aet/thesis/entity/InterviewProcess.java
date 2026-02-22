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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "interview_processes")
public class InterviewProcess {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "interview_process_id", nullable = false)
	private UUID id;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "topic_id", nullable = false)
	private Topic topic;

	@Column(name = "completed", nullable = false)
	private boolean completed = false;

	@OneToMany(mappedBy = "interviewProcess", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<Interviewee> interviewees = new ArrayList<>();

	@OneToMany(mappedBy = "interviewProcess", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<InterviewSlot> slots = new ArrayList<>();
}
