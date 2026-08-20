package de.tum.cit.aet.thesis.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.UUID;

class AiPreviewTokenServiceTest {

	private final AiPreviewTokenService service = new AiPreviewTokenService();

	@Test
	void validatesTokenForMatchingThesisAndUser() {
		UUID thesisId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		String token = service.issueToken(thesisId, userId);

		assertThat(service.isValid(token, thesisId, userId)).isTrue();
	}

	@Test
	void rejectsTokenBoundToADifferentThesis() {
		UUID userId = UUID.randomUUID();
		String token = service.issueToken(UUID.randomUUID(), userId);

		assertThat(service.isValid(token, UUID.randomUUID(), userId)).isFalse();
	}

	@Test
	void rejectsTokenBoundToADifferentUser() {
		UUID thesisId = UUID.randomUUID();
		String token = service.issueToken(thesisId, UUID.randomUUID());

		assertThat(service.isValid(token, thesisId, UUID.randomUUID())).isFalse();
	}

	@Test
	void rejectsTamperedSignature() {
		UUID thesisId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		String token = service.issueToken(thesisId, userId);

		// Flip the last signature character.
		char last = token.charAt(token.length() - 1);
		String tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');

		assertThat(service.isValid(tampered, thesisId, userId)).isFalse();
	}

	@Test
	void rejectsTokenForgedByAnotherKey() {
		UUID thesisId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		// A second instance has its own random signing key, so it cannot mint tokens this one trusts.
		String foreignToken = new AiPreviewTokenService().issueToken(thesisId, userId);

		assertThat(service.isValid(foreignToken, thesisId, userId)).isFalse();
	}

	@Test
	void rejectsNullAndMalformedTokens() {
		UUID thesisId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		assertThat(service.isValid(null, thesisId, userId)).isFalse();
		assertThat(service.isValid("", thesisId, userId)).isFalse();
		assertThat(service.isValid("no-dot-separator", thesisId, userId)).isFalse();
		assertThat(service.isValid(".", thesisId, userId)).isFalse();
		assertThat(service.isValid("not-base64.also-not-base64", thesisId, userId)).isFalse();
	}
}
