import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderWithProviders, screen, userEvent, act } from '../../../test/render'
import ApplicationReviewForm from './ApplicationReviewForm'
import { ApplicationState, type IApplication } from '../../requests/responses/application'

// Regression test for issue #754:
//   Application comments were lost when the reviewer switched to another
//   applicant before the debounced auto-save fired. The fix flushes any
//   pending comment edits in the application-switch cleanup so the typed
//   text is always persisted, even on a fast click-through.

const doRequest = vi.fn()
vi.mock('../../requests/request', () => ({
  doRequest: (...args: unknown[]) => doRequest(...args),
}))

vi.mock('../../hooks/authentication', () => ({
  useLoggedInUser: () => ({
    userId: 'reviewer-1',
    firstName: 'Rev',
    lastName: 'Iewer',
    avatar: null,
    universityId: 'reviewer1',
    email: 'reviewer@test.local',
    matriculationNumber: null,
    studyDegree: null,
    studyProgram: null,
    customData: null,
    joinedAt: '2026-01-01T00:00:00Z',
  }),
}))

vi.mock('../../providers/ApplicationsProvider/hooks', () => ({
  useApplicationsContextUpdater: () => () => undefined,
}))

const buildApplication = (overrides: Partial<IApplication> = {}): IApplication => ({
  applicationId: 'app-A',
  user: {
    userId: 'student-1',
    universityId: 'student1',
    firstName: 'Stu',
    lastName: 'Dent',
    email: 'stu@test.local',
    avatar: null,
    matriculationNumber: null,
    studyDegree: 'MASTER',
    studyProgram: null,
    customData: null,
    joinedAt: '2026-01-01T00:00:00Z',
    researchGroupName: null,
    researchGroupId: null,
    gender: null,
    nationality: null,
    projects: null,
    interests: null,
    specialSkills: null,
    enrolledAt: null,
    updatedAt: '2026-01-01T00:00:00Z',
    hasCv: false,
    hasDegreeReport: false,
    hasExaminationReport: false,
  },
  topic: null,
  thesisTitle: 'Some Title',
  thesisType: 'MASTER',
  motivation: 'irrelevant',
  state: ApplicationState.NOT_ASSESSED,
  desiredStartDate: '2026-06-01',
  comment: '',
  createdAt: '2026-05-01T00:00:00Z',
  reviewers: [],
  reviewedAt: null,
  researchGroup: {
    id: 'rg-1',
    name: 'Research Group',
    abbreviation: 'RG',
    head: {
      userId: 'head-1',
      universityId: 'head1',
      firstName: 'Head',
      lastName: 'Of Group',
      email: 'head@test.local',
      avatar: null,
      matriculationNumber: null,
      studyDegree: null,
      studyProgram: null,
      customData: null,
      joinedAt: '2026-01-01T00:00:00Z',
    },
  },
  ...overrides,
})

const findCommentSaveCalls = (): Array<{ applicationId: string; comment: string }> =>
  doRequest.mock.calls
    .filter((call) => /\/v2\/applications\/[^/]+\/comment$/.test(call[0]))
    .map((call) => {
      const match = (call[0] as string).match(/\/v2\/applications\/([^/]+)\/comment$/)
      const options = call[1] as { data?: { comment?: string } }
      return {
        applicationId: match?.[1] ?? '',
        comment: options.data?.comment ?? '',
      }
    })

describe('ApplicationReviewForm — issue #754 (comment auto-save)', () => {
  beforeEach(() => {
    doRequest.mockReset()
    // Default: callback-style invocations return a no-op unsubscribe so the
    // form's auto-save effect doesn't crash. Tests that need to assert on
    // save success can override per-call behaviour.
    doRequest.mockImplementation(
      (
        _url: string,
        _options: unknown,
        cb?: (res: { ok: boolean; status: number; data: unknown }) => void,
      ) => {
        // For Promise-style callers we'd return a Promise; the form only
        // uses the callback form for /comment, so the unsubscribe shape is
        // sufficient.
        cb?.({ ok: true, status: 200, data: { comment: '' } })
        return () => undefined
      },
    )
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('saves the typed comment when the user switches applicants before the debounce fires', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime.bind(vi) })

    const appA = buildApplication({ applicationId: 'app-A', comment: '' })
    const appB = buildApplication({ applicationId: 'app-B', comment: '' })

    const { rerender } = renderWithProviders(<ApplicationReviewForm application={appA} />)

    const textarea = screen.getByLabelText('Comment')
    await user.type(textarea, 'pending thoughts')

    // Switch to applicant B *before* the 500ms debounce fires. The cleanup
    // on the application-switch effect must flush the pending edit.
    rerender(<ApplicationReviewForm application={appB} />)

    const commentCalls = findCommentSaveCalls()
    expect(
      commentCalls,
      'expected one PUT /v2/applications/app-A/comment from the cleanup flush',
    ).toHaveLength(1)
    expect(commentCalls[0]).toEqual({
      applicationId: 'app-A',
      comment: 'pending thoughts',
    })
  })

  it('does not re-save when the typed value matches the last saved baseline', async () => {
    const user = userEvent.setup()

    const appA = buildApplication({ applicationId: 'app-A', comment: 'already saved' })
    const appB = buildApplication({ applicationId: 'app-B', comment: '' })

    const { rerender } = renderWithProviders(<ApplicationReviewForm application={appA} />)

    // The textarea is pre-filled from application.comment; touching it
    // back to the same value must not trigger any save.
    const textarea = screen.getByLabelText('Comment')
    await user.click(textarea)

    rerender(<ApplicationReviewForm application={appB} />)

    expect(findCommentSaveCalls()).toHaveLength(0)
  })

  it('still saves via the debounced effect when the user waits without switching', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime.bind(vi) })

    const appA = buildApplication({ applicationId: 'app-A', comment: '' })

    renderWithProviders(<ApplicationReviewForm application={appA} />)

    const textarea = screen.getByLabelText('Comment')
    await user.type(textarea, 'leisurely thoughts')

    // Advance past the 500ms debounce so the auto-save effect fires.
    await act(async () => {
      await vi.advanceTimersByTimeAsync(600)
    })

    const commentCalls = findCommentSaveCalls()
    expect(commentCalls).toHaveLength(1)
    expect(commentCalls[0]).toEqual({
      applicationId: 'app-A',
      comment: 'leisurely thoughts',
    })
  })
})
