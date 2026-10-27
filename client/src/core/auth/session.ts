import { GLOBAL_CONFIG } from '@/core/config/global'

/**
 * Client side of the application-owned session. The session itself lives in an HttpOnly cookie that
 * JavaScript can neither read nor store; this module only keeps the CSRF token that the server
 * hands out with the session and talks to the session endpoints.
 */
export interface ISessionInfo {
  csrfToken: string
  expiresAt: string
  /** The user the session belongs to; compared before a request is replayed under a replaced session. */
  userId?: string
}

let csrfToken: string | undefined
// The user of the latest session this tab saw, and the user this tab has been showing since it adopted its first
// session. The second one is never cleared while the page lives, not even while a re-login is under way, so that a
// request is never replayed under another user's session just because the current user is momentarily unknown.
let sessionUserId: string | undefined
let tabUserId: string | undefined
let identityChanged = false
let identityChangeHandler: (() => void) | undefined
let recoveryHandler: (() => Promise<boolean>) | undefined
let recoveryInFlight: Promise<boolean> | undefined
// Set once a logout has begun: from then on no silent re-login may create a new session.
let loggingOut = false
let recoveryBlockedUntil = 0

const LOGOUT_BROADCAST_CHANNEL = 'thesis-management-session-logout'
// Other tabs of the browser must not sign in again silently in the moment between ending the session and ending
// the Keycloak login (which they would find still alive).
const RECOVERY_BLOCK_AFTER_LOGOUT_MS = 30_000

const logoutChannel = (() => {
  try {
    return typeof BroadcastChannel === 'undefined'
      ? undefined
      : new BroadcastChannel(LOGOUT_BROADCAST_CHANNEL)
  } catch {
    return undefined
  }
})()

logoutChannel?.addEventListener('message', () => {
  recoveryBlockedUntil = Date.now() + RECOVERY_BLOCK_AFTER_LOGOUT_MS
})

const sessionUrl = () => `${GLOBAL_CONFIG.server_host}/api/v2/auth/session`

export const getCsrfToken = () => csrfToken

export const setCsrfToken = (token: string | undefined) => {
  csrfToken = token
}

export const getSessionUserId = () => sessionUserId

export const setSessionUserId = (userId: string | undefined) => {
  sessionUserId = userId
  tabUserId ??= userId
}

/** Forgets the current session (it ended) but not who this tab has been showing. */
export const forgetSession = () => {
  csrfToken = undefined
  sessionUserId = undefined
}

/** Forgets everything, including who this tab has been showing: after a logout, and between tests. */
export const clearSessionState = () => {
  forgetSession()
  tabUserId = undefined
  identityChanged = false
  loggingOut = false
  recoveryBlockedUntil = 0
}

/**
 * Whether the session now in place belongs to another user than the one this tab has been showing. It fails
 * closed: a session whose user is unknown does not count as the same user.
 */
export const sessionBelongsToOtherUser = () =>
  tabUserId !== undefined && sessionUserId !== tabUserId

/** True once the tab found out that it shows another user than the session; it then sends no request. */
export const isIdentityChangePending = () => identityChanged

/** True from the moment a logout begins (it may still fail, which is reported to the caller). */
export const isLoggingOut = () => loggingOut

/**
 * Registers what happens when the session turns out to belong to a different user than the one this tab was
 * showing (someone else logged in, in another tab). The default reloads the page, which drops all state of the
 * previous user.
 */
export const setIdentityChangeHandler = (handler: (() => void) | undefined) => {
  identityChangeHandler = handler
}

export function handleIdentityChange() {
  identityChanged = true

  if (identityChangeHandler) {
    identityChangeHandler()
  } else {
    window.location.reload()
  }
}

const readSessionInfo = async (response: Response): Promise<ISessionInfo> => {
  const info = (await response.json()) as ISessionInfo

  setCsrfToken(info.csrfToken)
  sessionUserId = info.userId
  tabUserId ??= info.userId

  return info
}

/**
 * Asks the server whether the browser holds a valid session.
 * Resolves to `undefined` when there is none and rejects when the server cannot be reached.
 */
export async function fetchSession(): Promise<ISessionInfo | undefined> {
  const response = await fetch(sessionUrl(), {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
  })

  if (response.status === 401) {
    forgetSession()
    return undefined
  }

  if (!response.ok) {
    throw new Error(`Unexpected response ${response.status} while checking the session`)
  }

  return readSessionInfo(response)
}

/**
 * Exchanges a Keycloak access token for a session. The Keycloak token is sent exactly once; the
 * server answers with the session cookie and a CSRF token.
 */
export async function createSession(keycloakToken: string): Promise<ISessionInfo> {
  const response = await fetch(sessionUrl(), {
    method: 'POST',
    credentials: 'include',
    headers: { Authorization: `Bearer ${keycloakToken}` },
  })

  if (!response.ok) {
    const body = (await response.json().catch(() => undefined)) as { message?: string } | undefined

    throw new SessionExchangeError(response.status, body?.message)
  }

  return readSessionInfo(response)
}

/** The server refused to turn the Keycloak login into a session (for example for a deleted account). */
export class SessionExchangeError extends Error {
  readonly status: number
  readonly serverMessage: string | undefined

  constructor(status: number, serverMessage?: string) {
    super(`Failed to create a session (${status})${serverMessage ? `: ${serverMessage}` : ''}`)
    this.name = 'SessionExchangeError'
    this.status = status
    this.serverMessage = serverMessage
  }
}

let csrfRefreshInFlight: Promise<string | undefined> | undefined

/**
 * Asks the server for the current CSRF token. Used when a request was rejected as a CSRF failure: the
 * session cookie is shared by all tabs, so another tab may have replaced the session (and its token).
 * Concurrent callers share one request. Resolves to `undefined` when there is no session or the server
 * cannot be reached.
 */
export function refreshCsrfToken(): Promise<string | undefined> {
  csrfRefreshInFlight ??= fetchSession()
    .then((info) => info?.csrfToken)
    .catch(() => undefined)
    .finally(() => {
      csrfRefreshInFlight = undefined
    })

  return csrfRefreshInFlight
}

/**
 * Ends the session on the server. If the server rejects the CSRF token (another tab replaced the session in
 * the meantime) the session is looked up again: if it is gone the logout is done, if it now belongs to another
 * user nothing is ended (the tab is reset instead, so Alice's logout can never end Bob's session), and otherwise
 * the logout is retried once with the current token. A failure to reach the server is reported to the caller,
 * which must not pretend that the user is logged out: the HttpOnly cookie would still sign them in again. The
 * CSRF token is kept so that the logout can be retried.
 */
export async function deleteSession(): Promise<void> {
  // the session is gone: forget it and who this tab showed, but keep refusing silent re-logins until the page leaves
  const endLocally = () => {
    forgetSession()
    tabUserId = undefined
  }

  const send = (token: string | undefined) =>
    fetch(sessionUrl(), {
      method: 'DELETE',
      credentials: 'include',
      headers: token ? { 'X-CSRF-Token': token } : {},
    })

  // From here on no silent re-login may create a session behind the back of the logout: tell the other tabs and let
  // a re-login that is already running finish, so that the DELETE below ends the session it created.
  loggingOut = true
  logoutChannel?.postMessage('logout')
  await recoveryInFlight

  try {
    const sentToken = csrfToken
    let response = await send(sentToken)

    if (response.status === 403) {
      const current = await fetchSession()

      if (!current) {
        endLocally()

        return
      }

      if (tabUserId !== undefined && current.userId !== tabUserId) {
        handleIdentityChange()

        throw new Error('The session now belongs to another user and was not ended')
      }

      if (current.csrfToken !== sentToken) {
        response = await send(current.csrfToken)
      }
    }

    if (!response.ok && response.status !== 401) {
      throw new Error(`The server did not end the session (${response.status})`)
    }
  } catch (error) {
    // the session is still there and the user stays logged in
    loggingOut = false

    throw error
  }

  endLocally()
}

/** Registers how the web app silently re-establishes a session (Keycloak check-sso + exchange). */
export const setSessionRecoveryHandler = (handler: (() => Promise<boolean>) | undefined) => {
  recoveryHandler = handler
}

/**
 * Tries once to re-establish an ended session. Concurrent callers share the single in-flight
 * attempt; a failed attempt resolves to `false` and is never retried automatically.
 */
export function recoverSession(): Promise<boolean> {
  if (!recoveryHandler || loggingOut || Date.now() < recoveryBlockedUntil) {
    return Promise.resolve(false)
  }

  recoveryInFlight ??= recoveryHandler()
    .catch(() => false)
    .finally(() => {
      recoveryInFlight = undefined
    })

  return recoveryInFlight
}
