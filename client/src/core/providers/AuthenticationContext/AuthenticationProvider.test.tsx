import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import AuthenticationProvider from '@/core/providers/AuthenticationContext/AuthenticationProvider'
import { useAuthenticationContext } from '@/core/hooks/authentication'
import * as keycloakClient from '@/core/auth/keycloakClient'
import * as session from '@/core/auth/session'
import { showSimpleError } from '@/core/utils/notification'

vi.mock('@/core/auth/keycloakClient', () => ({
  initKeycloak: vi.fn(),
  ensureKeycloak: vi.fn(),
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
    vi.mocked(keycloakClient.isKeycloakReachable).mockResolvedValue(false)
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
})
