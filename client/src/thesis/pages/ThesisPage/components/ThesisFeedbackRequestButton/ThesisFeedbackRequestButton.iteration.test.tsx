import { describe, expect, test, vi, beforeEach } from 'vitest'

// A supervisor's AI preview can contain follow-ups: drafts that report the defect of an earlier,
// addressed entry as recurring. The link to that entry must survive editing and saving, or the
// follow-up loses its history and the recurrence count the server defers on.

const requestMock = vi.hoisted(() => ({
  doRequest: vi.fn(),
}))

const handlerMock = vi.hoisted(() => ({
  getApiResponseErrorMessage: vi.fn(),
  ApiError: class ApiError extends Error {},
}))

const notifyMock = vi.hoisted(() => ({
  showSimpleError: vi.fn(),
  showSimpleSuccess: vi.fn(),
}))

const configMock = vi.hoisted(() => ({
  GLOBAL_CONFIG: { ai_enabled: true },
}))

vi.mock('@/core/requests/request', () => requestMock)
vi.mock('@/core/requests/handler', () => handlerMock)
vi.mock('@/core/utils/notification', () => notifyMock)
vi.mock('@/core/config/global', () => configMock)
vi.mock('@/core/hooks/useReviewProgress', () => ({
  useReviewProgress: () => ({ steps: [], total: 0, start: vi.fn(), stop: vi.fn() }),
}))

vi.mock('@/thesis/providers/ThesisProvider/hooks', () => ({
  useLoadedThesisContext: () => ({
    thesis: {
      thesisId: 'thesis-1',
      feedback: [
        {
          feedbackId: 'fb-grammar',
          type: 'PROPOSAL',
          feedback: 'Fix subject-verb agreement in the abstract.',
          completedAt: '2026-04-02T10:00:00Z',
        },
        {
          feedbackId: 'fb-figures',
          type: 'PROPOSAL',
          feedback: 'Number every figure.',
          completedAt: '2026-04-02T10:00:00Z',
        },
      ],
    },
    access: { student: false, supervisor: true, examiner: false },
    updateThesis: vi.fn(),
  }),
  // Runs the save callback directly, so the request it sends can be inspected.
  useThesisUpdateAction: (callback: () => Promise<unknown>) => [false, () => void callback()],
}))

import { renderWithProviders, screen, userEvent, waitFor } from '@/../test/render'
import ThesisFeedbackRequestButton from '@/thesis/pages/ThesisPage/components/ThesisFeedbackRequestButton/ThesisFeedbackRequestButton'

const okResponse = <T,>(data: T) => ({ ok: true as const, status: 200, data })

describe('ThesisFeedbackRequestButton — follow-ups from an AI preview', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  test('badges follow-up drafts, lists resolved entries, and saves the follow-up link', async () => {
    const user = userEvent.setup()
    requestMock.doRequest.mockImplementation((url: string) => {
      if (url === '/v2/ai-review/preview') {
        return Promise.resolve(
          okResponse({
            assessment: 'ACCEPTABLE',
            score: 70,
            summary: 'Better.',
            resolvedFeedbackIds: ['fb-figures'],
            documentVersionId: 'proposal-v1',
            drafts: [
              {
                feedback: 'Follow-up to "Fix subject-verb agreement": still wrong.',
                category: 'WRITING',
                severity: 'MINOR',
                followsUpId: 'fb-grammar',
              },
              { feedback: 'Add a related work section.', category: 'STRUCTURE', severity: 'MAJOR' },
            ],
          }),
        )
      }
      return Promise.resolve(okResponse({ thesisId: 'thesis-1' }))
    })

    renderWithProviders(<ThesisFeedbackRequestButton type='PROPOSAL' />)
    await user.click(screen.getAllByRole('button', { name: 'Request Changes' })[0])
    await screen.findByText('New feedback entries')
    await user.click(screen.getByRole('button', { name: 'Generate with AI' }))

    expect(await screen.findByText('Follow-up')).toBeInTheDocument()
    expect(screen.getByText('Resolved since the last review')).toBeInTheDocument()

    const saveButtons = screen.getAllByRole('button', { name: 'Request Changes' })
    await user.click(saveButtons[saveButtons.length - 1])

    await waitFor(() =>
      expect(requestMock.doRequest).toHaveBeenCalledWith(
        '/v2/theses/thesis-1/feedback',
        expect.objectContaining({
          method: 'POST',
          data: {
            type: 'PROPOSAL',
            requestedChanges: [
              expect.objectContaining({
                feedback: 'Follow-up to "Fix subject-verb agreement": still wrong.',
                followsUpId: 'fb-grammar',
                deferredToSupervisor: false,
                // Pinned to the revision the preview reviewed, even if a newer one exists by now.
                documentVersionId: 'proposal-v1',
              }),
              expect.objectContaining({
                feedback: 'Add a related work section.',
                followsUpId: null,
              }),
            ],
          },
        }),
      ),
    )
  })
})
