package de.tum.cit.aet.thesis.core.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

class CorsConfigTest {

	@Test
	void corsConfigurationSource_isWildcardForAllPathsWithExpectedRules() {
		String clientHost = "https://example.com";
		CorsConfigurationSource source = new CorsConfig().corsConfigurationSource(clientHost);
		assertInstanceOf(UrlBasedCorsConfigurationSource.class, source);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/any/path");
		CorsConfiguration config = source.getCorsConfiguration(request);
		assertNotNull(config);

		assertTrue(config.getAllowedOrigins().contains(clientHost));
		assertTrue(config.getAllowedMethods().containsAll(java.util.List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")));
		assertTrue(config.getAllowedHeaders().containsAll(java.util.List.of("Authorization", "Content-Type", "Accept", "X-Requested-With")));
		assertEquals(Boolean.TRUE, config.getAllowCredentials());
	}
}
