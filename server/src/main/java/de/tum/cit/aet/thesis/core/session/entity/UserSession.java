package de.tum.cit.aet.thesis.core.session.entity;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * A Thesis Management session. The browser holds an opaque cookie value made of an identifier and a
 * verifier; only the identifier and a hash of the verifier are stored here, so a copy of this table
 * does not allow anyone to impersonate a user.
 */
@Getter
@Setter
@Entity
@Table(name = "user_sessions")
public class UserSession {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false)
	private UUID id;

	@NotNull
	@Column(name = "token_identifier", nullable = false, updatable = false)
	private String tokenIdentifier;

	@NotNull
	@Column(name = "verifier_hash", nullable = false, updatable = false)
	private byte[] verifierHash;

	@NotNull
	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;

	@NotNull
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@NotNull
	@Column(name = "last_used_at", nullable = false)
	private Instant lastUsedAt;

	@NotNull
	@Column(name = "expires_at", nullable = false, updatable = false)
	private Instant expiresAt;

	@Column(name = "ended_at")
	private Instant endedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "end_reason")
	private UserSessionEndReason endReason;

	@Column(name = "rotation_required", nullable = false)
	private boolean rotationRequired = false;

	@Column(name = "superseded_by")
	private UUID supersededBy;

	@Column(name = "superseded_until")
	private Instant supersededUntil;

	@NotNull
	@Column(name = "csrf_token", nullable = false, updatable = false)
	private String csrfToken;

	@Column(name = "created_ip")
	private String createdIp;

	@Column(name = "last_ip")
	private String lastIp;

	@Column(name = "user_agent")
	private String userAgent;

	@Column(name = "user_agent_signature")
	private String userAgentSignature;
}
