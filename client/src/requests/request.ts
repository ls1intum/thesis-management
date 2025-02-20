import { GLOBAL_CONFIG } from '../config/global'
import { keycloak } from '../providers/AuthenticationContext/AuthenticationProvider'

export type ApiResponse<T> =
  | { ok: true; status: number; data: T }
  | { ok: false; status: number; data: undefined; error?: Error }
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
export type ResponseType = 'json' | 'blob'

export interface IRequestOptions {
  method: HttpMethod
  requiresAuth: boolean
  responseType?: ResponseType
  data?: any
  formData?: FormData
  params?: Record<string, string | number | boolean>
  controller?: AbortController
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
  const controller = options.controller || new AbortController()

  const executeRequest = async (): Promise<ApiResponse<T>> => {
    if (options.requiresAuth && keycloak.isTokenExpired(5)) {
      const updateSuccess = await keycloak.updateToken(5 * 60)

      if (!updateSuccess) {
        throw new Error('Failed to refresh access token. Please try to refresh the page')
      }
    }

    const jwtToken = keycloak.token

    if (options.requiresAuth && !jwtToken) {
      throw new Error('User not authenticated')
    }

    const params = new URLSearchParams()

    if (options.params) {
      for (const [key, value] of Object.entries(options.params)) {
        params.append(key, value.toString())
      }
    }

    if (options.data && options.formData) {
      throw new Error('Cannot send both data and formData')
    }

    const result = await fetch(`${GLOBAL_CONFIG.server_host}/api${url}?${params.toString()}`, {
      method: options.method,
      headers: {
        ...(options.requiresAuth ? { Authorization: `Bearer ${jwtToken}` } : {}),
        ...(options.data ? { 'Content-Type': 'application/json' } : {}),
      },
      body: options.formData ?? (options.data ? JSON.stringify(options.data) : undefined),
      signal: controller.signal,
    })

    if (result.status >= 200 && result.status < 300) {
      return {
        ok: true,
        status: result.status,
        data: options.responseType === 'blob' ? await result.blob() : await result.json(),
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
    promise.then((res) => !blacklistedCodes.includes(res.status) && cb(res))

    return () => {
      controller.abort()
    }
  }

  return promise
}
