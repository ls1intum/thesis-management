package de.tum.cit.aet.thesis.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
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
