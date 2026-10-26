package de.tum.cit.aet.thesis.core.session.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Creates, parses and verifies the opaque session credential.
 *
 * <p>The cookie value is {@code <identifier>.<verifier>}. The identifier (128 random bits) is stored in
 * clear and used to look the session up; the verifier (256 random bits) is independent of it and only its
 * SHA-256 hash is stored, so reading the session table does not reveal a usable credential. The
 * identifier alone never authenticates a request.</p>
 */
public final class UserSessionTokens {
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final int IDENTIFIER_BYTES = 16;
	private static final int VERIFIER_BYTES = 32;
	private static final int MAX_COOKIE_VALUE_LENGTH = 200;
	private static final Pattern PART = Pattern.compile("[A-Za-z0-9_-]+");

	private UserSessionTokens() {
	}

	/**
	 * A freshly generated credential.
	 *
	 * @param identifier public half used to look the session up
	 * @param verifier secret half that proves possession of the credential
	 */
	public record Issued(String identifier, String verifier) {
		/**
		 * Returns the value to put into the session cookie.
		 *
		 * @return {@code identifier.verifier}
		 */
		public String cookieValue() {
			return identifier + "." + verifier;
		}
	}

	/**
	 * A credential received from a client.
	 *
	 * @param identifier the public half
	 * @param verifier the secret half
	 */
	public record Parsed(String identifier, String verifier) {
	}

	/**
	 * Generates a new credential from a cryptographically secure random source.
	 *
	 * @return the new identifier and verifier
	 */
	public static Issued generate() {
		return new Issued(randomToken(IDENTIFIER_BYTES), randomToken(VERIFIER_BYTES));
	}

	/**
	 * Parses and validates the syntax of a cookie value without touching any stored state.
	 *
	 * @param cookieValue the raw cookie value, may be null
	 * @return the parts, or empty if the value is not a well-formed credential
	 */
	public static Optional<Parsed> parse(String cookieValue) {
		if (cookieValue == null || cookieValue.isEmpty() || cookieValue.length() > MAX_COOKIE_VALUE_LENGTH) {
			return Optional.empty();
		}

		String[] parts = cookieValue.split("\\.", -1);

		if (parts.length != 2 || !PART.matcher(parts[0]).matches() || !PART.matcher(parts[1]).matches()) {
			return Optional.empty();
		}

		return Optional.of(new Parsed(parts[0], parts[1]));
	}

	/**
	 * Hashes a verifier for storage. A fast hash is sufficient because the verifier is a long random value.
	 *
	 * @param verifier the verifier
	 * @return the SHA-256 hash
	 */
	public static byte[] hashVerifier(String verifier) {
		return sha256(verifier);
	}

	/**
	 * Compares a presented verifier with the stored hash in constant time.
	 *
	 * @param storedHash the hash stored with the session
	 * @param verifier the presented verifier
	 * @return whether the verifier matches
	 */
	public static boolean matches(byte[] storedHash, String verifier) {
		return MessageDigest.isEqual(storedHash, hashVerifier(verifier));
	}

	/**
	 * Returns a short correlation id for log lines. It is derived from a hash, so logs never contain a
	 * credential or its identifier.
	 *
	 * @param identifier the session identifier
	 * @return the first 12 hexadecimal characters of the identifier's SHA-256 hash
	 */
	public static String logId(String identifier) {
		return HexFormat.of().formatHex(sha256(identifier)).substring(0, 12);
	}

	/**
	 * Generates a random URL-safe token, for example a CSRF token.
	 *
	 * @param bytes number of random bytes
	 * @return the token, base64url encoded without padding
	 */
	public static String randomToken(int bytes) {
		byte[] value = new byte[bytes];
		RANDOM.nextBytes(value);

		return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
	}

	private static byte[] sha256(String value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is not available", e);
		}
	}
}
