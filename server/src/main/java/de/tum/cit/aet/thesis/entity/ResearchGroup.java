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

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "head_user_id", unique = true)
  private User head;

  @NotBlank
  @Column(name = "name", nullable = false, unique = true)
  private String name;

  @NotBlank
  @Column(name = "abbreviation", nullable = false, unique = true)
  private String abbreviation;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "website_url")
  private String websiteUrl;

  @Column(name = "campus")
  private String campus;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "updated_by")
  private User updatedBy;

  @NotNull
  @Column(name = "archived", nullable = false)
  private boolean archived = false;
}