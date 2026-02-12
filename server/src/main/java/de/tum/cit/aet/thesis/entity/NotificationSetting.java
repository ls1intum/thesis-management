package de.tum.cit.aet.thesis.entity;

import de.tum.cit.aet.thesis.entity.key.NotificationSettingId;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "notification_settings")
public class NotificationSetting {
	@EmbeddedId
	private NotificationSettingId id;

	@MapsId("userId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@NotNull
	@Column(name = "email", nullable = false)
	private String email;

	@UpdateTimestamp
	@NotNull
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
