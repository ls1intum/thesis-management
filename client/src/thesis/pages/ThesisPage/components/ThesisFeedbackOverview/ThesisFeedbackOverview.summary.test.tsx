import { describe, expect, test, vi, beforeEach } from 'vitest'
import { renderWithProviders, screen } from '@/../test/render'
import ThesisFeedbackOverview from '@/thesis/pages/ThesisPage/components/ThesisFeedbackOverview/ThesisFeedbackOverview'

// An AI review summary describes one specific revision of the proposal/thesis document. Once a
// newer document is uploaded the persisted summary is about the superseded one, so it must not
// keep presenting its score as the current assessment.

const requestedBy = {
  userId: 'u1',
  universityId: 'u1',
  firstName: 'Ada',
  lastName: 'Lovelace',
  email: 'ada@example.com',
  avatar: false,
}

// Newest-first, matching the server's ordering.
let proposals = [{ proposalId: 'proposal-v1' }]
let aiReviewSummaries: Array<Record<string, unknown>> = []
let feedback: Array<Record<string, unknown>> = []

vi.mock('@/thesis/providers/ThesisProvider/hooks', () => ({
  useLoadedThesisContext: () => ({
    thesis: {
      thesisId: 'thesis-1',
      proposals,
      aiReviewSummaries,
      feedback,
    },
    access: { student: false, supervisor: true, examiner: false },
    updateThesis: vi.fn(),
  }),
  useThesisUpdateAction: () => [false, vi.fn()],
}))

describe('ThesisFeedbackOverview — AI review summary staleness', () => {
  beforeEach(() => {
    proposals = [{ proposalId: 'proposal-v1' }]
    feedback = [
      {
        feedbackId: 'fb-1',
        type: 'PROPOSAL',
        feedback: 'Tighten the introduction.',
        requestedBy,
        requestedAt: '2026-04-01T10:00:00Z',
        completedAt: null,
        documentVersionId: 'proposal-v1',
      },
    ]
    aiReviewSummaries = [
      {
        type: 'PROPOSAL',
        score: 82,
        assessment: 'GOOD',
        summary: 'Solid proposal.',
        documentVersionId: 'proposal-v1',
        updatedAt: '2026-04-01T10:00:00Z',
      },
    ]
  })

  test('shows the summary while it matches the latest proposal', () => {
    renderWithProviders(<ThesisFeedbackOverview type='PROPOSAL' allowEdit />)

    expect(screen.getByText(/overall score: 82\/100/i)).toBeInTheDocument()
    expect(screen.getByText('Solid proposal.')).toBeInTheDocument()
  })

  test('hides the summary once a newer proposal has been uploaded', () => {
    proposals = [{ proposalId: 'proposal-v2' }, { proposalId: 'proposal-v1' }]

    renderWithProviders(<ThesisFeedbackOverview type='PROPOSAL' allowEdit />)

    expect(screen.queryByText(/overall score/i)).not.toBeInTheDocument()
    expect(screen.queryByText('Solid proposal.')).not.toBeInTheDocument()
    // The feedback table itself still renders — only the summary is version-scoped.
    expect(screen.getByText('Tighten the introduction.')).toBeInTheDocument()
  })

  test('still shows the summary when the review found nothing to flag', () => {
    // An AI review with no actionable findings saves a summary and no feedback rows at all.
    feedback = []

    renderWithProviders(<ThesisFeedbackOverview type='PROPOSAL' allowEdit />)

    expect(screen.getByText(/overall score: 82\/100/i)).toBeInTheDocument()
    expect(screen.getByText('Solid proposal.')).toBeInTheDocument()
    // Nothing to count or filter, so the table chrome stays away.
    expect(screen.queryByPlaceholderText(/search feedback/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/addressed/i)).not.toBeInTheDocument()
  })

  test('renders nothing when there is neither feedback nor a current summary', () => {
    feedback = []
    aiReviewSummaries = []

    renderWithProviders(<ThesisFeedbackOverview type='PROPOSAL' allowEdit />)

    // renderWithProviders injects Mantine's style tags, so assert on the section itself.
    expect(screen.queryByText('Feedback')).not.toBeInTheDocument()
    expect(screen.queryByText(/overall score/i)).not.toBeInTheDocument()
  })

  test('hides a legacy summary that carries no document version', () => {
    aiReviewSummaries = [
      {
        type: 'PROPOSAL',
        score: 82,
        assessment: 'GOOD',
        summary: 'Solid proposal.',
        updatedAt: '2026-04-01T10:00:00Z',
      },
    ]

    renderWithProviders(<ThesisFeedbackOverview type='PROPOSAL' allowEdit />)

    expect(screen.queryByText(/overall score/i)).not.toBeInTheDocument()
  })
})
