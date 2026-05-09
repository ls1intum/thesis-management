import { describe, expect, test, vi, beforeEach } from 'vitest'
import { renderWithProviders, screen, userEvent } from '../../../../../test/render'
import ThesisFeedbackOverview from './ThesisFeedbackOverview'

// Behavioral test for issue #463.
//
// Clicking the trash icon on a feedback row must NOT immediately delete
// the entry — it should open a confirmation dialog with Cancel / Confirm.
// Cancel keeps the row; Confirm calls the delete action exactly once.

const mockDeleteFeedback = vi.fn()
const mockToggleFeedback = vi.fn()

// The component calls useThesisUpdateAction twice per render, in this
// order: first to wire `toggleFeedback`, then `deleteFeedback`. We track
// the call count on each render to return the right action handler.
let useThesisUpdateActionCallCount = 0

// Mock the thesis-context hooks the component reads. Returning a controlled
// thesis lets us put one feedback item into the table without setting up
// the whole ThesisContext.Provider tree.
vi.mock('../../../../providers/ThesisProvider/hooks', () => ({
  useLoadedThesisContext: () => ({
    thesis: {
      thesisId: 'thesis-1',
      feedback: [
        {
          feedbackId: 'fb-1',
          type: 'PROPOSAL',
          feedback: 'Tighten the introduction.',
          requestedBy: {
            userId: 'u1',
            universityId: 'u1',
            firstName: 'Ada',
            lastName: 'Lovelace',
            email: 'ada@example.com',
            avatar: false,
          },
          requestedAt: '2026-04-01T10:00:00Z',
          completedAt: null,
        },
      ],
    },
    access: { student: false, supervisor: true, examiner: false },
    updateThesis: vi.fn(),
  }),
  useThesisUpdateAction: () => {
    useThesisUpdateActionCallCount += 1
    if (useThesisUpdateActionCallCount % 2 === 1) {
      return [false, mockToggleFeedback]
    }
    return [false, mockDeleteFeedback]
  },
}))

describe('ThesisFeedbackOverview — issue #463 (delete confirmation)', () => {
  beforeEach(() => {
    mockDeleteFeedback.mockClear()
    mockToggleFeedback.mockClear()
    useThesisUpdateActionCallCount = 0
  })

  test('clicking the trash icon does not immediately delete - it opens the confirmation modal', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ThesisFeedbackOverview type='PROPOSAL' allowEdit />)

    // The trash button is rendered with no accessible name (it contains
    // only the icon). It is the second button rendered (after the
    // checkbox-as-button is not rendered when not student); we locate it
    // by querying all buttons in the table.
    const buttons = screen.getAllByRole('button')
    expect(buttons.length).toBeGreaterThan(0)
    const trash = buttons[buttons.length - 1]

    await user.click(trash)

    expect(
      await screen.findByText(/this will permanently remove this feedback entry/i),
    ).toBeInTheDocument()
    expect(mockDeleteFeedback).not.toHaveBeenCalled()
  })

  test('Cancel closes the modal without calling deleteFeedback', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ThesisFeedbackOverview type='PROPOSAL' allowEdit />)

    const buttons = screen.getAllByRole('button')
    const trash = buttons[buttons.length - 1]
    await user.click(trash)

    await user.click(await screen.findByRole('button', { name: /cancel/i }))

    expect(mockDeleteFeedback).not.toHaveBeenCalled()
  })

  test('Confirm calls deleteFeedback with the feedback item exactly once', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ThesisFeedbackOverview type='PROPOSAL' allowEdit />)

    const buttons = screen.getAllByRole('button')
    const trash = buttons[buttons.length - 1]
    await user.click(trash)

    await user.click(await screen.findByRole('button', { name: /confirm/i }))

    expect(mockDeleteFeedback).toHaveBeenCalledTimes(1)
    expect(mockDeleteFeedback.mock.calls[0][0]).toMatchObject({ feedbackId: 'fb-1' })
  })
})
