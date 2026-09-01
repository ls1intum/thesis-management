import { describe, expect, test, vi, beforeEach } from 'vitest'

// The magic-wand button asks the server to classify one manually written feedback entry and fills
// in whichever of category and severity the AI committed to. The feedback text itself is never
// touched — only the two dropdowns the instructor would otherwise pick by hand.

const requestMock = vi.hoisted(() => ({
  doRequest: vi.fn(),
}))

const handlerMock = vi.hoisted(() => ({
  getApiResponseErrorMessage: vi.fn(),
  ApiError: class ApiError extends Error {},
}))

const notifyMock = vi.hoisted(() => ({
  showSimpleError: vi.fn(),
}))

const configMock = vi.hoisted(() => ({
  GLOBAL_CONFIG: { ai_enabled: true },
}))

vi.mock('@/core/requests/request', () => requestMock)
vi.mock('@/core/requests/handler', () => handlerMock)
vi.mock('@/core/utils/notification', () => notifyMock)
vi.mock('@/core/config/global', () => configMock)

vi.mock('@/thesis/providers/ThesisProvider/hooks', () => ({
  useLoadedThesisContext: () => ({
    thesis: { thesisId: 'thesis-1', feedback: [] },
    access: { student: false, supervisor: true, examiner: false },
    updateThesis: vi.fn(),
  }),
  useThesisUpdateAction: () => [false, vi.fn()],
}))

import { renderWithProviders, screen, userEvent, waitFor } from '@/../test/render'
import ThesisFeedbackRequestButton from '@/thesis/pages/ThesisPage/components/ThesisFeedbackRequestButton/ThesisFeedbackRequestButton'

const okResponse = <T,>(data: T) => ({ ok: true as const, status: 200, data })
const serverErrorResponse = { ok: false as const, status: 500, data: undefined }

const FEEDBACK_PLACEHOLDER = 'Describe the change you want the student to make…'
const WAND_LABEL = 'Suggest category and severity with AI'

// The modal mounts behind a Mantine transition, so its content is not in the DOM on the tick the
// click resolves — wait for it before querying anything inside.
const openModal = async (user: ReturnType<typeof userEvent.setup>) => {
  await user.click(screen.getAllByRole('button', { name: 'Request Changes' })[0])
  await screen.findByText('New feedback entries')
}

const categoryInput = () => screen.getByRole('combobox', { name: 'Category' })
const severityInput = () => screen.getByRole('combobox', { name: 'Severity' })

describe('ThesisFeedbackRequestButton — AI classification', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    handlerMock.getApiResponseErrorMessage.mockReturnValue('Something went wrong')
  })

  test('leaves the wand inert until the entry has feedback text', async () => {
    // Classifying an empty entry would spend an LLM call to label nothing, so the affordance
    // stays disabled until the instructor has actually written something.
    const user = userEvent.setup()
    renderWithProviders(<ThesisFeedbackRequestButton type='THESIS' />)
    await openModal(user)

    expect(screen.getByRole('button', { name: WAND_LABEL })).toBeDisabled()
    expect(requestMock.doRequest).not.toHaveBeenCalled()
  })

  test('fills both dropdowns from the suggestion', async () => {
    // The point of the feature is that the instructor stops picking these two values by hand.
    const user = userEvent.setup()
    requestMock.doRequest.mockResolvedValueOnce(
      okResponse({ category: 'CITATION', severity: 'MAJOR' }),
    )

    renderWithProviders(<ThesisFeedbackRequestButton type='THESIS' />)
    await openModal(user)
    await user.type(screen.getByPlaceholderText(FEEDBACK_PLACEHOLDER), 'Cite the original paper')
    await user.click(screen.getByRole('button', { name: WAND_LABEL }))

    expect(requestMock.doRequest).toHaveBeenCalledWith(
      '/v2/ai-review/classify-feedback',
      expect.objectContaining({
        method: 'POST',
        data: { thesisId: 'thesis-1', feedback: 'Cite the original paper' },
      }),
    )
    await waitFor(() => expect(categoryInput()).toHaveValue('Citation'))
    expect(severityInput()).toHaveValue('Major')
  })

  test('keeps the existing severity when the suggestion omits one', async () => {
    // NON_EMPTY serialization drops a field the AI left open; overwriting the instructor's own
    // pick with a blank would make the helper destructive.
    const user = userEvent.setup()
    requestMock.doRequest.mockResolvedValueOnce(okResponse({ category: 'STRUCTURE' }))

    renderWithProviders(<ThesisFeedbackRequestButton type='THESIS' />)
    await openModal(user)
    await user.type(screen.getByPlaceholderText(FEEDBACK_PLACEHOLDER), 'Reorder the sections')

    await user.click(severityInput())
    await user.click(await screen.findByText('Critical'))
    await user.click(screen.getByRole('button', { name: WAND_LABEL }))

    await waitFor(() => expect(categoryInput()).toHaveValue('Structure'))
    expect(severityInput()).toHaveValue('Critical')
  })

  test('reports a failed suggestion and changes nothing', async () => {
    // A failing call must be visible and must not silently wipe the row's classification.
    const user = userEvent.setup()
    requestMock.doRequest.mockResolvedValueOnce(serverErrorResponse)

    renderWithProviders(<ThesisFeedbackRequestButton type='THESIS' />)
    await openModal(user)
    await user.type(screen.getByPlaceholderText(FEEDBACK_PLACEHOLDER), 'Fix the figure caption')
    await user.click(screen.getByRole('button', { name: WAND_LABEL }))

    await waitFor(() =>
      expect(notifyMock.showSimpleError).toHaveBeenCalledWith('Something went wrong'),
    )
    expect(categoryInput()).toHaveValue('')
    expect(severityInput()).toHaveValue('')
  })

  test('reports a suggestion the AI could not make', async () => {
    // An empty body is a successful call with no answer — tell the instructor to pick manually
    // rather than leaving them staring at two untouched dropdowns.
    const user = userEvent.setup()
    requestMock.doRequest.mockResolvedValueOnce(okResponse({}))

    renderWithProviders(<ThesisFeedbackRequestButton type='THESIS' />)
    await openModal(user)
    await user.type(screen.getByPlaceholderText(FEEDBACK_PLACEHOLDER), 'Improve this part')
    await user.click(screen.getByRole('button', { name: WAND_LABEL }))

    await waitFor(() =>
      expect(notifyMock.showSimpleError).toHaveBeenCalledWith(
        'The AI could not classify this entry. Please select the values manually.',
      ),
    )
    expect(categoryInput()).toHaveValue('')
  })
})
