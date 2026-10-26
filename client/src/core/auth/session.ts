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
let sessionUserId: string | undefined
let identityChangeHandler: (() => void) | undefined
let recoveryHandler: (() => Promise<boolean>) | undefined
let recoveryInFlight: Promise<boolean> | undefined

const sessionUrl = () => `${GLOBAL_CONFIG.server_host}/api/v2/auth/session`

export const getCsrfToken = () => csrfToken

export const setCsrfToken = (token: string | undefined) => {
  csrfToken = token
}

export const getSessionUserId = () => sessionUserId

export const setSessionUserId = (userId: string | undefined) => {
  sessionUserId = userId
}

export const clearSessionState = () => {
  csrfToken = undefined
  sessionUserId = undefined
}

/**
 * Registers what happens when the session turns out to belong to a different user than the one this tab was
 * showing (someone else logged in, in another tab). The default reloads the page, which drops all state of the
 * previous user.
 */
export const setIdentityChangeHandler = (handler: (() => void) | undefined) => {
  identityChangeHandler = handler
}

export function handleIdentityChange() {
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
    clearSessionState()
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
    throw new Error(`Failed to create a session (${response.status})`)
  }

  return readSessionInfo(response)
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
 * the meantime) the token is refreshed and the logout is retried once. A session that is already gone counts as
 * ended. Any other failure is reported to the caller, which must not pretend that the user is logged out: the
 * HttpOnly cookie would still sign them in again. The CSRF token is kept so that the logout can be retried.
 */
export async function deleteSession(): Promise<void> {
  const send = (token: string | undefined) =>
    fetch(sessionUrl(), {
      method: 'DELETE',
      credentials: 'include',
      headers: token ? { 'X-CSRF-Token': token } : {},
    })

  const sentToken = csrfToken
  let response = await send(sentToken)

  if (response.status === 403) {
    const refreshedToken = await refreshCsrfToken()

    if (refreshedToken && refreshedToken !== sentToken) {
      response = await send(refreshedToken)
    }
  }

  if (!response.ok && response.status !== 401) {
    throw new Error(`The server did not end the session (${response.status})`)
  }

  clearSessionState()
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
  if (!recoveryHandler) {
    return Promise.resolve(false)
  }

  recoveryInFlight ??= recoveryHandler()
    .catch(() => false)
    .finally(() => {
      recoveryInFlight = undefined
    })

  return recoveryInFlight
}
