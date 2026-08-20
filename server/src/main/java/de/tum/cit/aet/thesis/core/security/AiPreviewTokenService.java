package de.tum.cit.aet.thesis.core.security;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Issues and validates short-lived, server-signed tokens that prove an AI feedback <em>preview</em>
 * was genuinely generated on the server for a specific thesis and user. The instructor
 * "Request Changes" flow echoes this token back when saving AI-reviewed drafts;
 * {@code ThesisService.requestChanges} only stamps {@code AI_REVIEWED_BY_HUMAN} on a row when the
 * token verifies, and records {@code HUMAN} otherwise. This stops a client from forging AI
 * provenance on hand-typed feedback — the token cannot be produced without server access.
 *
 * <p>Tokens are HMAC-SHA256 signed with a per-process random key, so they need no shared secret or
 * persistence. A process restart (or a second, key-less instance) simply invalidates outstanding
 * tokens, which fails safe: affected rows save as {@code HUMAN} rather than as forged AI rows.
 *
 * <p>This service is intentionally unconditional (not gated behind the AI feature flag) so that the
 * always-on {@code ThesisService} can validate tokens even when AI is disabled — in that case no
 * tokens are ever issued and every provenance claim collapses to {@code HUMAN}.
 */
@Service
public class AiPreviewTokenService {

	private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

	private final SecretKeySpec signingKey;

	public AiPreviewTokenService() {
		byte[] key = new byte[32];
		new SecureRandom().nextBytes(key);
		this.signingKey = new SecretKeySpec(key, HMAC_ALGORITHM);
	}

	/**
	 * Issues a signed token binding a freshly generated preview to {@code thesisId} and
	 * {@code userId}. The token is opaque to the client, which only echoes it back on save.
	 */
	public String issueToken(UUID thesisId, UUID userId) {
		String payload = thesisId + ":" + userId + ":" + Instant.now().toEpochMilli();
		String encodedPayload = ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
		return encodedPayload + "." + ENCODER.encodeToString(sign(encodedPayload));
	}

	/**
	 * Returns {@code true} only when {@code token} is a well-formed, correctly signed, unexpired
	 * token that this process issued for exactly this {@code thesisId} and {@code userId}. Any
	 * tampering, mismatch, or expiry returns {@code false} so the caller falls back to
	 * {@code HUMAN}.
	 */
	public boolean isValid(String token, UUID thesisId, UUID userId) {
		if (token == null) {
			return false;
		}

		int separator = token.indexOf('.');
		if (separator <= 0 || separator == token.length() - 1) {
			return false;
		}

		String encodedPayload = token.substring(0, separator);
		byte[] providedSignature;
		byte[] payloadBytes;
		try {
			providedSignature = DECODER.decode(token.substring(separator + 1));
			payloadBytes = DECODER.decode(encodedPayload);
		} catch (IllegalArgumentException ex) {
			return false;
		}

		// Constant-time comparison to avoid leaking signature bytes via timing.
		if (!MessageDigest.isEqual(sign(encodedPayload), providedSignature)) {
			return false;
		}

		String[] parts = new String(payloadBytes, StandardCharsets.UTF_8).split(":");
		if (parts.length != 3
				|| !parts[0].equals(thesisId.toString())
				|| !parts[1].equals(userId.toString())) {
			return false;
		}

		Instant issuedAt;
		try {
			issuedAt = Instant.ofEpochMilli(Long.parseLong(parts[2]));
		} catch (NumberFormatException ex) {
			return false;
		}

		Instant now = Instant.now();
		// Reject expired tokens and tokens dated in the future (clock games / tampering).
		return !issuedAt.isAfter(now) && !issuedAt.isBefore(now.minus(TOKEN_TTL));
	}

	private byte[] sign(String encodedPayload) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(signingKey);
			return mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8));
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Failed to sign AI preview token", ex);
		}
	}
}
