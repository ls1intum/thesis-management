import type { Scene } from '../types'
import {
  dismissPasskeyPrompt,
  expectVisible,
  goto,
  scrollTo,
  settle,
  tryClick,
} from '../helpers'

/**
 * Seed thesis IDs used by staff scenes. See
 * server/src/main/resources/db/changelog/manual/seed_dev_test_data.sql for
 * the full list; both theses below have `examiner` assigned as Examiner.
 */
const SEED_THESIS_WRITING = '00000000-0000-4000-d000-000000000001'
const SEED_THESIS_PROPOSAL = '00000000-0000-4000-d000-000000000002'
const SEED_THESIS_ASSESSED = '00000000-0000-4000-d000-000000000003' // examiner2 assigned
const SEED_THESIS_FINISHED = '00000000-0000-4000-d000-000000000004' // examiner assigned; has final grade
const SEED_THESIS_DROPPED = '00000000-0000-4000-d000-000000000005' // examiner2 assigned; has DRAFTED presentation

const SEED_INTERVIEW_PROCESS = '00000000-0000-4000-e600-000000000001' // topic 3, active
const SEED_INTERVIEWEE = '00000000-0000-4000-e700-000000000005' // student3, assessed

const SEED_RESEARCH_GROUP_ASE = '00000000-0000-4000-a000-000000000001'

/**
 * We capture most staff scenes as the `examiner` user because they see the
 * superset of features (dashboard + final grade + everything a supervisor
 * sees). Individual scenes may override the role if a screenshot must depict
 * a strictly supervisor-only or examiner-only view.
 */
export const staffScenes: Scene[] = [
  {
    filename: 'staff-01-dashboard',
    description: 'Examiner dashboard — My Tasks, My Theses, open applications',
    role: 'examiner',
    run: async (page) => {
      await goto(page, '/dashboard')
      await dismissPasskeyPrompt(page)
      await settle(page)
    },
  },
  {
    filename: 'staff-02-topic-create',
    description: '"Create Topic" modal with the empty topic form',
    role: 'examiner',
    run: async (page) => {
      await goto(page, '/topics')
      await expectVisible(page.getByRole('heading', { name: /manage topics/i }))
      const createButton = page.getByRole('button', { name: 'Create Topic', exact: true })
      await expectVisible(createButton)
      await createButton.click()
      await expectVisible(page.getByRole('dialog'))
      await settle(page)
    },
  },
  {
    filename: 'staff-03-topics-list',
    description: 'Manage Topics page with state filter and topic rows',
    role: 'examiner',
    run: async (page) => {
      await goto(page, '/topics')
      await expectVisible(page.getByRole('heading', { name: /manage topics/i }))
      await settle(page)
    },
  },
  {
    filename: 'staff-04-application-review',
    description: 'Application review page — sidebar + application detail pane',
    role: 'examiner',
    run: async (page) => {
      await goto(page, '/applications')
      await dismissPasskeyPrompt(page)
      // Click the first application in the sidebar if one exists.
      const firstApplication = page
        .locator('a[href^="/applications/"], [role="listitem"] a')
        .first()
      await tryClick(firstApplication, 5_000)
      await settle(page)
    },
  },
  {
    filename: 'staff-05-interview-new',
    description: '"New Interview Process" modal',
    role: 'examiner',
    run: async (page) => {
      await goto(page, '/interviews')
      await dismissPasskeyPrompt(page)
      const newButton = page.getByRole('button', { name: /new interview process/i }).first()
      await expectVisible(newButton)
      await newButton.click()
      await expectVisible(page.getByRole('dialog'))
      await settle(page)
    },
  },
  {
    filename: 'staff-06-interview-process',
    description: 'Interview process detail — interviewees + slot calendar',
    role: 'examiner2',
    run: async (page) => {
      // Navigate directly to the seeded active process for topic 3 (anomaly
      // detection). examiner2 owns that topic.
      await goto(page, `/interviews/${SEED_INTERVIEW_PROCESS}`)
      await dismissPasskeyPrompt(page)
      await settle(page)
    },
  },
  {
    filename: 'staff-07-interview-assessment',
    description: 'Interviewee assessment page — score input + notes editor',
    role: 'examiner2',
    run: async (page) => {
      await goto(
        page,
        `/interviews/${SEED_INTERVIEW_PROCESS}/interviewee/${SEED_INTERVIEWEE}`,
      )
      await dismissPasskeyPrompt(page)
      await settle(page)
    },
  },
  {
    filename: 'staff-08-thesis-config',
    description: 'Thesis Config accordion expanded (staff view)',
    role: 'examiner',
    run: async (page) => {
      // Direct navigation — the /theses table uses onRowClick, not <a> tags.
      await goto(page, `/theses/${SEED_THESIS_WRITING}`)
      const configHeading = page.getByRole('button', { name: 'Configuration', exact: true })
      await expectVisible(configHeading)
      await tryClick(configHeading)
      await scrollTo(configHeading)
      await settle(page)
    },
  },
  {
    filename: 'staff-09-proposal',
    description: 'Proposal review with Request Changes / Accept Proposal buttons',
    role: 'examiner',
    run: async (page) => {
      // Use the seeded PROPOSAL-state thesis so Accept / Request-Changes are
      // both visible and enabled.
      await goto(page, `/theses/${SEED_THESIS_PROPOSAL}`)
      const proposalHeading = page.getByRole('button', { name: 'Proposal', exact: true })
      await expectVisible(proposalHeading)
      await tryClick(proposalHeading)
      await scrollTo(proposalHeading)
      await settle(page)
    },
  },
  {
    filename: 'staff-10-presentation-approval',
    description: 'Presentation card in DRAFTED state (thesis 5 has a drafted presentation)',
    role: 'examiner2',
    run: async (page) => {
      await goto(page, `/theses/${SEED_THESIS_DROPPED}`)
      const presentationHeading = page.getByRole('button', { name: 'Presentation', exact: true })
      await expectVisible(presentationHeading)
      await tryClick(presentationHeading)
      await scrollTo(presentationHeading)
      await settle(page)
    },
  },
  {
    filename: 'staff-11-assessment',
    description: 'Assessment section on thesis 3 — summary, strengths, weaknesses, grade components',
    role: 'examiner2',
    run: async (page) => {
      // We aim for the *read* view of a saved assessment, not the Edit modal:
      // the modal-only path is fragile because e2e runs can advance thesis 3
      // to FINISHED, at which point `isThesisClosed()` hides the Edit button.
      // The read view surfaces the same content the guide caption describes.
      await goto(page, `/theses/${SEED_THESIS_ASSESSED}`)
      const summaryLabel = page.getByText('Summary').first()
      await expectVisible(summaryLabel)
      await scrollTo(summaryLabel)
      await settle(page)
    },
  },
  {
    filename: 'staff-12-final-grade',
    description: 'Final Grade section on thesis 4 (FINISHED, examiner view)',
    role: 'examiner',
    run: async (page) => {
      await goto(page, `/theses/${SEED_THESIS_FINISHED}`)
      const finalGradeHeading = page.getByRole('button', { name: 'Final Grade', exact: true })
      await expectVisible(finalGradeHeading)
      await tryClick(finalGradeHeading)
      await scrollTo(finalGradeHeading)
      await settle(page)
    },
  },
  {
    filename: 'staff-13-overview-gantt',
    description: 'Overview Gantt with several theses (staff view)',
    role: 'examiner',
    run: async (page) => {
      await goto(page, '/overview')
      await dismissPasskeyPrompt(page)
      await settle(page)
    },
  },
  {
    filename: 'staff-14-presentations-overview',
    description: 'Presentations calendar carousel + upcoming list',
    role: 'examiner',
    run: async (page) => {
      await goto(page, '/presentations')
      await dismissPasskeyPrompt(page)
      await settle(page)
    },
  },
  {
    filename: 'staff-15-feedback',
    description: 'Feedback overview table (staff view) — thesis 2 has a pending row',
    role: 'examiner',
    run: async (page) => {
      await goto(page, `/theses/${SEED_THESIS_PROPOSAL}`)
      const proposalHeading = page.getByRole('button', { name: 'Proposal', exact: true })
      await expectVisible(proposalHeading)
      await tryClick(proposalHeading)
      const feedbackAnchor = page.getByText(/feedback/i).first()
      await scrollTo(feedbackAnchor)
      await settle(page)
    },
  },
  {
    filename: 'staff-16-notifications',
    description: 'Settings — staff notification options + per-thesis table',
    role: 'examiner',
    run: async (page) => {
      await goto(page, '/settings/notifications')
      await dismissPasskeyPrompt(page)
      await settle(page)
    },
  },
  {
    filename: 'staff-18-request-changes-dialog',
    description:
      'Redesigned Request Changes dialog — structured entry with category/severity dropdowns and the "Generate with AI" button',
    role: 'supervisor',
    run: async (page) => {
      await goto(page, `/theses/${SEED_THESIS_PROPOSAL}`)
      const proposalHeading = page.getByRole('button', { name: 'Proposal', exact: true })
      await expectVisible(proposalHeading)
      // Only click when the accordion is collapsed — thesis 2 is PROPOSAL-state so the panel
      // is already open on load, and toggling it would hide the Request Changes button.
      const proposalExpanded = (await proposalHeading.getAttribute('aria-expanded')) === 'true'
      if (!proposalExpanded) {
        await proposalHeading.click()
      }
      const requestChangesButton = page.getByRole('button', { name: 'Request Changes' }).first()
      await expectVisible(requestChangesButton)
      await requestChangesButton.click()
      const dialog = page.getByRole('dialog')
      await expectVisible(dialog)
      await expectVisible(
        dialog.getByPlaceholder('Describe the change you want the student to make…').first(),
      )
      await settle(page)
    },
  },
  {
    filename: 'staff-19-request-changes-ai-drafts',
    description:
      'Request Changes dialog after clicking "Generate with AI" — assessment banner + editable AI-authored draft entries',
    role: 'supervisor',
    run: async (page) => {
      // Mock the preview endpoint so the shot never depends on a reachable LLM. Response
      // shape mirrors AIPreviewResponseDTO.
      await page.route(/\/v2\/ai-review\/preview(\?|$)/, async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            assessment: 'ACCEPTABLE',
            summary:
              'The proposal presents the problem clearly, but the bibliography is thin and the schedule needs more concrete deliverables.',
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

      await goto(page, `/theses/${SEED_THESIS_PROPOSAL}`)
      const proposalHeading = page.getByRole('button', { name: 'Proposal', exact: true })
      await expectVisible(proposalHeading)
      // Only click when the accordion is collapsed — thesis 2 is PROPOSAL-state so the panel
      // is already open on load, and toggling it would hide the Request Changes button.
      const proposalExpanded = (await proposalHeading.getAttribute('aria-expanded')) === 'true'
      if (!proposalExpanded) {
        await proposalHeading.click()
      }
      const requestChangesButton = page.getByRole('button', { name: 'Request Changes' }).first()
      await expectVisible(requestChangesButton)
      await requestChangesButton.click()
      const dialog = page.getByRole('dialog')
      await expectVisible(dialog)
      await dialog.getByRole('button', { name: 'Generate with AI' }).click()
      await expectVisible(dialog.getByText(/bibliography is thin/i))
      await settle(page)
    },
  },
  {
    filename: 'staff-17-group-settings',
    description: 'Research-group settings page (admin view)',
    role: 'admin',
    run: async (page) => {
      await goto(page, `/research-groups/${SEED_RESEARCH_GROUP_ASE}`)
      await dismissPasskeyPrompt(page)
      await settle(page)
    },
  },
]
