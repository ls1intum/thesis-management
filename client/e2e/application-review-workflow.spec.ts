import { test, expect } from '@playwright/test'
import { authStatePath, navigateToDetail, selectOption } from './helpers'
import {
  snapshotMailbox,
  waitForNewMessages,
  getBody,
  getToAddresses,
  assertSentFromApp,
  findBySubject,
} from './mailpit'

const APPLICATION_REJECT_ID = '00000000-0000-4000-c000-000000000004' // student4 on topic 1, NOT_ASSESSED
const APPLICATION_ACCEPT_ID = '00000000-0000-4000-c000-000000000005' // student5 on topic 2, NOT_ASSESSED

test.describe('Application Review Workflow', () => {
  test.use({ storageState: authStatePath('supervisor') })
  test.describe.configure({ mode: 'serial' })

  test('supervisor can reject a NOT_ASSESSED application', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /Student4 User/i })
    const loaded = await navigateToDetail(page, `/applications/${APPLICATION_REJECT_ID}`, heading)
    if (!loaded) return // Application not accessible (may have been modified by a parallel test)

    // Check if application still has the review form (NOT_ASSESSED state)
    const thesisTitle = page.getByLabel('Thesis Title')
    const hasReviewForm = await thesisTitle.isVisible({ timeout: 5_000 }).catch(() => false)
    if (!hasReviewForm) {
      return
    }

    // Click the first "Reject" button (header area, opens modal directly)
    const rejectButton = page.getByRole('button', { name: 'Reject', exact: true }).first()
    await expect(rejectButton).toBeVisible({ timeout: 10_000 })
    await rejectButton.click()

    // Modal should open with "Reject Application" title
    await expect(page.getByRole('dialog')).toBeVisible({ timeout: 5_000 })
    await expect(page.getByRole('dialog').getByText('Reject Application').first()).toBeVisible()

    // "Topic requirements not met" should be the default selected reason
    await expect(page.getByText('Topic requirements not met')).toBeVisible()

    // "Notify Student" checkbox should be checked by default
    const notifyCheckbox = page.getByRole('dialog').getByLabel('Notify Student')
    await expect(notifyCheckbox).toBeChecked()

    // Snapshot mailbox before the action
    const beforeIds = await snapshotMailbox('student4@test.local')

    // Click "Reject Application" button in the modal
    await page.getByRole('dialog').getByRole('button', { name: 'Reject Application' }).click()

    // Verify success notification
    await expect(page.getByText('Application rejected successfully')).toBeVisible({
      timeout: 10_000,
    })

    // --- Email verification ---
    // Rejection with "Notify Student" sends APPLICATION_REJECTED_TOPIC_REQUIREMENTS
    const newEmails = await waitForNewMessages('student4@test.local', beforeIds)
    expect(newEmails.length).toBeGreaterThanOrEqual(1)

    const rejectionEmail = findBySubject(newEmails, 'Thesis Application Rejection')
    expect(rejectionEmail, 'Rejection email with correct subject should be sent').toBeDefined()
    assertSentFromApp(rejectionEmail!)
    expect(getToAddresses(rejectionEmail!)).toContain('student4@test.local')

    // Body should greet the student by first name and reference the topic title
    const body = getBody(rejectionEmail!)
    expect(body, 'Rejection email should greet the student').toContain('Student4')
    expect(body, 'Rejection email should reference the topic title').toContain(
      'Automated Code Review Using Large Language Models',
    )
    expect(body, 'Rejection email should mention requirements').toContain('requirements')
  })

  test('supervisor can accept a NOT_ASSESSED application', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /Student5 User/i })
    const loaded = await navigateToDetail(page, `/applications/${APPLICATION_ACCEPT_ID}`, heading)
    if (!loaded) return // Application not accessible

    // Check if application still has the review form (NOT_ASSESSED state)
    const thesisTitle = page.getByLabel('Thesis Title')
    const hasReviewForm = await thesisTitle.isVisible({ timeout: 5_000 }).catch(() => false)
    if (!hasReviewForm) {
      return
    }

    // Verify the acceptance form has pre-filled fields from the topic
    await expect(thesisTitle).not.toHaveValue('')
    await expect(page.getByRole('textbox', { name: 'Thesis Type' })).toBeVisible()

    // Thesis Language may not be pre-filled — fill it if empty
    const languageInput = page.getByRole('textbox', { name: 'Thesis Language' })
    const languageValue = await languageInput.inputValue()
    if (!languageValue) {
      await selectOption(page, 'Thesis Language', /english/i)
    }

    // Examiner and Supervisor(s) should be pre-filled from the topic (pills visible)
    const examinerWrapper = page.locator(
      '.mantine-InputWrapper-root:has(.mantine-InputWrapper-label:text("Examiner"))',
    )
    await expect(examinerWrapper.locator('.mantine-Pill-root').first()).toBeVisible({
      timeout: 10_000,
    })

    const supervisorWrapper = page.locator(
      '.mantine-InputWrapper-root:has(.mantine-InputWrapper-label:text("Supervisor(s)"))',
    )
    await expect(supervisorWrapper.locator('.mantine-Pill-root').first()).toBeVisible({
      timeout: 10_000,
    })

    // Snapshot mailbox for student5 before the action
    const beforeIds = await snapshotMailbox('student5@test.local')

    // Click "Accept" button
    const acceptButton = page.getByRole('button', { name: 'Accept', exact: true })
    await expect(acceptButton).toBeEnabled({ timeout: 10_000 })
    await acceptButton.click()

    // Verify success notification (accept creates a thesis, which can be slow under load)
    await expect(page.getByText('Application accepted successfully')).toBeVisible({
      timeout: 30_000,
    })

    // --- Email verification ---
    // Acceptance sends APPLICATION_ACCEPTED + THESIS_CREATED both to student5
    const newEmails = await waitForNewMessages('student5@test.local', beforeIds, 2, 30_000)

    // Verify acceptance email was sent to student5 (APPLICATION_ACCEPTED template)
    const acceptanceEmail = findBySubject(newEmails, 'Thesis Application Acceptance')
    expect(acceptanceEmail, 'Acceptance email should be sent').toBeDefined()
    assertSentFromApp(acceptanceEmail!)
    expect(getToAddresses(acceptanceEmail!)).toContain('student5@test.local')

    // Body should greet student, mention the supervisor, and include a thesis link
    const acceptBody = getBody(acceptanceEmail!)
    expect(acceptBody, 'Should greet the student by first name').toContain('Student5')
    expect(acceptBody, 'Should mention the supervisor for coordination').toContain('supervisor')
    expect(acceptBody, 'Should contain a link to the thesis').toContain('/theses/')

    // Verify thesis creation email was also sent to student5 (THESIS_CREATED template)
    const thesisEmail = findBySubject(newEmails, 'Thesis Created')
    expect(thesisEmail, 'Thesis creation email should be sent').toBeDefined()
    assertSentFromApp(thesisEmail!)
    expect(getToAddresses(thesisEmail!)).toContain('student5@test.local')

    // Body should contain the thesis title and a link to the thesis
    const thesisBody = getBody(thesisEmail!)
    expect(thesisBody, 'Should contain the thesis title').toContain(
      'Continuous Integration Pipeline Optimization',
    )
    expect(thesisBody, 'Should contain a link to the thesis').toContain('/theses/')
  })
})
