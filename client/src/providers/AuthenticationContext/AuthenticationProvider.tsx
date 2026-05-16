import type { PropsWithChildren } from 'react'
import { useEffect, useMemo, useState } from 'react'
import type { IAuthenticationContext, IDecodedAccessToken, IDecodedRefreshToken } from './context'
import { AuthenticationContext } from './context'
import Keycloak from 'keycloak-js'
import { GLOBAL_CONFIG } from '../../config/global'
import { jwtDecode } from 'jwt-decode'
import type { IAuthenticationTokens } from '../../hooks/authentication'
import { getAuthenticationTokens, useAuthenticationTokens } from '../../hooks/authentication'
import { useSignal } from '../../hooks/utility'
import type { IUser } from '../../requests/responses/user'
import { doRequest } from '../../requests/request'
import { showSimpleError } from '../../utils/notification'
import { ApiError, getApiResponseErrorMessage } from '../../requests/handler'
import type { ILightResearchGroup } from '../../requests/responses/researchGroup'
import { getDeviceName, getPasskeyErrorMessage } from '../../utils/passkey'

const createKeycloakClient = () =>
  new Keycloak({
    realm: GLOBAL_CONFIG.keycloak.realm,
    url: GLOBAL_CONFIG.keycloak.host,
    clientId: GLOBAL_CONFIG.keycloak.client_id,
  })

export let keycloak = createKeycloakClient()

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

const getKeycloakInitOptions = (tokens?: IAuthenticationTokens, shouldCheckSso = false) => ({
  ...(shouldCheckSso ? { onLoad: 'check-sso' as const } : {}),
  pkceMethod: 'S256' as const,
  silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
  silentCheckSsoFallback: false,
  checkLoginIframe: false,
  token: tokens?.access_token,
  refreshToken: tokens?.refresh_token,
})

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

  const [universityId, setUniversityId] = useState<string>()
  const [user, setUser] = useState<IUser>()
  const [authenticationTokens, setAuthenticationTokens] = useAuthenticationTokens()
  const { signal: readySignal, triggerSignal: triggerReadySignal, ref: readyRef } = useSignal()
  const isReady = readyRef.isTriggerred
  const [researchGroups, setResearchGroups] = useState<ILightResearchGroup[]>([])
  const isPasskeySupportedByBrowser = isPasskeySupportedInBrowser()
  const [isPasskeyExtensionAvailable, setIsPasskeyExtensionAvailable] = useState<
    boolean | undefined
  >(undefined)
  const isPasskeySupported = isPasskeySupportedByBrowser ? isPasskeyExtensionAvailable : false

  const refreshAccessToken = (activeKeycloak = keycloak) =>
    activeKeycloak
      .updateToken(60 * 5)
      .then((isSuccess) => {
        if (!isSuccess) {
          setAuthenticationTokens(undefined)
        }
      })
      .catch(() => {
        setAuthenticationTokens(undefined)
      })

  const storeTokens = async (activeKeycloak = keycloak) => {
    const refreshToken = activeKeycloak.refreshToken
    const accessToken = activeKeycloak.token

    let decodedAccessToken: IDecodedAccessToken | undefined
    let decodedRefreshToken: IDecodedRefreshToken | undefined

    try {
      decodedAccessToken = accessToken ? jwtDecode<IDecodedAccessToken>(accessToken) : undefined
      decodedRefreshToken = refreshToken ? jwtDecode<IDecodedRefreshToken>(refreshToken) : undefined
    } catch (error) {
      console.error('Failed to decode authentication tokens', error)
      activeKeycloak.clearToken()
      setAuthenticationTokens(undefined)
      return
    }

    // refresh if already expired
    if (decodedRefreshToken?.exp && decodedRefreshToken.exp <= Date.now() / 1000) {
      activeKeycloak.clearToken()
      return setAuthenticationTokens(undefined)
    } else if (decodedAccessToken?.exp && decodedAccessToken.exp <= Date.now() / 1000) {
      return refreshAccessToken(activeKeycloak)
    }

    if (accessToken && refreshToken) {
      setAuthenticationTokens({
        access_token: accessToken,
        refresh_token: refreshToken,
      })
    } else {
      setAuthenticationTokens(undefined)
    }
  }

  const unsetKeycloakListeners = (activeKeycloak = keycloak) => {
    activeKeycloak.onAuthRefreshSuccess = undefined
    activeKeycloak.onAuthRefreshError = undefined
    activeKeycloak.onAuthLogout = undefined
  }

  const setKeycloakListeners = (activeKeycloak = keycloak) => {
    activeKeycloak.onAuthRefreshSuccess = () => void storeTokens(activeKeycloak)
    activeKeycloak.onAuthRefreshError = () => {
      activeKeycloak.clearToken()
      setAuthenticationTokens(undefined)
    }
    activeKeycloak.onAuthLogout = () => {
      activeKeycloak.clearToken()
      setAuthenticationTokens(undefined)
    }
  }

  const initializeKeycloakSession = async (
    tokens?: IAuthenticationTokens,
    options?: { passkeyFlow?: boolean },
  ) => {
    unsetKeycloakListeners()

    keycloak = createKeycloakClient()
    setKeycloakListeners(keycloak)

    const authenticated = await keycloak.init(
      getKeycloakInitOptions(tokens, options?.passkeyFlow ?? false),
    )
    await storeTokens(keycloak)

    return authenticated
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
      | { challenge?: string }
      | undefined

    if (!responseData?.challenge) {
      throw new Error('Passkey challenge response is missing the challenge value')
    }

    return responseData.challenge
  }

  useEffect(() => {
    setUser(undefined)

    const storedTokens = getAuthenticationTokens()

    void initializeKeycloakSession(storedTokens)
      .then(() => {
        triggerReadySignal()
      })
      .catch((error) => {
        console.error('Keycloak init error', error)
      })
      .finally(() => {
        if (!readyRef.isTriggerred) {
          triggerReadySignal()
        }
      })

    const refreshTokenFrequency = 60 * 1000
    const refreshTokenInterval = setInterval(() => {
      const refreshToken = keycloak.refreshToken
      let decodedRefreshToken: IDecodedRefreshToken | undefined

      try {
        decodedRefreshToken = refreshToken
          ? jwtDecode<IDecodedRefreshToken>(refreshToken)
          : undefined
      } catch {
        keycloak.clearToken()
        setAuthenticationTokens(undefined)
        return
      }

      if (decodedRefreshToken?.exp && decodedRefreshToken.exp <= Date.now() / 1000) {
        keycloak.clearToken()
        setAuthenticationTokens(undefined)
      }
    }, refreshTokenFrequency)

    return () => {
      clearInterval(refreshTokenInterval)
      unsetKeycloakListeners()
    }
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- mount-only keycloak setup: setAuthenticationTokens/triggerReadySignal are stable refs intentionally captured once
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
    if (!isReady) {
      return
    }

    if (authenticationTokens?.access_token) {
      let decodedAccessToken: IDecodedAccessToken

      try {
        decodedAccessToken = jwtDecode<IDecodedAccessToken>(authenticationTokens.access_token)
      } catch {
        keycloak.clearToken()
        setAuthenticationTokens(undefined)
        setUniversityId(undefined)
        return
      }

      setUniversityId(decodedAccessToken['preferred_username'] || undefined)
    } else {
      setUniversityId(undefined)
    }
  }, [authenticationTokens?.access_token, isReady, setAuthenticationTokens])

  useEffect(() => {
    setUser(undefined)

    if (isReady && universityId) {
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
  }, [universityId, isReady])

  useEffect(() => {
    if (!isReady || !universityId) {
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
  }, [isReady, universityId])

  const contextValue = useMemo<IAuthenticationContext>(() => {
    return {
      isReady,
      isAuthenticated: Boolean(authenticationTokens?.access_token),
      user: authenticationTokens?.access_token ? user : undefined,
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
        readySignal.then(() => {
          const tokens = getAuthenticationTokens()

          if (!tokens?.access_token) {
            const keycloakRedirectUri = getLoginRedirectUri(redirectUri)

            return keycloak.login(
              keycloakRedirectUri ? { redirectUri: keycloakRedirectUri } : undefined,
            )
          }
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

          const authenticated = await initializeKeycloakSession(undefined, {
            passkeyFlow: true,
          })
          if (!authenticated) {
            throw new Error('No active Keycloak session detected after passkey authentication')
          }
        }),
      registerPasskey: () =>
        readySignal.then(async () => {
          if (!isPasskeySupported) {
            throw new Error('Passkeys are not available in this environment')
          }

          const storedTokens = getAuthenticationTokens()
          if (!storedTokens?.access_token) {
            throw new Error('You must be logged in to register a passkey')
          }

          if (keycloak.authenticated && keycloak.isTokenExpired(5)) {
            try {
              await keycloak.updateToken(5 * 60)
            } catch (error) {
              throw new Error(
                await getPasskeyErrorMessage(
                  error,
                  'Failed to refresh the access token for passkey registration',
                ),
                { cause: error },
              )
            }
          }

          const accessToken = keycloak.token ?? storedTokens.access_token
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
            throw new Error(await getPasskeyErrorMessage(response, undefined, 'register'))
          }
        }),
      listCredentials: () =>
        readySignal.then(async () => {
          const storedTokens = getAuthenticationTokens()
          if (!storedTokens?.access_token) {
            throw new Error('You must be logged in to list credentials')
          }

          if (keycloak.authenticated && keycloak.isTokenExpired(5)) {
            try {
              await keycloak.updateToken(5 * 60)
            } catch {
              throw new Error('Failed to refresh the access token for credential listing')
            }
          }

          const accessToken = keycloak.token ?? storedTokens.access_token
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

          const storedTokens = getAuthenticationTokens()
          if (!storedTokens?.access_token) {
            throw new Error('You must be logged in to delete credentials')
          }

          if (keycloak.authenticated && keycloak.isTokenExpired(5)) {
            try {
              await keycloak.updateToken(5 * 60)
            } catch {
              throw new Error('Failed to refresh the access token for credential deletion')
            }
          }

          const accessToken = keycloak.token ?? storedTokens.access_token
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
      logout: (redirectUri: string) => {
        setAuthenticationTokens(undefined)

        const timeout = setTimeout(() => {
          window.location.href = `${window.location.origin}${redirectUri}`
        }, 2000)

        void readySignal.then(() => {
          if (keycloak.authenticated) {
            clearTimeout(timeout)

            void keycloak.logout({
              redirectUri: `${window.location.origin}${redirectUri}`,
            })
          }
        })
      },
      researchGroups: researchGroups,
      isPasskeySupported,
    }
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- researchGroups/setAuthenticationTokens/readySignal/access_token are captured by reference inside callbacks that read the latest value at call time; recomputing the entire context on each token refresh would re-render every consumer
  }, [
    isReady,
    user,
    authenticationTokens?.access_token,
    readySignal,
    researchGroups,
    isPasskeySupported,
  ])

  return <AuthenticationContext value={contextValue}>{children}</AuthenticationContext>
}

export default AuthenticationProvider
