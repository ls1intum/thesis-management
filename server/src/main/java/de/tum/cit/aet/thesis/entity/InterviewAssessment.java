package de.tum.cit.aet.thesis.entity;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "interview_assessments")
public class InterviewAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "interview_assessment_id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interviewee_id", nullable = false)
    private Interviewee interviewee;

    @Column(name = "interview_note")
    private String interviewNote;
}
