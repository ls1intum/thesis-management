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
  showSimpleSuccess: vi.fn(),
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
const NOTES_LABEL = 'Your notes'
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

  test('drops a suggestion for text the instructor has since rewritten', async () => {
    // The labels describe the text that was sent. Applying them to a rewritten entry would put
    // the old text's verdict on the new text, which is worse than leaving it unlabelled.
    const user = userEvent.setup()
    let resolveClassification: (value: unknown) => void = () => {}
    requestMock.doRequest.mockReturnValueOnce(
      new Promise((resolve) => {
        resolveClassification = resolve
      }),
    )

    renderWithProviders(<ThesisFeedbackRequestButton type='THESIS' />)
    await openModal(user)
    const feedback = screen.getByPlaceholderText(FEEDBACK_PLACEHOLDER)
    await user.type(feedback, 'Cite the original paper')
    await user.click(screen.getByRole('button', { name: WAND_LABEL }))

    await user.clear(feedback)
    await user.type(feedback, 'Figure 2 has no caption')
    resolveClassification(okResponse({ category: 'CITATION', severity: 'MAJOR' }))

    await waitFor(() =>
      expect(notifyMock.showSimpleError).toHaveBeenCalledWith(
        'You changed this entry while the AI was classifying it, so the suggestion was discarded.',
      ),
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

// "Import notes" turns the notes an instructor took offline into one entry per issue. Line breaks
// in such notes carry no meaning, so the split is the server's job — the client only sends the
// blob and appends whatever entries come back, labelled or not.
describe('ThesisFeedbackRequestButton — note import', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    handlerMock.getApiResponseErrorMessage.mockReturnValue('Something went wrong')
  })

  const openImportPanel = async (user: ReturnType<typeof userEvent.setup>) => {
    await openModal(user)
    await user.click(screen.getByRole('button', { name: 'Import notes' }))
    return screen.findByLabelText(NOTES_LABEL)
  }

  test('appends one entry per issue the split reports', async () => {
    // Two notes on one line become two rows — the whole point of importing rather than pasting
    // the block into a single entry.
    const user = userEvent.setup()
    requestMock.doRequest.mockResolvedValueOnce(
      okResponse({
        entries: [
          { feedback: 'Figure 3 is unreadable.', category: 'FIGURES', severity: 'MAJOR' },
          { feedback: 'Cite Smith for this claim.', category: 'CITATION', severity: 'MAJOR' },
        ],
      }),
    )

    renderWithProviders(<ThesisFeedbackRequestButton type='THESIS' />)
    const notes = await openImportPanel(user)
    await user.type(notes, 'fig 3 unreadable, and Smith not cited')
    await user.click(screen.getByRole('button', { name: 'Import' }))

    expect(requestMock.doRequest).toHaveBeenCalledWith(
      '/v2/ai-review/import-notes',
      expect.objectContaining({
        method: 'POST',
        data: { thesisId: 'thesis-1', notes: 'fig 3 unreadable, and Smith not cited' },
      }),
    )

    const textareas = await screen.findAllByPlaceholderText(FEEDBACK_PLACEHOLDER)
    expect(textareas).toHaveLength(2)
    expect(textareas[0]).toHaveValue('Figure 3 is unreadable.')
    expect(textareas[1]).toHaveValue('Cite Smith for this claim.')
    expect(notifyMock.showSimpleSuccess).toHaveBeenCalledWith('Added 2 entries from your notes.')
  })

  test('leaves the dropdowns open on an entry the split could not label', async () => {
    // A terse note often does not say enough to judge it; the wand or "Classify all" fills those
    // in afterwards, so importing must not guess on the instructor's behalf.
    const user = userEvent.setup()
    requestMock.doRequest.mockResolvedValueOnce(
      okResponse({ entries: [{ feedback: 'Chapter 4 is thin.' }] }),
    )

    renderWithProviders(<ThesisFeedbackRequestButton type='THESIS' />)
    const notes = await openImportPanel(user)
    await user.type(notes, 'ch 4 thin')
    await user.click(screen.getByRole('button', { name: 'Import' }))

    await waitFor(() =>
      expect(screen.getByPlaceholderText(FEEDBACK_PLACEHOLDER)).toHaveValue('Chapter 4 is thin.'),
    )
    expect(categoryInput()).toHaveValue('')
    expect(severityInput()).toHaveValue('')
  })

  test('keeps the panel open and reports notes that held no feedback', async () => {
    // An empty result is not an error the instructor can act on by retrying; leave their notes in
    // place so they can edit them instead of retyping.
    const user = userEvent.setup()
    requestMock.doRequest.mockResolvedValueOnce(okResponse({}))

    renderWithProviders(<ThesisFeedbackRequestButton type='THESIS' />)
    const notes = await openImportPanel(user)
    await user.type(notes, 'looks good')
    await user.click(screen.getByRole('button', { name: 'Import' }))

    await waitFor(() =>
      expect(notifyMock.showSimpleError).toHaveBeenCalledWith(
        'No feedback entries were found in these notes.',
      ),
    )
    expect(screen.getByLabelText(NOTES_LABEL)).toHaveValue('looks good')
  })

  test('raises the unsaved-changes prompt over the import panel', async () => {
    // Closing the modal with notes typed must not look like a dead button: the prompt replaces the
    // panel rather than rendering behind it.
    const user = userEvent.setup()
    renderWithProviders(<ThesisFeedbackRequestButton type='THESIS' />)
    const notes = await openImportPanel(user)
    await user.type(notes, 'ch 4 thin')
    await user.keyboard('{Escape}')

    expect(await screen.findByText('Unsaved changes')).toBeInTheDocument()
    expect(screen.queryByLabelText(NOTES_LABEL)).not.toBeInTheDocument()

    // Keeping the notes returns to the panel with the text still there.
    await user.click(screen.getByRole('button', { name: 'Keep editing' }))
    expect(await screen.findByLabelText(NOTES_LABEL)).toHaveValue('ch 4 thin')
  })

  test('refuses to save while notes are still waiting to be imported', async () => {
    // Saving writes entries, not notes: with notes still in the panel it would discard them, and
    // with nothing but notes it would send an empty change request.
    const user = userEvent.setup()
    renderWithProviders(<ThesisFeedbackRequestButton type='THESIS' />)
    const notes = await openImportPanel(user)
    await user.type(notes, 'ch 4 thin')
    await user.keyboard('{Escape}')

    expect(await screen.findByText(/have not been imported yet/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Save & close' })).toBeDisabled()
  })

  test('ignores an import that answers after the panel was cancelled', async () => {
    // Appending entries — or clearing the field — after the instructor walked away would undo a
    // deliberate cancellation.
    const user = userEvent.setup()
    let resolveImport: (value: unknown) => void = () => {}
    requestMock.doRequest.mockReturnValueOnce(
      new Promise((resolve) => {
        resolveImport = resolve
      }),
    )

    renderWithProviders(<ThesisFeedbackRequestButton type='THESIS' />)
    const notes = await openImportPanel(user)
    await user.type(notes, 'ch 4 thin')
    await user.click(screen.getByRole('button', { name: 'Import' }))
    await user.click(screen.getByRole('button', { name: 'Cancel' }))

    resolveImport(okResponse({ entries: [{ feedback: 'Chapter 4 is thin.' }] }))

    // The panel is gone and the batch still holds nothing but its empty starter row.
    await waitFor(() => expect(screen.queryByLabelText(NOTES_LABEL)).not.toBeInTheDocument())
    expect(screen.getAllByPlaceholderText(FEEDBACK_PLACEHOLDER)).toHaveLength(1)
    expect(screen.getByPlaceholderText(FEEDBACK_PLACEHOLDER)).toHaveValue('')
    expect(notifyMock.showSimpleSuccess).not.toHaveBeenCalled()
  })

  test('locks the notes field while the split is running', async () => {
    // The entries that come back describe the text that was sent, so edits made in the meantime
    // would silently be thrown away with it.
    const user = userEvent.setup()
    requestMock.doRequest.mockReturnValueOnce(new Promise(() => {}))

    renderWithProviders(<ThesisFeedbackRequestButton type='THESIS' />)
    const notes = await openImportPanel(user)
    await user.type(notes, 'ch 4 thin')
    await user.click(screen.getByRole('button', { name: 'Import' }))

    await waitFor(() => expect(screen.getByLabelText(NOTES_LABEL)).toBeDisabled())
  })

  test('does not import an empty note block', async () => {
    // Splitting nothing would spend an LLM call to produce nothing.
    const user = userEvent.setup()
    renderWithProviders(<ThesisFeedbackRequestButton type='THESIS' />)
    await openImportPanel(user)

    expect(screen.getByRole('button', { name: 'Import' })).toBeDisabled()
    expect(requestMock.doRequest).not.toHaveBeenCalled()
  })
})

// "Classify all" is the bulk form of the wand: it labels every entry that still misses a category
// or a severity, which is what a freshly imported reading pass looks like.
describe('ThesisFeedbackRequestButton — bulk classification', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    handlerMock.getApiResponseErrorMessage.mockReturnValue('Something went wrong')
  })

  const importTwoUnlabelledEntries = async (user: ReturnType<typeof userEvent.setup>) => {
    requestMock.doRequest.mockResolvedValueOnce(
      okResponse({
        entries: [{ feedback: 'Figure 3 is unreadable.' }, { feedback: 'Chapter 4 is thin.' }],
      }),
    )

    await openModal(user)
    await user.click(screen.getByRole('button', { name: 'Import notes' }))
    await user.type(await screen.findByLabelText(NOTES_LABEL), 'fig 3; ch 4 thin')
    await user.click(screen.getByRole('button', { name: 'Import' }))
    await screen.findAllByPlaceholderText(FEEDBACK_PLACEHOLDER)
  }

  test('stays disabled while every entry is already labelled', async () => {
    // Nothing to do, so the button must not invite a round of pointless LLM calls.
    const user = userEvent.setup()
    requestMock.doRequest.mockResolvedValueOnce(
      okResponse({
        entries: [{ feedback: 'Cite Smith.', category: 'CITATION', severity: 'MAJOR' }],
      }),
    )

    renderWithProviders(<ThesisFeedbackRequestButton type='THESIS' />)
    await openModal(user)
    await user.click(screen.getByRole('button', { name: 'Import notes' }))
    await user.type(await screen.findByLabelText(NOTES_LABEL), 'cite Smith')
    await user.click(screen.getByRole('button', { name: 'Import' }))
    await screen.findByDisplayValue('Cite Smith.')

    expect(screen.getByRole('button', { name: 'Classify all' })).toBeDisabled()
  })

  test('classifies every unlabelled entry in one click', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ThesisFeedbackRequestButton type='THESIS' />)
    await importTwoUnlabelledEntries(user)

    requestMock.doRequest
      .mockResolvedValueOnce(okResponse({ category: 'FIGURES', severity: 'MAJOR' }))
      .mockResolvedValueOnce(okResponse({ category: 'COMPLETENESS', severity: 'CRITICAL' }))

    await user.click(screen.getByRole('button', { name: 'Classify all' }))

    await waitFor(() => {
      const categories = screen.getAllByRole('combobox', { name: 'Category' })
      expect(categories[0]).toHaveValue('Figures')
      expect(categories[1]).toHaveValue('Completeness')
    })
    // One call per entry, each with that entry's own text.
    expect(requestMock.doRequest).toHaveBeenCalledWith(
      '/v2/ai-review/classify-feedback',
      expect.objectContaining({ data: { thesisId: 'thesis-1', feedback: 'Chapter 4 is thin.' } }),
    )
  })

  test('fills only the empty dropdown and leaves the instructor’s own pick alone', async () => {
    // A row enters the sweep for its missing label; the one it already carries was chosen by the
    // instructor. A button that labels what is missing must not quietly revise what is there.
    const user = userEvent.setup()
    requestMock.doRequest.mockResolvedValueOnce(
      okResponse({ entries: [{ feedback: 'Figure 3 is unreadable.' }] }),
    )

    renderWithProviders(<ThesisFeedbackRequestButton type='THESIS' />)
    await openModal(user)
    await user.click(screen.getByRole('button', { name: 'Import notes' }))
    await user.type(await screen.findByLabelText(NOTES_LABEL), 'fig 3')
    await user.click(screen.getByRole('button', { name: 'Import' }))
    await screen.findByDisplayValue('Figure 3 is unreadable.')

    await user.click(categoryInput())
    await user.click(await screen.findByText('Structure'))

    requestMock.doRequest.mockResolvedValueOnce(
      okResponse({ category: 'FIGURES', severity: 'MAJOR' }),
    )
    await user.click(screen.getByRole('button', { name: 'Classify all' }))

    await waitFor(() => expect(severityInput()).toHaveValue('Major'))
    expect(categoryInput()).toHaveValue('Structure')
  })

  test('reports a row whose only missing label the AI did not answer', async () => {
    // The response filled nothing the row was swept up for, so the severity dropdown is still
    // empty — reporting that as classified would tell the instructor a job was done that was not.
    const user = userEvent.setup()
    requestMock.doRequest.mockResolvedValueOnce(
      okResponse({ entries: [{ feedback: 'Figure 3 is unreadable.' }] }),
    )

    renderWithProviders(<ThesisFeedbackRequestButton type='THESIS' />)
    await openModal(user)
    await user.click(screen.getByRole('button', { name: 'Import notes' }))
    await user.type(await screen.findByLabelText(NOTES_LABEL), 'fig 3')
    await user.click(screen.getByRole('button', { name: 'Import' }))
    await screen.findByDisplayValue('Figure 3 is unreadable.')

    await user.click(categoryInput())
    await user.click(await screen.findByText('Structure'))

    // Only a category comes back, and that is the field the instructor already filled.
    requestMock.doRequest.mockResolvedValueOnce(okResponse({ category: 'FIGURES' }))
    await user.click(screen.getByRole('button', { name: 'Classify all' }))

    await waitFor(() =>
      expect(notifyMock.showSimpleError).toHaveBeenCalledWith(
        'The AI could not classify this entry. Please select the values manually.',
      ),
    )
    expect(categoryInput()).toHaveValue('Structure')
    expect(severityInput()).toHaveValue('')
  })

  test('skips an entry rewritten mid-run without counting it as a failure', async () => {
    // The instructor edited that row on purpose; labelling it from the old text would be wrong,
    // and calling it unclassifiable would be a lie.
    const user = userEvent.setup()
    renderWithProviders(<ThesisFeedbackRequestButton type='THESIS' />)
    await importTwoUnlabelledEntries(user)

    let resolveSecond: (value: unknown) => void = () => {}
    requestMock.doRequest
      .mockResolvedValueOnce(okResponse({ category: 'FIGURES', severity: 'MAJOR' }))
      .mockReturnValueOnce(
        new Promise((resolve) => {
          resolveSecond = resolve
        }),
      )

    await user.click(screen.getByRole('button', { name: 'Classify all' }))

    const textareas = screen.getAllByPlaceholderText(FEEDBACK_PLACEHOLDER)
    await user.clear(textareas[1])
    // paste instead of type: typing ~40 characters one by one exceeds the 5s test timeout on slow CI runners
    await user.click(textareas[1])
    await user.paste('Chapter 4 needs a related work section')
    resolveSecond(okResponse({ category: 'COMPLETENESS', severity: 'CRITICAL' }))

    // Wait for that response to have been handled — its row's wand goes idle again — so the
    // assertions below cannot pass simply by running before it lands.
    await waitFor(() =>
      expect(screen.getAllByRole('button', { name: WAND_LABEL })[1]).not.toBeDisabled(),
    )

    // The untouched row takes its labels; the rewritten one keeps its dropdowns open.
    const categories = screen.getAllByRole('combobox', { name: 'Category' })
    expect(categories[0]).toHaveValue('Figures')
    expect(categories[1]).toHaveValue('')
    expect(notifyMock.showSimpleError).not.toHaveBeenCalled()
  })

  test('reports partial failures once rather than per entry', async () => {
    // A notification per failed row would bury the modal under toasts.
    const user = userEvent.setup()
    renderWithProviders(<ThesisFeedbackRequestButton type='THESIS' />)
    await importTwoUnlabelledEntries(user)

    requestMock.doRequest
      .mockResolvedValueOnce(okResponse({ category: 'FIGURES', severity: 'MAJOR' }))
      .mockResolvedValueOnce(serverErrorResponse)

    await user.click(screen.getByRole('button', { name: 'Classify all' }))

    await waitFor(() =>
      expect(notifyMock.showSimpleError).toHaveBeenCalledWith(
        'The AI could not classify 1 of 2 entries. Please select those values manually.',
      ),
    )
    expect(notifyMock.showSimpleError).toHaveBeenCalledTimes(1)
  })
})
