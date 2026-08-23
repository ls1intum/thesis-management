import { test, expect } from '@playwright/test'
import type { Page } from '@playwright/test'
import { authStatePath, navigateToDetail } from '../helpers'

// Thesis d000-0002 is a PROPOSAL-state thesis owned by student2. The seed data already
// attaches a proposal PDF, so the "Get AI Feedback" and "Generate with AI" buttons render
// without needing a prior upload step.
const THESIS_ID = '00000000-0000-4000-d000-000000000002'
const THESIS_URL = `/theses/${THESIS_ID}`
const THESIS_TITLE = 'CI Pipeline Optimization Through Intelligent Test Selection'

// Thesis d000-0001 is a WRITING-state thesis owned by "student"; it has an uploaded
// thesis file in the seed data, so the AI feedback button for THESIS reviews renders.
const WRITING_THESIS_ID = '00000000-0000-4000-d000-000000000001'
const WRITING_THESIS_URL = `/theses/${WRITING_THESIS_ID}`
const WRITING_THESIS_TITLE = 'Automated Code Review Using Large Language Models'

/**
 * Fetch the current thesis using the browser's auth context. Doing this at test level (rather
 * than inside the route handler) avoids the awkward semantics of Playwright's {@link Route.fetch}
 * re-issuing a POST body as a GET, and gives us the full ThesisDto shape the client expects
 * once we append the mocked AI feedback rows.
 */
async function fetchThesis(page: Page, thesisId: string) {
  const response = await page.evaluate(async (id: string) => {
    // window.RUNTIME_ENVIRONMENT_VARIABLES is set by runtime-env.js
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const env = (window as unknown as { RUNTIME_ENVIRONMENT_VARIABLES?: Record<string, string> })
      .RUNTIME_ENVIRONMENT_VARIABLES
    const serverHost = (env?.SERVER_HOST ?? 'http://localhost:8180').replace(/\/+$/, '')
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const tokens = JSON.parse(localStorage.getItem('authentication_tokens') ?? '{}') as {
      access_token?: string
    }
    const res = await fetch(`${serverHost}/api/v2/theses/${id}`, {
      headers: tokens.access_token
        ? { Authorization: `Bearer ${tokens.access_token}` }
        : undefined,
    })
    return { status: res.status, body: await res.text() }
  }, thesisId)

  if (response.status !== 200) {
    throw new Error(`Failed to fetch thesis ${thesisId}: HTTP ${response.status}`)
  }
  return JSON.parse(response.body)
}

/**
 * Install a route mock for the auto AI feedback endpoint. Returns a ThesisDto based on the
 * currently loaded thesis with the given fake AI items appended to `feedback`.
 */
async function mockAutoEndpoint(
  page: Page,
  thesisId: string,
  feedbackItems: Array<{
    feedback: string
    category: string
    severity: string
    type: string
  }>,
) {
  const thesis = await fetchThesis(page, thesisId)
  const now = new Date().toISOString()
  const author = thesis.students?.[0] ?? {
    userId: '00000000-0000-4000-a000-000000000001',
    firstName: 'AI',
    lastName: 'Reviewer',
    universityId: 'ai',
    email: 'ai@test.local',
    avatar: false,
  }
  thesis.feedback = [
    ...(thesis.feedback ?? []),
    ...feedbackItems.map((item, index) => ({
      feedbackId: `aaaaaaaa-0000-4000-8000-00000000000${index + 1}`,
      type: item.type,
      feedback: item.feedback,
      requestedBy: author,
      requestedAt: now,
      completedAt: null,
      category: item.category,
      severity: item.severity,
      generationSource: 'AI',
    })),
  ]

  // The client appends `?` (empty query string) to every request, so a plain
  // `**/v2/ai-review/auto` glob does not match. A regex is unambiguous and cheap.
  await page.route(/\/v2\/ai-review\/auto(\?|$)/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(thesis),
    })
  })
}

test.describe('Proposal AI Feedback — student auto-generates feedback', () => {
  test.use({ storageState: authStatePath('student2') })

  test('student can generate AI feedback for their proposal and see it appear', async ({ page }) => {
    const heading = page.getByRole('heading', { name: THESIS_TITLE })
    await navigateToDetail(page, THESIS_URL, heading)

    // Only set up the mock AFTER the initial thesis load so `fetchThesis` uses the real endpoint.
    await mockAutoEndpoint(page, THESIS_ID, [
      {
        feedback:
          '**Missing abstract** — the proposal must contain an abstract of about half a page. (Page 1, Abstract)',
        category: 'STRUCTURE',
        severity: 'CRITICAL',
        type: 'PROPOSAL',
      },
      {
        feedback:
          '**Insufficient bibliography** — cite at least 6-8 peer-reviewed sources. (Page 5, Bibliography)',
        category: 'CITATION',
        severity: 'MAJOR',
        type: 'PROPOSAL',
      },
    ])

    const aiButton = page.getByRole('button', { name: /Get AI Feedback/i })
    await aiButton.scrollIntoViewIfNeeded()
    await aiButton.click()

    // The button flips to loading and then the feedback table re-renders with the
    // AI-authored rows. Assert on the visible content the AI would produce.
    await expect(page.getByText('Missing abstract').first()).toBeVisible({ timeout: 15_000 })
    await expect(page.getByText('Insufficient bibliography').first()).toBeVisible()

    // AI-authored rows carry a source badge that manual entries do not; the label is "AI".
    const aiBadges = page.getByText('AI', { exact: true })
    await expect(aiBadges.first()).toBeVisible()
  })
})

test.describe('Proposal AI Feedback — supervisor preview flow', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('supervisor can generate AI drafts inside the request-changes dialog and edit them before saving', async ({
    page,
  }) => {
    await page.route(/\/v2\/ai-review\/preview(\?|$)/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          assessment: 'ACCEPTABLE',
          summary:
            'The proposal covers the problem clearly but the bibliography is thin and the schedule needs work.',
          drafts: [
            {
              feedback:
                '**Thin bibliography** — increase to at least 6 peer-reviewed sources. (Page 5, Bibliography)',
              category: 'CITATION',
              severity: 'MAJOR',
            },
            {
              feedback:
                '**Schedule too coarse** — split each iteration into concrete deliverables. (Page 6, Schedule)',
              category: 'STRUCTURE',
              severity: 'MINOR',
            },
          ],
        }),
      })
    })

    const heading = page.getByRole('heading', { name: THESIS_TITLE })
    await navigateToDetail(page, THESIS_URL, heading)

    // Open the request-changes dialog. The button lives in the Proposal section.
    const requestChangesButton = page.getByRole('button', { name: 'Request Changes' }).first()
    await requestChangesButton.scrollIntoViewIfNeeded()
    await requestChangesButton.click()

    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 10_000 })

    // Kick off the AI preview from inside the dialog.
    await dialog.getByRole('button', { name: 'Generate with AI' }).click()

    // The AI assessment banner surfaces the summary the endpoint returned.
    await expect(dialog.getByText(/bibliography is thin/i)).toBeVisible({ timeout: 15_000 })

    // Draft entries are appended as editable rows — verify both showed up and that they
    // are wired to Textarea inputs (not plain text), so the instructor can edit them.
    const entryTextareas = dialog.getByPlaceholder(
      'Describe the change you want the student to make…',
    )
    await expect(entryTextareas).toHaveCount(2, { timeout: 15_000 })
    await expect(entryTextareas.nth(0)).toHaveValue(/Thin bibliography/)
    await expect(entryTextareas.nth(1)).toHaveValue(/Schedule too coarse/)

    // The instructor can edit an AI-generated draft in place before saving.
    await entryTextareas.nth(0).fill('Please expand the bibliography to at least 10 sources.')
    await expect(entryTextareas.nth(0)).toHaveValue(
      'Please expand the bibliography to at least 10 sources.',
    )

    // The instructor can also remove a draft they disagree with. There are two "Remove entry"
    // action icons — one per entry. Removing the second collapses the list to a single entry.
    await dialog.getByRole('button', { name: 'Remove entry' }).nth(1).click()
    await expect(entryTextareas).toHaveCount(1)
  })
})

test.describe('Thesis AI Feedback — student auto-generates thesis feedback', () => {
  test.use({ storageState: authStatePath('student') })

  test('student can generate AI feedback for their thesis draft and see it appear', async ({
    page,
  }) => {
    const heading = page.getByRole('heading', { name: WRITING_THESIS_TITLE })
    await navigateToDetail(page, WRITING_THESIS_URL, heading)

    await mockAutoEndpoint(page, WRITING_THESIS_ID, [
      {
        feedback:
          '**Missing threats to validity** — evaluation chapter must discuss limitations of the study. (Page 42, Evaluation)',
        category: 'METHODOLOGY',
        severity: 'MAJOR',
        type: 'THESIS',
      },
    ])

    const aiButton = page.getByRole('button', { name: /Get AI Feedback/i })
    await aiButton.scrollIntoViewIfNeeded()
    await aiButton.click()

    await expect(page.getByText('Missing threats to validity').first()).toBeVisible({
      timeout: 15_000,
    })
  })
})
