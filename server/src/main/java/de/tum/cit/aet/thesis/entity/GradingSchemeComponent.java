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

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "grading_scheme_components")
public class GradingSchemeComponent {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "component_id", nullable = false)
	private UUID id;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "research_group_id", nullable = false)
	private ResearchGroup researchGroup;

	@NotNull
	@Column(name = "name", nullable = false)
	private String name;

	@NotNull
	@Column(name = "weight", nullable = false, precision = 5, scale = 2)
	private BigDecimal weight;

	@NotNull
	@Column(name = "is_bonus", nullable = false)
	private Boolean isBonus = false;

	@NotNull
	@Column(name = "position", nullable = false)
	private Integer position = 0;
}
