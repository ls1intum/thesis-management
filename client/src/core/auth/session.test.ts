import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  clearSessionState,
  createSession,
  deleteSession,
  fetchSession,
  getCsrfToken,
  recoverSession,
  setSessionRecoveryHandler,
} from '@/core/auth/session'

const jsonResponse = (status: number, body?: unknown) =>
  new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json' },
  })

describe('session', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
    clearSessionState()
    setSessionRecoveryHandler(undefined)
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('fetchSession stores the CSRF token and sends the cookie', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { csrfToken: 'csrf-1', expiresAt: 'x' }))

    const session = await fetchSession()

    expect(session?.csrfToken).toBe('csrf-1')
    expect(getCsrfToken()).toBe('csrf-1')
    const [url, init] = fetchMock.mock.calls[0]
    expect(String(url)).toMatch(/\/api\/v2\/auth\/session$/)
    expect(init.credentials).toBe('include')
    expect(init.method).toBe('GET')
  })

  it('fetchSession returns undefined and forgets the CSRF token on 401', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(200, { csrfToken: 'csrf-1', expiresAt: 'x' }))
    await fetchSession()

    fetchMock.mockResolvedValueOnce(jsonResponse(401))
    const session = await fetchSession()

    expect(session).toBeUndefined()
    expect(getCsrfToken()).toBeUndefined()
  })

  it('fetchSession rejects when the server cannot be reached', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    await expect(fetchSession()).rejects.toThrow('Failed to fetch')
  })

  it('createSession sends the Keycloak token once as Bearer and stores the CSRF token', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { csrfToken: 'csrf-2', expiresAt: 'x' }))

    await createSession('keycloak-access-token')

    const [, init] = fetchMock.mock.calls[0]
    expect(init.method).toBe('POST')
    expect(init.credentials).toBe('include')
    expect(init.headers.Authorization).toBe('Bearer keycloak-access-token')
    expect(getCsrfToken()).toBe('csrf-2')
  })

  it('deleteSession sends the CSRF token and clears local state even when the server fails', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(200, { csrfToken: 'csrf-3', expiresAt: 'x' }))
    await fetchSession()
    fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'))

    await deleteSession()

    const [, init] = fetchMock.mock.calls[1]
    expect(init.method).toBe('DELETE')
    expect(init.headers['X-CSRF-Token']).toBe('csrf-3')
    expect(getCsrfToken()).toBeUndefined()
  })

  it('recoverSession shares one in-flight attempt between concurrent callers', async () => {
    let resolveHandler: (value: boolean) => void = () => undefined
    const handler = vi.fn(
      () =>
        new Promise<boolean>((resolve) => {
          resolveHandler = resolve
        }),
    )
    setSessionRecoveryHandler(handler)

    const results = Promise.all([recoverSession(), recoverSession(), recoverSession()])
    resolveHandler(true)

    expect(await results).toEqual([true, true, true])
    expect(handler).toHaveBeenCalledTimes(1)

    // a later failure starts a fresh attempt
    handler.mockResolvedValueOnce(false)
    expect(await recoverSession()).toBe(false)
    expect(handler).toHaveBeenCalledTimes(2)
  })

  it('recoverSession is false without a handler and when the handler throws', async () => {
    expect(await recoverSession()).toBe(false)

    setSessionRecoveryHandler(() => Promise.reject(new Error('keycloak down')))
    expect(await recoverSession()).toBe(false)
  })
})
