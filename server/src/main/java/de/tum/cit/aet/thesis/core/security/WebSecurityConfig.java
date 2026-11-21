package de.tum.cit.aet.thesis.core.security;

import de.tum.cit.aet.thesis.core.session.security.InvalidSessionTokenLimiter;
import de.tum.cit.aet.thesis.core.session.security.SessionCsrfTokenRepository;
import de.tum.cit.aet.thesis.core.session.security.UserAuthorities;
import de.tum.cit.aet.thesis.core.session.security.UserSessionAuthenticationFilter;
import de.tum.cit.aet.thesis.core.session.security.UserSessionCookies;
import de.tum.cit.aet.thesis.core.session.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.DispatcherType;

import java.time.Clock;
import java.util.Set;

/**
 * Two security chains. Keycloak is only trusted at the session exchange; every other request is
 * authenticated by the application's own session cookie.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
@Profile("!test")   // NOTE: this is a workaround to avoid overlapping definitions during test execution
public class WebSecurityConfig {
	private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

	private final UserSessionService sessionService;
	private final UserSessionCookies sessionCookies;
	private final UserAuthorities userAuthorities;
	private final Clock clock;
	private final InvalidSessionTokenLimiter invalidSessionTokenLimiter;

	/**
	 * Chain for {@code POST /v2/auth/session}: the one request that is authenticated by a Keycloak access
	 * token. Spring Security validates the token's signature, issuer and expiry; the principal name is the
	 * token's {@code preferred_username}, which is the university id.
	 */
	@Bean
	@Order(1)
	public SecurityFilterChain sessionExchangeFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setPrincipalClaimName("preferred_username");
		converter.setJwtGrantedAuthoritiesConverter(jwt -> java.util.List.of());

		RequestMatcher sessionExchange = PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/v2/auth/session");

		http
				.securityMatcher(sessionExchange)
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				// CSRF protection stays on; the one request this chain serves is exempt. It is authenticated only by a
				// bearer token, which a browser never attaches on its own, so a forged cross-site request cannot carry
				// it. No cookie authenticates this request (there is no session before it).
				.csrf(csrf -> csrf.ignoringRequestMatchers(sessionExchange))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
				.oauth2ResourceServer(server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));

		return http.build();
	}

	/**
	 * Chain for everything else. It has no OAuth2 resource server, so {@code Authorization: Bearer} headers
	 * are ignored: the session cookie is the only way to authenticate.
	 */
	@Bean
	@Order(2)
	public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
		UserSessionAuthenticationFilter sessionFilter = new UserSessionAuthenticationFilter(sessionService, sessionCookies, userAuthorities, clock, invalidSessionTokenLimiter);

		// CSRF protection applies to state-changing requests that are authenticated by the session cookie.
		// A request without a session cookie has nothing a forged request could abuse and is simply rejected as
		// unauthenticated (401), which also tells the web app to sign in again.
		RequestMatcher unsafeRequestWithSession = request -> !SAFE_METHODS.contains(request.getMethod())
				&& request.getAttribute(UserSessionAuthenticationFilter.SESSION_ATTRIBUTE) != null;

		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.csrf(csrf -> csrf
						.csrfTokenRepository(new SessionCsrfTokenRepository())
						.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
						.requireCsrfProtectionMatcher(unsafeRequestWithSession))
				.headers(headers -> headers
						.frameOptions(frame -> frame.deny())
						.contentSecurityPolicy(csp -> csp.policyDirectives("frame-ancestors 'none'"))
				)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.addFilterBefore(sessionFilter, CsrfFilter.class)
				.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.authorizeHttpRequests(auth -> auth
						// Tomcat re-dispatches a request to /error after sendError() or an unhandled exception (a denied
						// @PreAuthorize, a CSRF rejection, the 429 of the limiter, a 500). That dispatch is evaluated
						// again, and without this rule it would be refused as unauthenticated and the real status
						// replaced by a 401, which the web app answers by signing in again and replaying the request.
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						.requestMatchers(HttpMethod.GET, "/v2/topics/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/v2/published-theses/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/v2/published-presentations/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/v2/research-groups/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/v2/calendar/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/v2/avatars/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/actuator/info").permitAll()
						.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
						.anyRequest().authenticated()
				);

		return http.build();
	}
}
