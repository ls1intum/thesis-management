import { beforeEach, describe, expect, test, vi } from 'vitest'
import { renderWithProviders, screen, userEvent, waitFor, within } from '@/../test/render'
import SchoolsTab from '@/core/organization/pages/OrganizationPage/components/SchoolsTab'
import type { ISchool } from '@/core/organization/requests/responses/organization'

const doRequest = vi.hoisted(() => vi.fn())
const showSimpleError = vi.hoisted(() => vi.fn())
const showSimpleSuccess = vi.hoisted(() => vi.fn())

vi.mock('@/core/requests/request', () => ({ doRequest }))
vi.mock('@/core/utils/notification', () => ({ showSimpleError, showSimpleSuccess }))

const schools: ISchool[] = [
  {
    id: 's-mgt',
    name: 'TUM School of Management',
    abbreviation: 'MGT',
    websiteUrl: 'https://www.mgt.tum.de/',
    thesisPortalUrl: 'https://portal.mgt.tum.de/',
    departments: [
      { id: 'd1', schoolId: 's-mgt', name: 'Operations and Technology', abbreviation: 'OT' },
      { id: 'd2', schoolId: 's-mgt', name: 'Retired', active: false },
    ],
  },
  { id: 's-nat', name: 'TUM School of Natural Sciences', abbreviation: 'NAT', active: false },
]

const renderTab = (onChanged = vi.fn()) => {
  renderWithProviders(<SchoolsTab schools={schools} onChanged={onChanged} />)
  return { onChanged }
}

describe('SchoolsTab', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  test('lists schools with their portal, departments and inactive markers', async () => {
    const user = userEvent.setup()
    renderTab()

    expect(screen.getByText('TUM School of Management')).toBeInTheDocument()
    await user.click(screen.getByText('TUM School of Management'))

    expect(await screen.findByRole('link', { name: 'https://portal.mgt.tum.de/' })).toHaveAttribute(
      'href',
      'https://portal.mgt.tum.de/',
    )
    expect(screen.getByText('Operations and Technology')).toBeInTheDocument()
    expect(screen.getByText('Retired')).toBeInTheDocument()
    // the inactive school (list) and the inactive department (panel)
    expect(screen.getAllByText('Inactive')).toHaveLength(2)
  })

  test('shows that the default portal is used when a school has none', async () => {
    const user = userEvent.setup()
    renderTab()

    await user.click(screen.getByText('TUM School of Natural Sciences'))

    expect(await screen.findByText('Default portal')).toBeInTheDocument()
  })

  test('rejects a portal that is not an http(s) URL without calling the server', async () => {
    const user = userEvent.setup()
    renderTab()

    await user.click(screen.getByRole('button', { name: /add school/i }))
    const dialog = await screen.findByRole('dialog', { name: 'Add School' })
    await user.type(within(dialog).getByLabelText(/^Name/), 'New School')
    await user.type(within(dialog).getByLabelText(/^Abbreviation/), 'NEW')
    await user.type(within(dialog).getByLabelText(/^Thesis portal/), 'javascript:alert(1)')
    await user.click(within(dialog).getByRole('button', { name: 'Create' }))

    expect(await within(dialog).findByText('Please enter a valid http(s) URL')).toBeInTheDocument()
    expect(doRequest).not.toHaveBeenCalled()
  })

  test('creates a school and asks the page to reload', async () => {
    const user = userEvent.setup()
    const { onChanged } = renderTab()
    doRequest.mockResolvedValue({ ok: true, status: 200, data: {} })

    await user.click(screen.getByRole('button', { name: /add school/i }))
    const dialog = await screen.findByRole('dialog', { name: 'Add School' })
    await user.type(within(dialog).getByLabelText(/^Name/), 'TUM School of Medicine and Health')
    await user.type(within(dialog).getByLabelText(/^Abbreviation/), 'MH')
    await user.type(within(dialog).getByLabelText(/^Thesis portal/), 'https://portal.mh.tum.de/')
    await user.click(within(dialog).getByRole('button', { name: 'Create' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalled())
    expect(doRequest).toHaveBeenCalledWith(
      '/v2/schools',
      expect.objectContaining({
        method: 'POST',
        data: {
          name: 'TUM School of Medicine and Health',
          abbreviation: 'MH',
          websiteUrl: null,
          thesisPortalUrl: 'https://portal.mh.tum.de/',
          active: true,
        },
      }),
    )
    expect(showSimpleSuccess).toHaveBeenCalledWith('School created')
  })

  test('shows the message of the server when a save fails', async () => {
    const user = userEvent.setup()
    const { onChanged } = renderTab()
    doRequest.mockResolvedValue({
      ok: false,
      status: 409,
      data: undefined,
      error: new Error('A school with this abbreviation already exists'),
    })

    await user.click(screen.getByRole('button', { name: /add school/i }))
    const dialog = await screen.findByRole('dialog', { name: 'Add School' })
    await user.type(within(dialog).getByLabelText(/^Name/), 'Duplicate')
    await user.type(within(dialog).getByLabelText(/^Abbreviation/), 'MGT')
    await user.click(within(dialog).getByRole('button', { name: 'Create' }))

    await waitFor(() =>
      expect(showSimpleError).toHaveBeenCalledWith(
        'A school with this abbreviation already exists',
      ),
    )
    expect(onChanged).not.toHaveBeenCalled()
  })

  test('edits a school with a PUT request that keeps the id', async () => {
    const user = userEvent.setup()
    const { onChanged } = renderTab()
    doRequest.mockResolvedValue({ ok: true, status: 200, data: {} })

    await user.click(screen.getByText('TUM School of Management'))
    await user.click(await screen.findByRole('button', { name: 'Edit School' }))
    const dialog = await screen.findByRole('dialog', { name: 'Edit School' })
    const portal = within(dialog).getByLabelText(/^Thesis portal/)
    await user.clear(portal)
    await user.type(portal, 'https://portal.new.tum.de/')
    await user.click(within(dialog).getByRole('button', { name: 'Save' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalled())
    expect(doRequest).toHaveBeenCalledWith(
      '/v2/schools/s-mgt',
      expect.objectContaining({
        method: 'PUT',
        data: expect.objectContaining({
          thesisPortalUrl: 'https://portal.new.tum.de/',
          abbreviation: 'MGT',
        }),
      }),
    )
  })

  test('adds a department to the school it was opened for', async () => {
    const user = userEvent.setup()
    const { onChanged } = renderTab()
    doRequest.mockResolvedValue({ ok: true, status: 200, data: {} })

    await user.click(screen.getByText('TUM School of Management'))
    await user.click(await screen.findByRole('button', { name: /add department/i }))
    const dialog = await screen.findByRole('dialog', {
      name: /add department to tum school of management/i,
    })
    await user.type(within(dialog).getByLabelText(/^Name/), 'Economics and Policy')
    await user.click(within(dialog).getByRole('button', { name: 'Create' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalled())
    expect(doRequest).toHaveBeenCalledWith(
      '/v2/departments',
      expect.objectContaining({
        method: 'POST',
        data: { schoolId: 's-mgt', name: 'Economics and Policy', abbreviation: null, active: true },
      }),
    )
  })

  test('deletes a school only after confirmation and explains a refusal', async () => {
    const user = userEvent.setup()
    const { onChanged } = renderTab()
    doRequest.mockResolvedValue({
      ok: false,
      status: 400,
      data: undefined,
      error: new Error(
        'The school is still used by departments, study programs or research groups. Deactivate it instead.',
      ),
    })

    await user.click(screen.getByText('TUM School of Management'))
    await user.click(await screen.findByRole('button', { name: 'Delete School' }))
    expect(doRequest).not.toHaveBeenCalled()

    await user.click(await screen.findByRole('button', { name: 'Confirm' }))

    await waitFor(() =>
      expect(showSimpleError).toHaveBeenCalledWith(
        expect.stringContaining('Deactivate it instead'),
      ),
    )
    expect(doRequest).toHaveBeenCalledWith(
      '/v2/schools/s-mgt',
      expect.objectContaining({ method: 'DELETE' }),
    )
    expect(onChanged).not.toHaveBeenCalled()
  })
})
