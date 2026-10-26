package de.tum.cit.aet.thesis.core.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.tum.cit.aet.thesis.mock.BaseSessionIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.servlet.http.Cookie;

import java.util.List;

/** The cookie variant production uses: an https client host, so {@code __Host-sid} with {@code Secure}. */
@Testcontainers
@TestPropertySource(properties = "thesis-management.client.host=https://thesis.example.org")
class UserSessionSecureCookieTest extends BaseSessionIntegrationTest {
	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Test
	void httpsClientHost_usesTheHostPrefixedSecureCookieEndToEnd() throws Exception {
		createTestUser("secure-user", List.of("student"));

		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/v2/auth/session")
						.header(HttpHeaders.AUTHORIZATION, keycloakBearer("secure-user"))
						.header(HttpHeaders.USER_AGENT, CHROME_MAC))
				.andExpect(status().isOk())
				.andReturn();

		String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
		assertThat(setCookie).startsWith("__Host-sid=")
				.contains("Secure")
				.contains("HttpOnly")
				.contains("SameSite=Strict")
				.contains("Path=/")
				.doesNotContain("Domain");
		String value = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));

		mockMvc.perform(MockMvcRequestBuilders.get("/v2/auth/session")
						.cookie(new Cookie("__Host-sid", value))
						.header(HttpHeaders.USER_AGENT, CHROME_MAC))
				.andExpect(status().isOk());
		// the unprefixed name is not accepted in this configuration
		mockMvc.perform(MockMvcRequestBuilders.get("/v2/auth/session")
						.cookie(new Cookie("sid", value))
						.header(HttpHeaders.USER_AGENT, CHROME_MAC))
				.andExpect(status().isUnauthorized());
		// and an unknown prefixed cookie is cleared with the same name and attributes
		mockMvc.perform(MockMvcRequestBuilders.get("/v2/user-info")
						.cookie(new Cookie("__Host-sid", "unknown.value")))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.allOf(
						org.hamcrest.Matchers.startsWith("__Host-sid="),
						org.hamcrest.Matchers.containsString("Max-Age=0"),
						org.hamcrest.Matchers.containsString("Secure"))));
	}
}
