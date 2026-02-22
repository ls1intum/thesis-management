import { test, expect } from '@playwright/test'
import { authStatePath, createTestPdfBuffer, navigateTo } from './helpers'

// Thesis d000-0002 is in PROPOSAL state, assigned to student2 with advisor
const THESIS_ID = '00000000-0000-4000-d000-000000000002'
const THESIS_URL = `/theses/${THESIS_ID}`
const THESIS_TITLE = 'CI Pipeline Optimization Through Intelligent Test Selection'

test.describe('Proposal Upload - Student uploads proposal', () => {
  test.use({ storageState: authStatePath('student2') })

  test('student can upload a proposal PDF to a thesis in PROPOSAL state', async ({ page }) => {
    await navigateTo(page, THESIS_URL)

    // Wait for thesis page to load
    await expect(page.getByRole('heading', { name: THESIS_TITLE })).toBeVisible({
      timeout: 15_000,
    })

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

    // Click "Upload File" button in the modal
    await page.getByRole('dialog').getByRole('button', { name: 'Upload File' }).click()

    // Modal should close after successful upload
    await expect(page.getByRole('dialog')).not.toBeVisible({ timeout: 15_000 })
  })
})

test.describe('Proposal Feedback - Advisor requests changes', () => {
  test.use({ storageState: authStatePath('advisor') })

  test('advisor can request changes on a proposal', async ({ page }) => {
    await navigateTo(page, THESIS_URL)

    // Wait for thesis page to load
    await expect(page.getByRole('heading', { name: THESIS_TITLE })).toBeVisible({
      timeout: 15_000,
    })

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

    // Click "Request Changes" button in the modal
    await page
      .getByRole('dialog')
      .getByRole('button', { name: 'Request Changes' })
      .click()

    // Modal should close after successful submission
    await expect(page.getByRole('dialog')).not.toBeVisible({ timeout: 15_000 })

    // Feedback should appear in the proposal section
    await expect(page.getByText('Please add a literature review section').first()).toBeVisible({
      timeout: 10_000,
    })
  })
})
