import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const { mockInit } = vi.hoisted(() => ({ mockInit: vi.fn() }))

vi.mock('keycloak-js', () => ({
  default: vi.fn().mockImplementation(function () {
    return { init: mockInit }
  }),
}))

import { initKeycloak, isKeycloakReachable } from '@/core/auth/keycloakClient'

describe('initKeycloak', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    mockInit.mockReset()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('gives up on a silent check that Keycloak never answers', async () => {
    mockInit.mockReturnValue(new Promise(() => undefined))

    const result = initKeycloak({ checkSso: true })
    const assertion = expect(result).rejects.toThrow(/did not answer/)
    await vi.advanceTimersByTimeAsync(10_000)

    await assertion
  })

  it('returns the outcome of a silent check that answers in time', async () => {
    mockInit.mockResolvedValue(true)

    await expect(initKeycloak({ checkSso: true })).resolves.toBe(true)
  })

  it('does not wrap an initialisation without a silent check', async () => {
    mockInit.mockResolvedValue(false)

    await expect(initKeycloak({ checkSso: false })).resolves.toBe(false)
    expect(mockInit.mock.calls[0][0].onLoad).toBeUndefined()
  })
})

describe('isKeycloakReachable', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('is true when Keycloak answers with success', async () => {
    fetchMock.mockResolvedValue({ ok: true })

    await expect(isKeycloakReachable()).resolves.toBe(true)
  })

  it('is false when the error answer of an unavailable Keycloak is readable (same origin or CORS enabled)', async () => {
    fetchMock.mockResolvedValue({ ok: false, status: 503 })

    await expect(isKeycloakReachable()).resolves.toBe(false)
  })

  it('falls back to a network-level check when the browser may not read the answer', async () => {
    fetchMock
      .mockRejectedValueOnce(new TypeError('Failed to fetch'))
      .mockResolvedValueOnce({ type: 'opaque' })

    await expect(isKeycloakReachable()).resolves.toBe(true)
    expect(fetchMock.mock.calls[1][1].mode).toBe('no-cors')
  })

  it('is false when Keycloak cannot be reached at all', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    await expect(isKeycloakReachable()).resolves.toBe(false)
  })
})
