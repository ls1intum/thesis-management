import { beforeEach, describe, expect, test, vi } from 'vitest'
import { renderWithProviders, screen, userEvent, waitFor } from '@/../test/render'
import ActiveSessions from '@/core/admin/pages/SettingsPage/components/ActiveSessions/ActiveSessions'
import { doRequest } from '@/core/requests/request'

vi.mock('@/core/requests/request', () => ({ doRequest: vi.fn() }))
vi.mock('@/core/utils/notification', () => ({
  showSimpleError: vi.fn(),
  showSimpleSuccess: vi.fn(),
}))

const sessionsResponse = (sessions: unknown[]) => ({ ok: true, status: 200, data: sessions })

const current = {
  id: 'current-id',
  createdAt: '2026-03-02T09:00:00Z',
  lastUsedAt: '2026-03-02T10:00:00Z',
  ip: '203.0.113.7',
  device: 'Chrome on macOS',
  current: true,
}
const other = {
  id: 'other-id',
  createdAt: '2026-03-01T09:00:00Z',
  lastUsedAt: '2026-03-01T10:00:00Z',
  ip: '198.51.100.4',
  device: 'Safari on iOS',
  current: false,
}

describe('ActiveSessions', () => {
  beforeEach(() => {
    vi.mocked(doRequest).mockReset()
  })

  test('lists the sessions and marks the current one without an end button', async () => {
    vi.mocked(doRequest).mockResolvedValue(sessionsResponse([current, other]) as never)

    renderWithProviders(<ActiveSessions />)

    expect(await screen.findByText('Chrome on macOS')).toBeInTheDocument()
    expect(screen.getByText('Safari on iOS')).toBeInTheDocument()
    expect(screen.getByText('This device')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: /^End session/ })).toHaveLength(1)
  })

  test('ends another session and reloads the list', async () => {
    const user = userEvent.setup()
    vi.mocked(doRequest)
      .mockResolvedValueOnce(sessionsResponse([current, other]) as never)
      .mockResolvedValueOnce({ ok: true, status: 204, data: undefined } as never)
      .mockResolvedValueOnce(sessionsResponse([current]) as never)

    renderWithProviders(<ActiveSessions />)
    await user.click(await screen.findByRole('button', { name: /^End session/ }))

    await waitFor(() => expect(screen.queryByText('Safari on iOS')).not.toBeInTheDocument())
    expect(doRequest).toHaveBeenCalledWith(
      '/v2/auth/sessions/other-id',
      expect.objectContaining({ method: 'DELETE', requiresAuth: true }),
    )
  })

  test('offers to end all other sessions only when there are other sessions', async () => {
    const user = userEvent.setup()
    vi.mocked(doRequest)
      .mockResolvedValueOnce(sessionsResponse([current, other]) as never)
      .mockResolvedValueOnce({ ok: true, status: 204, data: undefined } as never)
      .mockResolvedValueOnce(sessionsResponse([current]) as never)

    renderWithProviders(<ActiveSessions />)
    await user.click(await screen.findByRole('button', { name: 'End all other sessions' }))

    await waitFor(() =>
      expect(
        screen.queryByRole('button', { name: 'End all other sessions' }),
      ).not.toBeInTheDocument(),
    )
    expect(doRequest).toHaveBeenCalledWith(
      '/v2/auth/sessions',
      expect.objectContaining({ method: 'DELETE', requiresAuth: true }),
    )
  })

  test('tolerates missing optional fields', async () => {
    vi.mocked(doRequest).mockResolvedValue(sessionsResponse([{ id: 'x' }]) as never)

    renderWithProviders(<ActiveSessions />)

    expect(await screen.findByText('Unknown device')).toBeInTheDocument()
  })

  test('says that the list could not be loaded instead of claiming there are no sessions', async () => {
    vi.mocked(doRequest).mockResolvedValue({
      ok: false,
      status: 500,
      data: undefined,
    } as never)

    renderWithProviders(<ActiveSessions />)

    expect(await screen.findByText(/could not be loaded/)).toBeInTheDocument()
    expect(screen.queryByText('No active sessions found.')).not.toBeInTheDocument()
  })
})
