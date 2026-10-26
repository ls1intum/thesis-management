import type { Scene } from '../types'
import {
  dismissPasskeyPrompt,
  expectVisible,
  goto,
  scrollTo,
  settle,
  tryClick,
} from '../helpers'
import { createTestPdfBuffer } from '../../e2e/helpers'

/**
 * Known seed IDs (see server/src/main/resources/db/changelog/manual/
 * seed_dev_test_data.sql). Referenced by scenes that need a specific state —
 * following the same pattern the e2e specs use.
 */
const SEED_THESIS_WRITING = '00000000-0000-4000-d000-000000000001' // 'student' assigned
const SEED_THESIS_PROPOSAL = '00000000-0000-4000-d000-000000000002' // 'student2' assigned, PROPOSAL state
const SEED_OPEN_TOPIC = '00000000-0000-4000-b000-000000000002' // CI Pipeline Optimization
const SEED_INTERVIEW_PROCESS = '00000000-0000-4000-e600-000000000001'

const SERVER_HOST = process.env.SERVER_URL ?? 'http://localhost:8180'

/**
 * The `filename` values match the placeholders spelled out in that guide, so
 * running the capture job produces images that "just work" when dropped into
 * `documentation/static/img/screenshots/`.
 *
 * To add a new student screenshot:
 *   1. Add a placeholder to student-guide.md with the desired filename.
 *   2. Append a `Scene` object here whose `filename` matches.
 *   3. Re-run `./capture-screenshots.sh`.
 */
export const studentScenes: Scene[] = [
  {
    filename: 'student-02-landing-topics',
    description: 'Public landing page with topic grid and filter panel',
    role: 'anonymous',
    run: async (page) => {
      await goto(page, '/')
      // The landing hero uses <Title order={2}> (h2); we anchor on the page
      // title text so future heading-level tweaks don't break this scene.
      await expectVisible(page.getByText(/find a thesis topic/i).first())
      // The view toggle is a Mantine SegmentedControl (radio-group under the
      // hood). Prefer Grid so the shot matches the caption's "topic grid"
      // wording rather than the default compact list.
      await tryClick(page.getByText('Grid', { exact: true }).first())
      await settle(page)
    },
  },
  {
    filename: 'student-03-topic-detail',
    description: 'Detail page of a single open topic with the Apply Now button',
    role: 'student',
    run: async (page) => {
      await goto(page, '/')
      await dismissPasskeyPrompt(page)
      // Open the first topic card we find on the landing page.
      const firstTopicLink = page.locator('a[href^="/topics/"]').first()
      await expectVisible(firstTopicLink)
      await firstTopicLink.click()
      await settle(page)
    },
  },
  {
    filename: 'student-04-application-userinfo',
    description: 'Application wizard — "Your Information" step',
    role: 'student',
    run: async (page) => {
      // The wizard auto-advances to step 2 (Your Information) whenever a
      // `topicId` is in the URL — see ReplaceApplicationPage.tsx:
      //   active={Math.max(step, topicId || applicationId ? 1 : 0)}
      // so we skip the topic-picker by pre-selecting a seeded open topic.
      await goto(page, `/submit-application/${SEED_OPEN_TOPIC}`)
      await dismissPasskeyPrompt(page)
      await expectVisible(page.getByLabel('First Name'))
      await settle(page)
    },
  },
  {
    filename: 'student-05-application-motivation',
    description: 'Application wizard — motivation & final submission step',
    role: 'student',
    run: async (page) => {
      // Enter the wizard on step 2, fill the required uploads + consent, then
      // click "Update Information" — that transitions the stepper to step 3
      // (Motivation). There is no way to reach step 3 via `onStepClick` alone
      // because the wizard blocks forward jumps (see ReplaceApplicationPage
      // updateStep: `if (value > step) return`).
      await goto(page, `/submit-application/${SEED_OPEN_TOPIC}`)
      await dismissPasskeyPrompt(page)
      await expectVisible(page.getByLabel('First Name'))

      const pdfBuffer = createTestPdfBuffer()
      for (const label of ['Examination Report', 'CV']) {
        const wrapper = page.locator(
          `.mantine-InputWrapper-root:has(.mantine-InputWrapper-label:text("${label}"))`,
        )
        const alreadyUploaded = await wrapper
          .locator('iframe, .mantine-Card-root')
          .first()
          .isVisible({ timeout: 2_000 })
          .catch(() => false)
        if (alreadyUploaded) continue

        const fileInput = wrapper.locator('input[type="file"]')
        await fileInput.waitFor({ state: 'attached', timeout: 15_000 })
        await fileInput.setInputFiles({
          name: `${label.toLowerCase().replace(/ /g, '-')}.pdf`,
          mimeType: 'application/pdf',
          buffer: pdfBuffer,
        })
      }

      const privacyCheckbox = page.getByRole('checkbox', { name: /privacy/i })
      if (!(await privacyCheckbox.isChecked())) {
        await privacyCheckbox.check()
      }

      const updateButton = page.getByRole('button', { name: 'Update Information', exact: true })
      await expectVisible(updateButton)
      await updateButton.click()

      // Step 3 (Motivation) is identified by the Thesis Type select.
      await expectVisible(page.getByRole('combobox', { name: /thesis type/i }))
      await settle(page)
    },
  },
  {
    filename: 'student-06-application-list',
    description: 'Dashboard "My Applications" table with mixed state badges',
    role: 'student',
    run: async (page) => {
      await goto(page, '/dashboard')
      await dismissPasskeyPrompt(page)
      const applicationsTable = page.getByRole('heading', { name: /applications/i }).first()
      await scrollTo(applicationsTable)
      await settle(page)
    },
  },
  {
    // Seed process 1 = active topic-3 interview process (anomaly detection).
    // student4 is pre-seeded with a booked slot on this process, so this
    // scene documents the *confirmation* view (Interview Scheduled +
    // topic/slot summary) rather than the pre-booking carousel.
    filename: 'student-07-interview-booking',
    description: 'Interview booking page — confirmation of the currently-booked slot',
    role: 'student4',
    run: async (page) => {
      // InterviewBookingPage waits for `auth.isReady && auth.isAuthenticated`
      // before firing its authenticated fetches, so no more races with
      // Keycloak's async init. Wait for the topic title to render in the
      // Summary panel — that only appears after `/topic` returns 200.
      await goto(page, `/interview_booking/${SEED_INTERVIEW_PROCESS}`)
      await dismissPasskeyPrompt(page)
      await expectVisible(page.getByRole('heading', { name: /interview scheduled/i }))
      await expectVisible(page.getByText(/real-time anomaly detection/i).first())
      await settle(page)
    },
  },
  {
    filename: 'student-08-thesis-header',
    description: 'Thesis page header — title and top-of-page section list',
    role: 'student',
    run: async (page) => {
      // Navigate directly to the seeded thesis. The /theses browse page uses
      // a mantine-datatable with `onRowClick` (no <a> tags), so we jump to the
      // detail page by URL — same pattern the e2e specs use.
      await goto(page, `/theses/${SEED_THESIS_WRITING}`)
      await dismissPasskeyPrompt(page)
      await expectVisible(page.getByRole('button', { name: /proposal/i }).first())
      // Ensure the shot starts at the top of the page, so it captures the
      // title and the first accordion sections rather than a scrolled state
      // inherited from earlier scenes.
      await page.evaluate(() => window.scrollTo(0, 0))
      await settle(page)
    },
  },
  {
    filename: 'student-09-proposal',
    description: 'Proposal section — expanded panel with PDF preview and version history',
    role: 'student',
    run: async (page) => {
      // ThesisProposalSection uses `defaultValue={state === PROPOSAL ? 'open' : ''}`,
      // so for the WRITING-state seed thesis the panel starts collapsed. Open
      // it explicitly, then anchor the shot so the PDF preview + history table
      // are inside the viewport instead of below it.
      await goto(page, `/theses/${SEED_THESIS_WRITING}`)
      await dismissPasskeyPrompt(page)
      const proposalHeading = page.getByRole('button', { name: 'Proposal', exact: true })
      await expectVisible(proposalHeading)
      const alreadyOpen = (await proposalHeading.getAttribute('aria-expanded')) === 'true'
      if (!alreadyOpen) {
        await proposalHeading.click()
      }
      // Panel content only renders after expansion — wait for the file
      // history table's "Uploaded By" column before scrolling.
      await expectVisible(page.getByText('Uploaded By').first())
      await proposalHeading.evaluate((el) => {
        el.scrollIntoView({ block: 'start', behavior: 'instant' })
        // Small offset so the sticky header does not clip the accordion label.
        window.scrollBy(0, -80)
      })
      await settle(page)
    },
  },
  {
    filename: 'student-10-writing',
    description: 'Writing section — file table and orange university-submission reminder',
    role: 'student',
    run: async (page) => {
      // For a WRITING-state thesis, ThesisWritingSection is already expanded
      // (`defaultValue='open'`), so the fix is to anchor the viewport at the
      // outer "Thesis" accordion header. `scrollIntoViewIfNeeded` was a no-op
      // here because the header sat at the bottom edge of the viewport —
      // "partially visible" counts as visible enough to skip the scroll.
      await goto(page, `/theses/${SEED_THESIS_WRITING}`)
      await dismissPasskeyPrompt(page)
      const thesisHeading = page.getByRole('button', { name: 'Thesis', exact: true }).first()
      await expectVisible(thesisHeading)
      await thesisHeading.evaluate((el) => {
        el.scrollIntoView({ block: 'start', behavior: 'instant' })
        window.scrollBy(0, -80)
      })
      await settle(page)
    },
  },
  {
    filename: 'student-11-feedback',
    description: 'Proposal feedback overview (student view) — thesis 1 has one completed row',
    role: 'student',
    run: async (page) => {
      await goto(page, `/theses/${SEED_THESIS_WRITING}`)
      await dismissPasskeyPrompt(page)
      const proposalHeading = page.getByRole('button', { name: 'Proposal', exact: true })
      await expectVisible(proposalHeading)
      const alreadyOpen = (await proposalHeading.getAttribute('aria-expanded')) === 'true'
      if (!alreadyOpen) {
        await proposalHeading.click()
      }
      // Feedback overview lives inside the Proposal panel — wait for a row
      // in the "Requested Change" table before scrolling to it.
      const feedbackHeading = page.getByText('Requested Change').first()
      await expectVisible(feedbackHeading)
      await feedbackHeading.evaluate((el) => {
        el.scrollIntoView({ block: 'start', behavior: 'instant' })
        window.scrollBy(0, -120)
      })
      await settle(page)
    },
  },
  {
    filename: 'student-17-ai-feedback',
    description:
      'Proposal feedback overview after clicking "Get AI Feedback" — rows carry AI, severity, category, and version badges',
    role: 'student2',
    run: async (page) => {
      // Navigate first so localStorage is reachable (Playwright starts on about:blank where
      // storage access throws) and the initial GET /theses/{id} populates our snapshot.
      await goto(page, `/theses/${SEED_THESIS_PROPOSAL}`)
      await dismissPasskeyPrompt(page)

      const proposalId = '00000000-0000-4000-e000-000000000002' // Seed proposal for thesis 2
      const thesis = await page.evaluate(
        async ({ thesisId, host }: { thesisId: string; host: string }) => {
          const tokens = JSON.parse(localStorage.getItem('authentication_tokens') ?? '{}') as {
            access_token?: string
          }
          const res = await fetch(`${host}/api/v2/theses/${thesisId}`, {
            headers: tokens.access_token
              ? { Authorization: `Bearer ${tokens.access_token}` }
              : undefined,
          })
          return await res.json()
        },
        { thesisId: SEED_THESIS_PROPOSAL, host: SERVER_HOST },
      )

      const now = new Date().toISOString()
      const author = thesis.students?.[0]
      thesis.feedback = [
        ...(thesis.feedback ?? []),
        {
          feedbackId: 'aaaaaaaa-0000-4000-8000-000000000001',
          type: 'PROPOSAL',
          feedback:
            '**Missing abstract** — the proposal must contain an abstract of about half a page. (Page 1, Abstract)',
          requestedBy: author,
          requestedAt: now,
          completedAt: null,
          category: 'STRUCTURE',
          severity: 'CRITICAL',
          generationSource: 'AI',
          documentVersionId: proposalId,
        },
        {
          feedbackId: 'aaaaaaaa-0000-4000-8000-000000000002',
          type: 'PROPOSAL',
          feedback:
            '**Insufficient bibliography** — cite at least 6-8 peer-reviewed sources. (Page 5, Bibliography)',
          requestedBy: author,
          requestedAt: now,
          completedAt: null,
          category: 'CITATION',
          severity: 'MAJOR',
          generationSource: 'AI',
          documentVersionId: proposalId,
        },
      ]

      await page.route(/\/v2\/ai-review\/auto(\?|$)/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(thesis),
        })
      })

      const proposalHeading = page.getByRole('button', { name: 'Proposal', exact: true })
      await expectVisible(proposalHeading)
      const alreadyOpen = (await proposalHeading.getAttribute('aria-expanded')) === 'true'
      if (!alreadyOpen) {
        await proposalHeading.click()
      }
      const aiButton = page.getByRole('button', { name: /Get AI Feedback/i })
      await expectVisible(aiButton)
      await aiButton.click()
      // Wait for the AI-authored rows to land in the table.
      await expectVisible(page.getByText('Missing abstract').first())
      const feedbackHeading = page.getByText('Requested Change').first()
      await feedbackHeading.evaluate((el) => {
        el.scrollIntoView({ block: 'start', behavior: 'instant' })
        window.scrollBy(0, -120)
      })
      await settle(page)
    },
  },
  {
    filename: 'student-12-presentation-draft',
    description: '"Create Presentation Draft" modal, opened from the thesis page',
    role: 'student',
    run: async (page) => {
      // Presentation accordion is `defaultValue='open'` — don't toggle it,
      // or the "Create Presentation Draft" button (inside the panel) disappears.
      await goto(page, `/theses/${SEED_THESIS_WRITING}`)
      const draftButton = page.getByRole('button', { name: /create presentation draft/i })
      await expectVisible(draftButton)
      await draftButton.click()
      await expectVisible(page.getByRole('dialog'))
      await settle(page)
    },
  },
  {
    filename: 'student-14-overview-gantt',
    description: 'Thesis overview Gantt for a student with a single thesis',
    role: 'student',
    run: async (page) => {
      await goto(page, '/overview')
      await dismissPasskeyPrompt(page)
      await settle(page)
    },
  },
  {
    filename: 'student-15-settings-notifications',
    description: 'Settings — Notification Settings tab (student view)',
    role: 'student',
    run: async (page) => {
      await goto(page, '/settings/notifications')
      await dismissPasskeyPrompt(page)
      await settle(page)
    },
  },
  {
    filename: 'student-16-data-export',
    description: 'Data Export page with the "Request Data Export" button',
    role: 'student',
    run: async (page) => {
      await goto(page, '/data-export')
      await dismissPasskeyPrompt(page)
      await settle(page)
    },
  },
]
