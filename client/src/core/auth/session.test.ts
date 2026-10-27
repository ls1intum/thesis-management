import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  clearSessionState,
  createSession,
  deleteSession,
  fetchSession,
  getCsrfToken,
  recoverSession,
  refreshCsrfToken,
  setCsrfToken,
  setIdentityChangeHandler,
  setSessionRecoveryHandler,
  setSessionUserId,
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
    setIdentityChangeHandler(undefined)
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

  it('deleteSession sends the CSRF token and clears local state when the server ends the session', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(200, { csrfToken: 'csrf-3', expiresAt: 'x' }))
    await fetchSession()
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }))

    await deleteSession()

    const [, init] = fetchMock.mock.calls[1]
    expect(init.method).toBe('DELETE')
    expect(init.headers['X-CSRF-Token']).toBe('csrf-3')
    expect(getCsrfToken()).toBeUndefined()
  })

  it('deleteSession treats an already ended session (401) as done', async () => {
    setCsrfToken('stale')
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 401 }))

    await expect(deleteSession()).resolves.toBeUndefined()
    expect(getCsrfToken()).toBeUndefined()
  })

  it('deleteSession reports a failure and keeps the CSRF token so that the logout can be retried', async () => {
    setCsrfToken('csrf-4')
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 500 }))

    await expect(deleteSession()).rejects.toThrow(/500/)
    expect(getCsrfToken()).toBe('csrf-4')

    fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'))
    await expect(deleteSession()).rejects.toThrow('Failed to fetch')
    expect(getCsrfToken()).toBe('csrf-4')
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

  it('deleteSession resyncs a stale CSRF token and retries the logout once', async () => {
    setCsrfToken('stale')
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 403 }))
      .mockResolvedValueOnce(jsonResponse(200, { csrfToken: 'fresh', expiresAt: 'x' }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))

    await deleteSession()

    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(fetchMock.mock.calls[0][1].headers['X-CSRF-Token']).toBe('stale')
    expect(fetchMock.mock.calls[2][1].method).toBe('DELETE')
    expect(fetchMock.mock.calls[2][1].headers['X-CSRF-Token']).toBe('fresh')
    expect(getCsrfToken()).toBeUndefined()
  })

  it('refreshCsrfToken shares one request between concurrent callers', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { csrfToken: 'shared', expiresAt: 'x' }))

    const tokens = await Promise.all([refreshCsrfToken(), refreshCsrfToken(), refreshCsrfToken()])

    expect(tokens).toEqual(['shared', 'shared', 'shared'])
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('deleteSession is done when the resync after a 403 shows that the session is already gone', async () => {
    setCsrfToken('stale')
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 403 }))
      .mockResolvedValueOnce(new Response(null, { status: 401 }))

    await expect(deleteSession()).resolves.toBeUndefined()

    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(getCsrfToken()).toBeUndefined()
  })

  it('deleteSession reports a network failure of the resync instead of treating it as ended', async () => {
    setCsrfToken('stale')
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 403 }))
      .mockRejectedValueOnce(new TypeError('Failed to fetch'))

    await expect(deleteSession()).rejects.toThrow('Failed to fetch')
    expect(getCsrfToken()).toBe('stale')
  })

  it('deleteSession never ends the session of another user that replaced the one it wanted to end', async () => {
    const identityChanged = vi.fn()
    setIdentityChangeHandler(identityChanged)
    setSessionUserId('alice')
    setCsrfToken('alice-token')
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 403 }))
      .mockResolvedValueOnce(
        jsonResponse(200, { csrfToken: 'bob-token', expiresAt: 'x', userId: 'bob' }),
      )

    await expect(deleteSession()).rejects.toThrow(/another user/i)

    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(identityChanged).toHaveBeenCalledTimes(1)
  })
})
