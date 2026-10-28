import type { PropsWithChildren } from 'react'
import { useEffect, useMemo, useRef, useState } from 'react'
import type {
  IAuthenticationContext,
  IDecodedAccessToken,
} from '@/core/providers/AuthenticationContext/context'
import { AuthenticationContext } from '@/core/providers/AuthenticationContext/context'
import { GLOBAL_CONFIG } from '@/core/config/global'
import { jwtDecode } from 'jwt-decode'
import { useSignal } from '@/core/hooks/utility'
import { useClearAvatarCacheOnSignOut } from '@/core/components/CustomAvatar/useClearAvatarCacheOnSignOut'
import type { IUser } from '@/core/user/requests/responses/user'
import { doRequest } from '@/core/requests/request'
import { showSimpleError } from '@/core/utils/notification'
import { ApiError, getApiResponseErrorMessage } from '@/core/requests/handler'
import type { ILightResearchGroup } from '@/core/group/requests/responses/researchGroup'
import { getDeviceName, getPasskeyErrorMessage } from '@/core/utils/passkey'
import {
  ensureKeycloak,
  ensureSsoChecked,
  getKeycloak,
  initKeycloak,
  isKeycloakReachable,
} from '@/core/auth/keycloakClient'
import {
  SessionExchangeError,
  abandonSession,
  createSession,
  deleteSession,
  fetchSession,
  forgetSession,
  isLoggingOut,
  isRecoveryBlocked,
  setSessionRecoveryHandler,
} from '@/core/auth/session'

const LOGIN_UNAVAILABLE_NOTICE_INTERVAL_MS = 15_000

/** The browser's Keycloak login belongs to another account than the application session of this tab. */
class KeycloakIdentityMismatchError extends Error {
  constructor() {
    super(
      'Your browser is signed in to Keycloak as another account than the one you are using here. Please log out and log in again.',
    )
    this.name = 'KeycloakIdentityMismatchError'
  }
}

// Keycloak tokens used to be persisted here. Authentication tokens must not live in web storage.
const LEGACY_TOKEN_STORAGE_KEY = 'authentication_tokens'

const removeLegacyTokens = () => {
  try {
    localStorage.removeItem(LEGACY_TOKEN_STORAGE_KEY)
  } catch {
    // storage may be unavailable (private mode, blocked site data); nothing to clean up then
  }
}

const isPasskeySupportedInBrowser = () =>
  typeof window !== 'undefined' &&
  window.isSecureContext &&
  typeof window.PublicKeyCredential !== 'undefined' &&
  typeof navigator !== 'undefined' &&
  typeof navigator.credentials !== 'undefined'

const getPasskeyEndpoint = (path: string) => {
  const baseUrl = GLOBAL_CONFIG.keycloak.host.replace(/\/+$/, '')
  const realm = encodeURIComponent(GLOBAL_CONFIG.keycloak.realm)
  const clientId = encodeURIComponent(GLOBAL_CONFIG.keycloak.client_id)

  return `${baseUrl}/realms/${realm}/passkey/${clientId}/${path}`
}

const getAccountCredentialsEndpoint = (credentialId?: string) => {
  const baseUrl = GLOBAL_CONFIG.keycloak.host.replace(/\/+$/, '')
  const realm = encodeURIComponent(GLOBAL_CONFIG.keycloak.realm)
  const encodedCredentialId = credentialId ? `/${encodeURIComponent(credentialId)}` : ''

  return `${baseUrl}/realms/${realm}/account/credentials${encodedCredentialId}`
}

const toBase64Url = (buffer: ArrayBuffer) => {
  const bytes = new Uint8Array(buffer)
  let binary = ''

  for (const byte of bytes) {
    binary += String.fromCharCode(byte)
  }

  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '')
}

const fromBase64Url = (value: string) => {
  const base64 = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
  const binary = atob(padded)
  const bytes = new Uint8Array(binary.length)

  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index)
  }

  return bytes
}

const isObjectRecord = (value: unknown): value is Record<string, unknown> =>
  !!value && typeof value === 'object' && !Array.isArray(value)

const normalizeCredentialFromMetadata = (
  containerType: string | undefined,
  metadata: unknown,
): { id: string; type?: string; userLabel?: string; createdDate?: number } | undefined => {
  if (!isObjectRecord(metadata) || !isObjectRecord(metadata.credential)) {
    return undefined
  }

  const credential = metadata.credential
  const id = typeof credential.id === 'string' ? credential.id : undefined
  if (!id) {
    return undefined
  }

  const createdDate =
    typeof credential.createdDate === 'number' ? credential.createdDate : undefined
  const userLabel = typeof credential.userLabel === 'string' ? credential.userLabel : undefined
  const typeFromCredential = typeof credential.type === 'string' ? credential.type : undefined

  return {
    id,
    type: typeFromCredential ?? containerType,
    userLabel,
    createdDate,
  }
}

const normalizeCredentialFromFlatEntry = (
  entry: unknown,
): { id: string; type?: string; userLabel?: string; createdDate?: number } | undefined => {
  if (!isObjectRecord(entry) || typeof entry.id !== 'string') {
    return undefined
  }

  return {
    id: entry.id,
    type: typeof entry.type === 'string' ? entry.type : undefined,
    userLabel: typeof entry.userLabel === 'string' ? entry.userLabel : undefined,
    createdDate: typeof entry.createdDate === 'number' ? entry.createdDate : undefined,
  }
}

const parseAccountCredentialsResponse = (
  payload: unknown,
): { id: string; type?: string; userLabel?: string; createdDate?: number }[] => {
  if (!Array.isArray(payload)) {
    throw new Error('Credential endpoint returned an invalid response')
  }

  const credentials: { id: string; type?: string; userLabel?: string; createdDate?: number }[] = []
  const seenCredentialIds = new Set<string>()

  for (const entry of payload) {
    const flatCredential = normalizeCredentialFromFlatEntry(entry)
    if (flatCredential && !seenCredentialIds.has(flatCredential.id)) {
      seenCredentialIds.add(flatCredential.id)
      credentials.push(flatCredential)
      continue
    }

    if (!isObjectRecord(entry)) {
      continue
    }

    const containerType = typeof entry.type === 'string' ? entry.type : undefined
    const metadatas = Array.isArray(entry.userCredentialMetadatas)
      ? entry.userCredentialMetadatas
      : undefined

    if (!metadatas) {
      continue
    }

    for (const metadata of metadatas) {
      const normalizedCredential = normalizeCredentialFromMetadata(containerType, metadata)
      if (!normalizedCredential || seenCredentialIds.has(normalizedCredential.id)) {
        continue
      }

      seenCredentialIds.add(normalizedCredential.id)
      credentials.push(normalizedCredential)
    }
  }

  return credentials
}

const getLoginRedirectUri = (redirectUri?: string) => {
  if (!redirectUri) {
    return undefined
  }

  if (/^https?:\/\//.test(redirectUri)) {
    return redirectUri
  }

  return `${window.location.origin}${redirectUri.startsWith('/') ? redirectUri : `/${redirectUri}`}`
}

const AuthenticationProvider = (props: PropsWithChildren) => {
  const { children } = props

  const [hasSession, setHasSession] = useState(false)
  const hasSessionRef = useRef(false)
  const loginUnavailableNoticeAt = useRef(0)
  // Set when Keycloak knows the user but the server refused to turn that login into a session. Calling login()
  // again would only bounce between the web app and Keycloak (which answers at once), so it reports instead.
  const sessionExchangeFailed = useRef(false)
  const universityIdRef = useRef<string | undefined>(undefined)
  const [user, setUser] = useState<IUser>()

  useClearAvatarCacheOnSignOut(hasSession)
  const { signal: readySignal, triggerSignal: triggerReadySignal, ref: readyRef } = useSignal()
  const isReady = readyRef.isTriggerred
  const [researchGroups, setResearchGroups] = useState<ILightResearchGroup[]>([])
  const isPasskeySupportedByBrowser = isPasskeySupportedInBrowser()
  const [isPasskeyExtensionAvailable, setIsPasskeyExtensionAvailable] = useState<
    boolean | undefined
  >(undefined)
  const isPasskeySupported = isPasskeySupportedByBrowser ? isPasskeyExtensionAvailable : false

  useEffect(() => {
    universityIdRef.current = user?.universityId
  }, [user])

  const updateHasSession = (value: boolean) => {
    hasSessionRef.current = value
    setHasSession(value)
  }

  /**
   * Asks Keycloak whether the browser is logged in (this also completes the redirect back from the
   * login page) and, if so, exchanges that login for an application session. A silent attempt (nobody asked for
   * it) must also give way to a logout that another tab began; an explicit login only to one of this tab.
   */
  const establishSessionFromKeycloak = async (silent: boolean): Promise<boolean> => {
    const mayCreateSession = () => !(silent ? isRecoveryBlocked() : isLoggingOut())

    // The silent check waits for Keycloak and would hang if it is down, so look first. Not when the browser has
    // just come back from the Keycloak login: its one-time code must be exchanged now, and Keycloak just answered.
    const returningFromLogin = /[?#&]code=/.test(`${window.location.search}${window.location.hash}`)

    if (!returningFromLogin && !(await isKeycloakReachable())) {
      throw new Error('Keycloak is unreachable')
    }

    const authenticated = await initKeycloak({ checkSso: true })
    const keycloakToken = getKeycloak()?.token

    // A logout that began while Keycloak was being asked must not be undone by a session created now.
    if (!authenticated || !keycloakToken || !mayCreateSession()) {
      return false
    }

    try {
      await createSession(keycloakToken)
    } catch (error) {
      sessionExchangeFailed.current = true

      throw error
    }

    sessionExchangeFailed.current = false

    // The logout may have begun while the exchange was under way, after it ended the session this one replaced.
    if (!mayCreateSession()) {
      await abandonSession()

      return false
    }

    return true
  }

  const showLoginUnavailableNotice = (message: string) => {
    // Some pages ask for a login repeatedly (the interview booking page every second): tell the user once
    // in a while, not with every call.
    if (Date.now() - loginUnavailableNoticeAt.current > LOGIN_UNAVAILABLE_NOTICE_INTERVAL_MS) {
      loginUnavailableNoticeAt.current = Date.now()
      showSimpleError(message)
    }
  }

  /**
   * Returns a valid Keycloak access token for Keycloak-native features. When the browser has no
   * Keycloak session, `allowLoginRedirect` decides whether to send the user to the login page
   * (explicit actions) or to fail (passive reads such as listing passkeys).
   */
  const getKeycloakAccessToken = async (allowLoginRedirect: boolean): Promise<string> => {
    let client = getKeycloak()

    if (!client?.authenticated) {
      await ensureSsoChecked()
      client = getKeycloak()
    }

    if (!client?.authenticated) {
      if (allowLoginRedirect) {
        await client?.login({ redirectUri: window.location.href })
      }

      throw new Error('Please sign in again to continue')
    }

    await client.updateToken(60)

    if (!client.token) {
      throw new Error('Keycloak did not return an access token')
    }

    // The application session and the browser's Keycloak login are independent: Keycloak may have switched to
    // another account (in another application or tab). Credential operations must act on the account this tab is
    // logged in as, never on whichever account Keycloak currently knows.
    let keycloakUsername: unknown
    try {
      keycloakUsername = jwtDecode<IDecodedAccessToken>(client.token).preferred_username
    } catch {
      keycloakUsername = undefined
    }

    if (!universityIdRef.current || keycloakUsername !== universityIdRef.current) {
      throw new KeycloakIdentityMismatchError()
    }

    return client.token
  }

  const recoverSessionSilently = async (): Promise<boolean> => {
    // Another tab may have signed in again already: the session cookie is shared, so adopt that session
    // instead of logging in a second time (which would replace it and desynchronise the other tabs).
    const existingSession = await fetchSession().catch(() => undefined)

    if (existingSession) {
      updateHasSession(true)

      return true
    }

    forgetSession()

    try {
      if (await establishSessionFromKeycloak(true)) {
        updateHasSession(true)

        return true
      }
    } catch (error) {
      console.error('Silent login after the session ended failed', error)
    }

    updateHasSession(false)
    setUser(undefined)

    return false
  }

  const requestPasskeyChallenge = async () => {
    const response = await fetch(getPasskeyEndpoint('challenge'), {
      method: 'GET',
      credentials: 'include',
      cache: 'no-store',
    })

    if (!response.ok) {
      if (response.status === 404 || response.status === 405) {
        setIsPasskeyExtensionAvailable(false)
      }
      throw new Error(await getPasskeyErrorMessage(response))
    }

    const responseData = (await response.json().catch(() => undefined)) as
      { challenge?: string } | undefined

    if (!responseData?.challenge) {
      throw new Error('Passkey challenge response is missing the challenge value')
    }

    return responseData.challenge
  }

  const passkeySavedLocallyButRejectedByServerMessage =
    'Your device saved the passkey, but the server rejected it. Please contact support to clean it up.'

  useEffect(() => {
    removeLegacyTokens()
    setUser(undefined)
    setSessionRecoveryHandler(recoverSessionSilently)

    const bootstrap = async () => {
      try {
        // A valid session needs no Keycloak at all, which keeps the application usable while Keycloak is down.
        const existingSession = await fetchSession().catch((error) => {
          console.error('Failed to check the session', error)

          return undefined
        })

        if (existingSession) {
          updateHasSession(true)

          // Back from a Keycloak login that a passkey action started: keycloak-js has to process the one-time
          // code in the address now, otherwise the next Keycloak call would try to use it after it expired.
          if (/[?#&]code=/.test(`${window.location.search}${window.location.hash}`)) {
            void ensureSsoChecked().catch((error) => {
              console.error('Failed to complete the Keycloak login', error)
            })
          }

          return
        }

        if (await establishSessionFromKeycloak(true)) {
          updateHasSession(true)
        }
      } catch (error) {
        console.error('Authentication error', error)
        showSimpleError(
          error instanceof SessionExchangeError && error.serverMessage
            ? error.serverMessage
            : 'Authentication service is unavailable. Please try again.',
        )
      }
    }

    void bootstrap().finally(() => {
      if (!readyRef.isTriggerred) {
        triggerReadySignal()
      }
    })

    return () => {
      setSessionRecoveryHandler(undefined)
    }
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- mount-only session bootstrap: the setters and helpers it calls only touch refs and stable state setters
  }, [])

  useEffect(() => {
    if (!isPasskeySupportedByBrowser) {
      setIsPasskeyExtensionAvailable(false)
      return
    }

    setIsPasskeyExtensionAvailable(undefined)

    let isMounted = true

    const validatePasskeyAvailability = async () => {
      try {
        const response = await fetch(getPasskeyEndpoint('health'), {
          method: 'GET',
          credentials: 'include',
        })

        if (isMounted) {
          setIsPasskeyExtensionAvailable(response.ok)
        }
      } catch {
        if (isMounted) {
          setIsPasskeyExtensionAvailable(false)
        }
      }
    }

    void validatePasskeyAvailability()

    return () => {
      isMounted = false
    }
  }, [isPasskeySupportedByBrowser])

  useEffect(() => {
    setUser(undefined)

    if (isReady && hasSession) {
      return doRequest<IUser>(
        '/v2/user-info',
        {
          method: 'GET',
          requiresAuth: true,
        },
        (res) => {
          if (res.ok) {
            setUser(res.data)
          } else {
            showSimpleError(getApiResponseErrorMessage(res))
          }
        },
      )
    }
  }, [hasSession, isReady])

  useEffect(() => {
    if (!isReady || !hasSession) {
      return
    }

    return doRequest<ILightResearchGroup[]>(
      '/v2/research-groups/light/active',
      {
        method: 'GET',
        requiresAuth: true,
      },
      (res) => {
        if (res.ok) {
          const sortedGroups = res.data.sort((a, b) => a.name.localeCompare(b.name))
          setResearchGroups(sortedGroups)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }, [isReady, hasSession])

  const contextValue = useMemo<IAuthenticationContext>(() => {
    return {
      isReady,
      isAuthenticated: hasSession,
      user: hasSession ? user : undefined,
      groups: [],
      updateUser: setUser,
      updateInformation: async (data, avatar, examinationReport, cv, degreeReport) => {
        const formData = new FormData()

        formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }))

        if (avatar) {
          formData.append('avatar', avatar)
        }

        if (examinationReport) {
          formData.append('examinationReport', examinationReport)
        }

        if (cv) {
          formData.append('cv', cv)
        }

        if (degreeReport) {
          formData.append('degreeReport', degreeReport)
        }

        const response = await doRequest<IUser>('/v2/user-info', {
          method: 'PUT',
          requiresAuth: true,
          formData,
        })

        if (response.ok) {
          setUser(response.data)
        } else {
          throw new ApiError(response)
        }
      },
      login: (redirectUri?: string) =>
        readySignal.then(async () => {
          if (hasSessionRef.current) {
            return
          }

          if (sessionExchangeFailed.current) {
            showLoginUnavailableNotice(
              'Signing in failed: the server could not turn your login into a session. Please reload the page to try again.',
            )

            return
          }

          // Logging in is the one thing that needs Keycloak: say so instead of sending the user to a dead page.
          if (!(await isKeycloakReachable())) {
            showLoginUnavailableNotice(
              'The login service is currently unavailable. Please try again in a few minutes.',
            )

            return
          }

          const client = await ensureKeycloak()
          const keycloakRedirectUri = getLoginRedirectUri(redirectUri)

          return client.login(
            keycloakRedirectUri ? { redirectUri: keycloakRedirectUri } : undefined,
          )
        }),
      loginWithPasskey: () =>
        readySignal.then(async () => {
          if (!isPasskeySupported) {
            throw new Error('Passkeys are not available in this environment')
          }

          const challenge = await requestPasskeyChallenge()
          const passkeyCredential = await navigator.credentials.get({
            publicKey: {
              challenge: fromBase64Url(challenge),
              rpId: GLOBAL_CONFIG.passkey_rp_id,
              userVerification: 'preferred',
            },
          })

          if (!passkeyCredential || !(passkeyCredential instanceof PublicKeyCredential)) {
            throw new Error('No passkey credential was returned')
          }

          if (!(passkeyCredential.response instanceof AuthenticatorAssertionResponse)) {
            throw new Error('Received an unexpected passkey authentication response')
          }

          if (!passkeyCredential.response.userHandle) {
            throw new Error('Passkey did not return a user handle')
          }

          const response = await fetch(getPasskeyEndpoint('authenticate'), {
            method: 'POST',
            credentials: 'include',
            headers: {
              Accept: 'application/json',
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
              credentialId: toBase64Url(passkeyCredential.rawId),
              userHandle: toBase64Url(passkeyCredential.response.userHandle),
              clientDataJSON: toBase64Url(passkeyCredential.response.clientDataJSON),
              authenticatorData: toBase64Url(passkeyCredential.response.authenticatorData),
              signature: toBase64Url(passkeyCredential.response.signature),
              challenge,
            }),
          })

          if (response.status !== 204) {
            throw new Error(await getPasskeyErrorMessage(response, undefined, 'login'))
          }

          if (!(await establishSessionFromKeycloak(false))) {
            throw new Error('No active Keycloak session detected after passkey authentication')
          }

          updateHasSession(true)
        }),
      registerPasskey: () =>
        readySignal.then(async () => {
          if (!isPasskeySupported) {
            throw new Error('Passkeys are not available in this environment')
          }

          if (!hasSessionRef.current) {
            throw new Error('You must be logged in to register a passkey')
          }

          let accessToken: string
          try {
            accessToken = await getKeycloakAccessToken(true)
          } catch (error) {
            if (error instanceof KeycloakIdentityMismatchError) {
              throw error
            }

            throw new Error(
              await getPasskeyErrorMessage(
                error,
                'Failed to refresh the access token for passkey registration',
              ),
              { cause: error },
            )
          }

          const challenge = await requestPasskeyChallenge()

          let decodedToken: IDecodedAccessToken | undefined
          try {
            decodedToken = jwtDecode<IDecodedAccessToken>(accessToken)
          } catch {
            decodedToken = undefined
          }

          const userHandle = decodedToken?.sub
          if (!userHandle) {
            throw new Error('Cannot register a passkey: access token has no subject')
          }
          const userIdBytes = new TextEncoder().encode(userHandle)
          const username = decodedToken?.preferred_username ?? decodedToken?.email ?? userHandle
          const tokenDisplayName =
            typeof decodedToken?.name === 'string' ? decodedToken?.name.trim() : ''
          const displayName = tokenDisplayName.length > 0 ? tokenDisplayName : username

          const passkeyCredential = await navigator.credentials.create({
            publicKey: {
              challenge: fromBase64Url(challenge),
              rp: { name: GLOBAL_CONFIG.passkey_rp_name, id: GLOBAL_CONFIG.passkey_rp_id },
              user: { id: userIdBytes, name: username, displayName },
              pubKeyCredParams: [
                { type: 'public-key', alg: -7 },
                { type: 'public-key', alg: -257 },
              ],
              authenticatorSelection: {
                residentKey: 'required',
                userVerification: 'preferred',
              },
              attestation: 'none',
            },
          })

          if (!passkeyCredential || !(passkeyCredential instanceof PublicKeyCredential)) {
            throw new Error('No passkey credential was returned')
          }

          if (!(passkeyCredential.response instanceof AuthenticatorAttestationResponse)) {
            throw new Error('Received an unexpected passkey registration response')
          }

          const response = await fetch(getPasskeyEndpoint('save'), {
            method: 'POST',
            credentials: 'include',
            headers: {
              Authorization: `Bearer ${accessToken}`,
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
              deviceName: getDeviceName(),
              credentialId: toBase64Url(passkeyCredential.rawId),
              clientDataJSON: toBase64Url(passkeyCredential.response.clientDataJSON),
              attestationObject: toBase64Url(passkeyCredential.response.attestationObject),
              challenge,
            }),
          })

          if (!response.ok) {
            throw new Error(
              await getPasskeyErrorMessage(
                response,
                passkeySavedLocallyButRejectedByServerMessage,
                'register',
                { preferFallback: true },
              ),
            )
          }
        }),
      listCredentials: () =>
        readySignal.then(async () => {
          if (!hasSessionRef.current) {
            throw new Error('You must be logged in to list credentials')
          }

          let accessToken: string
          try {
            accessToken = await getKeycloakAccessToken(false)
          } catch (error) {
            if (error instanceof KeycloakIdentityMismatchError) {
              throw error
            }

            throw new Error('Failed to refresh the access token for credential listing', {
              cause: error,
            })
          }

          const response = await fetch(getAccountCredentialsEndpoint(), {
            method: 'GET',
            credentials: 'include',
            headers: {
              Accept: 'application/json',
              Authorization: `Bearer ${accessToken}`,
            },
          })

          if (!response.ok) {
            throw new Error(await getPasskeyErrorMessage(response))
          }

          const responseData = (await response.json().catch(() => undefined)) as unknown
          return parseAccountCredentialsResponse(responseData)
        }),
      deleteCredential: (credentialId: string) =>
        readySignal.then(async () => {
          if (!credentialId.trim()) {
            throw new Error('Credential id is required')
          }

          if (!hasSessionRef.current) {
            throw new Error('You must be logged in to delete credentials')
          }

          let accessToken: string
          try {
            accessToken = await getKeycloakAccessToken(true)
          } catch (error) {
            if (error instanceof KeycloakIdentityMismatchError) {
              throw error
            }

            throw new Error('Failed to refresh the access token for credential deletion', {
              cause: error,
            })
          }

          const response = await fetch(getAccountCredentialsEndpoint(credentialId), {
            method: 'DELETE',
            credentials: 'include',
            headers: {
              Accept: 'application/json',
              Authorization: `Bearer ${accessToken}`,
            },
          })

          if (!response.ok) {
            throw new Error(await getPasskeyErrorMessage(response))
          }
        }),
      logout: async (redirectUri: string) => {
        await readySignal

        // End the application session first (it works without Keycloak). If that fails the HttpOnly cookie still
        // signs the user in, so do not act as if they were logged out: say so and stay logged in.
        try {
          await deleteSession()
        } catch (error) {
          console.error('Failed to end the session', error)
          showSimpleError(
            'Logging out failed: the server could not end your session. You are still logged in, please try again.',
          )

          return false
        }

        updateHasSession(false)
        setUser(undefined)

        const target = `${window.location.origin}${redirectUri}`

        // Then end the Keycloak single-sign-on session if Keycloak is reachable. Otherwise the next page load
        // would silently sign the user in again.
        try {
          if (await isKeycloakReachable()) {
            // The silent check yields the id token that lets Keycloak end the login without asking for a
            // confirmation; without a Keycloak login there is nothing to end. It is made afresh: an earlier
            // result of this page load may predate a login that another tab has made since.
            await initKeycloak({ checkSso: true })
            const client = getKeycloak()

            if (client?.authenticated) {
              await client.logout({ redirectUri: target })

              return true
            }
          }
        } catch (error) {
          console.error('Failed to end the Keycloak session', error)
        }

        window.location.href = target

        return true
      },
      researchGroups: researchGroups,
      isPasskeySupported,
    }
  }, [isReady, user, hasSession, readySignal, researchGroups, isPasskeySupported])

  return <AuthenticationContext value={contextValue}>{children}</AuthenticationContext>
}

export default AuthenticationProvider
