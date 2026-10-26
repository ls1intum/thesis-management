import Keycloak from 'keycloak-js'
import { GLOBAL_CONFIG } from '@/core/config/global'

/**
 * Keycloak is only used to prove who the user is: for logging in, and for Keycloak-native features
 * such as passkeys. Its tokens stay in memory inside the keycloak-js instance and are never
 * persisted. A page load that already has an application session does not start Keycloak at all.
 */
let keycloak: Keycloak | undefined

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
export async function initKeycloak(options: { checkSso: boolean }): Promise<boolean> {
  const client = createKeycloakClient()
  keycloak = client

  return client.init({
    ...(options.checkSso ? { onLoad: 'check-sso' as const } : {}),
    pkceMethod: 'S256',
    silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
    silentCheckSsoFallback: false,
    checkLoginIframe: false,
  })
}

/** Returns the initialised client, creating it (without a silent SSO check) if none exists yet. */
export async function ensureKeycloak(): Promise<Keycloak> {
  if (!keycloak) {
    await initKeycloak({ checkSso: false })
  }

  return keycloak as Keycloak
}

/**
 * Cheap reachability probe used before redirecting to Keycloak for logout, so that an unavailable
 * Keycloak does not turn a logout into a browser error page.
 */
export async function isKeycloakReachable(timeoutMs = 2000): Promise<boolean> {
  try {
    await fetch(
      `${GLOBAL_CONFIG.keycloak.host.replace(/\/+$/, '')}/realms/${encodeURIComponent(GLOBAL_CONFIG.keycloak.realm)}/.well-known/openid-configuration`,
      { mode: 'no-cors', cache: 'no-store', signal: AbortSignal.timeout(timeoutMs) },
    )

    return true
  } catch {
    return false
  }
}
