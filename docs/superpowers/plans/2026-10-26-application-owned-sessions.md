# Application-Owned Sessions Implementation Plan

> **Historical document.** This records the plan as written before implementation. File names, paths and steps may differ from the final code; the spec, the code and `documentation/**` are authoritative.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** After one Keycloak login, users hold a Thesis Management session (server-side, opaque, cookie-based) so the application keeps working for logged-in users while Keycloak is down.

**Architecture:** `POST /v2/auth/session` exchanges a Keycloak access token once for a database-backed session (split token in an HttpOnly cookie, hashed verifier, 4 h idle / 24 h absolute). A new filter authenticates every other request from the cookie and loads roles from `user_groups` as today; Bearer tokens are accepted only at the exchange endpoint. The web app stops storing tokens, bootstraps from `GET /v2/auth/session`, and starts keycloak-js only for login and passkey features.

**Tech Stack:** Spring Boot 4.1 / Spring Security, JPA + Liquibase (Postgres), JUnit 5 + Testcontainers (real Keycloak), React 19 + TypeScript, keycloak-js 26.2.4, Vitest, Playwright.

**Spec:** `docs/superpowers/specs/2026-10-26-application-owned-sessions-design.md` (section numbers below refer to it).

## Global Constraints

- Idle timeout `PT4H`, absolute timeout `PT24H`, max 10 sessions per user, last-used write interval `PT5M`, rotation grace `PT30S`, ended-session retention `P30D`, invalid-token limit 20 per minute per IP.
- Cookie `__Host-sid`, `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/`, no `Domain`, persistent (`Max-Age` = remaining absolute lifetime). When `thesis-management.client.host` starts with `http://` use name `sid` and no `Secure`.
- Cookie value `<identifier>.<verifier>`: identifier 128 random bits, verifier 256 random bits (base64url, `SecureRandom`); only SHA-256 of the verifier is stored; compare with `MessageDigest.isEqual`.
- Never log raw tokens or cookie values; log the first 12 hex characters of `SHA-256(identifier)`.
- CSRF header `X-CSRF-Token` on `POST`/`PUT`/`PATCH`/`DELETE` that carry a session cookie; the exchange endpoint is exempt.
- Bearer tokens are ignored on every endpoint except `POST /v2/auth/session`.
- Authorization stays in `user_groups`, read on every request; no roles in the session row.
- No `@Transactional` on service methods (project rule); `@Modifying` repository methods carry their own.
- Server code: tabs, Javadoc on public classes/methods like the surrounding code, checkstyle clean (`./gradlew checkstyleMain checkstyleTest`), `./gradlew spotlessApply`.
- Never use the project's two banned tier words (see the user's saved vocabulary rule) in code, comments, docs or commit messages; say "user interface", "web app", "application server" or the component name instead. Commit messages: project style, no AI attribution lines (the user's global instruction overrides any default).
- Do not stage the unrelated working-tree change in `.gitignore`; stage files explicitly.

## Review Focus

- Cookie present but its row is gone or expired: `401`, cookie cleared, web app does one silent re-login and never loops (Tasks 4, 7).
- Two parallel requests arrive while a session rotates: neither fails (Task 5).
- A user is deleted or anonymised while holding a live session: next request is `401` (Tasks 4, 5).
- Page load with a valid session while Keycloak is unreachable makes no Keycloak request (Task 7, e2e drill in Task 9).
- No session and Keycloak down: public pages load, login shows a clear error (Task 7).
- Logout while Keycloak is down still ends the session and clears local state (Task 7).

---

### Task 1: Server starts without Keycloak (Phase 0)

**Files:** Modify `server/build.gradle:65`, `server/src/main/resources/application.yml` (remove `spring.security.oauth2.client.*`). Test: manual boot check; optional `server/src/test/java/.../KeycloakUnavailableStartupTest.java` not required.

**Interfaces:** Produces: server boots with Keycloak unreachable; `resourceserver.jwt` config unchanged.

- [ ] **Step 1: Reproduce.** Start the server with Postgres up and `KEYCLOAK_HOST=http://localhost:9` (closed port). Run `cd server && KEYCLOAK_HOST=http://localhost:9 ./gradlew bootRun`. Record whether it fails at startup (expected: failure fetching OIDC discovery). Note the exact message.
- [ ] **Step 2: Remove** the dependency `spring-boot-starter-security-oauth2-client` and the `spring.security.oauth2.client` block (registration and provider). Keep `resourceserver.jwt.jwk-set-uri` and `issuer-uri`.
- [ ] **Step 3: Verify** the same command now boots (`Started ThesisManagementApplication`) and `curl localhost:8180/api/actuator/health` answers; with Keycloak up, an existing Bearer request still works. Run `./gradlew test` once.
- [ ] **Step 4: Commit** `Start application server without a reachable Keycloak`.

### Task 2: Session persistence and configuration

**Files:** Create `server/src/main/resources/db/changelog/changes/40_user_sessions.sql` (add to `db.changelog-master.xml`), `core/session/entity/UserSession.java`, `core/session/entity/UserSessionEndReason.java`, `core/session/repository/UserSessionRepository.java`, `core/session/config/UserSessionProperties.java`, a `Clock` bean (reuse an existing one if present, else in `core/session/config/`). Modify `application.yml` (defaults), `docs/CONFIGURATION.md` later (Task 10). Test: `core/session/repository/UserSessionRepositoryTest` (existing repository test style with `BaseIntegrationTest` or `@DataJpaTest` if the project uses it).

**Interfaces:** Produces:
- `UserSession` fields per spec 5.1: `UUID id`, `String tokenIdentifier`, `byte[] verifierHash`, `User user`, `Instant createdAt, lastUsedAt, expiresAt, endedAt`, `UserSessionEndReason endReason` (`LOGOUT, EXPIRED, REVOKED, ROTATED, USER_AGENT_MISMATCH, EVICTED`), `boolean rotationRequired`, `UUID supersededBy`, `Instant supersededUntil`, `String csrfToken, createdIp, lastIp, userAgent, userAgentSignature`.
- `UserSessionRepository extends JpaRepository<UserSession, UUID>`: `Optional<UserSession> findByTokenIdentifier(String)`, `List<UserSession> findByUserIdAndEndedAtIsNullOrderByCreatedAtAsc(UUID)`, `@Modifying int markRotationRequiredForUser(UUID userId)`, `@Modifying int endAllForUser(UUID userId, Instant endedAt, UserSessionEndReason reason)`, `@Modifying int deleteEndedBefore(Instant cutoff)`, `@Modifying int touchLastUsed(UUID id, Instant at, String ip)`, `List<UserSession> findByEndedAtIsNullAndExpiresAtBefore(Instant)`.
- `UserSessionProperties` record bound to `thesis-management.session` (fields per Global Constraints plus `UserAgentMismatchAction userAgentMismatchAction` = `END`), registered with `@EnableConfigurationProperties`.

- [ ] **Step 1: Write failing repository test** `findByTokenIdentifier_returnsRowAndHashRoundTrips` and `markRotationRequiredForUser_flagsOnlyThatUsersActiveSessions` and `deleteEndedBefore_removesOnlyOldEndedRows`.
- [ ] **Step 2: Run** `./gradlew test --tests '*UserSessionRepositoryTest'` → FAIL (types missing).
- [ ] **Step 3: Implement** migration (table per 5.1, unique index on `token_identifier`, index on `user_id`, FK `ON DELETE CASCADE`), entity, repository, properties, `application.yml` defaults.
- [ ] **Step 4: Run** the test → PASS; run `./gradlew test` for Liquibase/validate sanity.
- [ ] **Step 5: Commit** `Add user session table, entity and configuration`.

### Task 3: Session tokens, User-Agent signature and session service

**Files:** Create `core/session/service/UserSessionTokens.java`, `core/session/service/UserAgentSignature.java`, `core/session/service/UserSessionService.java`, `core/session/service/ClientInfo.java`. Test: `UserSessionTokensTest`, `UserAgentSignatureTest`, `UserSessionServiceTest` (unit, mocked repository, fixed `Clock`).

**Interfaces:** Produces:
- `UserSessionTokens.generate() -> Issued(String identifier, String verifier, String cookieValue())`; `parse(String) -> Optional<Parsed(String identifier, String verifier)>`; `hashVerifier(String) -> byte[]`; `matches(byte[] storedHash, String verifier) -> boolean` (constant time); `logId(String identifier) -> String`.
- `UserAgentSignature.of(String userAgent) -> String` e.g. `"chrome|macos"`; versions, device model and mobile markers ignored; unknown → `"other|other"`.
- `record ClientInfo(String ip, String userAgent)`.
- `UserSessionService` (constructor: repository, properties, `Clock`, `UserRepository`):
  - `Created create(User user, ClientInfo client)` → `record Created(UserSession session, String cookieValue)`; ends the oldest sessions beyond `maxPerUser` with `EVICTED`.
  - `AuthResult authenticate(String cookieValue, ClientInfo client)` → sealed `AuthResult`: `Authenticated(UserSession session, User user, Optional<String> newCookieValue)` | `Rejected(Reason reason)`; `Reason` ∈ `MALFORMED, UNKNOWN, BAD_VERIFIER, ENDED, EXPIRED_ABSOLUTE, EXPIRED_IDLE, USER_GONE, USER_AGENT_MISMATCH, SUPERSEDED_EXPIRED`, with `boolean countsAsGuess()` true only for `MALFORMED, UNKNOWN, BAD_VERIFIER`.
  - `void end(UserSession s, UserSessionEndReason r)`, `List<UserSession> listActive(UUID userId)`, `int endOthers(UUID userId, UUID currentSessionId)`, `boolean endOwn(UUID userId, UUID sessionId)`, `void revokeAll(UUID userId)`, `void markForRotation(UUID userId)`, `int cleanup()`.
- Rules (spec 5.4/5.5): reject user with `anonymizedAt != null` or `deletionRequestedAt != null` (`USER_GONE`, end session `REVOKED`); idle = `lastUsedAt + idleTimeout`; absolute = `expiresAt`; touch `lastUsedAt` only when older than `lastUsedWriteInterval`; coarse-signature mismatch ends the session when action is `END`; `rotationRequired` → new row (same `createdAt`, `expiresAt`, `csrfToken`; new identifier/verifier), old row `supersededBy`/`supersededUntil = now + rotationGrace`, `endReason ROTATED` only after grace; a superseded row inside grace authenticates without rotating, outside grace → `SUPERSEDED_EXPIRED`.

- [ ] **Step 1: Failing tests** (names, with exact assertions): `UserSessionTokensTest`: `generatedTokensAreDistinctAndParseRoundTrips`, `parseRejectsMissingSeparatorEmptyPartsAndOversizedValue`, `verifierMatchesOnlyOriginal`, `logIdDoesNotContainIdentifier`. `UserAgentSignatureTest`: Chrome 130 vs 131 on macOS equal; Chrome vs Firefox differ; macOS vs Windows differ; iPhone Safari and Android Chrome map to `safari|ios` and `chrome|android`; `null` and garbage → `other|other`. `UserSessionServiceTest`: `authenticate_validSession_touchesLastUsedOnlyAfterWriteInterval`, `authenticate_idleBoundary` (valid at 4h−1s, `EXPIRED_IDLE` at 4h), `authenticate_absoluteBoundary` (valid at 24h−1s, `EXPIRED_ABSOLUTE` at 24h even if active), `authenticate_badVerifierDoesNotRevealUnknownVsBad` (both `countsAsGuess`), `authenticate_anonymisedUser_rejectedAndSessionEnded`, `authenticate_userAgentFamilyChange_endsSession`, `authenticate_userAgentVersionChange_stillValid`, `create_eleventhSession_evictsOldest`, `rotation_createsNewRowKeepingExpiryAndCsrf`, `rotation_oldIdentifierValidInsideGraceNotRotatingAgain`, `rotation_oldIdentifierRejectedAfterGrace`, `cleanup_marksOverdueEndedAndDeletesOldEnded`.
- [ ] **Step 2: Run** `./gradlew test --tests 'de.tum.cit.aet.thesis.core.session.*'` → FAIL.
- [ ] **Step 3: Implement** the three classes against the signatures above (SHA-256 via `MessageDigest`, base64url without padding, cookie value max length 200).
- [ ] **Step 4: Run** → PASS. `./gradlew checkstyleMain checkstyleTest spotlessApply`.
- [ ] **Step 5: Commit** `Add session token handling and session service`.

### Task 4: Security wiring and session endpoints

**Files:** Create `core/session/security/UserSessionCookies.java`, `UserSessionAuthentication.java`, `UserSessionAuthenticationFilter.java`, `SessionCsrfTokenRepository.java`, `core/session/controller/UserSessionController.java`, `core/session/dto/SessionInfoDto.java` (`csrfToken`, `expiresAt`). Modify `core/security/WebSecurityConfig.java` (two chains), delete `core/security/JwtAuthConverter.java` (move role lookup into `core/session/security/UserAuthorities.java`), `core/security/CorsConfig.java` (allow header `X-CSRF-Token`), `core/security/CurrentUserProvider.java` (`Authentication` instead of `JwtAuthenticationToken`), `core/user/service/AuthenticationService.java` (`getAuthenticatedUser`, `getAuthenticatedUserWithResearchGroup` take `Authentication`; `updateAuthenticatedUser(JwtAuthenticationToken)` unchanged; `updateUserInformation` stops applying `email`), `core/user/controller/UserInfoController.java` (`GET` becomes a plain read via `getAuthenticatedUser`; other methods take `Authentication`), `core/user/controller/UserDeletionController.java` (types), `core/user/controller/AvatarController.java` only if its anonymous check needs it. Test: `server/src/test/.../mock/BaseIntegrationTest.java` and `TestSecurityConfig.java` as described below.

**Interfaces:** Produces:
- `UserSessionCookies`: `ResponseCookie create(String value, Duration maxAge)`, `ResponseCookie clear()`, `Optional<String> read(HttpServletRequest)`, `String name()` (name/`Secure` derived from `thesis-management.client.host`).
- `UserSessionAuthentication extends AbstractAuthenticationToken` (`getName()` = university id, `getCredentials()` = null, holds `UserSession`).
- `UserSessionAuthenticationFilter extends OncePerRequestFilter` per spec 5.4; sets request attribute `UserSessionAuthenticationFilter.SESSION_ATTRIBUTE`; on rejection writes `401`, `Set-Cookie` clear, logs `WARN` for guesses; on rotation adds the new cookie.
- `SessionCsrfTokenRepository implements CsrfTokenRepository`: `loadToken` reads the session attribute; `saveToken` no-op.
- Endpoints (spec 5.3) in `UserSessionController`: `POST /v2/auth/session` (param `JwtAuthenticationToken`), `GET /v2/auth/session`, `DELETE /v2/auth/session`; all session responses `Cache-Control: no-store`; logout response adds `Clear-Site-Data: "cache"`.
- Chain 1 `@Order(1)` matches `POST /v2/auth/session` only (oauth2 resource server, `JwtAuthenticationConverter` with `principalClaimName("preferred_username")`, CSRF off). Chain 2 `@Order(2)` has no `oauth2ResourceServer`, adds the filter before `CsrfFilter`, CSRF required only for unsafe methods **and** a session attribute present, existing `permitAll` GET rules kept.

- [ ] **Step 1: Adapt test infrastructure so the existing ~19 classes keep working.** In `BaseIntegrationTest.createTestUser`, replace the `GET /v2/user-info` call with `POST /v2/auth/session` using the same test Bearer header (the test chain accepts Bearer everywhere; user is created by `updateAuthenticatedUser`). Add `userSessionRepository.deleteAll()` to the cleanup if rows are not removed by the user FK cascade. Make `TestSecurityConfig` expose the same endpoints unchanged. Run `./gradlew test` and expect compile errors only from the signature changes below.
- [ ] **Step 2: Failing tests** in a new `core/session/UserSessionControllerTest extends BaseIntegrationTest` (test chain): `exchange_createsSessionAndReturnsCookieAndCsrfToken` (response has `Set-Cookie` with `sid=` and `HttpOnly; SameSite=Strict`, body `csrfToken`, DB row exists, hash differs from cookie verifier); `exchange_anonymisedUserIsRejected`; `exchange_newUserGetsStudentGroupBeforeSessionExists`. Existing `AuthenticationServiceTest` gains `updateUserInformation_ignoresEmailFromPayload`.
- [ ] **Step 3: Run** → FAIL.
- [ ] **Step 4: Implement** the classes above; delete `JwtAuthConverter`; update controller/service types; `GET /v2/user-info` plain read; `updateUserInformation` no longer calls `setEmail`; CORS header.
- [ ] **Step 5: Run** `./gradlew test` (whole server suite) → PASS; `checkstyleMain checkstyleTest spotlessApply`.
- [ ] **Step 6: Commit** `Authenticate API requests with application-owned sessions`.

### Task 5: Lifecycle hooks, detection and forwarded headers

**Files:** Modify `core/user/service/AccessManagementService.java` (call `UserSessionService.markForRotation(user.getId())` from `addGroup`/`removeGroup`; constructor gains `UserSessionService`; update `TestSecurityConfig`'s manual construction), `core/user/service/UserDeletionService.java` (`revokeAll(user.getId())` in `performSoftDeletion` and the full-deletion path before groups are deleted), `server/src/main/resources/application.yml` (`server.forward-headers-strategy: framework`). Create `core/session/cron/UserSessionCleanup.java` (`@Scheduled(fixedDelayString = "PT1H")`, calls `cleanup()`), `core/session/security/InvalidSessionTokenLimiter.java` (per-IP sliding window, `boolean tooMany(String ip)`, `void record(String ip)`). Test: `UserSessionLifecycleTest extends BaseIntegrationTest`.

**Interfaces:** Consumes Task 3 service. Produces: `InvalidSessionTokenLimiter` used by the filter: only requests carrying an invalid cookie from a blocked IP get `429`; valid or cookieless requests are never blocked.

- [ ] **Step 1: Failing tests:** `roleChange_flagsSessionsAndNextRequestRotatesCookie` (assign advisor role, next call returns a `Set-Cookie` with a new value and the old value still works once inside grace, fails after the clock passes grace); `parallelRequestsDuringRotation_neitherFails`; `userDeletion_revokesSessions` (soft and full); `limiter_blocksOnlyInvalidCookiesFromThatIp` (21 bad cookies → `429` for a bad cookie, a valid session from the same IP still `200`); `cleanupJob_deletesEndedOlderThanRetention`.
- [ ] **Step 2: Run** → FAIL. **Step 3: Implement.** **Step 4: Run** full server tests → PASS, checkstyle, spotless.
- [ ] **Step 5: Commit** `Rotate sessions on role changes, revoke on deletion, detect guessing`.

### Task 6: Real-chain integration tests including the Keycloak outage

**Files:** Modify `server/src/test/.../mock/BaseKeycloakIntegrationTest.java` (helper `SessionLogin sessionFor(String username)` performing the real exchange: Keycloak token → `POST /v2/auth/session` → returns cookie + csrf token), `core/security/KeycloakSecurityIntegrationTest.java` and `core/user/service/AccessManagementServiceIntegrationTest.java` (use session cookies instead of Bearer). Create `core/session/UserSessionSecurityIntegrationTest extends BaseKeycloakIntegrationTest`.

**Interfaces:** Consumes the real filter chains (profile `keycloak-test`). Produces: tests that prove spec G1, G3, G5.

- [ ] **Step 1: Write the tests** (all against the real chains): `exchange_withRealKeycloakToken_setsCookieWithExactFlags`; `session_authenticatesAndRolesComeFromUserGroups`; `bearerRejectedOnEveryEndpointExceptExchange` (including with a valid Keycloak token on `GET /v2/user-info`); `unsafeRequestWithoutCsrfToken_is403` and `...withWrongCsrfToken_is403` and `...withCorrectToken_succeeds`; `unsafeRequestWithoutAnyCookie_is401NotCsrf403`; `unknownCookie_is401AndClearsCookie`; `logout_endsSessionAndCookieNoLongerWorks`; `listAndEndSessions_onlyOwn` (after Task 8, add then); **`session_keepsWorking_whenKeycloakContainerIsStopped`** (create session, stop the Keycloak container, assert `GET /v2/user-info` and a protected action still `200`, and `POST /v2/auth/session` fails).
- [ ] **Step 2: Run** `./gradlew test --tests '*Keycloak*' --tests '*UserSessionSecurity*'` (needs Docker). Expected: FAIL until helpers exist, then PASS.
- [ ] **Step 3: Fix** whatever the real chain reveals in Tasks 3–5 code.
- [ ] **Step 4: Commit** `Test sessions against a real Keycloak, including an outage`.

### Task 7: Web app: session bootstrap, requests, lazy Keycloak

**Files:** Create `client/src/core/auth/session.ts` (`fetchSession()`, `createSession(keycloakToken)`, `deleteSession()`, `getCsrfToken()`, `setCsrfToken()`, `clearSessionState()`), `client/src/core/auth/keycloakClient.ts` (`createKeycloakClient()`, `getKeycloak()`, `ensureKeycloakSession(): Promise<Keycloak>`). Modify `core/requests/request.ts`, `core/providers/AuthenticationContext/AuthenticationProvider.tsx`, `core/hooks/authentication.ts` (remove token storage hooks; keep `useAuthenticationContext`, `useUser`, `useLoggedInUser`, `useHasGroupAccess`), `core/providers/AuthenticationContext/context.ts` (unchanged contract except removed unused types). Test: `client/src/core/requests/request.test.ts`, `client/src/core/auth/session.test.ts` (Vitest, mocked `fetch`).

**Interfaces:** Produces: `doRequest` signature unchanged. `request.ts` sends `credentials: 'include'`, no `Authorization`, adds `X-CSRF-Token` for non-GET; on `401` for `requiresAuth` calls `recoverSession()` once (shared in-flight promise), replays once, never twice. `recoverSession()` = keycloak `check-sso` + `createSession`; failure → provider marks unauthenticated.

- [ ] **Step 1: Failing tests** `request.test.ts`: `sendsCredentialsAndNoAuthorizationHeader`; `addsCsrfTokenOnlyOnUnsafeMethods`; `on401RecoversOnceAndReplaysOnce`; `concurrent401sShareOneRecovery`; `secondConsecutive401IsReturnedWithoutRetry`. `session.test.ts`: `fetchSessionStoresCsrfToken`, `fetchSessionReturnsUndefinedOn401`.
- [ ] **Step 2: Run** `cd client && pnpm exec vitest run src/core/requests src/core/auth` → FAIL.
- [ ] **Step 3: Implement.** Provider startup per spec 5.7: remove legacy `authentication_tokens` from `localStorage`; `fetchSession()`; if 200 → ready, authenticated, **no Keycloak call**; else lazily init keycloak-js with `check-sso` and `createSession`. Keycloak tokens in memory only. Passkey/credential functions call `ensureKeycloakSession()` (redirect login if no Keycloak session). `logout`: `deleteSession()`, clear state, then best-effort `keycloak.logout` keeping the existing 2 s fallback. Remove the refresh-token interval and `onAuthRefreshError` clearing. `isAuthenticated` derives from session state.
- [ ] **Step 4: Run** vitest, `pnpm exec eslint src/`, `pnpm exec tsc --noEmit` (ignore mantine-datatable errors), `pnpm build` → pass.
- [ ] **Step 5: Commit** `Use application-owned sessions in the web app`.

### Task 8: Active sessions (server API and settings interface)

**Files:** Modify `core/session/controller/UserSessionController.java` (+ `GET /v2/auth/sessions`, `DELETE /v2/auth/sessions/{id}`, `DELETE /v2/auth/sessions`), create `core/session/dto/UserSessionDto.java` (`id`, `createdAt`, `lastUsedAt`, `ip`, `device` label from signature, `current`). Client: create `client/src/core/user/components/ActiveSessions/ActiveSessions.tsx` plus request helpers under `client/src/core/user/requests/`, add to the existing user settings page. Tests: extend `UserSessionSecurityIntegrationTest` (`listSessions_returnsOnlyOwnAndMarksCurrent`, `endOtherSession_works_andCannotEndAnotherUsersSession_returns404`, `endAllOthers_keepsCurrent`); client unit test for the component rendering; e2e in Task 9.

- [ ] Steps: failing server tests → implement endpoints → pass; implement component → `eslint`, `tsc`, `build`; commit `Let users see and end their active sessions`.

### Task 9: End-to-end tests and outage drill

**Files:** Modify `client/e2e/auth.setup.ts`, `client/e2e/passkey.spec.ts` (replace `access_token` in `localStorage` assertions with: session cookie present, no `authentication_tokens` in storage), `client/e2e/user/auth.spec.ts`; create `client/e2e/user/keycloak-outage.spec.ts` (log in, `context.route` aborts every request to the Keycloak origin `http://localhost:8181`, reload, assert still authenticated and a navigation to an authenticated page works) and `client/e2e/user/active-sessions.spec.ts` (two contexts, end one from the other).

- [ ] Steps: update setup, run `./execute-e2e-local.sh` for the touched specs (see saved reference notes for worktree pitfalls), make them pass; run the full e2e suite once; commit `Update end-to-end tests for sessions and add outage drill`.

### Task 10: Documentation and production hardening

**Files:** Modify `docker-compose.prod.yml` (Traefik HSTS middleware `stsSeconds=31536000`, attached to both routers), `CLAUDE.md` (Authorization Architecture: Keycloak only creates sessions), `docs/DEVELOPMENT.md` (auth flow section, client/server roles), `docs/CONFIGURATION.md` (session properties), `docs/PRODUCTION.md` (HSTS, cookies, outage behaviour, re-login after outages longer than Keycloak's SSO idle timeout), `docs/DATA_RETENTION.md` (session rows, IP and User-Agent, 30 days), `docs/endpoints.yaml` and the Postman collection note (session cookie and CSRF header, new endpoints).

- [ ] Steps: edit; grep for banned words; `docker compose -f docker-compose.prod.yml config` validates; commit `Document sessions, add HSTS`.

### Task 11: Whole-branch verification

- [ ] Run `cd server && ./gradlew test checkstyleMain checkstyleTest`, `cd client && pnpm exec eslint src/ && pnpm exec tsc --noEmit && pnpm exec vitest run && pnpm build`, full e2e. Manual drill: log in locally, stop Keycloak (`docker stop` the container), reload and use the app for several minutes; restart the server with Keycloak down.
- [ ] Dispatch a fresh reviewer (`deep-code-reviewer`) on `git diff develop...HEAD`; fix findings; re-run tests.
