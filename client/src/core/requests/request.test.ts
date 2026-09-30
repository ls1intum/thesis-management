import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'

vi.mock('@/core/providers/AuthenticationContext/AuthenticationProvider', () => ({
  keycloak: {
    isTokenExpired: () => false,
    updateToken: () => Promise.resolve(true),
    token: 'test-token',
  },
}))

import { doRequest } from '@/core/requests/request'

describe('doRequest', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  test('parses a JSON body', async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ id: 1 }), { status: 200 }))

    const response = await doRequest<{ id: number }>('/v2/things', {
      method: 'GET',
      requiresAuth: true,
    })

    expect(response).toEqual({ ok: true, status: 200, data: { id: 1 } })
  })

  test('treats 204 No Content as success without parsing a body', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    const response = await doRequest<undefined>('/v2/things/1', {
      method: 'DELETE',
      requiresAuth: true,
    })

    expect(response).toEqual({ ok: true, status: 204, data: undefined })
  })

  test('returns the message of the server for an error response', async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ message: 'Still in use' }), {
        status: 400,
        headers: { 'content-type': 'application/json' },
      }),
    )

    const response = await doRequest('/v2/things/1', { method: 'DELETE', requiresAuth: true })

    expect(response.ok).toBe(false)
    expect(response.status).toBe(400)
    expect(response.ok ? undefined : response.error?.message).toBe('Still in use')
  })

  test('sends the bearer token and the JSON body', async () => {
    fetchMock.mockResolvedValue(new Response('{}', { status: 200 }))

    await doRequest('/v2/things', { method: 'POST', requiresAuth: true, data: { name: 'x' } })

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toContain('/api/v2/things')
    expect(init.headers).toMatchObject({
      Authorization: 'Bearer test-token',
      'Content-Type': 'application/json',
    })
    expect(init.body).toBe(JSON.stringify({ name: 'x' }))
  })
})
