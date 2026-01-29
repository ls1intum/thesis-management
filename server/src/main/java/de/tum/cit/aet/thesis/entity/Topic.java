package de.tum.cit.aet.thesis.entity;

import de.tum.cit.aet.thesis.constants.ThesisRoleName;
import de.tum.cit.aet.thesis.constants.TopicState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "topics")
public class Topic {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "topic_id", nullable = false)
  private UUID id;

  @NotNull
  @Column(name = "title", nullable = false)
  private String title;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "thesis_types", columnDefinition = "text[]")
  private Set<String> thesisTypes = new HashSet<>();

  @NotNull
  @Column(name = "problem_statement", nullable = false)
  private String problemStatement;

  @NotNull
  @Column(name = "requirements", nullable = false)
  private String requirements;

  @NotNull
  @Column(name = "goals", nullable = false)
  private String goals;

  @NotNull
  @Column(name = "\"references\"", nullable = false)
  private String references;

  @Column(name = "closed_at")
  private Instant closedAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  @UpdateTimestamp
  @NotNull
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @CreationTimestamp
  @NotNull
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name="intended_start")
  private Instant intendedStart;

  @Column(name="application_deadline")
  private Instant applicationDeadline;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "research_group_id", nullable = false)
  private ResearchGroup researchGroup;

  @OneToMany(mappedBy = "topic", fetch = FetchType.EAGER)
  @OrderBy("position ASC")
  private List<TopicRole> roles = new ArrayList<>();

  public List<User> getAdvisors() {
    List<User> result = new ArrayList<>();

    for (TopicRole role : getRoles()) {
        if (role.getId().getRole() == ThesisRoleName.ADVISOR) {
            result.add(role.getUser());
        }
    }

    return result;
  }

  public TopicState getTopicState() {
    if (this.closedAt != null) {
      return TopicState.CLOSED;
    } else if (this.publishedAt == null) {
      return TopicState.DRAFT;
    } else {
      return TopicState.OPEN;
    }
  }
}