package de.tum.cit.aet.thesis.core.session.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Optional;

class UserSessionTokensTest {
	@Test
	void generatedTokensAreDistinctAndParseRoundTrips() {
		UserSessionTokens.Issued first = UserSessionTokens.generate();
		UserSessionTokens.Issued second = UserSessionTokens.generate();

		assertThat(first.identifier()).isNotEqualTo(second.identifier());
		assertThat(first.verifier()).isNotEqualTo(second.verifier());
		assertThat(Base64.getUrlDecoder().decode(first.identifier())).hasSize(16);
		assertThat(Base64.getUrlDecoder().decode(first.verifier())).hasSize(32);

		Optional<UserSessionTokens.Parsed> parsed = UserSessionTokens.parse(first.cookieValue());

		assertThat(parsed).isPresent();
		assertThat(parsed.get().identifier()).isEqualTo(first.identifier());
		assertThat(parsed.get().verifier()).isEqualTo(first.verifier());
	}

	@Test
	void parseRejectsMissingSeparatorEmptyPartsAndOversizedValue() {
		assertThat(UserSessionTokens.parse(null)).isEmpty();
		assertThat(UserSessionTokens.parse("")).isEmpty();
		assertThat(UserSessionTokens.parse("nodot")).isEmpty();
		assertThat(UserSessionTokens.parse(".verifier")).isEmpty();
		assertThat(UserSessionTokens.parse("identifier.")).isEmpty();
		assertThat(UserSessionTokens.parse("a.b.c")).isEmpty();
		assertThat(UserSessionTokens.parse("ident ifier.verifier")).isEmpty();
		assertThat(UserSessionTokens.parse("a".repeat(150) + "." + "b".repeat(150))).isEmpty();
	}

	@Test
	void verifierMatchesOnlyOriginal() {
		UserSessionTokens.Issued issued = UserSessionTokens.generate();
		byte[] stored = UserSessionTokens.hashVerifier(issued.verifier());

		assertThat(stored).hasSize(32);
		assertThat(stored).isNotEqualTo(issued.verifier().getBytes());
		assertThat(UserSessionTokens.matches(stored, issued.verifier())).isTrue();
		assertThat(UserSessionTokens.matches(stored, UserSessionTokens.generate().verifier())).isFalse();
		assertThat(UserSessionTokens.matches(stored, "")).isFalse();
	}

	@Test
	void logIdDoesNotContainIdentifierAndIsStable() {
		String identifier = UserSessionTokens.generate().identifier();

		String logId = UserSessionTokens.logId(identifier);

		assertThat(logId).hasSize(12).matches("[0-9a-f]{12}");
		assertThat(identifier).doesNotContain(logId);
		assertThat(UserSessionTokens.logId(identifier)).isEqualTo(logId);
	}

	@Test
	void randomTokensHaveRequestedEntropy() {
		assertThat(Base64.getUrlDecoder().decode(UserSessionTokens.randomToken(32))).hasSize(32);
		assertThat(UserSessionTokens.randomToken(32)).isNotEqualTo(UserSessionTokens.randomToken(32));
	}
}
