package de.tum.cit.aet.thesis.mock;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Lets the production security chains run in tests without a Keycloak server: the only thing replaced is
 * the decoder that verifies the Keycloak access token presented to the session exchange endpoint. It
 * verifies the HMAC signature and expiry of the tokens produced by
 * {@link BaseIntegrationTest#generateTestAuthenticationHeader}.
 */
@TestConfiguration
@Profile("session-test")
public class SessionTestSecurityConfig {
	@Bean
	public JwtDecoder jwtDecoder() {
		return token -> {
			try {
				var decoded = JWT.require(Algorithm.HMAC256("test-secret-key-for-jwt-tokens")).build().verify(token);

				Map<String, Object> claims = new HashMap<>();
				decoded.getClaims().forEach((key, value) -> {
					if (key.equals("exp") || key.equals("iat")) {
						claims.put(key, Instant.ofEpochSecond(value.asLong()));
					} else {
						claims.put(key, value.as(Object.class));
					}
				});
				claims.putIfAbsent("preferred_username", decoded.getSubject());

				return Jwt.withTokenValue(token)
						.header("alg", decoded.getAlgorithm())
						.claims(c -> c.putAll(claims))
						.build();
			} catch (JWTVerificationException e) {
				throw new BadJwtException(e.getMessage(), e);
			}
		};
	}
}
