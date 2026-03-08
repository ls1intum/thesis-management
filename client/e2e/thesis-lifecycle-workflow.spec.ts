import { test, expect } from '@playwright/test'
import { authStatePath, createTestPdfBuffer, expandAccordion, navigateTo } from './helpers'
import {
  snapshotMailbox,
  waitForNewMessages,
  getSubject,
  getBody,
  getToAddresses,
  assertSentFromApp,
} from './mailpit'

// Thesis 13: PROPOSAL state, student4, supervisor2/examiner (ASE)
const THESIS_13_URL = '/theses/00000000-0000-4000-d000-000000000013'
const THESIS_13_TITLE = 'E2E Gap1: Proposal Acceptance Test Thesis'

// Thesis 14: WRITING state, student5, supervisor/examiner (ASE), has thesis file uploaded
const THESIS_14_URL = '/theses/00000000-0000-4000-d000-000000000014'
const THESIS_14_TITLE = 'E2E Gap1: Final Submission Test Thesis'

// Thesis 15: WRITING state, student3, supervisor/examiner (ASE)
const THESIS_15_URL = '/theses/00000000-0000-4000-d000-000000000015'
const THESIS_15_TITLE = 'E2E Gap1: Close Thesis Test'

test.describe('Thesis Lifecycle - Accept Proposal', () => {
  test('supervisor2 can accept proposal (PROPOSAL -> WRITING)', async ({ browser }) => {
    test.setTimeout(90_000)

    const context = await browser.newContext({ storageState: authStatePath('supervisor2') })
    const page = await context.newPage()

    try {
      await navigateTo(page, THESIS_13_URL)
      await expect(page.getByRole('heading', { name: THESIS_13_TITLE })).toBeVisible({
        timeout: 15_000,
      })

      // Expand the Proposal accordion to find the Accept button
      await expandAccordion(page, 'Proposal', page.getByRole('button', { name: 'Accept Proposal' }))

      // Verify the "Accept Proposal" button is visible
      const acceptButton = page.getByRole('button', { name: 'Accept Proposal' })
      await expect(acceptButton).toBeVisible()

      // Snapshot mailbox BEFORE accepting
      const beforeIds = await snapshotMailbox('student4@test.local')

      // Click "Accept Proposal"
      await acceptButton.click()

      // Verify confirmation dialog opens with correct title and text
      const dialog = page.getByRole('dialog')
      await expect(dialog).toBeVisible({ timeout: 10_000 })
      await expect(dialog.getByRole('heading', { name: 'Accept Proposal', exact: true })).toBeVisible()
      await expect(dialog.getByText(/accept the proposal/i)).toBeVisible()

      // Verify Cancel and Confirm buttons are present
      await expect(dialog.getByRole('button', { name: 'Cancel' })).toBeVisible()
      await expect(dialog.getByRole('button', { name: 'Confirm' })).toBeVisible()

      // Confirm the action
      await dialog.getByRole('button', { name: 'Confirm' }).click()

      // Verify success notification
      await expect(page.getByText('Proposal accepted successfully')).toBeVisible({
        timeout: 15_000,
      })

      // Verify email sent to student4
      const newEmails = await waitForNewMessages('student4@test.local', beforeIds)
      expect(newEmails.length).toBeGreaterThanOrEqual(1)

      const email = newEmails[0]
      assertSentFromApp(email)
      expect(getToAddresses(email)).toContain('student4@test.local')
      expect(getSubject(email)).toBeTruthy()

      // Verify email body contains relevant content
      const body = getBody(email)
      expect(body, 'Should greet the student').toContain('Student4')
      expect(body, 'Should contain thesis title').toContain(THESIS_13_TITLE)
      expect(body, 'Should contain link to thesis').toContain('/theses/')
    } finally {
      await context.close()
    }
  })
})

test.describe('Thesis Lifecycle - Final Submission', () => {
  test('student can submit thesis (WRITING -> SUBMITTED)', async ({ browser }) => {
    test.setTimeout(90_000)

    const context = await browser.newContext({ storageState: authStatePath('student5') })
    const page = await context.newPage()

    try {
      await navigateTo(page, THESIS_14_URL)
      await expect(page.getByRole('heading', { name: THESIS_14_TITLE })).toBeVisible({
        timeout: 15_000,
      })

      // Expand the Thesis accordion to find the submission button
      await expandAccordion(page, 'Thesis', page.getByText('Mark Submission as Final'))

      // Verify "Mark Submission as Final" button is visible and enabled (thesis file exists)
      const submitButton = page.getByRole('button', { name: 'Mark Submission as Final' })
      await expect(submitButton).toBeVisible({ timeout: 10_000 })

      if (!(await submitButton.isEnabled())) {
        const presentationFileRow = page.locator('tr').filter({ hasText: 'Presentation (PDF)' }).first()
        await expect(presentationFileRow).toBeVisible({ timeout: 10_000 })

        await presentationFileRow.locator('button').last().click()

        const uploadDialog = page.getByRole('dialog')
        await expect(uploadDialog).toBeVisible({ timeout: 10_000 })
        await expect(
          uploadDialog.getByRole('heading', { name: 'File Upload', exact: true }),
        ).toBeVisible()

        await uploadDialog.locator('input[type="file"]').setInputFiles({
          name: 'final-submission-presentation.pdf',
          mimeType: 'application/pdf',
          buffer: createTestPdfBuffer(),
        })

        const uploadFileButton = uploadDialog.getByRole('button', { name: 'Upload File' })
        await expect(uploadFileButton).toBeEnabled({ timeout: 10_000 })
        await uploadFileButton.click()

        await expect(uploadDialog).toBeHidden({ timeout: 10_000 })
        await expect(page.getByText('File uploaded successfully')).toBeVisible({
          timeout: 10_000,
        })
      }

      await expect(submitButton).toBeEnabled()

      // Click "Mark Submission as Final"
      await submitButton.click()

      // Verify confirmation dialog opens with correct title and text
      const dialog = page.getByRole('dialog')
      await expect(dialog).toBeVisible({ timeout: 10_000 })
      await expect(dialog.getByRole('heading', { name: 'Final Submission', exact: true })).toBeVisible()
      await expect(
        dialog.getByText(
          'Are you sure you want to submit your thesis? This action cannot be undone.',
        ),
      ).toBeVisible()
      await expect(dialog.getByText(/official submission website/i)).toBeVisible()

      // Verify Cancel and Confirm buttons
      await expect(dialog.getByRole('button', { name: 'Cancel' })).toBeVisible()
      await expect(dialog.getByRole('button', { name: 'Confirm' })).toBeVisible()

      // Confirm the submission
      await dialog.getByRole('button', { name: 'Confirm' }).click()

      // Verify success notification
      await expect(page.getByText('Thesis submitted successfully')).toBeVisible({
        timeout: 15_000,
      })
    } finally {
      await context.close()
    }
  })
})

test.describe('Thesis Lifecycle - Close Thesis', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('supervisor can close thesis (DROPPED_OUT)', async ({ page }) => {
    test.setTimeout(90_000)

    await navigateTo(page, THESIS_15_URL)
    await expect(page.getByRole('heading', { name: THESIS_15_TITLE })).toBeVisible({
      timeout: 15_000,
    })

    // Expand the Configuration accordion
    await expandAccordion(page, 'Configuration', page.getByRole('button', { name: 'Close Thesis' }))

    // Verify "Close Thesis" button is visible (red outline)
    const closeButton = page.getByRole('button', { name: 'Close Thesis' })
    await expect(closeButton).toBeVisible()

    // Click "Close Thesis"
    await closeButton.click()

    // Verify confirmation dialog opens with correct title and text
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 10_000 })
    await expect(dialog.getByRole('heading', { name: 'Close Thesis', exact: true })).toBeVisible()
    await expect(dialog.getByText(/DROPPED OUT/i)).toBeVisible()
    await expect(dialog.getByText(/cannot be undone/i)).toBeVisible()

    // Verify Cancel and Confirm buttons
    await expect(dialog.getByRole('button', { name: 'Cancel' })).toBeVisible()
    await expect(dialog.getByRole('button', { name: 'Confirm' })).toBeVisible()

    // Confirm the close
    await dialog.getByRole('button', { name: 'Confirm' }).click()

    // Verify success notification
    await expect(page.getByText('Thesis closed successfully')).toBeVisible({ timeout: 15_000 })

    // Verify "This thesis is closed" alert appears
    await expect(page.getByText('This thesis is closed')).toBeVisible({ timeout: 15_000 })
  })
})
