import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
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

const Probe = () => {
  const auth = useAuthenticationContext()

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
    vi.mocked(keycloakClient.ensureSsoChecked).mockResolvedValue(true)
    vi.mocked(keycloakClient.getKeycloak).mockReturnValue({ authenticated: true, logout } as never)

    renderProvider()
    await waitFor(() => expect(screen.getByTestId('authenticated').textContent).toBe('true'))
    fireEvent.click(screen.getByRole('button', { name: 'log out' }))

    await waitFor(() =>
      expect(logout).toHaveBeenCalledWith({ redirectUri: `${window.location.origin}/` }),
    )
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
    expect(keycloakClient.ensureSsoChecked).not.toHaveBeenCalled()
    expect(screen.getByTestId('authenticated').textContent).toBe('false')
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
})
