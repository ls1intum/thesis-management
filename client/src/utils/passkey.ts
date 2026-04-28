export const getPasskeyErrorMessage = (error: unknown, fallback = 'Passkey operation failed') => {
  if (error instanceof DOMException && error.name === 'NotAllowedError') {
    return 'Passkey request was cancelled or timed out'
  }

  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message
  }

  return fallback
}
