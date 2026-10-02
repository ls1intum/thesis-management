import { beforeEach, describe, expect, test, vi } from 'vitest'
import { MemoryRouter } from 'react-router'
import { renderWithProviders, screen, userEvent } from '@/../test/render'
import UserInformationForm from '@/core/user/components/UserInformationForm/UserInformationForm'
import { AuthenticationContext } from '@/core/providers/AuthenticationContext/context'
import type { IAuthenticationContext } from '@/core/providers/AuthenticationContext/context'
import type { IUser } from '@/core/user/requests/responses/user'
import type { IStudyProgram } from '@/core/organization/requests/responses/organization'

let studyPrograms: IStudyProgram[] = []

// The organization hook loads the study programs through the callback form of doRequest.
vi.mock('@/core/requests/request', () => ({
  doRequest: vi.fn((url: string, _options: unknown, callback?: (response: unknown) => void) => {
    if (callback && url === '/v2/study-programs') {
      callback({ ok: true, status: 200, data: studyPrograms })
    }
    if (callback && url === '/v2/schools') {
      callback({ ok: true, status: 200, data: [] })
    }
    return () => undefined
  }),
}))

const user = {
  userId: 'u1',
  firstName: 'Ada',
  lastName: 'Lovelace',
  avatar: null,
  email: 'ada@example.com',
  universityId: 'ab12cde',
  hasCv: false,
  hasDegreeReport: false,
  hasExaminationReport: false,
  customData: {},
} as unknown as IUser

const renderForm = () => {
  const context = {
    user,
    updateInformation: vi.fn(),
    updateUser: vi.fn(),
  } as unknown as IAuthenticationContext

  renderWithProviders(
    <MemoryRouter>
      <AuthenticationContext value={context}>
        <UserInformationForm requireCompletion={true} />
      </AuthenticationContext>
    </MemoryRouter>,
  )
}

const leaveStudyProgramEmpty = async () => {
  const session = userEvent.setup()
  await session.click(screen.getByRole('combobox', { name: /Study Program/ }))
  await session.tab()
  await session.tab()
}

describe('UserInformationForm - study program', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  test('asks for the study program when the instance has study programs', async () => {
    studyPrograms = [{ id: 'p1', key: 'INFORMATICS', name: 'Informatics' }]
    renderForm()

    await leaveStudyProgramEmpty()

    expect(await screen.findByText('Please state your study program')).toBeInTheDocument()
  })

  test('does not block the profile when every study program is deactivated', async () => {
    studyPrograms = [{ id: 'p1', key: 'OLD', name: 'Discontinued', active: false }]
    renderForm()

    await leaveStudyProgramEmpty()

    expect(screen.queryByText('Please state your study program')).not.toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: /Study Program/ })).not.toBeRequired()
  })

  test('does not block the profile when no study program is configured yet', async () => {
    studyPrograms = []
    renderForm()

    await leaveStudyProgramEmpty()

    expect(screen.queryByText('Please state your study program')).not.toBeInTheDocument()
    // and it is not marked as required either, so the browser does not block the form
    expect(screen.getByRole('combobox', { name: /Study Program/ })).not.toBeRequired()
  })
})
