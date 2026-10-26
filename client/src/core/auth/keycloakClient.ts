import Keycloak from 'keycloak-js'
import { GLOBAL_CONFIG } from '@/core/config/global'

/**
 * Keycloak is only used to prove who the user is: for logging in, and for Keycloak-native features
 * such as passkeys. Its tokens stay in memory inside the keycloak-js instance and are never
 * persisted. A page load that already has an application session does not start Keycloak at all.
 */
let keycloak: Keycloak | undefined
let ssoCheck: Promise<boolean> | undefined

// keycloak-js settles the silent check only when the hidden iframe posts a message; if Keycloak is down
// the iframe never does, so the check must be bounded.
const SSO_CHECK_TIMEOUT_MS = 10_000

function withTimeout<T>(promise: Promise<T>, timeoutMs: number, message: string): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(message)), timeoutMs)

    promise.then(
      (value) => {
        clearTimeout(timer)
        resolve(value)
      },
      (error: unknown) => {
        clearTimeout(timer)
        reject(error instanceof Error ? error : new Error(String(error)))
      },
    )
  })
}

const createKeycloakClient = () =>
  new Keycloak({
    realm: GLOBAL_CONFIG.keycloak.realm,
    url: GLOBAL_CONFIG.keycloak.host,
    clientId: GLOBAL_CONFIG.keycloak.client_id,
  })

export const getKeycloak = () => keycloak

/**
 * Creates a fresh keycloak-js client and initialises it. With `checkSso` a silent single-sign-on
 * check is made. Resolves to whether the browser currently holds a Keycloak session.
 */
export function initKeycloak(options: { checkSso: boolean }): Promise<boolean> {
  const client = createKeycloakClient()
  keycloak = client

  const initialisation = client.init({
    ...(options.checkSso ? { onLoad: 'check-sso' as const } : {}),
    pkceMethod: 'S256',
    silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
    silentCheckSsoFallback: false,
    checkLoginIframe: false,
  })

  const initialised = options.checkSso
    ? withTimeout(
        initialisation,
        SSO_CHECK_TIMEOUT_MS,
        'Keycloak did not answer the silent login check',
      )
    : initialisation

  if (options.checkSso) {
    // remember the outcome so that features asking later do not repeat the silent check
    ssoCheck = initialised
    initialised.catch(() => {
      if (ssoCheck === initialised) {
        ssoCheck = undefined
      }
    })
  }

  return initialised
}

/** Runs the silent single-sign-on check once per page load, however many features ask for it. */
export function ensureSsoChecked(): Promise<boolean> {
  return ssoCheck ?? initKeycloak({ checkSso: true })
}

/** Returns the initialised client, creating it (without a silent SSO check) if none exists yet. */
export async function ensureKeycloak(): Promise<Keycloak> {
  if (!keycloak) {
    await initKeycloak({ checkSso: false })
  }

  return keycloak as Keycloak
}

/**
 * Cheap reachability probe, used before anything redirects to Keycloak or waits for it, so that an unavailable
 * Keycloak does not turn a login or logout into a browser error page or an endless wait.
 *
 * It asks for the realm's discovery document. A readable answer must be a success. Keycloak is usually on
 * another origin than the web app, and an error page from a proxy in front of an unavailable Keycloak then
 * typically carries no CORS headers, so the browser hides it: it looks like a network failure, and the
 * fallback (a network-level check) reports "reachable". That case is not detected here; the 10 second bound on
 * the silent check is the backstop, and the login redirect ends on the proxy's error page.
 */
export async function isKeycloakReachable(timeoutMs = 2000): Promise<boolean> {
  const url = `${GLOBAL_CONFIG.keycloak.host.replace(/\/+$/, '')}/realms/${encodeURIComponent(GLOBAL_CONFIG.keycloak.realm)}/.well-known/openid-configuration`

  const probe = async (init: RequestInit) => {
    const controller = new AbortController()
    const timer = setTimeout(() => controller.abort(), timeoutMs)

    try {
      return await fetch(url, { ...init, cache: 'no-store', signal: controller.signal })
    } finally {
      clearTimeout(timer)
    }
  }

  try {
    return (await probe({})).ok
  } catch {
    // blocked by cross-origin rules or not reachable: tell the two apart with a request whose answer is opaque
  }

  try {
    await probe({ mode: 'no-cors' })

    return true
  } catch {
    return false
  }
}
