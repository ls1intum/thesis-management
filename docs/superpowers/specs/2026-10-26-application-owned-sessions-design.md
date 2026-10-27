# Application-owned sessions: design and implementation plan

Status: implemented (the code, `documentation/docs/**` and the tests are authoritative where this document differs). Date: 2026-10-26.

## 1. Intent

Thesis Management currently stops working whenever the central Keycloak (TUM/AET) is unavailable.
Keycloak, with TUM Login behind it, should only answer one question: *is this person who they claim to be?*
Everything after that (who is a student, supervisor or examiner; whether a session is still acceptable) belongs
to Thesis Management.

Target behaviour: a user who has already logged in keeps working through a Keycloak outage of at least two hours.
Logging in (authenticating) still needs Keycloak, which is unavoidable.

The mechanism is an **application-owned session**. After a successful Keycloak login the web app exchanges the
Keycloak token once for a Thesis Management session. From then on the application server never talks to Keycloak
to serve a request, and the web app never talks to Keycloak to call the application server.

This follows the OWASP Session Management Cheat Sheet as completely as practical (section 7), and the IETF
guidance for browser-based applications (RFC 10017) that session credentials must not live in JavaScript-readable storage.

## 2. Goals, non-goals, anti-goals

Goals
- G1. Logged-in users keep full use of the application during a Keycloak outage, for as long as their session is valid.
- G2. The application server starts and restarts while Keycloak is down.
- G3. Authentication (Keycloak) and authorization (database) stay strictly separate. Keycloak is consulted only at login.
- G4. Follow the OWASP Session Management Cheat Sheet; every deviation is listed and justified (section 7).
- G5. Real logout: ending a session on the server makes its token useless immediately.

Non-goals
- N1. Logging in during a Keycloak outage. Not possible.
- N2. An administrator "block this user" feature. The session design provides the revocation hook (section 5.7); a block feature and its user interface are a separate piece of work.
- N3. Preserving unsaved form input when a session ends.
- N4. Changing how Keycloak is configured or what AET's Keycloak does.
- N5. Replacing Keycloak token usage for Keycloak-native features (passkey registration and listing). These keep needing a live Keycloak.

Anti-goals (would make this a failure even if it "works")
- A1. Any session credential, access token or refresh token in `localStorage` or `sessionStorage`.
- A2. Two ways to authenticate an API request after the migration (cookie and Bearer).
- A3. Roles or permissions copied into the session. Authorization is read from `user_groups` on every request, as today (see `CLAUDE.md`, "Authorization Architecture").
- A4. A design that needs Keycloak after login to keep a session alive.

## 3. Current state (verified in the code)

| Fact | Where |
|---|---|
| Every API call carries a Keycloak access token as `Authorization: Bearer`. The server validates signature, issuer and expiry only. | `WebSecurityConfig.java`, `application.yml` (`resourceserver.jwt`) |
| Roles come from `user_groups` on every request. | `JwtAuthConverter.java` |
| Access tokens live 5 minutes (dev realm; production values unknown). The web app force-refreshes on every page load (`keycloak.init` with stored tokens calls `updateToken(-1)`) and again whenever a token expires. | `AuthenticationProvider.tsx`, `request.ts`, keycloak-js `init` |
| A failed refresh, including a plain network error, wipes the stored tokens and logs the user out locally. | `AuthenticationProvider.tsx` `onAuthRefreshError` |
| Keycloak access and refresh tokens are stored in `localStorage` (`authentication_tokens`). | `hooks/authentication.ts` |
| The server declares an OAuth2 *client* registration (`spring.security.oauth2.client.provider.keycloak.issuer-uri`) and depends on `spring-boot-starter-security-oauth2-client`, although no code uses an OAuth2 client. Spring Boot resolves an `issuer-uri` by fetching Keycloak's discovery document at startup, so **the server probably cannot start while Keycloak is down** (to be confirmed, see Phase 0). | `application.yml`, `server/build.gradle:65` |
| Profile data (email, names, matriculation number) is synced from JWT claims on every `GET /v2/user-info`, which also sets `lastLoginAt` and re-enables disabled accounts. | `UserInfoController.getInfo`, `AuthenticationService.updateAuthenticatedUser` |
| `User.disabled` is a **data-retention** flag (set after one year of inactivity or during account deletion; cleared by logging in again). It is not an administrative block. | `DataRetentionService`, `UserDeletionService`, `documentation/docs/admins/data-retention.mdx` |
| Controllers and `CurrentUserProvider` are typed to `JwtAuthenticationToken`. | `UserInfoController`, `UserDeletionController`, `CurrentUserProvider`, `AuthenticationService` |
| About 19 integration test classes authenticate with an HMAC-signed test JWT through `TestSecurityConfig` (profile `test`). | `BaseIntegrationTest.createRandomAuthentication`, `TestSecurityConfig` |
| Production routes web app and API under one host (`APP_HOSTNAME`, API under `/api`), so they are same-origin. Local dev uses `localhost:3100` and `localhost:8180` (same site, different origin). | `docker-compose.prod.yml`, `CorsConfig.java` |

## 4. Decisions

Made by the project owner
- D1. Application-owned sessions, not token-lifetime tweaks (no dependence on AET changing Keycloak settings).
- D2. Server-side sessions with opaque tokens, stored in the database. Not a stateless JWT.
- D3. Follow the OWASP Session Management Cheat Sheet completely, wherever it does not contradict G1.
- D4. Idle timeout 4 hours, absolute timeout 24 hours.
- D5. Survive a Keycloak outage of at least 2 hours.
- D6. An administrator "block user" feature is out of scope (N2). Disabling users stays a Thesis Management concern for later.
- D7. Session rows keep IP address and normalised User-Agent for 30 days after the session ends (C7).
- D8. Add HSTS to production if it is missing. It is missing (checked live, 2026-10-26): see 5.9.
- D9. The server ignores the `email` sent to `PUT /v2/user-info` (5.9).
- D10. Everything ships as one pull request, with one commit per phase. Avoid additional proxies: HSTS is added to the existing Traefik, no new component.
- D11. Confirmed by the owner: persistent session cookie up to 24 hours (C2), limit of 10 sessions per user (C8), and one silent re-login attempt with a single replay of the failed request when a session ends while Keycloak is reachable (5.7).

My calls (please check these when reading)
- C1. Token is a split credential: cookie value `<identifier>.<verifier>`; identifier and verifier are independent random values; only a hash of the verifier is stored.
- C2. Cookie: `__Host-sid`, `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/`, no `Domain`, persistent (`Max-Age` = remaining absolute lifetime). Where the client host is plain `http` (local dev) the `__Host-` prefix and `Secure` are dropped, derived from `thesis-management.client.host`, not from a new flag.
- C3. CSRF defence: Spring's CSRF filter with a token stored on the session row and delivered in JSON (not a cookie). `SameSite=Strict` is defence in depth only, as OWASP says.
- C4. Last-used time is written at most every 5 minutes per session.
- C5. User-Agent binding compares only a coarse signature, **browser family and operating system family** (for example "Chrome, macOS"), and ends the session when either differs. The full User-Agent string changes with every browser update (the version number is part of it), so exact matching would log users out after updates; versions, device model and "mobile" markers are therefore ignored and only logged. Within one browser the coarse signature is stable. The same cookie cannot move to another browser by itself, so a change signals a copied cookie or a user-agent switching extension. The check is one isolated method with a setting (`thesis-management.session.user-agent-mismatch-action`: `END` or `LOG`) so it can be relaxed without a release. An IP address change is only logged and flagged, never fatal (eduroam, VPN and mobile networks change addresses legitimately). The security value of this binding is modest, because an attacker who steals a cookie can copy the header; it is defence in depth, as the cheat sheet says.
- C6. Rotation after a privilege change keeps the old identifier valid for a 30 second grace window so parallel requests do not fail.
- C7. Ended sessions are kept 30 days as the "recent account activity" history that OWASP recommends, then deleted. They contain IP and normalised User-Agent, so `documentation/docs/admins/data-retention.mdx` and the privacy statement need an entry (section 9).
- C8. At most 10 concurrent sessions per user; creating the 11th ends the oldest.
- C9. `GET /v2/user-info` becomes a plain read. Profile sync from Keycloak claims, `lastLoginAt` and reactivation of disabled accounts move to session creation. This is safe to move: `updateAuthenticatedUser` is called from `UserInfoController.getInfo` only. The cost is that a change made in Keycloak (name, email, matriculation number) reaches Thesis Management at the next login, so at most 24 hours later, not on every page load.
- C10. No client heartbeat. With a 4-hour idle timeout a keep-alive is not needed in v1.
- C11. Replaced by D10: one pull request.

## 5. Design

### 5.1 Session data

New table `user_sessions` (Liquibase change `47_user_sessions.sql`, registered in `db.changelog-master.xml`):

| Column | Notes |
|---|---|
| `id` UUID, primary key | Internal row id. Used in the "active sessions" API. Never put in a cookie. |
| `token_identifier` text, unique, indexed | Public half of the cookie value. 128 random bits, base64url. |
| `verifier_hash` bytea | SHA-256 of the verifier (256 random bits). The verifier is never stored. |
| `user_id` UUID, FK `users(user_id)` ON DELETE CASCADE | |
| `created_at`, `last_used_at`, `expires_at` | `expires_at` = `created_at` + absolute timeout. Idle expiry is `last_used_at` + idle timeout. |
| `ended_at` nullable, `end_reason` nullable | `LOGOUT`, `EXPIRED`, `REVOKED`, `ROTATED`, `USER_AGENT_MISMATCH`, `EVICTED` (stored as text). |
| `rotation_required` boolean | Set when the user's roles change. |
| `superseded_by` UUID nullable, `superseded_until` timestamp nullable, `rotated_from` UUID nullable | Grace window after rotation (C6); `rotated_from` points the replacement at its predecessor, so that ending all other sessions spares the replacement of the current one. |
| `csrf_token` text | Copied unchanged on rotation. |
| `created_ip`, `last_ip`, `user_agent` (truncated to 255), `user_agent_signature` (browser family and operating system family) | For C5, C7 and the active-sessions list. |

Everything is read and written with Spring Data repositories. No `@Transactional` on services (project rule);
create, rotate and revoke are written to be idempotent. `@Modifying` repository methods carry their own transactions.

### 5.2 Token handling

- `UserSessionTokens` generates identifier and verifier with `SecureRandom`, builds and parses the cookie value, and hashes verifiers.
- Lookup is by `token_identifier`. The stored hash is compared with `MessageDigest.isEqual` (constant time). The identifier alone never authenticates.
- The raw cookie value is never logged. Logs carry the first 12 hex characters of `SHA-256(identifier)` for correlation.

### 5.3 Endpoints

All under `/v2/auth` (new `UserSessionController`).

| Method and path | Authenticated by | Behaviour |
|---|---|---|
| `POST /v2/auth/session` | Keycloak access token as Bearer (the **only** place a Bearer token is accepted) | Calls `AuthenticationService.updateAuthenticatedUser(Jwt)` (creates or updates the user, assigns the default student group, rejects anonymised or deletion-requested accounts, reactivates disabled accounts, sets `lastLoginAt`), then creates a session (replacing the one the browser still holds for the same user), sets the cookie, returns `{ csrfToken, expiresAt, userId }`. The web app compares `userId` before replaying a request after the session was replaced. Exempt from CSRF (it is authenticated by a Bearer token, which a cross-site attacker cannot attach; CORS only allows the client origin). Applies the 10-session cap. |
| `GET /v2/auth/session` | Session cookie | `200 { csrfToken, expiresAt, userId }` or `401`. Lets the web app learn at startup whether it has a session. A request without any cookie is a normal 401 and is not counted as suspicious. |
| `DELETE /v2/auth/session` | Session cookie + CSRF | Ends the session on the server. Response clears the cookie and sends `Clear-Site-Data: "cache"` and `Cache-Control: no-store`. |
| `GET /v2/auth/sessions` | Session cookie | The caller's active sessions: row id, created, last used, IP, readable device label, `current` flag. |
| `DELETE /v2/auth/sessions/{id}` and `DELETE /v2/auth/sessions` | Cookie + CSRF | End one of the caller's sessions (this can also be the current one, which then clears the cookie) / all sessions except the current one. Only the caller's own rows are reachable; others are reported as not found. |

Responses that carry session data set `Cache-Control: no-store`.

### 5.4 Security configuration

Two `SecurityFilterChain` beans replace the single one in `WebSecurityConfig.java`.

1. **Chain 1** matches only `POST /v2/auth/session`. It uses `oauth2ResourceServer` with the existing Keycloak `JwtDecoder` and the standard `JwtAuthenticationConverter` (no database roles needed here). CSRF is not applied.
2. **Chain 2** matches everything else. It has **no** `oauth2ResourceServer`, so `Authorization: Bearer` headers are ignored everywhere else (anti-goal A2). It adds `UserSessionAuthenticationFilter` and CSRF protection, and keeps the existing `permitAll` rules for public GET endpoints.

`UserSessionAuthenticationFilter`:
1. Reads the session cookie (`__Host-sid`, or `sid` over plain http). No cookie: continue anonymous.
2. Parses it, loads the row by identifier, verifies the verifier hash in constant time. Unknown identifier, bad verifier, malformed value: continue anonymously (public endpoints keep working, protected ones answer `401` through the entry point), clear the cookie in the response, log a security event, count it against the source IP (section 5.6).
3. Rejects an ended session, an absolute-expired session (`expires_at`), an idle-expired session (`last_used_at` + idle timeout), and a session whose user is anonymised or has a pending deletion (the request continues anonymously as above, the cookie is cleared, the session is ended with the matching reason).
4. Enforces user-agent binding (C5).
5. If `rotation_required`, rotates (section 5.5). If the row is superseded and inside its grace window, accepts it without rotating again; outside the window, rejects it.
6. Touches `last_used_at` when older than 5 minutes (C4).
7. Loads authorities from `user_groups` for the user, exactly as `JwtAuthConverter` does today, and sets a `UserSessionAuthentication` whose `getName()` is the university id.

`JwtAuthConverter` is replaced by this filter; its role-loading code moves to a small shared component.

CSRF: `CsrfFilter` with a custom `CsrfTokenRepository` that reads the token from the authenticated session row and never writes a cookie. Unsafe methods (`POST`, `PUT`, `PATCH`, `DELETE`) must carry `X-CSRF-Token`. `CorsConfig` adds that header to the allowed headers. Missing or wrong token: `403`. The check applies to state-changing requests that carry a valid session; one without a session is rejected as unauthenticated (`401`).

Typing changes: `UserInfoController`, `UserDeletionController`, `CurrentUserProvider`, `AuthenticationService.getAuthenticatedUser*` accept `Authentication` and use `getName()`. This is required anyway, and it lets the test chain (below) keep working.

### 5.5 Session lifecycle

- **Create**: only through `POST /v2/auth/session`, only after a verified Keycloak token. A new identifier is generated on every login (no pre-login session exists, so no fixation).
- **Use**: section 5.4.
- **Rotate** (renewal after privilege change): create a new row with new identifier and verifier, same `created_at`, `expires_at`, `csrf_token`; mark the old row `superseded_by` the new one with `superseded_until` = now + 30 s; send `Set-Cookie` with the new value and `Max-Age` equal to the remaining absolute lifetime. Triggered by `rotation_required`.
- **Role changes** set `rotation_required` on all of the affected user's sessions. The choke points are the group mutations in `AccessManagementService` (`addStudentGroup`, `assignAdvisorRole`, `assignSupervisorRole`, and the group removal near line 165) through one `UserSessionService.markForRotation(userId)` call. The affected user is never logged out for this, which matters during an outage.
- **Revoke all** (`UserSessionService.revokeAll(userId)`, always ending with the reason `REVOKED`): called from `UserDeletionService` where accounts are marked deleted or anonymised. This is also the hook a future "block user" feature would use (N2).
- **Expire and clean up**: a scheduled job (new class next to the existing `@Scheduled` jobs; `@EnableScheduling` is already on) hourly marks overdue sessions ended and deletes rows ended more than 30 days ago (C7). Expiry is *also* enforced on every request, so the job is housekeeping, not security.
- **Logout**: `DELETE /v2/auth/session`.
- **Known limitation: the rotated cookie is delivered once.** The replacement cookie is sent only in the response of the request that performed the rotation. If that response is lost or aborted, the browser keeps the old cookie, which keeps working for the 30 second grace window; after that the browser must sign in again (silently while Keycloak is reachable, see 5.7). This was accepted instead of re-sending the cookie on later requests.

### 5.6 Detection and logging

- Unknown-identifier, malformed and bad-verifier cookies (the signatures of guessing, nothing else; `Reason.countsAsGuess()`) are counted per source IP (sliding window of 60 seconds) and logged at `WARN` with the reason, the IP, the HTTP method and the path (not the cookie or its identifier). Once an IP has more than the configured number in the window (default 20), the server answers `429` with `Retry-After: 60` **only to requests from that IP that carry such an invalid cookie**, and logs each throttled request at `WARN`. There is no fixed block period: the throttle ends when the counted cookies leave the window. Requests with a valid session, or with no cookie, are never blocked, so users behind shared campus addresses (eduroam, VPN) are unaffected. Ended, expired, user-agent-mismatch and superseded sessions are legitimate stale cookies; they are logged at `INFO` and not counted. This sits behind Traefik's existing rate limit and adds session-specific alerting (OWASP section "Session Attacks Detection"). The client IP comes from the forwarded headers (5.9).
- Coarse User-Agent signature mismatch (C5): end the session (or only log, per setting), log `WARN`. Differences in version, device or mobile markers are not logged at all: the signature drops them before the comparison. IP change: log `INFO` (when the last-used timestamp is written) and store `last_ip`.
- Logged at `INFO`: session created (user id, hashed identifier, IP), ended (user id, hashed identifier, reason; this covers logout, expiry and the user-agent mismatch), rotated, other sessions ended, sessions revoked. Never log raw tokens, cookie values, or `Cookie` headers.

### 5.7 Web app

Files: `AuthenticationProvider.tsx`, `request.ts`, `hooks/authentication.ts`, `context.ts`, plus the new modules `core/auth/keycloakClient.ts` (Keycloak client) and `core/auth/session.ts` (CSRF token and session state).

Startup
1. Delete the legacy `authentication_tokens` entry from `localStorage` (anti-goal A1, and cleans up existing users).
2. `GET /v2/auth/session`. **200**: the user is authenticated, store `csrfToken`, mark ready. Keycloak is not contacted at all (G1).
3. **401 or no network**: initialise keycloak-js with `check-sso` exactly as today (this also handles the redirect back after login). If it reports authenticated, `POST /v2/auth/session` with that Keycloak token, store `csrfToken`, then the Keycloak tokens are kept **in memory only**. If Keycloak is unreachable, the user is simply anonymous and sees the existing "Authentication service is unavailable" message.

Requests (`doRequest`)
- Remove all Keycloak token handling and the `Authorization` header. Send `credentials: 'include'`.
- Send `X-CSRF-Token` on `POST`, `PUT`, `PATCH`, `DELETE`.
- On `401` for a request that required authentication (my call, so daily users are not shown a login prompt every 24 hours): make **one** silent attempt to re-establish the session, `check-sso` against Keycloak followed by `POST /v2/auth/session`, then replay the failed request once. Replaying is safe because a `401` means the server rejected the request before processing it. Concurrent `401`s share the single in-flight attempt, and a failed attempt (or Keycloak being down) ends the attempt without retrying: the session is marked ended and the user interface offers login. A replayed request that gets `401` again is never retried a second time.

Keycloak-native features (`registerPasskey`, `listCredentials`, `deleteCredential`, `loginWithPasskey`): obtain a Keycloak token through `getKeycloakAccessToken(allowLoginRedirect)` in `AuthenticationProvider.tsx`, which relies on the lazy `ensureKeycloak()` / `ensureSsoChecked()` of `core/auth/keycloakClient.ts` to initialise keycloak-js on first use and, if allowed, falls back to a redirect login when no Keycloak session exists. The token must belong to the account of the application session, otherwise the call is refused. These fail with a clear message while Keycloak is down (N5).

Login: unchanged redirect to Keycloak, then the exchange above.

Logout: `DELETE /v2/auth/session`, then clear local state, then end the Keycloak session on a best-effort basis (only if a 2 second reachability probe succeeds; otherwise the user is simply sent to the target page).

The "refresh token expiry" interval and `onAuthRefreshError` clearing are removed. Session lifetime is the server's business.

### 5.8 Configuration

New properties (documented in `documentation/docs/admins/configuration.mdx`; only the first four have their own environment variable, the others are set through Spring's relaxed binding of `thesis-management.session.*`):

| Property | Default |
|---|---|
| `thesis-management.session.idle-timeout` | `PT4H` |
| `thesis-management.session.absolute-timeout` | `PT24H` |
| `thesis-management.session.max-per-user` | `10` |
| `thesis-management.session.last-used-write-interval` | `PT5M` |
| `thesis-management.session.rotation-grace` | `PT30S` |
| `thesis-management.session.ended-retention` | `P30D` |
| `thesis-management.session.invalid-token-limit-per-minute` | `20` |
| `thesis-management.session.user-agent-mismatch-action` | `END` (or `LOG`, see C5) |

Cookie flags follow `thesis-management.client.host` (C2). A `Clock` bean is injected so tests control time.

### 5.9 Operations

- **HSTS** (`Strict-Transport-Security`). Checked against the live system on 2026-10-26: `https://thesis.aet.cit.tum.de/` is served by `nginx/1.30.5` and neither the web app nor `/api` sends the header. In the repository, TLS terminates in Traefik (`websecure` entrypoint with the `letsencrypt` resolver in `docker-compose.prod.yml`) and nginx is the web app container (`client/nginx/nginx.conf`, port 80, no TLS). Verified against the live system: the certificate for `thesis.aet.cit.tum.de` is issued by Let's Encrypt (the resolver configured in the compose file), plain `http` gets a 308 redirect to `https`, and the deploy workflow connects to one host and runs `docker-compose.prod.yml`. So there is exactly one TLS-terminating proxy (Traefik); nginx only serves the static web app inside its own container, and no additional proxy is introduced. The header is added as a Traefik middleware attached to both routers (`stsSeconds` of one year, no `includeSubdomains` and no `preload`, because `aet.cit.tum.de` hosts other services that this project does not own). It takes effect at the next manual production release; verify with `curl -sI https://thesis.aet.cit.tum.de/`.
- **Forwarded headers**: the API's `www-authenticate` response named an `http://` URL, so the server did not honour `X-Forwarded-Proto` and `X-Forwarded-For`. `server.forward-headers-strategy` is set to `native` (Tomcat's `RemoteIpValve`), so that the server sees `https` and the real client IP. This is required for the per-IP detection in 5.6 and avoids wrong redirect and metadata URLs. The `framework` strategy was rejected: Spring's `ForwardedHeaderFilter` also reads the RFC 7239 `Forwarded` header, which Traefik passes through unchanged, so a client could fake its address. `native` ignores `Forwarded` and reads only `X-Forwarded-For` / `X-Forwarded-Proto`, from the peers in `server.tomcat.remoteip.internal-proxies` (the default covers private and Docker ranges). Consequently Traefik must be the only peer that can reach the server port (production only `expose`s 8080 inside the Docker network); a different proxy or load balancer must be added to `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES` and must overwrite, not append to, a client-supplied `X-Forwarded-For`.
- **Email is read-only for users.** The web app already shows email as read-only ("synchronized from your university account"), but `PUT /v2/user-info` still applies the `email` it receives (`AuthenticationService.updateUserInformation`). The server now ignores the payload value, so only the Keycloak claim at login can change it. This also closes the OWASP "re-authenticate before changing critical information" item, because no such change remains.
- **Encrypted backups**: the session table holds hashed verifiers, IP addresses and User-Agent strings. `supporting_scripts/thesis-management-backup.sh` writes unencrypted zip files, so backup encryption is **not implemented**; the gap is documented in `documentation/docs/admins/production-setup.mdx` (encrypt the disk or the backup directory on the host).

## 6. Phased implementation plan

Everything ships in one pull request (D10), one commit per phase. Order matters: Phase 0 comes first. It is independent of the rest, so it is the commit to split off if the sessions work takes long and the startup fix should not wait.

### Phase 0: server starts without Keycloak (first commit)
1. Reproduce: start the server with `KEYCLOAK_HOST` pointing to a closed port; record the failure.
2. Remove the unused OAuth2 client: dependency `spring-boot-starter-security-oauth2-client` in `server/build.gradle`, and `spring.security.oauth2.client.*` in `application.yml`. Confirm nothing references `ClientRegistration`, `OAuth2AuthorizedClient` or `oauth2Login` (a search already found none).
3. Keep `resourceserver.jwt.jwk-set-uri` plus `issuer-uri`; verify the decoder does not fetch discovery at startup.
4. Verify the server now boots with Keycloak unreachable; add a test or a documented manual check.
5. Run the server test suite (`./gradlew test`), `spotlessApply`.

### Phase 1: server session core
1. Migration `47_user_sessions.sql`; entity `UserSession`; `UserSessionRepository` (lookup by identifier, list active by user, `@Modifying` bulk end, delete ended before cutoff, mark rotation by user).
2. `UserSessionProperties` (section 5.8), `Clock` bean, `UserSessionTokens`.
3. `UserSessionService`: `create`, `authenticate(cookieValue, request)` (all checks of 5.4), `rotate`, `end`, `endOthers`, `revokeAll(userId)`, `markForRotation`, `cleanup`.
4. Unit tests with a fixed clock: token round trip and tamper cases, constant-time path, idle and absolute expiry boundaries, session cap eviction, rotation and grace window, throttled `last_used_at`.

### Phase 2: server security wiring and endpoints
1. `UserSessionAuthenticationFilter`, `UserSessionAuthentication`, CSRF repository, `UserSessionCookies` (build, clear, flags).
2. Two filter chains in `WebSecurityConfig`; remove `JwtAuthConverter`, move role loading into a shared component; CORS header `X-CSRF-Token`.
3. `AuthenticationService`: add `updateAuthenticatedUser(Jwt)` used only by session creation (C9); change `getAuthenticatedUser*` to `Authentication`; `GET /v2/user-info` becomes a plain read. Adjust the controllers and `CurrentUserProvider` types.
4. `UserSessionController` (section 5.3).
5. Hooks: `markForRotation` from the `AccessManagementService` group mutations; `revokeAll` from `UserDeletionService`; cleanup job.
6. Detection and logging (5.6); forwarded-header configuration (5.9); `updateUserInformation` stops applying the payload `email`, with a test (5.9). HSTS is added in Phase 5.
7. Tests, two layers (my call, because the existing tests and the real chain cannot share one configuration):
   - **Existing integration classes (about 19, profile `test`)**: unchanged. `TestSecurityConfig` stays a deliberately simple chain (HMAC test JWT as Bearer, roles from the token claim, CSRF off). It is a test shortcut and is not evidence for session security. Only the controller parameter types change (section 5.4), and `AuthenticationServiceTest` is adapted to the new `updateAuthenticatedUser(Jwt)` signature.
   - **Session security tests run against the real chains** with `BaseSessionIntegrationTest` (profile `session-test`, `SessionTestSecurityConfig` supplies a test `JwtDecoder` instead of a Keycloak one; `UserSessionControllerTest`, `UserSessionLifecycleTest`, `UserSessionSecureCookieTest` and others). They cover, among others: exchange of a token sets a cookie with the exact flags; the session authenticates; roles come from `user_groups`; missing or wrong CSRF token is `403`; unknown cookie continues anonymously (`401` on protected endpoints) and clears the cookie; idle and absolute expiry (injected clock); rotation after a role change and its grace window; revoke on account deletion; user-agent mismatch ends the session; logout; list and end only own sessions; Bearer is ignored on every endpoint except the exchange.
   - **`KeycloakSecurityIntegrationTest`** (`BaseKeycloakIntegrationTest`, profile `keycloak-test`) uses a real Keycloak container and contains the **outage test: pause the Keycloak container, then verify an existing session still works**. In the browser, `client/e2e/user/keycloak-outage.spec.ts` blocks every request to the Keycloak origin and verifies the same for a logged-in user.
   - Pure unit tests (Phase 1) cover the token, expiry and rotation logic with a fixed clock.
8. `./gradlew test`, `spotlessApply`, checkstyle.

### Phase 3: web app
1. Keycloak client module with lazy init, CSRF token state, session bootstrap (5.7).
2. Rewrite `request.ts`; provider startup, login, logout, passkey functions; remove token persistence and the refresh interval; legacy storage cleanup.
3. Update `IAuthenticationContext` only where the contract changes.
4. Unit tests (vitest) for `request.ts` (credentials, CSRF header, 401 handling) and the provider bootstrap paths with mocked `fetch`.
5. End-to-end tests: update `e2e/auth.setup.ts` and `e2e/passkey.spec.ts` (they currently assert `access_token` in `localStorage`; they should assert the session cookie and the absence of tokens in storage); add an **outage drill** that logs in, blocks every request to the Keycloak origin, reloads, and verifies the user is still logged in and can use the application.
6. `pnpm exec eslint src/`, `pnpm exec tsc --noEmit`, `pnpm build`.

### Phase 4: active sessions user interface
1. Settings page section listing sessions (device label, IP, created, last used, "this device"), with "end session" and "end all other sessions".
2. E2E test: two browser contexts, end one from the other.

### Phase 5: documentation and operations
- `CLAUDE.md` "Authorization Architecture": add that Keycloak is consulted only to create a session.
- `documentation/docs/developer/development-setup.mdx` (the authentication flow section), `documentation/docs/admins/configuration.mdx` (properties), `documentation/docs/admins/production-setup.mdx` (HSTS, cookie behaviour, forwarded headers, outage behaviour, what happens after a long outage), `documentation/docs/admins/data-retention.mdx` (session rows, IP, retention), `documentation/static/files/endpoints.yaml` and the Postman collection (new endpoints; Bearer no longer accepted).
- HSTS Traefik middleware in `docker-compose.prod.yml` (5.9), and `documentation/docs/admins/production-setup.mdx` states where it is set.
- Production note: deploy the server and the web app together. A browser tab opened before the deployment gets `401`s and is sent to login once.

## 7. OWASP Session Management Cheat Sheet: how each requirement is met

| Requirement | How |
|---|---|
| Entropy, CSPRNG, whole ID random | 128-bit identifier and 256-bit verifier from `SecureRandom` |
| Meaningless to the client, meaning stored on the server | Opaque cookie; user and everything else in `user_sessions` |
| Cookies only; no other channel for session IDs | Only `__Host-sid`; Bearer ignored outside `POST /v2/auth/session` |
| Generic name | `__Host-sid` (no framework fingerprint) |
| `Secure`, `HttpOnly`, `SameSite=Strict`, `__Host-`, no `Domain`, `Path=/` | C2 (the prefix and `Secure` are dropped only for plain-`http` local development) |
| Hash stored at rest, constant-time compare, verifier independent of identifier | 5.1, 5.2 |
| Restrict access to the store, encrypted backups | Database access control as today; backup encryption is not implemented and the gap is documented (5.9) |
| TLS for the whole session, HSTS | Existing HTTPS; HSTS verified or added (5.9) |
| No tokens in `localStorage` or `sessionStorage` | Removed, plus cleanup of legacy entries (5.7) |
| Server-enforced idle and absolute timeouts | 4 hours and 24 hours, checked on every request (D4) |
| Renew ID after privilege change | Role changes flag sessions for rotation (5.5) |
| Strict mode: never accept an ID the server did not generate | Unknown identifiers are rejected, not adopted |
| Invalidate on logout on both sides; visible logout | `DELETE /v2/auth/session`; cookie cleared; existing logout control kept |
| `Cache-Control: no-store` on session responses; `Clear-Site-Data` on logout | 5.3 |
| CSRF token in addition to `SameSite` | C3 |
| Detect guessing, anomalies; bind to client properties; alert | 5.6 (User-Agent bound, IP logged) |
| Log session lifecycle; never log raw IDs; log hashed IDs | 5.6 |
| Concurrent sessions: allow, let users see and end them, keep history | Cap of 10 (C8), Phase 4 interface, 30-day history (C7) |
| Re-authenticate after high-risk events | The application has no passwords, and after the email fix in 5.9 users cannot change any identity attribute. Nothing remains that needs fresh authentication. |

Deliberate deviations
- **Persistent cookie** (OWASP prefers non-persistent where login need not survive the browser): users must still be signed in the next working day, and a Keycloak outage must not lose them. Server-side expiry stays authoritative.
- **Timeouts longer than the example ranges** (examples: 15 to 30 minutes idle, 4 to 8 hours absolute): the project owner chose 4 and 24 hours (D4). A shorter idle timeout would lock out active users during an outage.
- **"Use the framework's session management"**: Spring Session JDBC stores session IDs unhashed. Hashing at rest was judged more valuable, so the store is custom, built on Spring Security's filter and CSRF interfaces.
- **"Issue a new ID when an unknown one arrives"** applies to anonymous pre-login sessions. This design creates none, so an unknown ID is a plain `401`.

## 8. Risks and rollback

| Risk | Mitigation |
|---|---|
| Large cross-cutting change (server, web app, tests) in a single pull request | One commit per phase so it can be reviewed commit by commit; Phase 0 can be split off; the outage test and the outage end-to-end drill are the acceptance criteria |
| Dev and e2e tooling relied on Bearer tokens | Test profile keeps its JWT shortcut; Postman documentation updated; e2e login helper updated |
| Cookie not sent in some environment (cross-site setups) | Checked in the repository: the deployed environments use `docker-compose.prod.yml` (one host, API under `/api`, same-origin); local development and `docker-compose.showcase.yml` serve the web app on `localhost:3100` and the server on `localhost:8180` (same site, different origin, plain `http`, handled by C2). `credentials: 'include'` plus `allowCredentials` is already configured. The design assumes no environment serves web app and API from different sites; a staging setup that does would need `SameSite=None` and would break this design, so it must be flagged before it is built |
| User-Agent normalisation causes false logouts | Normalisation strips versions; mismatch logged with reason; the check is one isolated method that can be relaxed to log-only |
| After an outage longer than Keycloak's own SSO idle timeout, users must log in once more when Keycloak returns | Inherent (N1); documented in `documentation/docs/admins/production-setup.mdx` |
| Rollback | Revert the pull request; the `user_sessions` table is additive and can stay |

## 9. Items for the project owner

Decided: 1. no block feature now (D6, and `User.disabled` is the retention flag and must not be reused for it later); 2. IP and User-Agent for 30 days (D7); 3. add HSTS (D8).

Also decided: 4. the server ignores the payload email (D9); 5. TLS terminates in Traefik, verified live, no extra proxy (5.9); 6. one pull request (D10).

Still open
1. **Privacy statement**: session rows store IP address and a coarse User-Agent signature plus the full (truncated) User-Agent string for the session lifetime plus 30 days. Someone needs to update the privacy statement if it enumerates what is stored (inside this repository or elsewhere, to be confirmed).

## 10. Left to the builder

- Exact class and package names (follow the existing `core/security` and `core/user` layout).
- Internal structure of the per-IP counter; hashing details such as hex versus base64.
- Device label wording in the sessions list.
- Commit boundaries inside the sessions pull request.
