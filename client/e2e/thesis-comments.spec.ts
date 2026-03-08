import { test, expect } from '@playwright/test'
import { authStatePath, expandAccordion, navigateTo, createTestPdfBuffer } from './helpers'

const THESIS_ID = '00000000-0000-4000-d000-000000000017'
const THESIS_URL = `/theses/${THESIS_ID}`
const THESIS_TITLE = 'E2E Gap3: Comments Test Thesis'

test.describe('Thesis Comments - Supervisor', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('supervisor can view existing comments', async ({ page }) => {
    await navigateTo(page, THESIS_URL)
    await expect(page.getByRole('heading', { name: THESIS_TITLE })).toBeVisible({ timeout: 15_000 })

    // Expand the Supervisor Comments accordion
    await expandAccordion(page, 'Supervisor Comments', page.getByText('distributed consensus'))

    // Verify both seeded comments are visible
    await expect(page.getByText('distributed consensus algorithms')).toBeVisible()
    await expect(page.getByText('Good progress on the implementation chapter')).toBeVisible()

    // Verify supervisor comments show "Not visible to student" badge
    const badges = page.getByText('Not visible to student')
    await expect(badges.first()).toBeVisible()

    // Verify the comment form is present (textarea + post button)
    await expect(page.getByPlaceholder('Add a comment or file...')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Post Comment' })).toBeVisible()

    // Verify "Post Comment" is disabled when textarea is empty
    await expect(page.getByRole('button', { name: 'Post Comment' })).toBeDisabled()
  })

  test('supervisor can add a comment', async ({ page }) => {
    await navigateTo(page, THESIS_URL)
    await expect(page.getByRole('heading', { name: THESIS_TITLE })).toBeVisible({ timeout: 15_000 })

    await expandAccordion(page, 'Supervisor Comments', page.getByText('distributed consensus'))

    // Type a comment
    const textarea = page.getByPlaceholder('Add a comment or file...')
    await textarea.fill('E2E test comment: review the methodology section carefully')

    // Verify "Post Comment" button becomes enabled after typing
    const postButton = page.getByRole('button', { name: 'Post Comment' })
    await expect(postButton).toBeEnabled()

    // Post the comment
    await postButton.click()

    // Verify the new comment text appears on the page
    await expect(page.getByText('review the methodology section carefully')).toBeVisible({
      timeout: 10_000,
    })

    // Verify the new comment shows the supervisor's name
    await expect(page.getByText('Supervisor User').first()).toBeVisible()

    // Verify the textarea is cleared after posting
    await expect(textarea).toHaveValue('')

    // Verify "Post Comment" is disabled again after posting
    await expect(postButton).toBeDisabled()
  })

  test('supervisor can add comment with file', async ({ page }) => {
    await navigateTo(page, THESIS_URL)
    await expect(page.getByRole('heading', { name: THESIS_TITLE })).toBeVisible({ timeout: 15_000 })

    await expandAccordion(page, 'Supervisor Comments', page.getByText('distributed consensus'))

    // Type a message
    await page.getByPlaceholder('Add a comment or file...').fill('E2E comment with PDF attachment')

    // Click "Attach File" to open the upload modal
    await page.getByRole('button', { name: 'Attach File' }).click()

    // Verify the "File Upload" modal opens
    const uploadDialog = page.getByRole('dialog')
    await expect(uploadDialog).toBeVisible({ timeout: 5_000 })

    // Set file on the hidden file input inside the dropzone in the modal
    const fileInput = uploadDialog.locator('input[type="file"]')
    await fileInput.setInputFiles({
      name: 'test-review-notes.pdf',
      mimeType: 'application/pdf',
      buffer: createTestPdfBuffer(),
    })

    // Click "Upload File" button in the modal
    await uploadDialog.getByRole('button', { name: 'Upload File' }).click()

    // Modal should close after upload
    await expect(uploadDialog).not.toBeVisible({ timeout: 10_000 })

    // Verify "Attach File" button is disabled after attaching (only one file per comment)
    await expect(page.getByRole('button', { name: 'Attach File' })).toBeDisabled()

    // Post the comment
    await page.getByRole('button', { name: 'Post Comment' }).click()

    // Verify the comment text appears
    await expect(page.getByText('E2E comment with PDF attachment')).toBeVisible({ timeout: 10_000 })

    // Verify the file attachment indicator is visible (filename text)
    await expect(page.getByText('test-review-notes.pdf').first()).toBeVisible({ timeout: 10_000 })
  })
})

test.describe('Thesis Comments - Student cannot see supervisor comments', () => {
  test('student cannot see supervisor comments', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authStatePath('student2') })
    const page = await context.newPage()

    try {
      await navigateTo(page, THESIS_URL)

      // Verify the thesis page loaded correctly
      await expect(page.getByRole('heading', { name: THESIS_TITLE })).toBeVisible({
        timeout: 15_000,
      })

      // Verify the student CANNOT see the "Supervisor Comments" accordion
      await expect(page.getByText('Supervisor Comments')).toBeHidden()

      // Verify the student cannot see any of the seeded supervisor comment text
      await expect(page.getByText('distributed consensus algorithms')).toBeHidden()
      await expect(page.getByText('Good progress on the implementation chapter')).toBeHidden()

      // Verify the "Not visible to student" badge is not shown
      await expect(page.getByText('Not visible to student')).toBeHidden()
    } finally {
      await context.close()
    }
  })
})
