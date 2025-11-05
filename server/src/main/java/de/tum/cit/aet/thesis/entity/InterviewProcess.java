package de.tum.cit.aet.thesis.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
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
