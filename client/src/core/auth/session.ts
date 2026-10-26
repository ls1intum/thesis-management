import { GLOBAL_CONFIG } from '@/core/config/global'

/**
 * Client side of the application-owned session. The session itself lives in an HttpOnly cookie that
 * JavaScript can neither read nor store; this module only keeps the CSRF token that the server
 * hands out with the session and talks to the session endpoints.
 */
export interface ISessionInfo {
  csrfToken: string
  expiresAt: string
}

let csrfToken: string | undefined
let recoveryHandler: (() => Promise<boolean>) | undefined
let recoveryInFlight: Promise<boolean> | undefined

const sessionUrl = () => `${GLOBAL_CONFIG.server_host}/api/v2/auth/session`

export const getCsrfToken = () => csrfToken

export const setCsrfToken = (token: string | undefined) => {
  csrfToken = token
}

export const clearSessionState = () => {
  csrfToken = undefined
}

const readSessionInfo = async (response: Response): Promise<ISessionInfo> => {
  const info = (await response.json()) as ISessionInfo

  setCsrfToken(info.csrfToken)

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

/**
 * Asks the server for the current CSRF token. Used when a request was rejected as a CSRF failure: the
 * session cookie is shared by all tabs, so another tab may have replaced the session (and its token).
 * Resolves to `undefined` when there is no session or the server cannot be reached.
 */
export async function refreshCsrfToken(): Promise<string | undefined> {
  try {
    return (await fetchSession())?.csrfToken
  } catch {
    return undefined
  }
}

/** Ends the session on the server. Local state is cleared even if the server cannot be reached. */
export async function deleteSession(): Promise<void> {
  try {
    await fetch(sessionUrl(), {
      method: 'DELETE',
      credentials: 'include',
      headers: csrfToken ? { 'X-CSRF-Token': csrfToken } : {},
    })
  } catch (error) {
    console.error('Failed to end the session on the server', error)
  } finally {
    clearSessionState()
  }
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
