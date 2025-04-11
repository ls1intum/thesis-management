package de.tum.cit.aet.thesis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Entity
@Table(name = "research_groups")
public class ResearchGroup {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "research_group_id", nullable = false)
  private UUID id;

  @NotNull
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "head_user_id", nullable = false, unique = true)
  private User head;

  @NotBlank
  @Column(name = "name", nullable = false, unique = true)
  private String name;

  @Column(name = "abbreviation")
  private String abbreviation;

  @Column(name = "description")
  private String description;

  @Column(name = "website_url")
  private String websiteUrl;

  @Column(name = "campus")
  private String campus;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "updated_by", nullable = false)
  private User updatedBy;

  @NotNull
  @Column(name = "archived", nullable = false)
  private boolean archived = false;
}