import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { MemoryRouter } from 'react-router'
import { renderWithProviders, screen, userEvent, waitFor } from '@/../test/render'
import ProfilePicturePrompt from '@/core/user/components/ProfilePicturePrompt/ProfilePicturePrompt'
import { AuthenticationContext } from '@/core/providers/AuthenticationContext/context'
import type { IAuthenticationContext } from '@/core/providers/AuthenticationContext/context'
import type { IUser } from '@/core/user/requests/responses/user'

const doRequest = vi.hoisted(() => vi.fn())

vi.mock('@/core/requests/request', () => ({ doRequest }))
const showSimpleError = vi.hoisted(() => vi.fn())

vi.mock('@/core/utils/notification', () => ({
  showSimpleError,
  showSimpleSuccess: vi.fn(),
}))

const createUser = (overrides: Partial<IUser> = {}): IUser =>
  ({
    userId: 'user-1',
    firstName: 'Ada',
    lastName: 'Lovelace',
    avatar: null,
    email: 'ada@example.com',
    ...overrides,
  }) as IUser

const renderPrompt = (user: IUser | undefined, updateUser = vi.fn(), path = '/dashboard') => {
  const context = { user, updateUser } as unknown as IAuthenticationContext

  renderWithProviders(
    <MemoryRouter initialEntries={[path]}>
      <AuthenticationContext value={context}>
        <ProfilePicturePrompt />
      </AuthenticationContext>
    </MemoryRouter>,
  )

  return { updateUser }
}

describe('ProfilePicturePrompt', () => {
  beforeEach(() => {
    doRequest.mockReset()
    showSimpleError.mockReset()
    localStorage.clear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  test('asks a user without picture who has not dismissed the prompt', async () => {
    renderPrompt(createUser())

    expect(await screen.findByText('Put a face to your name')).toBeInTheDocument()
  })

  test('does not ask a user who already has a picture', () => {
    renderPrompt(createUser({ avatar: 'picture.png' }))

    expect(screen.queryByText('Put a face to your name')).not.toBeInTheDocument()
  })

  test('does not ask a user who dismissed the prompt before', () => {
    renderPrompt(createUser({ avatarPromptDismissed: true }))

    expect(screen.queryByText('Put a face to your name')).not.toBeInTheDocument()
  })

  test('does not ask while the prompt is disabled via local storage', () => {
    localStorage.setItem('profile_picture_prompt_disabled', 'true')

    renderPrompt(createUser())

    expect(screen.queryByText('Put a face to your name')).not.toBeInTheDocument()
  })

  test('stores the dismissal on the server and closes when the user declines', async () => {
    const user = userEvent.setup()
    const dismissedUser = createUser({ avatarPromptDismissed: true })
    doRequest.mockResolvedValue({ ok: true, status: 200, data: dismissedUser })

    const { updateUser } = renderPrompt(createUser())

    await user.click(await screen.findByRole('button', { name: /maybe not/i }))

    await waitFor(() => expect(updateUser).toHaveBeenCalledWith(dismissedUser))
    expect(doRequest).toHaveBeenCalledWith(
      '/v2/user-info/dismiss-avatar-prompt',
      expect.objectContaining({ method: 'POST', requiresAuth: true }),
    )
    await waitFor(() =>
      expect(screen.queryByText('Put a face to your name')).not.toBeInTheDocument(),
    )
  })
  test('does not ask on the logout page', () => {
    renderPrompt(createUser(), vi.fn(), '/logout')

    expect(screen.queryByText('Put a face to your name')).not.toBeInTheDocument()
  })

  test.each([
    ['the request throws', () => doRequest.mockRejectedValue(new Error('network down'))],
    [
      'the server answers with an error',
      () => doRequest.mockResolvedValue({ ok: false, status: 500 }),
    ],
  ])('closes for this session and tells the user when %s', async (_name, arrange) => {
    const user = userEvent.setup()
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    arrange()

    const { updateUser } = renderPrompt(createUser())

    await user.click(await screen.findByRole('button', { name: /maybe not/i }))

    await waitFor(() =>
      expect(screen.queryByText('Put a face to your name')).not.toBeInTheDocument(),
    )
    await waitFor(() => expect(showSimpleError).toHaveBeenCalled())
    expect(updateUser).not.toHaveBeenCalled()
    consoleError.mockRestore()
  })

  test('imports the picture from Gravatar and stores the returned user', async () => {
    const user = userEvent.setup()
    const withPicture = createUser({ avatar: 'picture.png' })
    doRequest.mockResolvedValue({ ok: true, status: 200, data: withPicture })

    const { updateUser } = renderPrompt(createUser())

    await user.click(await screen.findByRole('button', { name: /import from gravatar/i }))

    await waitFor(() => expect(updateUser).toHaveBeenCalledWith(withPicture))
    expect(doRequest).toHaveBeenCalledWith(
      '/v2/user-info/import-profile-picture',
      expect.objectContaining({ method: 'POST' }),
    )
  })
})
