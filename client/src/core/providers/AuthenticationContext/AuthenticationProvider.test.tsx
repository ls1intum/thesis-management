import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useState } from 'react'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import AuthenticationProvider from '@/core/providers/AuthenticationContext/AuthenticationProvider'
import { useAuthenticationContext } from '@/core/hooks/authentication'
import * as keycloakClient from '@/core/auth/keycloakClient'
import * as session from '@/core/auth/session'
import { recoverSession } from '@/core/auth/session'
import { showSimpleError } from '@/core/utils/notification'

vi.mock('@/core/auth/keycloakClient', () => ({
  initKeycloak: vi.fn(),
  ensureKeycloak: vi.fn(),
  ensureSsoChecked: vi.fn(),
  getKeycloak: vi.fn(),
  isKeycloakReachable: vi.fn(),
}))

vi.mock('@/core/auth/session', async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  fetchSession: vi.fn(),
  createSession: vi.fn(),
  deleteSession: vi.fn(),
}))

vi.mock('@/core/requests/request', () => ({
  doRequest: vi.fn(
    (
      url: string,
      _options: unknown,
      cb: (res: { ok: boolean; status: number; data: unknown }) => void,
    ) => {
      cb({
        ok: true,
        status: 200,
        data: url.includes('user-info')
          ? { userId: 'user-1', universityId: 'ab12cde', groups: ['student'] }
          : [],
      })
      return () => undefined
    },
  ),
}))

vi.mock('@/core/utils/notification', () => ({ showSimpleError: vi.fn() }))

const fakeJwt = (claims: Record<string, unknown>) =>
  `${btoa(JSON.stringify({ alg: 'none' }))}.${btoa(JSON.stringify(claims))}.signature`

const Probe = () => {
  const auth = useAuthenticationContext()
  const [credentialsResult, setCredentialsResult] = useState('')

  return (
    <div>
      <span data-testid='ready'>{String(auth.isReady)}</span>
      <span data-testid='authenticated'>{String(auth.isAuthenticated)}</span>
      <span data-testid='user'>{auth.user?.universityId ?? ''}</span>
      <button type='button' onClick={() => void auth.login('/dashboard')}>
        log in
      </button>
      <button type='button' onClick={() => auth.logout('/')}>
        log out
      </button>
      <button
        type='button'
        onClick={() =>
          void auth
            .listCredentials()
            .then(() => setCredentialsResult('listed'))
            .catch((error: Error) => setCredentialsResult(error.message))
        }
      >
        list credentials
      </button>
      <span data-testid='credentials'>{credentialsResult}</span>
    </div>
  )
}

const renderProvider = () =>
  render(
    <AuthenticationProvider>
      <Probe />
    </AuthenticationProvider>,
  )

describe('AuthenticationProvider startup', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.mocked(keycloakClient.isKeycloakReachable).mockResolvedValue(true)
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('uses an existing session without contacting Keycloak', async () => {
    vi.mocked(session.fetchSession).mockResolvedValue({ csrfToken: 'c', expiresAt: 'x' })

    renderProvider()

    await waitFor(() => expect(screen.getByTestId('user').textContent).toBe('ab12cde'))
    expect(screen.getByTestId('authenticated').textContent).toBe('true')
    expect(keycloakClient.initKeycloak).not.toHaveBeenCalled()
    expect(keycloakClient.getKeycloak).not.toHaveBeenCalled()
  })

  it('exchanges the Keycloak login for a session when there is none', async () => {
    vi.mocked(session.fetchSession).mockResolvedValue(undefined)
    vi.mocked(keycloakClient.initKeycloak).mockResolvedValue(true)
    vi.mocked(keycloakClient.getKeycloak).mockReturnValue({
      authenticated: true,
      token: 'kc-token',
    } as never)
    vi.mocked(session.createSession).mockResolvedValue({ csrfToken: 'c', expiresAt: 'x' })

    renderProvider()

    await waitFor(() => expect(screen.getByTestId('authenticated').textContent).toBe('true'))
    expect(keycloakClient.initKeycloak).toHaveBeenCalledWith({ checkSso: true })
    expect(session.createSession).toHaveBeenCalledWith('kc-token')
  })

  it('stays anonymous and reports the problem when Keycloak is unavailable and there is no session', async () => {
    vi.mocked(session.fetchSession).mockResolvedValue(undefined)
    vi.mocked(keycloakClient.initKeycloak).mockRejectedValue(new Error('Keycloak down'))

    renderProvider()

    await waitFor(() => expect(screen.getByTestId('ready').textContent).toBe('true'))
    expect(screen.getByTestId('authenticated').textContent).toBe('false')
    expect(showSimpleError).toHaveBeenCalled()
    expect(session.createSession).not.toHaveBeenCalled()
  })

  it('removes legacy tokens from localStorage', async () => {
    localStorage.setItem('authentication_tokens', JSON.stringify({ access_token: 'a' }))
    vi.mocked(session.fetchSession).mockResolvedValue({ csrfToken: 'c', expiresAt: 'x' })

    renderProvider()

    await waitFor(() => expect(screen.getByTestId('ready').textContent).toBe('true'))
    expect(localStorage.getItem('authentication_tokens')).toBeNull()
  })

  it('still loads when the session check cannot reach the server', async () => {
    vi.mocked(session.fetchSession).mockRejectedValue(new TypeError('Failed to fetch'))
    vi.mocked(keycloakClient.initKeycloak).mockResolvedValue(false)

    renderProvider()

    await waitFor(() => expect(screen.getByTestId('ready').textContent).toBe('true'))
    expect(screen.getByTestId('authenticated').textContent).toBe('false')
  })

  it('does not redirect to an unreachable Keycloak when logging in', async () => {
    vi.mocked(session.fetchSession).mockResolvedValue(undefined)
    vi.mocked(keycloakClient.initKeycloak).mockResolvedValue(false)
    vi.mocked(keycloakClient.isKeycloakReachable).mockResolvedValue(false)

    renderProvider()
    await waitFor(() => expect(screen.getByTestId('ready').textContent).toBe('true'))
    fireEvent.click(screen.getByRole('button', { name: 'log in' }))

    await waitFor(() =>
      expect(showSimpleError).toHaveBeenCalledWith(expect.stringMatching(/login service/i)),
    )
    expect(keycloakClient.ensureKeycloak).not.toHaveBeenCalled()
  })

  it('redirects to Keycloak when logging in and Keycloak is reachable', async () => {
    const login = vi.fn().mockResolvedValue(undefined)
    vi.mocked(session.fetchSession).mockResolvedValue(undefined)
    vi.mocked(keycloakClient.initKeycloak).mockResolvedValue(false)
    vi.mocked(keycloakClient.isKeycloakReachable).mockResolvedValue(true)
    vi.mocked(keycloakClient.ensureKeycloak).mockResolvedValue({ login } as never)

    renderProvider()
    await waitFor(() => expect(screen.getByTestId('ready').textContent).toBe('true'))
    fireEvent.click(screen.getByRole('button', { name: 'log in' }))

    await waitFor(() =>
      expect(login).toHaveBeenCalledWith({ redirectUri: `${window.location.origin}/dashboard` }),
    )
  })

  it('ends the session and then the Keycloak login, using the id token of a silent check', async () => {
    const logout = vi.fn().mockResolvedValue(undefined)
    vi.mocked(session.fetchSession).mockResolvedValue({ csrfToken: 'c', expiresAt: 'x' })
    vi.mocked(session.deleteSession).mockResolvedValue(undefined)
    vi.mocked(keycloakClient.isKeycloakReachable).mockResolvedValue(true)
    vi.mocked(keycloakClient.initKeycloak).mockResolvedValue(true)
    vi.mocked(keycloakClient.getKeycloak).mockReturnValue({ authenticated: true, logout } as never)

    renderProvider()
    await waitFor(() => expect(screen.getByTestId('authenticated').textContent).toBe('true'))
    fireEvent.click(screen.getByRole('button', { name: 'log out' }))

    await waitFor(() =>
      expect(logout).toHaveBeenCalledWith({ redirectUri: `${window.location.origin}/` }),
    )
    // a fresh check, not the cached result of this page load (a login made in another tab since would be missed)
    expect(keycloakClient.initKeycloak).toHaveBeenCalledWith({ checkSso: true })
    expect(keycloakClient.ensureSsoChecked).not.toHaveBeenCalled()
    expect(session.deleteSession).toHaveBeenCalled()
    expect(screen.getByTestId('authenticated').textContent).toBe('false')
  })

  it('does not contact Keycloak for the logout when it is unreachable', async () => {
    vi.mocked(session.fetchSession).mockResolvedValue({ csrfToken: 'c', expiresAt: 'x' })
    vi.mocked(session.deleteSession).mockResolvedValue(undefined)
    vi.mocked(keycloakClient.isKeycloakReachable).mockResolvedValue(false)

    renderProvider()
    await waitFor(() => expect(screen.getByTestId('authenticated').textContent).toBe('true'))
    fireEvent.click(screen.getByRole('button', { name: 'log out' }))

    await waitFor(() => expect(session.deleteSession).toHaveBeenCalled())
    expect(keycloakClient.initKeycloak).not.toHaveBeenCalled()
    expect(screen.getByTestId('authenticated').textContent).toBe('false')
  })

  it('does not bounce between the web app and Keycloak when the server refuses the login', async () => {
    const keycloakLogin = vi.fn().mockResolvedValue(undefined)
    vi.mocked(session.fetchSession).mockResolvedValue(undefined)
    vi.mocked(keycloakClient.initKeycloak).mockResolvedValue(true)
    vi.mocked(keycloakClient.getKeycloak).mockReturnValue({
      authenticated: true,
      token: 'kc-token',
    } as never)
    vi.mocked(session.createSession).mockRejectedValue(
      new session.SessionExchangeError(403, 'This account has been deleted'),
    )
    vi.mocked(keycloakClient.ensureKeycloak).mockResolvedValue({ login: keycloakLogin } as never)

    renderProvider()
    await waitFor(() => expect(screen.getByTestId('ready').textContent).toBe('true'))
    expect(showSimpleError).toHaveBeenCalledWith('This account has been deleted')

    fireEvent.click(screen.getByRole('button', { name: 'log in' }))

    await waitFor(() => expect(showSimpleError).toHaveBeenCalledTimes(2))
    expect(keycloakLogin).not.toHaveBeenCalled()
  })

  it('processes the Keycloak callback in the address when it returns to a page that already has a session', async () => {
    window.history.replaceState(null, '', '/settings#state=abc&code=def&session_state=ghi')
    vi.mocked(session.fetchSession).mockResolvedValue({ csrfToken: 'c', expiresAt: 'x' })
    vi.mocked(keycloakClient.ensureSsoChecked).mockResolvedValue(true)

    try {
      renderProvider()

      await waitFor(() => expect(keycloakClient.ensureSsoChecked).toHaveBeenCalled())
      expect(screen.getByTestId('authenticated').textContent).toBe('true')
    } finally {
      window.history.replaceState(null, '', '/')
    }
  })

  it('finishes loading without waiting for Keycloak when it is unreachable and there is no session', async () => {
    vi.mocked(session.fetchSession).mockResolvedValue(undefined)
    vi.mocked(keycloakClient.isKeycloakReachable).mockResolvedValue(false)

    renderProvider()

    await waitFor(() => expect(screen.getByTestId('ready').textContent).toBe('true'))
    expect(screen.getByTestId('authenticated').textContent).toBe('false')
    expect(keycloakClient.initKeycloak).not.toHaveBeenCalled()
    expect(showSimpleError).toHaveBeenCalled()
  })

  it('does not let the reachability probe stand in the way of the redirect back from the Keycloak login', async () => {
    window.location.hash = '#state=abc&code=def'
    vi.mocked(session.fetchSession).mockResolvedValue(undefined)
    vi.mocked(keycloakClient.isKeycloakReachable).mockResolvedValue(false)
    vi.mocked(keycloakClient.initKeycloak).mockResolvedValue(true)
    vi.mocked(keycloakClient.getKeycloak).mockReturnValue({
      authenticated: true,
      token: 'kc-token',
    } as never)
    vi.mocked(session.createSession).mockResolvedValue({ csrfToken: 'c', expiresAt: 'x' })

    renderProvider()

    await waitFor(() => expect(screen.getByTestId('authenticated').textContent).toBe('true'))
    expect(session.createSession).toHaveBeenCalledWith('kc-token')
    window.location.hash = ''
  })

  it('adopts a session another tab has just created instead of logging in again', async () => {
    vi.mocked(session.fetchSession).mockResolvedValueOnce({ csrfToken: 'c', expiresAt: 'x' })
    renderProvider()
    await waitFor(() => expect(screen.getByTestId('authenticated').textContent).toBe('true'))

    vi.mocked(session.fetchSession).mockResolvedValue({ csrfToken: 'other-tab', expiresAt: 'x' })

    await expect(recoverSession()).resolves.toBe(true)
    expect(keycloakClient.initKeycloak).not.toHaveBeenCalled()
    expect(session.createSession).not.toHaveBeenCalled()
  })

  it('stays logged in and says so when the server could not end the session', async () => {
    const logout = vi.fn()
    vi.mocked(session.fetchSession).mockResolvedValue({ csrfToken: 'c', expiresAt: 'x' })
    vi.mocked(session.deleteSession).mockRejectedValue(
      new Error('The server did not end the session (500)'),
    )
    vi.mocked(keycloakClient.isKeycloakReachable).mockResolvedValue(true)
    vi.mocked(keycloakClient.getKeycloak).mockReturnValue({ authenticated: true, logout } as never)

    renderProvider()
    await waitFor(() => expect(screen.getByTestId('authenticated').textContent).toBe('true'))
    fireEvent.click(screen.getByRole('button', { name: 'log out' }))

    await waitFor(() =>
      expect(showSimpleError).toHaveBeenCalledWith(expect.stringMatching(/logging out failed/i)),
    )
    expect(screen.getByTestId('authenticated').textContent).toBe('true')
    expect(logout).not.toHaveBeenCalled()
  })

  it('tells the user only once in a while that login is unavailable, even if login is requested repeatedly', async () => {
    vi.mocked(session.fetchSession).mockResolvedValue(undefined)
    vi.mocked(keycloakClient.initKeycloak).mockResolvedValue(false)
    vi.mocked(keycloakClient.isKeycloakReachable).mockResolvedValue(false)

    renderProvider()
    await waitFor(() => expect(screen.getByTestId('ready').textContent).toBe('true'))
    vi.mocked(showSimpleError).mockClear()

    const button = screen.getByRole('button', { name: 'log in' })
    for (let i = 0; i < 5; i++) {
      fireEvent.click(button)
    }

    await waitFor(() =>
      expect(showSimpleError).toHaveBeenCalledWith(expect.stringMatching(/login service/i)),
    )
    await new Promise((resolve) => setTimeout(resolve, 50))
    expect(showSimpleError).toHaveBeenCalledTimes(1)
  })

  it('refuses Keycloak features when the browser is signed in to Keycloak as another user than the application session', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response('[]', { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
    vi.mocked(session.fetchSession).mockResolvedValue({ csrfToken: 'c', expiresAt: 'x' })
    vi.mocked(keycloakClient.ensureSsoChecked).mockResolvedValue(true)
    vi.mocked(keycloakClient.getKeycloak).mockReturnValue({
      authenticated: true,
      token: fakeJwt({ preferred_username: 'bob' }),
      updateToken: vi.fn().mockResolvedValue(true),
    } as never)

    renderProvider()
    await waitFor(() => expect(screen.getByTestId('user').textContent).toBe('ab12cde'))
    fireEvent.click(screen.getByRole('button', { name: 'list credentials' }))

    await waitFor(() =>
      expect(screen.getByTestId('credentials').textContent).toMatch(/another account/i),
    )
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/account/credentials'))).toBe(
      false,
    )
    vi.unstubAllGlobals()
  })

  it('allows Keycloak features when the Keycloak login is the application session user', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response('[]', { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
    vi.mocked(session.fetchSession).mockResolvedValue({ csrfToken: 'c', expiresAt: 'x' })
    vi.mocked(keycloakClient.ensureSsoChecked).mockResolvedValue(true)
    vi.mocked(keycloakClient.getKeycloak).mockReturnValue({
      authenticated: true,
      token: fakeJwt({ preferred_username: 'ab12cde' }),
      updateToken: vi.fn().mockResolvedValue(true),
    } as never)

    renderProvider()
    await waitFor(() => expect(screen.getByTestId('user').textContent).toBe('ab12cde'))
    fireEvent.click(screen.getByRole('button', { name: 'list credentials' }))

    await waitFor(() => expect(screen.getByTestId('credentials').textContent).toBe('listed'))
    vi.unstubAllGlobals()
  })
})
