import { describe, expect, test, vi } from 'vitest'
import { renderWithProviders, screen, userEvent } from '@/../test/render'
import StudyProgramSelect from '@/core/organization/components/StudyProgramSelect'
import type { IStudyProgram } from '@/core/organization/requests/responses/organization'

const cit = { id: 's1', name: 'TUM School of CIT', abbreviation: 'CIT' }
const mgt = { id: 's2', name: 'TUM School of Management', abbreviation: 'MGT' }

const programs: IStudyProgram[] = [
  { id: 'p1', key: 'INFORMATICS', name: 'Informatics', school: cit },
  { id: 'p2', key: 'MANAGEMENT', name: 'Management', school: mgt },
  { id: 'p3', key: 'OLD', name: 'Discontinued Program', active: false, school: cit },
  { id: 'p4', key: 'OTHER', name: 'Other' },
]

const open = async (user: ReturnType<typeof userEvent.setup>) => {
  await user.click(screen.getByRole('combobox', { name: 'Study Program' }))
}

describe('StudyProgramSelect', () => {
  test('groups the programs by school and puts programs without school last', async () => {
    const user = userEvent.setup()
    renderWithProviders(
      <StudyProgramSelect
        label='Study Program'
        studyPrograms={programs}
        value={null}
        onChange={() => undefined}
      />,
    )

    await open(user)

    const groups = screen.getAllByText(/TUM School|^Other$/, {
      selector: '.mantine-Select-groupLabel',
    })
    expect(groups.map((group) => group.textContent)).toEqual([
      'TUM School of CIT',
      'TUM School of Management',
      'Other',
    ])
    expect(
      await screen.findByRole('option', { name: 'Informatics', hidden: true }),
    ).toBeInTheDocument()
  })

  test('hides deactivated programs unless they are the selected one', async () => {
    const user = userEvent.setup()
    const { unmount } = renderWithProviders(
      <StudyProgramSelect
        label='Study Program'
        studyPrograms={programs}
        value={null}
        onChange={() => undefined}
      />,
    )

    await open(user)
    expect(
      screen.queryByRole('option', { name: 'Discontinued Program', hidden: true }),
    ).not.toBeInTheDocument()
    unmount()

    renderWithProviders(
      <StudyProgramSelect
        label='Study Program'
        studyPrograms={programs}
        value='p3'
        onChange={() => undefined}
      />,
    )
    expect(screen.getByRole('combobox', { name: 'Study Program' })).toHaveValue(
      'Discontinued Program',
    )
  })

  test('reports the selected id', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    renderWithProviders(
      <StudyProgramSelect
        label='Study Program'
        studyPrograms={programs}
        value={null}
        onChange={onChange}
      />,
    )

    await open(user)
    await user.click(await screen.findByRole('option', { name: 'Management', hidden: true }))

    expect(onChange).toHaveBeenCalledWith('p2')
  })
})
