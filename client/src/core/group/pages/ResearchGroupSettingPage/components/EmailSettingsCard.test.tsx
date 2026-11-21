import { beforeEach, describe, expect, test, vi } from 'vitest'
import { renderWithProviders, screen, userEvent } from '@/../test/render'
import EmailSettingsCard from '@/core/group/pages/ResearchGroupSettingPage/components/EmailSettingsCard'
import { doRequest } from '@/core/requests/request'

vi.mock('@/core/requests/request', () => ({ doRequest: vi.fn() }))
vi.mock('@/core/utils/notification', () => ({
  showSimpleError: vi.fn(),
  showSimpleSuccess: vi.fn(),
}))

const STORED_SIGNATURE = '<p>Kind regards,<br/>The Example Chair</p>'

describe('EmailSettingsCard', () => {
  beforeEach(() => {
    vi.mocked(doRequest).mockReset()
  })

  // The server replaces `emailSettings` as a unit. A save that leaves out the signature would
  // therefore clear a signature that was configured through the API.
  test('sends the stored email signature back when only the notification email changes', async () => {
    const user = userEvent.setup()

    renderWithProviders(
      <EmailSettingsCard
        researchgroupEmailSettings={{
          applicationNotificationEmail: 'old@example.edu',
          emailSignature: STORED_SIGNATURE,
        }}
        setResearchgroupEmailSettings={vi.fn()}
      />,
    )

    const input = screen.getByRole('textbox')
    await user.clear(input)
    await user.type(input, 'new@example.edu')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(doRequest).toHaveBeenCalledTimes(1)
    const [, options] = vi.mocked(doRequest).mock.calls[0]
    expect(options).toMatchObject({
      method: 'POST',
      data: {
        emailSettings: {
          applicationNotificationEmail: 'new@example.edu',
          emailSignature: STORED_SIGNATURE,
        },
      },
    })
  })

  test('sends no signature when none is stored', async () => {
    const user = userEvent.setup()

    renderWithProviders(
      <EmailSettingsCard
        researchgroupEmailSettings={{ applicationNotificationEmail: 'old@example.edu' }}
        setResearchgroupEmailSettings={vi.fn()}
      />,
    )

    const input = screen.getByRole('textbox')
    await user.clear(input)
    await user.type(input, 'new@example.edu')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    const [, options] = vi.mocked(doRequest).mock.calls[0]
    expect(options).toMatchObject({
      data: { emailSettings: { applicationNotificationEmail: 'new@example.edu' } },
    })
    expect(
      (options as { data: { emailSettings: { emailSignature?: unknown } } }).data.emailSettings
        .emailSignature ?? null,
    ).toBeNull()
  })
})
