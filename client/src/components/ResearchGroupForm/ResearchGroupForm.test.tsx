import { describe, expect, test, vi, beforeEach } from 'vitest'
import { renderWithProviders, screen, userEvent } from '../../../test/render'
import ResearchGroupForm from './ResearchGroupForm'
import { IResearchGroup } from '../../requests/responses/researchGroup'

// Behavioral test for issue #521.
//
// The form must:
//   - Disable both Submit and "Discard changes" when no edits are pending
//   - Enable both buttons once the user edits a field
//   - Restore the original values when "Discard changes" is clicked, and
//     return both buttons to the disabled state
//   - Submit the current values via the onSubmit prop

// `doRequest` is used by the KeycloakUserAutocomplete inside the form to
// search Keycloak for group heads. We don't exercise that path in this
// test, so stub it out to avoid any unexpected network calls.
vi.mock('../../requests/request', () => ({
  doRequest: vi.fn().mockReturnValue(() => undefined),
}))

const initial: Partial<IResearchGroup> = {
  name: 'Intelligent Systems',
  abbreviation: 'IS',
  campus: 'Garching',
  description: 'Original description',
  websiteUrl: 'https://example.com',
  head: {
    userId: 'u1',
    universityId: 'u1',
    firstName: 'Ada',
    lastName: 'Lovelace',
    email: 'ada@example.com',
    avatar: false,
  },
}

const renderForm = (onSubmit: (values: unknown) => void = () => undefined) =>
  renderWithProviders(
    <ResearchGroupForm initialResearchGroup={initial} onSubmit={onSubmit} submitLabel='Save' />,
  )

describe('ResearchGroupForm — issue #521 (discardable form)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  test('renders both Submit and Discard buttons in a disabled state when pristine', () => {
    renderForm()

    const discard = screen.getByRole('button', { name: /discard changes/i })
    const submit = screen.getByRole('button', { name: /^save$/i })

    expect(discard).toBeDisabled()
    expect(submit).toBeDisabled()
  })

  test('editing a field enables both buttons', async () => {
    const user = userEvent.setup()
    renderForm()

    const description = screen.getByLabelText(/description/i)
    await user.clear(description)
    await user.type(description, 'A different description')

    expect(screen.getByRole('button', { name: /discard changes/i })).toBeEnabled()
    expect(screen.getByRole('button', { name: /^save$/i })).toBeEnabled()
  })

  test('clicking Discard restores the original value and re-disables both buttons', async () => {
    const user = userEvent.setup()
    renderForm()

    const description = screen.getByLabelText(/description/i) as HTMLTextAreaElement
    await user.clear(description)
    await user.type(description, 'A different description')
    expect(description).toHaveValue('A different description')

    await user.click(screen.getByRole('button', { name: /discard changes/i }))

    expect(description).toHaveValue('Original description')
    expect(screen.getByRole('button', { name: /discard changes/i })).toBeDisabled()
    expect(screen.getByRole('button', { name: /^save$/i })).toBeDisabled()
  })

  test('Submit invokes onSubmit with the current form values', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()
    renderForm(onSubmit)

    const description = screen.getByLabelText(/description/i)
    await user.clear(description)
    await user.type(description, 'New description')

    await user.click(screen.getByRole('button', { name: /^save$/i }))

    expect(onSubmit).toHaveBeenCalledTimes(1)
    expect(onSubmit.mock.calls[0][0]).toMatchObject({
      name: 'Intelligent Systems',
      abbreviation: 'IS',
      description: 'New description',
    })
  })
})
