import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { doRequest } from '@/core/requests/request'
import { clearSessionState, setCsrfToken, setSessionRecoveryHandler } from '@/core/auth/session'

const jsonResponse = (status: number, body: unknown = {}) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json' },
  })

describe('doRequest', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
    clearSessionState()
    setCsrfToken('csrf-token')
    setSessionRecoveryHandler(undefined)
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('sends the cookie and never an Authorization header', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { ok: 1 }))

    const result = await doRequest('/v2/user-info', { method: 'GET', requiresAuth: true })

    expect(result.ok).toBe(true)
    const [, init] = fetchMock.mock.calls[0]
    expect(init.credentials).toBe('include')
    expect(init.headers.Authorization).toBeUndefined()
  })

  it('adds the CSRF token only on unsafe methods', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200))

    await doRequest('/v2/x', { method: 'GET', requiresAuth: true })
    await doRequest('/v2/x', { method: 'POST', requiresAuth: true, data: { a: 1 } })
    await doRequest('/v2/x', { method: 'DELETE', requiresAuth: true })

    expect(fetchMock.mock.calls[0][1].headers['X-CSRF-Token']).toBeUndefined()
    expect(fetchMock.mock.calls[1][1].headers['X-CSRF-Token']).toBe('csrf-token')
    expect(fetchMock.mock.calls[2][1].headers['X-CSRF-Token']).toBe('csrf-token')
  })

  it('on 401 recovers the session once and replays the request once', async () => {
    const handler = vi.fn().mockResolvedValue(true)
    setSessionRecoveryHandler(handler)
    fetchMock
      .mockResolvedValueOnce(jsonResponse(401))
      .mockResolvedValueOnce(jsonResponse(200, { done: true }))

    const result = await doRequest<{ done: boolean }>('/v2/x', {
      method: 'GET',
      requiresAuth: true,
    })

    expect(result.ok).toBe(true)
    expect(handler).toHaveBeenCalledTimes(1)
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('returns the second consecutive 401 without retrying again', async () => {
    const handler = vi.fn().mockResolvedValue(true)
    setSessionRecoveryHandler(handler)
    fetchMock.mockResolvedValue(jsonResponse(401))

    const result = await doRequest('/v2/x', { method: 'GET', requiresAuth: true })

    expect(result.ok).toBe(false)
    expect(result.status).toBe(401)
    expect(handler).toHaveBeenCalledTimes(1)
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('does not replay when recovery fails', async () => {
    setSessionRecoveryHandler(() => Promise.resolve(false))
    fetchMock.mockResolvedValue(jsonResponse(401))

    const result = await doRequest('/v2/x', { method: 'GET', requiresAuth: true })

    expect(result.status).toBe(401)
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('shares one recovery between concurrent 401s', async () => {
    const handler = vi.fn().mockResolvedValue(true)
    setSessionRecoveryHandler(handler)
    fetchMock.mockImplementation(() => {
      const first = fetchMock.mock.calls.length <= 3
      return Promise.resolve(first ? jsonResponse(401) : jsonResponse(200))
    })

    const results = await Promise.all([
      doRequest('/v2/a', { method: 'GET', requiresAuth: true }),
      doRequest('/v2/b', { method: 'GET', requiresAuth: true }),
      doRequest('/v2/c', { method: 'GET', requiresAuth: true }),
    ])

    expect(results.every((r) => r.ok)).toBe(true)
    expect(handler).toHaveBeenCalledTimes(1)
  })

  it('does not try to recover public requests that return 401', async () => {
    const handler = vi.fn().mockResolvedValue(true)
    setSessionRecoveryHandler(handler)
    fetchMock.mockResolvedValue(jsonResponse(401))

    const result = await doRequest('/v2/topics', { method: 'GET', requiresAuth: false })

    expect(result.status).toBe(401)
    expect(handler).not.toHaveBeenCalled()
  })
})
