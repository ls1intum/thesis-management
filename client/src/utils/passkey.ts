import type { IKeycloakCredential } from '../providers/AuthenticationContext/context'

const extractMessageFromPasskeyErrorBody = (body: unknown): string | undefined => {
  if (typeof body === 'string') {
    const trimmedBody = body.trim()
    return trimmedBody.length > 0 ? trimmedBody : undefined
  }

  if (Array.isArray(body)) {
    for (const entry of body) {
      const message = extractMessageFromPasskeyErrorBody(entry)
      if (message) {
        return message
      }
    }
    return undefined
  }

  if (!body || typeof body !== 'object') {
    return undefined
  }

  const payload = body as Record<string, unknown>
  const candidateKeys = ['error_description', 'errorMessage', 'error', 'message', 'detail']

  for (const key of candidateKeys) {
    const message = extractMessageFromPasskeyErrorBody(payload[key])
    if (message) {
      return message
    }
  }

  return undefined
}

export const getPasskeyErrorMessage = async (
  error: unknown,
  fallback = 'Passkey operation failed',
) => {
  if (error instanceof Response) {
    const text = await error.text().catch(() => '')

    if (!text) {
      return `Request failed with status ${error.status}`
    }

    try {
      const parsed = JSON.parse(text) as unknown
      const extractedMessage = extractMessageFromPasskeyErrorBody(parsed)
      if (extractedMessage) {
        return extractedMessage
      }
    } catch {
      // Response body is not JSON, use raw text below.
    }

    return text
  }

  if (error instanceof DOMException && error.name === 'NotAllowedError') {
    return 'Passkey request was cancelled or timed out'
  }

  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message
  }

  return fallback
}

export const isPasskeyCredential = (credential: IKeycloakCredential) =>
  credential.type?.toLowerCase().includes('webauthn') ?? false
