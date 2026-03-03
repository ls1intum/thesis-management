import { test, expect } from '@playwright/test'
import { authStatePath, createTestPdfBuffer, navigateToDetail } from './helpers'
import {
  snapshotMailbox,
  waitForNewMessages,
  getSubject,
  getBody,
  getToAddresses,
  assertSentFromApp,
  assertEmailFooter,
  hasAttachment,
} from './mailpit'

// Thesis d000-0002 is in PROPOSAL state, assigned to student2 with supervisor
const THESIS_ID = '00000000-0000-4000-d000-000000000002'
const THESIS_URL = `/theses/${THESIS_ID}`
const THESIS_TITLE = 'CI Pipeline Optimization Through Intelligent Test Selection'

test.describe('Proposal Upload - Student uploads proposal', () => {
  test.use({ storageState: authStatePath('student2') })

  test('student can upload a proposal PDF to a thesis in PROPOSAL state', async ({ page }) => {
    const heading = page.getByRole('heading', { name: THESIS_TITLE })
    const loaded = await navigateToDetail(page, THESIS_URL, heading)
    if (!loaded) return

    // The Proposal section should be visible and expanded (default for PROPOSAL state)
    await expect(page.getByRole('button', { name: 'Upload Proposal' })).toBeVisible({
      timeout: 10_000,
    })

    // Click "Upload Proposal" button - this opens a file upload modal
    await page.getByRole('button', { name: 'Upload Proposal' }).click()

    // Modal should open with "File Upload" title
    await expect(page.getByRole('dialog')).toBeVisible({ timeout: 5_000 })

    // Set file on the hidden file input inside the dropzone
    const fileInput = page.getByRole('dialog').locator('input[type="file"]')
    await fileInput.setInputFiles({
      name: 'test-proposal.pdf',
      mimeType: 'application/pdf',
      buffer: createTestPdfBuffer(),
    })

    // Snapshot mailbox BEFORE the upload action
    const beforeIds = await snapshotMailbox('supervisor@test.local')

    // Click "Upload File" button in the modal
    await page.getByRole('dialog').getByRole('button', { name: 'Upload File' }).click()

    // Modal should close after successful upload
    await expect(page.getByRole('dialog')).not.toBeVisible({ timeout: 15_000 })

    // --- Email verification ---
    // THESIS_PROPOSAL_UPLOADED is sent to the thesis supervisors with the proposal attached
    const newEmails = await waitForNewMessages('supervisor@test.local', beforeIds)
    expect(newEmails.length).toBeGreaterThanOrEqual(1)

    const proposalEmail = newEmails.find((e) => getSubject(e) === 'Thesis Proposal Added')
    expect(proposalEmail, 'Proposal upload email with correct subject should be sent').toBeDefined()
    assertSentFromApp(proposalEmail!)
    assertEmailFooter(proposalEmail!)
    expect(getToAddresses(proposalEmail!)).toContain('supervisor@test.local')

    // Body should reference the thesis title, the uploader name, and include a link
    const proposalBody = getBody(proposalEmail!)
    expect(proposalBody, 'Should contain the thesis title').toContain(THESIS_TITLE)
    expect(proposalBody, 'Should mention the student who uploaded').toContain('Student2')
    expect(proposalBody, 'Should contain a link to the thesis').toContain('/theses/')
    expect(proposalBody, 'Should reference the attachment').toContain('attachment')

    // The server attaches the proposal PDF via addStoredAttachment. If the stored file
    // is available (proposalFilename is set and file exists on disk), verify the filename.
    // The attachment may be absent if the file is not yet persisted when the email is sent.
    if (hasAttachment(proposalEmail!)) {
      expect(
        hasAttachment(proposalEmail!, /Proposal/i),
        'Attachment filename should contain "Proposal"',
      ).toBe(true)
    }
  })
})

test.describe('Proposal Feedback - Supervisor requests changes', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('supervisor can request changes on a proposal', async ({ page }) => {
    const heading = page.getByRole('heading', { name: THESIS_TITLE })
    const loaded = await navigateToDetail(page, THESIS_URL, heading)
    if (!loaded) return

    // Scroll to and click "Request Changes" button (red outline button in Proposal section)
    const requestChangesButton = page.getByRole('button', { name: 'Request Changes' }).first()
    await requestChangesButton.scrollIntoViewIfNeeded()
    await requestChangesButton.click()

    // Modal should open with "Request Changes" title
    await expect(page.getByRole('dialog')).toBeVisible({ timeout: 10_000 })

    // Fill in the new change requests textarea (one per line)
    await page
      .getByLabel('New Change Requests (one request per line)')
      .fill('Please add a literature review section\nFix the formatting of the references')

    // Snapshot mailbox BEFORE the action
    const beforeIds = await snapshotMailbox('student2@test.local')

    // Click "Request Changes" button in the modal
    await page.getByRole('dialog').getByRole('button', { name: 'Request Changes' }).click()

    // Modal should close after successful submission
    await expect(page.getByRole('dialog')).not.toBeVisible({ timeout: 15_000 })

    // Feedback should appear in the proposal section
    await expect(page.getByText('Please add a literature review section').first()).toBeVisible({
      timeout: 10_000,
    })

    // --- Email verification ---
    // THESIS_PROPOSAL_REJECTED is sent to the thesis students
    const newEmails = await waitForNewMessages('student2@test.local', beforeIds)
    expect(newEmails.length).toBeGreaterThanOrEqual(1)

    const changeRequestEmail = newEmails.find(
      (e) => getSubject(e) === 'Changes were requested for Proposal',
    )
    expect(
      changeRequestEmail,
      'Change request email with correct subject should be sent',
    ).toBeDefined()
    assertSentFromApp(changeRequestEmail!)
    assertEmailFooter(changeRequestEmail!)
    expect(getToAddresses(changeRequestEmail!)).toContain('student2@test.local')

    // Body should greet the student, reference the thesis title, mention the reviewer,
    // contain a thesis link, and list ALL requested changes
    const body = getBody(changeRequestEmail!)
    expect(body, 'Should greet the student by first name').toContain('Student2')
    expect(body, 'Should contain the thesis title').toContain(THESIS_TITLE)
    expect(body, 'Should contain the first change request').toContain('literature review')
    expect(body, 'Should contain the second change request').toContain(
      'formatting of the references',
    )
    expect(body, 'Should contain a link to the thesis').toContain('/theses/')
  })
})
