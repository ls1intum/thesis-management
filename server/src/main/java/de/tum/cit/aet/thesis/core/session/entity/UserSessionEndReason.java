package de.tum.cit.aet.thesis.core.session.entity;

/**
 * Why a user session ended. Stored as text so the history of ended sessions stays readable.
 */
public enum UserSessionEndReason {
	LOGOUT,
	EXPIRED,
	REVOKED,
	ROTATED,
	USER_AGENT_MISMATCH,
	EVICTED
}
