import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { useAuthenticationContext } from '@/core/hooks/authentication'
import type AuthenticationProviderComponent from './AuthenticationProvider'
import type * as SessionApi from '@/core/auth/session'

vi.mock('@/core/auth/keycloakClient', () => ({
  initKeycloak: vi.fn(),
  ensureKeycloak: vi.fn(),
  ensureSsoChecked: vi.fn(),
  getKeycloak: vi.fn(),
  isKeycloakReachable: vi.fn(),
}))
vi.mock('@/core/utils/notification', () => ({ showSimpleError: vi.fn() }))

const listeners: ((event: { data: unknown }) => void)[] = []
const posted: unknown[] = []
const receive = (data: unknown) => act(() => listeners.forEach((listener) => listener({ data })))
const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { 'content-type': 'application/json' } })

let AuthenticationProvider: typeof AuthenticationProviderComponent
let session: typeof SessionApi
const fetchMock = vi.fn()

const Probe = () => {
  const auth = useAuthenticationContext()
  return (
    <>
      <span data-testid='authenticated'>{String(auth.isAuthenticated)}</span>
      <span data-testid='ready'>{String(auth.isReady)}</span>
      <span data-testid='user'>{auth.user?.firstName}</span>
      <button type='button' onClick={() => void auth.login('/dashboard')}>
        Log in
      </button>
    </>
  )
}

describe('AuthenticationProvider across browser tabs', () => {
  beforeAll(async () => {
    vi.stubGlobal(
      'BroadcastChannel',
      class {
        addEventListener(_type: string, listener: (event: { data: unknown }) => void) {
          listeners.push(listener)
        }
        postMessage(message: unknown) {
          posted.push(message)
        }
      },
    )
    session = await import('@/core/auth/session')
    AuthenticationProvider = (await import('./AuthenticationProvider')).default
  })

  beforeEach(() => {
    session.clearSessionState()
    posted.length = 0
    vi.clearAllMocks()
    fetchMock.mockImplementation(async (url: string) => {
      if (url.includes('/auth/session')) {
        return json({ csrfToken: 'alice-csrf', expiresAt: 'later', userId: 'alice' })
      }
      if (url.includes('/user-info')) {
        return json({
          userId: 'alice',
          universityId: 'ab12cde',
          firstName: 'Alice',
          groups: ['student'],
        })
      }
      return json([])
    })
    vi.stubGlobal('fetch', fetchMock)
  })

  afterAll(() => vi.unstubAllGlobals())
  afterEach(() => vi.restoreAllMocks())

  const renderProvider = () =>
    render(
      <AuthenticationProvider>
        <Probe />
      </AuthenticationProvider>,
    )

  const signIn = async () => {
    renderProvider()
    await waitFor(() => expect(screen.getByTestId('user')).toHaveTextContent('Alice'))
  }

  it('clears private user state immediately when another tab completes logout', async () => {
    await signIn()

    receive({ type: 'logout' })
    receive({ type: 'logout-completed', userId: 'alice' })

    await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('false'))
    expect(screen.getByTestId('user')).toBeEmptyDOMElement()
    expect(session.getCsrfToken()).toBeUndefined()
    expect(await session.recoverSession()).toBe(false)
  })

  it('keeps the user signed in when another tab starts logout but it fails', async () => {
    await signIn()

    receive({ type: 'logout' })
    expect(screen.getByTestId('user')).toHaveTextContent('Alice')
    receive({ type: 'logout-failed' })

    expect(screen.getByTestId('authenticated')).toHaveTextContent('true')
    expect(screen.getByTestId('user')).toHaveTextContent('Alice')
    expect(session.isRecoveryBlocked()).toBe(false)
  })

  it('does not restore the user from a session check that finishes after logout', async () => {
    await signIn()
    let finishCheck: (response: Response) => void = () => undefined
    fetchMock.mockReturnValueOnce(
      new Promise<Response>((resolve) => {
        finishCheck = resolve
      }),
    )
    const recovery = session.recoverSession()

    receive({ type: 'logout' })
    receive({ type: 'logout-completed', userId: 'alice' })
    vi.spyOn(Date, 'now').mockReturnValue(Date.now() + 31_000)
    await act(async () => {
      finishCheck(json({ csrfToken: 'alice-csrf', expiresAt: 'later', userId: 'alice' }))
      await recovery
    })

    expect(screen.getByTestId('authenticated')).toHaveTextContent('false')
    expect(screen.getByTestId('user')).toBeEmptyDOMElement()
  })

  it('does not announce or adopt a late silent exchange after logout, even after the temporary block expires', async () => {
    const { initKeycloak, getKeycloak, isKeycloakReachable } =
      await import('@/core/auth/keycloakClient')
    vi.mocked(isKeycloakReachable).mockResolvedValue(true)
    vi.mocked(initKeycloak).mockResolvedValue(true)
    vi.mocked(getKeycloak).mockReturnValue({ authenticated: true, token: 'kc-token' } as never)
    let finishExchange: (response: Response) => void = () => undefined
    fetchMock.mockImplementation(async (_url: string, options: RequestInit) => {
      if (options.method === 'POST') {
        return new Promise<Response>((resolve) => {
          finishExchange = resolve
        })
      }
      return new Response(null, { status: options.method === 'DELETE' ? 204 : 401 })
    })
    renderProvider()
    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({ method: 'POST' }),
      ),
    )

    receive({ type: 'logout' })
    receive({ type: 'logout-completed', userId: 'alice' })
    vi.spyOn(Date, 'now').mockReturnValue(Date.now() + 31_000)
    await act(async () => {
      finishExchange(json({ csrfToken: 'alice-csrf', expiresAt: 'later', userId: 'alice' }))
    })
    await waitFor(() => expect(screen.getByTestId('ready')).toHaveTextContent('true'))

    expect(screen.getByTestId('authenticated')).toHaveTextContent('false')
    expect(posted).toEqual([])
    expect(fetchMock).toHaveBeenCalledWith(
      expect.any(String),
      expect.objectContaining({ method: 'DELETE' }),
    )
  })

  it('cancels a login redirect when logout completes while Keycloak is being initialized', async () => {
    const { initKeycloak, isKeycloakReachable, ensureKeycloak } =
      await import('@/core/auth/keycloakClient')
    fetchMock.mockResolvedValue(new Response(null, { status: 401 }))
    vi.mocked(isKeycloakReachable).mockResolvedValue(true)
    vi.mocked(initKeycloak).mockResolvedValue(false)
    renderProvider()
    await waitFor(() => expect(screen.getByTestId('ready')).toHaveTextContent('true'))
    let finishInit: (client: Awaited<ReturnType<typeof ensureKeycloak>>) => void = () => undefined
    vi.mocked(ensureKeycloak).mockReturnValueOnce(
      new Promise((resolve) => {
        finishInit = resolve
      }),
    )
    fireEvent.click(screen.getByRole('button', { name: 'Log in' }))
    await waitFor(() => expect(ensureKeycloak).toHaveBeenCalled())

    receive({ type: 'logout' })
    receive({ type: 'logout-completed', userId: 'alice' })
    const login = vi.fn().mockResolvedValue(undefined)
    await act(async () => {
      finishInit({ login } as never)
    })

    expect(login).not.toHaveBeenCalled()
  })

  it('keeps a valid session check when the other tab has only started a logout that later fails', async () => {
    await signIn()
    let finishCheck: (response: Response) => void = () => undefined
    fetchMock.mockReturnValueOnce(
      new Promise<Response>((resolve) => {
        finishCheck = resolve
      }),
    )
    const recovery = session.recoverSession()

    receive({ type: 'logout' })
    await act(async () => {
      finishCheck(json({ csrfToken: 'alice-csrf', expiresAt: 'later', userId: 'alice' }))
      await recovery
    })
    receive({ type: 'logout-failed' })

    expect(screen.getByTestId('authenticated')).toHaveTextContent('true')
    expect(screen.getByTestId('user')).toHaveTextContent('Alice')
    expect(session.getCsrfToken()).toBe('alice-csrf')
    const { doRequest } = await import('@/core/requests/request')
    await act(async () => {
      await doRequest('/v2/test-mutation', { method: 'PUT', requiresAuth: true })
    })
    expect(fetchMock).toHaveBeenLastCalledWith(
      expect.stringContaining('/v2/test-mutation'),
      expect.objectContaining({
        headers: expect.objectContaining({ 'X-CSRF-Token': 'alice-csrf' }),
      }),
    )
  })

  it("does not let a fallback login undo another tab's logout", async () => {
    await signIn()
    const { isKeycloakReachable, ensureKeycloak } = await import('@/core/auth/keycloakClient')
    vi.mocked(isKeycloakReachable).mockResolvedValue(true)
    const login = vi.fn().mockResolvedValue(undefined)
    vi.mocked(ensureKeycloak).mockResolvedValue({ login } as never)

    receive({ type: 'logout' })
    receive({ type: 'logout-completed', userId: 'alice' })
    await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('false'))
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Log in' }))
    })

    expect(login).not.toHaveBeenCalled()
  })
})
