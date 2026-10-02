import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { renderWithProviders, waitFor } from '@/../test/render'
import { clearAvatarCache } from '@/core/components/CustomAvatar/avatarCache'
import AvatarInput from '@/core/user/components/UserInformationForm/components/AvatarInput/AvatarInput'
import { AuthenticationContext } from '@/core/providers/AuthenticationContext/context'
import type { IAuthenticationContext } from '@/core/providers/AuthenticationContext/context'
import type { IUser } from '@/core/user/requests/responses/user'

const doRequest = vi.hoisted(() => vi.fn())

vi.mock('@/core/requests/request', () => ({ doRequest }))
vi.mock('@/core/utils/notification', () => ({
  showSimpleError: vi.fn(),
  showSimpleSuccess: vi.fn(),
}))

const user = {
  userId: 'u1',
  firstName: 'Ada',
  lastName: 'Lovelace',
  avatar: 'abc123.png',
  email: 'ada@example.com',
} as unknown as IUser

const renderInput = (value?: File) => {
  const context = {
    user,
    isAuthenticated: true,
    updateUser: vi.fn(),
  } as unknown as IAuthenticationContext

  return renderWithProviders(
    <AuthenticationContext value={context}>
      <AvatarInput value={value} onChange={() => undefined} label='Avatar' />
    </AuthenticationContext>,
  )
}

describe('AvatarInput', () => {
  beforeEach(() => {
    URL.createObjectURL = vi.fn((blob: Blob | MediaSource) => `blob:mock-${(blob as Blob).size}`)
    URL.revokeObjectURL = vi.fn()
    // hand out the picture of the user
    doRequest.mockResolvedValue({ ok: true, status: 200, data: new Blob(['x'.repeat(7)]) })
  })

  afterEach(() => {
    clearAvatarCache()
    vi.clearAllMocks()
  })

  test('shows the current picture, loaded with the login token', async () => {
    renderInput()

    // The avatar endpoint answers 404 to anonymous requests for people who are not publicly listed, so the
    // picture has to be fetched with authentication (and shown as blob), never as a plain image URL.
    await waitFor(() => expect(document.querySelector('img')).not.toBeNull())
    expect(document.querySelector('img')?.getAttribute('src')).toMatch(/^blob:/)
    expect(doRequest).toHaveBeenCalledWith(
      '/v2/avatars/u1?filename=abc123.png',
      expect.objectContaining({ requiresAuth: true, responseType: 'blob' }),
    )
  })

  test('previews a newly chosen picture before it is saved', async () => {
    const chosen = new File(['12345'], 'new.png', { type: 'image/png' })

    renderInput(chosen)

    await waitFor(() =>
      expect(document.querySelector('img')?.getAttribute('src')).toBe('blob:mock-5'),
    )
  })
})
