import { beforeEach, describe, expect, test, vi } from 'vitest'
import { renderWithProviders, screen, userEvent, waitFor, within } from '@/../test/render'
import StudyProgramsTab from '@/core/organization/pages/OrganizationPage/components/StudyProgramsTab'
import type { ISchool, IStudyProgram } from '@/core/organization/requests/responses/organization'

const doRequest = vi.hoisted(() => vi.fn())
const showSimpleError = vi.hoisted(() => vi.fn())
const showSimpleSuccess = vi.hoisted(() => vi.fn())

vi.mock('@/core/requests/request', () => ({ doRequest }))
vi.mock('@/core/utils/notification', () => ({ showSimpleError, showSimpleSuccess }))

const cit = { id: 's-cit', name: 'TUM School of CIT', abbreviation: 'CIT' }
const schools: ISchool[] = [
  { ...cit },
  { id: 's-mgt', name: 'TUM School of Management', abbreviation: 'MGT' },
]

const studyPrograms: IStudyProgram[] = [
  { id: 'p1', key: 'COMPUTER_SCIENCE', name: 'Informatics', school: cit },
  {
    id: 'p2',
    key: 'MANAGEMENT_AND_TECHNOLOGY',
    name: 'Management and Technology',
    school: schools[1],
  },
  { id: 'p3', key: 'OLD', name: 'Discontinued', active: false },
]

const renderTab = (onChanged = vi.fn()) => {
  renderWithProviders(
    <StudyProgramsTab studyPrograms={studyPrograms} schools={schools} onChanged={onChanged} />,
  )
  return { onChanged }
}

describe('StudyProgramsTab', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  test('lists the study programs with school and status', () => {
    renderTab()

    expect(screen.getByText('Informatics')).toBeInTheDocument()
    expect(screen.getByText('COMPUTER_SCIENCE')).toBeInTheDocument()
    expect(screen.getByText('CIT')).toBeInTheDocument()
    expect(screen.getAllByText('Active')).toHaveLength(2)
    expect(screen.getByText('Inactive')).toBeInTheDocument()
  })

  test('filters by name, key and school', async () => {
    const user = userEvent.setup()
    renderTab()
    const search = screen.getByPlaceholderText('Search study programs...')

    await user.type(search, 'management')
    expect(screen.queryByText('Informatics')).not.toBeInTheDocument()
    expect(screen.getByText('Management and Technology')).toBeInTheDocument()

    await user.clear(search)
    await user.type(search, 'cit')
    expect(screen.getByText('Informatics')).toBeInTheDocument()
    expect(screen.queryByText('Management and Technology')).not.toBeInTheDocument()

    await user.clear(search)
    await user.type(search, 'does not exist')
    expect(screen.getByText('No Study Programs Found')).toBeInTheDocument()
  })

  test('creates a study program without key and school', async () => {
    const user = userEvent.setup()
    const { onChanged } = renderTab()
    doRequest.mockResolvedValue({ ok: true, status: 200, data: {} })

    await user.click(screen.getByRole('button', { name: /add study program/i }))
    const dialog = await screen.findByRole('dialog', { name: 'Add Study Program' })
    await user.type(within(dialog).getByLabelText(/^Name/), 'Data Engineering and Analytics')
    await user.click(within(dialog).getByRole('button', { name: 'Create' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalled())
    expect(doRequest).toHaveBeenCalledWith(
      '/v2/study-programs',
      expect.objectContaining({
        method: 'POST',
        data: { name: 'Data Engineering and Analytics', key: null, schoolId: null, active: true },
      }),
    )
  })

  test('edits a study program and keeps its school preselected', async () => {
    const user = userEvent.setup()
    const { onChanged } = renderTab()
    doRequest.mockResolvedValue({ ok: true, status: 200, data: {} })

    await user.click(screen.getByRole('button', { name: 'Edit study program Informatics' }))
    const dialog = await screen.findByRole('dialog', { name: 'Edit Study Program' })
    expect(within(dialog).getByRole('combobox', { name: /^School/ })).toHaveValue(
      'TUM School of CIT (CIT)',
    )

    const name = within(dialog).getByLabelText(/^Name/)
    await user.clear(name)
    await user.type(name, 'Informatics (B.Sc.)')
    await user.click(within(dialog).getByRole('button', { name: 'Save' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalled())
    expect(doRequest).toHaveBeenCalledWith(
      '/v2/study-programs/p1',
      expect.objectContaining({
        method: 'PUT',
        data: {
          name: 'Informatics (B.Sc.)',
          key: 'COMPUTER_SCIENCE',
          schoolId: 's-cit',
          active: true,
        },
      }),
    )
  })

  test('explains why a study program in use cannot be deleted', async () => {
    const user = userEvent.setup()
    const { onChanged } = renderTab()
    doRequest.mockResolvedValue({
      ok: false,
      status: 400,
      data: undefined,
      error: new Error(
        'The study program is still used by students or theses. Deactivate it instead.',
      ),
    })

    const row = screen.getByText('Informatics').closest('tr') as HTMLElement
    await user.click(within(row).getByRole('button', { name: 'Delete' }))
    await user.click(await screen.findByRole('button', { name: 'Confirm' }))

    await waitFor(() =>
      expect(showSimpleError).toHaveBeenCalledWith(
        expect.stringContaining('still used by students or theses'),
      ),
    )
    expect(doRequest).toHaveBeenCalledWith(
      '/v2/study-programs/p1',
      expect.objectContaining({ method: 'DELETE' }),
    )
    expect(onChanged).not.toHaveBeenCalled()
  })
})
