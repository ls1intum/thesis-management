import { GLOBAL_CONFIG } from '@/core/config/global'
import { getCsrfToken, recoverSession, refreshCsrfToken } from '@/core/auth/session'

export type ApiResponse<T> =
  | { ok: true; status: number; data: T }
  | { ok: false; status: number; data: undefined; error?: Error }
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
export type ResponseType = 'json' | 'blob'

export interface IRequestOptions {
  method: HttpMethod
  requiresAuth: boolean
  responseType?: ResponseType
  data?: unknown
  formData?: FormData
  params?: Record<string, string | number | boolean>
  controller?: AbortController
}

/** Reads the body of a successful response; responses without content (204) have none. */
const readSuccessBody = async (result: Response, responseType?: ResponseType) => {
  if (responseType === 'blob') {
    return result.blob()
  }

  if (result.status === 204 || result.status === 205) {
    return undefined
  }

  const text = await result.text()

  return text ? JSON.parse(text) : undefined
}

export function doRequest<T>(url: string, options: IRequestOptions): Promise<ApiResponse<T>>
export function doRequest<T>(
  url: string,
  options: IRequestOptions,
  cb: (res: ApiResponse<T>) => unknown,
): () => void
export function doRequest<T>(
  url: string,
  options: IRequestOptions,
  cb?: (res: ApiResponse<T>) => unknown,
): Promise<ApiResponse<T>> | (() => void) {
  const controller = options.controller ?? new AbortController()

  const executeRequest = async (isReplay = false): Promise<ApiResponse<T>> => {
    const params = new URLSearchParams()

    if (options.params) {
      for (const [key, value] of Object.entries(options.params)) {
        params.append(key, value.toString())
      }
    }

    if (options.data && options.formData) {
      throw new Error('Cannot send both data and formData')
    }

    const csrfToken = options.method === 'GET' ? undefined : getCsrfToken()

    const result = await fetch(`${GLOBAL_CONFIG.server_host}/api${url}?${params.toString()}`, {
      method: options.method,
      credentials: 'include',
      headers: {
        ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
        ...(options.data ? { 'Content-Type': 'application/json' } : {}),
      },
      body: options.formData ?? (options.data ? JSON.stringify(options.data) : undefined),
      signal: controller.signal,
    })

    // The session ended (24 hour limit, ended from another device). A 401 means the server rejected
    // the request before processing it, so replaying it once after a silent re-login is safe.
    if (result.status === 401 && options.requiresAuth && !isReplay && (await recoverSession())) {
      return executeRequest(true)
    }

    // A 403 for a request that carried a CSRF token may mean that another tab replaced the session and with it
    // the token. Replay once if the token really changed; otherwise it is an ordinary permission error.
    if (result.status === 403 && csrfToken && !isReplay) {
      const refreshedToken = await refreshCsrfToken()

      if (refreshedToken && refreshedToken !== csrfToken) {
        return executeRequest(true)
      }
    }

    if (result.status >= 200 && result.status < 300) {
      return {
        ok: true,
        status: result.status,
        data: (await readSuccessBody(result, options.responseType)) as T,
      }
    } else {
      let errorMessage: string | undefined = undefined

      if (result.headers.get('content-type') === 'application/json') {
        const json = await result.json()

        if (json.message) {
          errorMessage = json.message
        }
      }

      return {
        ok: false,
        status: result.status,
        data: undefined,
        error: errorMessage ? new Error(errorMessage) : undefined,
      }
    }
  }

  const promise = executeRequest().catch<ApiResponse<T>>((error) => {
    return {
      ok: false,
      status: error.name === 'AbortError' ? 1005 : 1000,
      data: undefined,
      error,
    }
  })

  const blacklistedCodes = [1005]

  if (cb) {
    void promise.then((res) => !blacklistedCodes.includes(res.status) && cb(res))

    return () => {
      controller.abort()
    }
  }

  return promise
}
