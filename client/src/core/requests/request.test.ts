import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { doRequest } from '@/core/requests/request'
import {
  clearSessionState,
  forgetSession,
  setCsrfToken,
  setIdentityChangeHandler,
  setSessionRecoveryHandler,
  setSessionUserId,
} from '@/core/auth/session'

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
    setIdentityChangeHandler(undefined)
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

  it('treats an empty 204 response as a success without a body', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    const result = await doRequest('/v2/auth/sessions/abc', {
      method: 'DELETE',
      requiresAuth: true,
    })

    expect(result.ok).toBe(true)
    expect(result.status).toBe(204)
    expect(result.data).toBeUndefined()
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

  it('resyncs a stale CSRF token on 403 (session replaced in another tab) and replays once', async () => {
    setCsrfToken('old-token')
    fetchMock
      .mockResolvedValueOnce(jsonResponse(403))
      .mockResolvedValueOnce(jsonResponse(200, { csrfToken: 'new-token', expiresAt: 'x' }))
      .mockResolvedValueOnce(jsonResponse(200, { done: true }))

    const result = await doRequest('/v2/x', { method: 'POST', requiresAuth: true, data: { a: 1 } })

    expect(result.ok).toBe(true)
    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(fetchMock.mock.calls[0][1].headers['X-CSRF-Token']).toBe('old-token')
    expect(fetchMock.mock.calls[2][1].headers['X-CSRF-Token']).toBe('new-token')
  })

  it('returns a 403 unchanged when the CSRF token is still current (a real permission error)', async () => {
    setCsrfToken('same-token')
    fetchMock
      .mockResolvedValueOnce(jsonResponse(403))
      .mockResolvedValueOnce(jsonResponse(200, { csrfToken: 'same-token', expiresAt: 'x' }))

    const result = await doRequest('/v2/x', { method: 'POST', requiresAuth: true, data: { a: 1 } })

    expect(result.status).toBe(403)
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('does not resync on 403 for requests that sent no CSRF token', async () => {
    fetchMock.mockResolvedValue(jsonResponse(403))

    const result = await doRequest('/v2/x', { method: 'GET', requiresAuth: true })

    expect(result.status).toBe(403)
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('does not try to recover public requests that return 401', async () => {
    const handler = vi.fn().mockResolvedValue(true)
    setSessionRecoveryHandler(handler)
    fetchMock.mockResolvedValue(jsonResponse(401))

    const result = await doRequest('/v2/topics', { method: 'GET', requiresAuth: false })

    expect(result.status).toBe(401)
    expect(handler).not.toHaveBeenCalled()
  })

  it('parses a JSON body', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { id: 1 }))

    const response = await doRequest<{ id: number }>('/v2/things', {
      method: 'GET',
      requiresAuth: true,
    })

    expect(response).toEqual({ ok: true, status: 200, data: { id: 1 } })
  })

  it('returns the message of the server for an error response', async () => {
    fetchMock.mockResolvedValue(jsonResponse(400, { message: 'Still in use' }))

    const response = await doRequest('/v2/things/1', { method: 'DELETE', requiresAuth: true })

    expect(response.ok).toBe(false)
    expect(response.status).toBe(400)
    expect(response.ok ? undefined : response.error?.message).toBe('Still in use')
  })

  it('sends the JSON body with its content type', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, {}))

    await doRequest('/v2/things', { method: 'POST', requiresAuth: true, data: { name: 'x' } })

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toContain('/api/v2/things')
    expect(init.headers).toMatchObject({ 'Content-Type': 'application/json' })
    expect(init.body).toBe(JSON.stringify({ name: 'x' }))
  })

  it('does not replay a request after recovery when the session now belongs to another user', async () => {
    const identityChanged = vi.fn()
    setIdentityChangeHandler(identityChanged)
    setSessionUserId('alice')
    setSessionRecoveryHandler(() => {
      setSessionUserId('bob')
      return Promise.resolve(true)
    })
    fetchMock.mockResolvedValue(jsonResponse(401))

    const result = await doRequest('/v2/account', { method: 'DELETE', requiresAuth: true })

    expect(result.status).toBe(401)
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(identityChanged).toHaveBeenCalledTimes(1)
  })

  it('does not replay a request after a CSRF resync when the session now belongs to another user', async () => {
    const identityChanged = vi.fn()
    setIdentityChangeHandler(identityChanged)
    setSessionUserId('alice')
    setCsrfToken('alice-token')
    fetchMock
      .mockResolvedValueOnce(jsonResponse(403))
      .mockResolvedValueOnce(
        jsonResponse(200, { csrfToken: 'bob-token', expiresAt: 'x', userId: 'bob' }),
      )

    const result = await doRequest('/v2/user-info', {
      method: 'PUT',
      requiresAuth: true,
      data: { a: 1 },
    })

    expect(result.status).toBe(403)
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(identityChanged).toHaveBeenCalledTimes(1)
  })

  it('still replays when the session was replaced for the same user', async () => {
    const identityChanged = vi.fn()
    setIdentityChangeHandler(identityChanged)
    setSessionUserId('alice')
    setCsrfToken('old')
    fetchMock
      .mockResolvedValueOnce(jsonResponse(403))
      .mockResolvedValueOnce(
        jsonResponse(200, { csrfToken: 'new', expiresAt: 'x', userId: 'alice' }),
      )
      .mockResolvedValueOnce(jsonResponse(200, { done: true }))

    const result = await doRequest('/v2/x', { method: 'POST', requiresAuth: true, data: { a: 1 } })

    expect(result.ok).toBe(true)
    expect(identityChanged).not.toHaveBeenCalled()
  })

  it('does not replay a request that started while the tab did not know its session, under the session of another user', async () => {
    // the re-login clears what is known about the session; Alice's DELETE is sent and rejected during that time
    const identityChanged = vi.fn()
    setIdentityChangeHandler(identityChanged)
    setSessionUserId('alice')
    setSessionRecoveryHandler(() => {
      forgetSession()
      setSessionUserId('bob')
      return Promise.resolve(true)
    })
    fetchMock.mockResolvedValue(jsonResponse(401))

    const result = await doRequest('/v2/account', { method: 'DELETE', requiresAuth: true })

    expect(result.status).toBe(401)
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(identityChanged).toHaveBeenCalledTimes(1)
  })

  it('does not replay when the re-login leaves the session user unknown', async () => {
    setIdentityChangeHandler(vi.fn())
    setSessionUserId('alice')
    setSessionRecoveryHandler(() => {
      forgetSession()
      return Promise.resolve(true)
    })
    fetchMock.mockResolvedValue(jsonResponse(401))

    await doRequest('/v2/x', { method: 'POST', requiresAuth: true, data: { a: 1 } })

    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('sends nothing once the tab has found out that it shows another user than the session', async () => {
    setIdentityChangeHandler(vi.fn())
    setSessionUserId('alice')
    setSessionRecoveryHandler(() => {
      setSessionUserId('bob')
      return Promise.resolve(true)
    })
    fetchMock.mockResolvedValue(jsonResponse(401))
    await doRequest('/v2/first', { method: 'GET', requiresAuth: true })
    fetchMock.mockClear()

    const result = await doRequest('/v2/second', { method: 'POST', requiresAuth: true, data: {} })

    expect(result.ok).toBe(false)
    expect(fetchMock).not.toHaveBeenCalled()
  })
})
