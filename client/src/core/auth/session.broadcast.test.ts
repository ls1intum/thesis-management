import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type * as SessionApi from '@/core/auth/session'

type SessionModule = typeof SessionApi
type Listener = (event: { data: unknown }) => void

class FakeBroadcastChannel {
  static instances: FakeBroadcastChannel[] = []

  readonly posted: unknown[] = []
  private readonly listeners: Listener[] = []

  constructor(readonly name: string) {
    FakeBroadcastChannel.instances.push(this)
  }

  postMessage(message: unknown) {
    this.posted.push(message)
  }

  addEventListener(_type: string, listener: Listener) {
    this.listeners.push(listener)
  }

  /** What another tab sends to this one. */
  receive(data: unknown) {
    this.listeners.forEach((listener) => listener({ data }))
  }
}

const jsonResponse = (status: number, body?: unknown) =>
  new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json' },
  })

describe('session, between the tabs of one browser', () => {
  const fetchMock = vi.fn()
  let session: SessionModule
  let channel: FakeBroadcastChannel

  beforeEach(async () => {
    vi.resetModules()
    FakeBroadcastChannel.instances = []
    vi.stubGlobal('BroadcastChannel', FakeBroadcastChannel)
    vi.stubGlobal('fetch', fetchMock)

    session = await import('@/core/auth/session')
    channel = FakeBroadcastChannel.instances[0]
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('refuses silent re-logins after another tab began a logout, and allows them again once it failed', async () => {
    const handler = vi.fn().mockResolvedValue(true)
    session.setSessionRecoveryHandler(handler)

    channel.receive({ type: 'logout' })
    expect(session.isRecoveryBlocked()).toBe(true)
    expect(await session.recoverSession()).toBe(false)
    expect(handler).not.toHaveBeenCalled()

    channel.receive({ type: 'logout-failed' })
    expect(session.isRecoveryBlocked()).toBe(false)
    expect(await session.recoverSession()).toBe(true)
  })

  it('lets the other tabs know when a logout begins and when it failed', async () => {
    session.setCsrfToken('csrf')
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 500 }))

    await expect(session.deleteSession()).rejects.toThrow(/500/)

    expect(channel.posted).toEqual([{ type: 'logout' }, { type: 'logout-failed' }])
  })

  it('tells the other tabs who logged in', async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse(200, { csrfToken: 'a', expiresAt: 'x', userId: 'alice' }),
    )

    await session.createSession('kc-token')

    expect(channel.posted).toEqual([{ type: 'login', userId: 'alice' }])
  })

  it('starts a tab over when another tab logged in as somebody else, but not for the same user', async () => {
    const identityChanged = vi.fn()
    session.setIdentityChangeHandler(identityChanged)
    session.setSessionUserId('alice')

    channel.receive({ type: 'login', userId: 'alice' })
    expect(identityChanged).not.toHaveBeenCalled()

    channel.receive({ type: 'login', userId: 'bob' })
    expect(identityChanged).toHaveBeenCalledTimes(1)
    expect(session.isIdentityChangePending()).toBe(true)
  })

  it('leaves a tab alone that shows nobody yet when another tab logs in', () => {
    const identityChanged = vi.fn()
    session.setIdentityChangeHandler(identityChanged)

    channel.receive({ type: 'login', userId: 'bob' })

    expect(identityChanged).not.toHaveBeenCalled()
  })

  it('abandonSession ends the session it was given the token of and forgets it', async () => {
    session.setCsrfToken('csrf-late')
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }))

    await session.abandonSession()

    const [, init] = fetchMock.mock.calls[0]
    expect(init.method).toBe('DELETE')
    expect(init.headers['X-CSRF-Token']).toBe('csrf-late')
    expect(session.getCsrfToken()).toBeUndefined()
  })

  it('abandonSession never throws, not even when the server cannot be reached', async () => {
    session.setCsrfToken('csrf-late')
    fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'))

    await expect(session.abandonSession()).resolves.toBeUndefined()
    expect(session.getCsrfToken()).toBeUndefined()
  })
})
