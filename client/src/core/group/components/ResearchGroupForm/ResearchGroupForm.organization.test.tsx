import { beforeEach, describe, expect, test, vi } from 'vitest'
import { renderWithProviders, screen, userEvent, waitFor } from '@/../test/render'
import ResearchGroupForm from '@/core/group/components/ResearchGroupForm/ResearchGroupForm'
import type { IResearchGroup } from '@/core/group/requests/responses/researchGroup'
import type { ISchool } from '@/core/organization/requests/responses/organization'

const schools: ISchool[] = [
  {
    id: 's-cit',
    name: 'School of CIT',
    abbreviation: 'CIT',
    departments: [
      { id: 'd-cs', schoolId: 's-cit', name: 'Computer Science' },
      { id: 'd-math', schoolId: 's-cit', name: 'Mathematics' },
      { id: 'd-old', schoolId: 's-cit', name: 'Retired Department', active: false },
    ],
  },
  {
    id: 's-mgt',
    name: 'School of Management',
    abbreviation: 'MGT',
    departments: [{ id: 'd-ops', schoolId: 's-mgt', name: 'Operations' }],
  },
  { id: 's-old', name: 'Closed School', abbreviation: 'OLD', active: false, departments: [] },
]

// The organization hook loads /v2/schools and /v2/study-programs through the callback form of
// doRequest; the Keycloak autocomplete of the form also uses doRequest, which we leave inert.
// The lists arrive asynchronously like over the network (`loadDelay`).
let loadDelay = 0
vi.mock('@/core/requests/request', () => ({
  doRequest: vi.fn((url: string, _options: unknown, callback?: (response: unknown) => void) => {
    if (callback && url === '/v2/schools') {
      setTimeout(() => callback({ ok: true, status: 200, data: schools }), loadDelay)
    }
    if (callback && url === '/v2/study-programs') {
      setTimeout(() => callback({ ok: true, status: 200, data: [] }), loadDelay)
    }
    return () => undefined
  }),
}))

const initial: Partial<IResearchGroup> = {
  name: 'Intelligent Systems',
  abbreviation: 'IS',
  campus: 'Garching',
  description: 'Description',
  websiteUrl: 'https://example.com',
  head: { userId: 'u1', firstName: 'Ada', lastName: 'Lovelace', avatar: null },
}

const renderForm = (
  initialResearchGroup: Partial<IResearchGroup> = initial,
  onSubmit: (values: unknown) => void = () => undefined,
) =>
  renderWithProviders(
    <ResearchGroupForm
      initialResearchGroup={initialResearchGroup}
      onSubmit={onSubmit}
      submitLabel='Save'
    />,
  )

const choose = async (user: ReturnType<typeof userEvent.setup>, select: string, option: string) => {
  await user.click(screen.getByRole('combobox', { name: select }))
  await user.click(await screen.findByRole('option', { name: option, hidden: true }))
}

describe('ResearchGroupForm - school and department', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    loadDelay = 0
  })

  test('keeps the school and department of the group while the lists are still loading', async () => {
    loadDelay = 50
    renderForm({ ...initial, school: schools[0], department: schools[0].departments?.[0] })

    // before the lists arrived the current values must not be dropped by the selects
    expect(screen.getByRole('combobox', { name: 'School' })).toHaveValue('School of CIT (CIT)')
    expect(screen.getByRole('combobox', { name: 'Department' })).toHaveValue('Computer Science')

    await waitFor(() =>
      expect(screen.getByRole('button', { name: /discard changes/i })).toBeDisabled(),
    )
    await new Promise((resolve) => setTimeout(resolve, 120))

    expect(screen.getByRole('combobox', { name: 'School' })).toHaveValue('School of CIT (CIT)')
    expect(screen.getByRole('combobox', { name: 'Department' })).toHaveValue('Computer Science')
    // nothing was changed by loading, so there is nothing to save
    expect(screen.getByRole('button', { name: /^save$/i })).toBeDisabled()
  })

  test('the department can only be chosen after a school was selected', () => {
    renderForm()

    expect(screen.getByRole('combobox', { name: 'Department' })).toBeDisabled()
  })

  test('offers only the departments of the selected school and hides inactive ones', async () => {
    const user = userEvent.setup()
    renderForm()

    await choose(user, 'School', 'School of CIT (CIT)')
    await user.click(screen.getByRole('combobox', { name: 'Department' }))

    expect(
      await screen.findByRole('option', { name: 'Computer Science', hidden: true }),
    ).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Mathematics', hidden: true })).toBeInTheDocument()
    expect(
      screen.queryByRole('option', { name: 'Operations', hidden: true }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('option', { name: 'Retired Department', hidden: true }),
    ).not.toBeInTheDocument()
  })

  test('does not offer inactive schools', async () => {
    const user = userEvent.setup()
    renderForm()

    await user.click(screen.getByRole('combobox', { name: 'School' }))

    expect(
      await screen.findByRole('option', { name: 'School of CIT (CIT)', hidden: true }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('option', { name: 'Closed School (OLD)', hidden: true }),
    ).not.toBeInTheDocument()
  })

  test('changing the school clears the department', async () => {
    const user = userEvent.setup()
    renderForm({ ...initial, school: schools[0], department: schools[0].departments?.[0] })

    expect(screen.getByRole('combobox', { name: 'Department' })).toHaveValue('Computer Science')

    await choose(user, 'School', 'School of Management (MGT)')

    expect(screen.getByRole('combobox', { name: 'Department' })).toHaveValue('')
  })

  test('submits the selected school and department', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()
    renderForm(initial, onSubmit)

    await choose(user, 'School', 'School of CIT (CIT)')
    await choose(user, 'Department', 'Mathematics')
    await user.click(screen.getByRole('button', { name: /^save$/i }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalled())
    expect(onSubmit.mock.calls[0][0]).toMatchObject({ schoolId: 's-cit', departmentId: 'd-math' })
  })

  test('submits null when no school is selected', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()
    renderForm(initial, onSubmit)

    await user.type(screen.getByLabelText(/description/i), ' changed')
    await user.click(screen.getByRole('button', { name: /^save$/i }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalled())
    expect(onSubmit.mock.calls[0][0]).toMatchObject({ schoolId: null, departmentId: null })
  })
})
