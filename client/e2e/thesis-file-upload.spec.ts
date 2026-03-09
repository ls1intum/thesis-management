import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo, expandAccordion, createTestPdfBuffer } from './helpers'

const THESIS_1_ID = '00000000-0000-4000-d000-000000000001'
const THESIS_1_URL = `/theses/${THESIS_1_ID}`

test.describe('Thesis File Upload - Student uploads thesis PDF', () => {
  test.use({ storageState: authStatePath('student') })

  test('student uploads thesis PDF and verifies it in file history', async ({ page }) => {
    test.setTimeout(90_000)

    await navigateTo(page, THESIS_1_URL)
    await expect(page.getByRole('heading', { name: /automated code review/i })).toBeVisible({
      timeout: 30_000,
    })

    // Expand the Thesis accordion, then the Files sub-accordion
    await expandAccordion(page, 'Thesis', page.getByText('Files').first())
    await expandAccordion(page, 'Files', page.getByRole('button', { name: 'Upload Thesis' }))

    // Click "Upload Thesis" button to open modal
    const uploadThesisButton = page.getByRole('button', { name: 'Upload Thesis' })
    await uploadThesisButton.scrollIntoViewIfNeeded()
    await uploadThesisButton.click()

    // Verify modal opens with expected heading
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 10_000 })
    await expect(
      dialog.getByRole('heading', { name: 'File Upload', exact: true }),
    ).toBeVisible()

    // Verify dropzone text
    await expect(dialog.getByText(/Drag the file here or click to select file/i)).toBeVisible()

    // Verify "Upload File" button is disabled before file selection
    const uploadFileButton = dialog.getByRole('button', { name: 'Upload File' })
    await expect(uploadFileButton).toBeDisabled()

    // Upload a test PDF file
    await dialog.locator('input[type="file"]').setInputFiles({
      name: 'thesis-draft-v2.pdf',
      mimeType: 'application/pdf',
      buffer: createTestPdfBuffer(),
    })

    // Verify "Upload File" button becomes enabled
    await expect(uploadFileButton).toBeEnabled({ timeout: 10_000 })

    // Click "Upload File" and verify modal closes
    await uploadFileButton.click()
    await expect(dialog).toBeHidden({ timeout: 10_000 })

    // Verify success notification
    await expect(page.getByText('File uploaded successfully')).toBeVisible({ timeout: 10_000 })

    // Verify file history shows the new file (Thesis v2 or higher)
    const filesSection = page.getByLabel('Files')
    await expect(filesSection.getByText(/Thesis v\d+/i).first()).toBeVisible({ timeout: 10_000 })
  })

  test('student uploads presentation file via file types table', async ({ page }) => {
    test.setTimeout(90_000)

    await navigateTo(page, THESIS_1_URL)
    await expect(page.getByRole('heading', { name: /automated code review/i })).toBeVisible({
      timeout: 30_000,
    })

    // Expand the Thesis and Files accordions
    await expandAccordion(page, 'Thesis', page.getByText('Files').first())
    await expandAccordion(page, 'Files', page.getByRole('button', { name: 'Upload Thesis' }))

    // Locate the "Presentation (PDF)" row in the file types table
    const presentationRow = page.locator('tr').filter({ hasText: 'Presentation (PDF)' }).first()
    await presentationRow.scrollIntoViewIfNeeded()
    await expect(presentationRow).toBeVisible({ timeout: 10_000 })

    // Click the upload button (last button in the row)
    await presentationRow.locator('button').last().click()

    // Verify upload modal opens
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 10_000 })
    await expect(
      dialog.getByRole('heading', { name: 'File Upload', exact: true }),
    ).toBeVisible()

    // Upload a test PDF file
    await dialog.locator('input[type="file"]').setInputFiles({
      name: 'final-presentation.pdf',
      mimeType: 'application/pdf',
      buffer: createTestPdfBuffer(),
    })

    // Click upload and verify success
    const uploadFileButton = dialog.getByRole('button', { name: 'Upload File' })
    await expect(uploadFileButton).toBeEnabled({ timeout: 10_000 })
    await uploadFileButton.click()

    await expect(dialog).toBeHidden({ timeout: 10_000 })

    // Verify the file history now has a Presentation entry (confirms upload succeeded)
    const filesSection = page.getByLabel('Files')
    await expect(filesSection.getByText(/Presentation v\d+/i).first()).toBeVisible({ timeout: 10_000 })
  })
})

test.describe('Thesis File Upload - Supervisor uploads and verifies download', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('supervisor uploads thesis file and verifies download button', async ({ page }) => {
    test.setTimeout(90_000)

    await navigateTo(page, THESIS_1_URL)
    await expect(page.getByRole('heading', { name: /automated code review/i })).toBeVisible({
      timeout: 30_000,
    })

    // Expand the Thesis and Files accordions
    await expandAccordion(page, 'Thesis', page.getByText('Files').first())
    await expandAccordion(page, 'Files', page.getByRole('button', { name: 'Upload Thesis' }))

    // Click "Upload Thesis"
    const uploadThesisButton = page.getByRole('button', { name: 'Upload Thesis' })
    await uploadThesisButton.scrollIntoViewIfNeeded()
    await uploadThesisButton.click()

    // Upload a test PDF
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 10_000 })

    await dialog.locator('input[type="file"]').setInputFiles({
      name: 'supervisor-thesis-upload.pdf',
      mimeType: 'application/pdf',
      buffer: createTestPdfBuffer(),
    })

    const uploadFileButton = dialog.getByRole('button', { name: 'Upload File' })
    await expect(uploadFileButton).toBeEnabled({ timeout: 10_000 })
    await uploadFileButton.click()
    await expect(dialog).toBeHidden({ timeout: 10_000 })
    await expect(page.getByText('File uploaded successfully')).toBeVisible({ timeout: 10_000 })

    // Scope assertions to the Files section
    const filesSection = page.getByLabel('Files')

    // Verify "Download" button exists and is visible within Files section
    const downloadButton = filesSection.getByRole('button', { name: 'Download' }).first()
    await downloadButton.scrollIntoViewIfNeeded()
    await expect(downloadButton).toBeVisible()

    // Verify file preview area exists within Files section (AuthenticatedFilePreview renders an iframe)
    await expect(filesSection.locator('iframe')).toHaveCount(1)

    // Verify file history shows the uploaded file
    await expect(filesSection.getByText(/Thesis v\d+/i).first()).toBeVisible({ timeout: 10_000 })
  })
})
