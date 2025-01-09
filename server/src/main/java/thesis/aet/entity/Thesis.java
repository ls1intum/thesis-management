package de.tum.cit.aet.thesis.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import de.tum.cit.aet.thesis.constants.ThesisFeedbackType;
import de.tum.cit.aet.thesis.constants.ThesisRoleName;
import de.tum.cit.aet.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.constants.ThesisVisibility;
import de.tum.cit.aet.thesis.dto.LightUserDto;
import de.tum.cit.aet.thesis.entity.jsonb.ThesisMetadata;

import java.time.Instant;
import java.util.*;

@Getter
@Setter
@Entity
@Table(name = "theses")
public class Thesis {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "thesis_id", nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "title", nullable = false)
    private String title;

    @NotNull
    @Column(name = "type", nullable = false)
    private String type;

    @NotNull
    @Column(name = "language", nullable = false)
    private String language;

    @NotNull
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private ThesisMetadata metadata;

    @NotNull
    @Column(name = "info", nullable = false)
    private String info;

    @NotNull
    @Column(name = "abstract", nullable = false)
    private String abstractField;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private ThesisState state;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private ThesisVisibility visibility;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "keywords", columnDefinition = "text[]")
    private Set<String> keywords = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private Application application;

    @Column(name = "final_grade")
    private String finalGrade;

    @Column(name = "final_feedback")
    private String finalFeedback;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @CreationTimestamp
    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "thesis", fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    private List<ThesisRole> roles = new ArrayList<>();

    @OneToMany(mappedBy = "thesis", fetch = FetchType.EAGER)
    @OrderBy("createdAt DESC")
    private List<ThesisProposal> proposals = new ArrayList<>();

    @OneToMany(mappedBy = "thesis", fetch = FetchType.EAGER)
    @OrderBy("createdAt DESC")
    private List<ThesisAssessment> assessments = new ArrayList<>();

    @OneToMany(mappedBy = "thesis", fetch = FetchType.EAGER)
    @OrderBy("scheduledAt ASC")
    private List<ThesisPresentation> presentations = new ArrayList<>();

    @OneToMany(mappedBy = "thesis", fetch = FetchType.EAGER)
    @OrderBy("requestedAt ASC")
    private List<ThesisFeedback> feedback = new ArrayList<>();

    @OneToMany(mappedBy = "thesis", fetch = FetchType.EAGER)
    @OrderBy("uploadedAt DESC")
    private List<ThesisFile> files = new ArrayList<>();

    @OneToMany(mappedBy = "thesis", fetch = FetchType.EAGER)
    private Set<ThesisStateChange> states = new HashSet<>();

    public List<User> getStudents() {
        List<User> result = new ArrayList<>();

        for (ThesisRole role : getRoles()) {
            if (role.getId().getRole() == ThesisRoleName.STUDENT) {
                result.add(role.getUser());
            }
        }

        return result;
    }

    public List<User> getAdvisors() {
        List<User> result = new ArrayList<>();

        for (ThesisRole role : getRoles()) {
            if (role.getId().getRole() == ThesisRoleName.ADVISOR) {
                result.add(role.getUser());
            }
        }

        return result;
    }

    public List<User> getSupervisors() {
        List<User> result = new ArrayList<>();

        for (ThesisRole role : getRoles()) {
            if (role.getId().getRole() == ThesisRoleName.SUPERVISOR) {
                result.add(role.getUser());
            }
        }

        return result;
    }

    public boolean hasSupervisorAccess(User user) {
        if (user == null) {
            return false;
        }

        if (user.hasAnyGroup("admin")) {
            return true;
        }

        for (ThesisRole role : roles) {
            if (
                    role.getId().getRole().equals(ThesisRoleName.SUPERVISOR) &&
                    user.hasAnyGroup("supervisor") &&
                    role.getUser().getId().equals(user.getId())
            ) {
                return true;
            }
        }

        return false;
    }

    public boolean hasAdvisorAccess(User user) {
        if (user == null) {
            return false;
        }

        if (hasSupervisorAccess(user)) {
            return true;
        }

        for (ThesisRole role : roles) {
            if (
                    role.getId().getRole().equals(ThesisRoleName.ADVISOR) &&
                    user.hasAnyGroup("advisor") &&
                    role.getUser().getId().equals(user.getId())
            ) {
                return true;
            }
        }

        return false;
    }

    public boolean hasStudentAccess(User user) {
        if (user == null) {
            return false;
        }

        if (hasAdvisorAccess(user)) {
            return true;
        }

        for (ThesisRole role : roles) {
            if (role.getId().getRole().equals(ThesisRoleName.STUDENT) && role.getUser().getId().equals(user.getId())) {
                return true;
            }
        }

        return false;
    }

    public boolean hasReadAccess(User user) {
        if (visibility == ThesisVisibility.PUBLIC && state == ThesisState.FINISHED) {
            return true;
        }

        if (user == null) {
            return false;
        }

        if (hasStudentAccess(user)) {
            return true;
        }

        if (visibility.equals(ThesisVisibility.PUBLIC)) {
            return true;
        }

        if (visibility.equals(ThesisVisibility.INTERNAL) && user.hasAnyGroup("advisor", "supervisor")) {
            return true;
        }

        if (visibility.equals(ThesisVisibility.STUDENT) && user.hasAnyGroup("student", "advisor", "supervisor")) {
            return true;
        }

        return false;
    }

    public Optional<ThesisPresentation> getPresentation(UUID presentationId) {
        for (ThesisPresentation presentation : getPresentations()) {
            if (presentation.getId().equals(presentationId)) {
                return Optional.of(presentation);
            }
        }

        return Optional.empty();
    }

    public Optional<ThesisFeedback> getFeedbackItem(UUID feedbackId) {
        for (ThesisFeedback item : getFeedback()) {
            if (item.getId().equals(feedbackId)) {
                return Optional.of(item);
            }
        }

        return Optional.empty();
    }

    public Optional<ThesisFile> getFileById(UUID fileId) {
        for (ThesisFile item : getFiles()) {
            if (item.getId().equals(fileId)) {
                return Optional.of(item);
            }
        }

        return Optional.empty();
    }

    public Optional<ThesisFile> getLatestFile(String type) {
        for (ThesisFile item : getFiles()) {
            if (item.getType().equals(type)) {
                return Optional.of(item);
            }
        }

        return Optional.empty();
    }

    public Optional<ThesisProposal> getProposalById(UUID proposalId) {
        for (ThesisProposal item : getProposals()) {
            if (item.getId().equals(proposalId)) {
                return Optional.of(item);
            }
        }

        return Optional.empty();
    }
}