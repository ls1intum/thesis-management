import { describe, expect, test, vi, beforeEach } from 'vitest'
import { renderWithProviders, screen } from '@/../test/render'
import ThesisFeedbackOverview from '@/thesis/pages/ThesisPage/components/ThesisFeedbackOverview/ThesisFeedbackOverview'

// Comparison across iterations: an AI review of a newer version follows up addressed entries whose
// defect recurs, defers repeated recurrences to the supervisor, and acknowledges what was fixed.

const requestedBy = {
  userId: 'u1',
  universityId: 'u1',
  firstName: 'Ada',
  lastName: 'Lovelace',
  email: 'ada@example.com',
  avatar: false,
}

let aiReviewSummaries: Array<Record<string, unknown>> = []
let feedback: Array<Record<string, unknown>> = []

vi.mock('@/thesis/providers/ThesisProvider/hooks', () => ({
  useLoadedThesisContext: () => ({
    thesis: {
      thesisId: 'thesis-1',
      proposals: [{ proposalId: 'proposal-v2' }, { proposalId: 'proposal-v1' }],
      aiReviewSummaries,
      feedback,
    },
    access: { student: true, supervisor: false, examiner: false },
    updateThesis: vi.fn(),
  }),
  useThesisUpdateAction: () => [false, vi.fn()],
}))

const entry = (overrides: Record<string, unknown>) => ({
  type: 'PROPOSAL',
  requestedBy,
  requestedAt: '2026-04-01T10:00:00Z',
  completedAt: null,
  ...overrides,
})

describe('ThesisFeedbackOverview — comparison across iterations', () => {
  beforeEach(() => {
    feedback = [
      entry({
        feedbackId: 'fb-grammar',
        feedback: 'Fix subject-verb agreement in the abstract.',
        completedAt: '2026-04-02T10:00:00Z',
        documentVersionId: 'proposal-v1',
      }),
      entry({
        feedbackId: 'fb-figures',
        feedback: 'Number every figure.',
        completedAt: '2026-04-02T10:00:00Z',
        documentVersionId: 'proposal-v1',
      }),
      entry({
        feedbackId: 'fb-follow-up',
        feedback: 'Follow-up to "Fix subject-verb agreement in the abstract.": Still wrong.',
        followsUpId: 'fb-grammar',
        documentVersionId: 'proposal-v2',
      }),
    ]
    aiReviewSummaries = [
      {
        type: 'PROPOSAL',
        score: 75,
        assessment: 'ACCEPTABLE',
        summary:
          'Better.\n\nWell done: one issue from earlier feedback is resolved in this version.',
        documentVersionId: 'proposal-v2',
        resolvedFeedbackIds: ['fb-figures'],
        updatedAt: '2026-04-03T10:00:00Z',
      },
    ]
  })

  test('badges a follow-up and lists the entries the review found resolved', () => {
    renderWithProviders(<ThesisFeedbackOverview type='PROPOSAL' allowEdit />)

    expect(screen.getByText('Follow-up')).toBeInTheDocument()
    expect(screen.getByText('Resolved since the last review')).toBeInTheDocument()
    // Once in the resolved list, once in the table.
    expect(screen.getAllByText('Number every figure.')).toHaveLength(2)
  })

  test('labels a deferral as one to discuss with the supervisor', () => {
    feedback = [
      ...feedback,
      entry({
        feedbackId: 'fb-deferred',
        feedback: 'Discuss with your supervisor: this issue keeps recurring.',
        followsUpId: 'fb-follow-up',
        deferredToSupervisor: true,
      }),
    ]

    renderWithProviders(<ThesisFeedbackOverview type='PROPOSAL' allowEdit />)

    expect(screen.getByText('Discuss with supervisor')).toBeInTheDocument()
  })

  test('shows no resolved list when the review acknowledged nothing, or the entry is gone', () => {
    aiReviewSummaries = [{ ...aiReviewSummaries[0], resolvedFeedbackIds: ['deleted-entry'] }]

    renderWithProviders(<ThesisFeedbackOverview type='PROPOSAL' allowEdit />)

    expect(screen.queryByText('Resolved since the last review')).not.toBeInTheDocument()
  })
})
