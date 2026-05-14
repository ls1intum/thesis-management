import type { IKeycloakCredential } from '../providers/AuthenticationContext/context'

export type PasskeyOperation = 'login' | 'register' | 'list' | 'delete'

const defaultPasskeyErrorMessage = 'Something went wrong. Please try again later.'
const invalidPasskeyMessage = 'This passkey is not valid for your account.'
const duplicatePasskeyMessage = 'This passkey is already registered for your account.'

const getResponseMessage = (text: string) => {
  try {
    const body = JSON.parse(text) as Record<string, unknown>
    const message = body.errorMessage ?? body.message ?? body.error

    return typeof message === 'string' ? message : text
  } catch {
    return text
  }
}

export const getPasskeyErrorMessage = async (
  error: unknown,
  fallback = defaultPasskeyErrorMessage,
  operation?: PasskeyOperation,
) => {
  let message = ''
  let status: number | undefined

  if (error instanceof Response) {
    status = error.status
    message = getResponseMessage(await error.text().catch(() => ''))
  } else if (error instanceof Error) {
    message = error.message
  }

  if ((operation === 'register' && status === 409) || message === duplicatePasskeyMessage) {
    return duplicatePasskeyMessage
  }

  if (
    (operation === 'login' && status !== undefined && status >= 400 && status < 500) ||
    message === invalidPasskeyMessage
  ) {
    return invalidPasskeyMessage
  }

  return fallback
}

export const isPasskeyCredential = (credential: IKeycloakCredential) =>
  credential.type?.toLowerCase().includes('webauthn') ?? false

export const getDeviceName = () => {
  const navigatorWithUaData = navigator as Navigator & {
    userAgentData?: { platform?: string; brands?: { brand?: string }[] }
  }
  // Prefer Client Hints platform, then legacy navigator.platform, then a stable fallback.
  const userAgentDataPlatform = navigatorWithUaData.userAgentData?.platform?.trim() ?? ''
  const navigatorPlatformRaw = (navigator as unknown as Record<string, unknown>).platform
  const navigatorPlatform =
    typeof navigatorPlatformRaw === 'string' ? navigatorPlatformRaw.trim() : ''
  const rawPlatform =
    userAgentDataPlatform !== ''
      ? userAgentDataPlatform
      : navigatorPlatform !== ''
        ? navigatorPlatform
        : 'Unknown Platform'
  // Normalize noisy platform strings to concise labels used in the saved device name.
  const platform = rawPlatform.startsWith('Mac')
    ? 'Mac'
    : rawPlatform.startsWith('Win')
      ? 'Win'
      : rawPlatform.startsWith('Linux')
        ? 'Linux'
        : rawPlatform

  const brands = navigatorWithUaData.userAgentData?.brands ?? []
  // Use browser brand hints first; if unavailable, infer from user agent.
  const browserFromBrands =
    brands
      .find(
        (entry) =>
          entry.brand !== undefined &&
          !(
            entry.brand.toLowerCase().startsWith('not') &&
            entry.brand.toLowerCase().endsWith('brand')
          ),
      )
      ?.brand?.trim() ?? ''

  const userAgentRaw = (navigator as unknown as Record<string, unknown>).userAgent
  const userAgent = typeof userAgentRaw === 'string' ? userAgentRaw : ''
  let browser = browserFromBrands
  if (browser === '') {
    if (userAgent.includes('Firefox')) {
      browser = 'Firefox'
    } else if (userAgent.includes('Edg')) {
      browser = 'Edge'
    } else if (userAgent.includes('Chrome')) {
      browser = 'Chrome'
    } else if (userAgent.includes('Safari')) {
      browser = 'Safari'
    } else {
      browser = 'Unknown Browser'
    }
  }

  return `${platform} - ${browser}`
}
