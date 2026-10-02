import { doRequest } from '@/core/requests/request'

const MAX_ATTEMPTS = 3
const RETRY_DELAY_MS = 500

// Object URLs by avatar path. The same picture is shown many times on one page (every row of the theses overview
// repeats the supervisor), so each picture is requested once. Failures are not kept, the next render tries again.
const cache = new Map<string, Promise<string | undefined>>()

const wait = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms))

// Missing (404), forbidden or unauthenticated pictures will not appear by asking again; network errors, rate limits
// and server errors may.
const isRetryable = (status: number) => status === 429 || status >= 500

const fetchAvatar = async (path: string): Promise<string | undefined> => {
  for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt += 1) {
    const response = await doRequest<Blob>(path, {
      method: 'GET',
      requiresAuth: true,
      responseType: 'blob',
    })

    if (response.ok) {
      return URL.createObjectURL(response.data)
    }

    if (!isRetryable(response.status) || attempt === MAX_ATTEMPTS) {
      return undefined
    }

    await wait(RETRY_DELAY_MS * attempt)
  }

  return undefined
}

export const loadAvatar = (path: string): Promise<string | undefined> => {
  const cached = cache.get(path)

  if (cached) {
    return cached
  }

  const pending = fetchAvatar(path).then((url) => {
    if (!url) {
      cache.delete(path)
    }

    return url
  })

  cache.set(path, pending)

  return pending
}

export const clearAvatarCache = () => {
  const entries = [...cache.values()]
  cache.clear()

  for (const entry of entries) {
    void entry.then((url) => url && URL.revokeObjectURL(url))
  }
}
