import { test, expect } from '@playwright/test'
import { authStatePath, expandAccordion, fillRichTextEditor, navigateTo } from './helpers'

const THESIS_ID = '00000000-0000-4000-d000-000000000016'
const THESIS_URL = `/theses/${THESIS_ID}`
const THESIS_TITLE = 'E2E Gap2: Content Editing Test Thesis'

test.describe('Thesis Content Editing - Supervisor', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('supervisor can edit thesis configuration', async ({ page }) => {
    test.setTimeout(90_000)

    await navigateTo(page, THESIS_URL)
    await expect(page.getByRole('heading', { name: THESIS_TITLE })).toBeVisible({ timeout: 15_000 })

    // Expand Configuration accordion
    await expandAccordion(page, 'Configuration', page.getByRole('button', { name: 'Update' }))

    // Verify form fields are visible and have expected initial values
    const titleInput = page.getByRole('textbox', { name: 'Thesis Title' })
    await expect(titleInput).toBeVisible()
    await expect(titleInput).toHaveValue(THESIS_TITLE)
    await expect(titleInput).toBeEnabled()

    // Verify Update button is visible for supervisor
    await expect(page.getByRole('button', { name: 'Update' })).toBeVisible()

    // Verify Close Thesis button is visible for supervisor
    await expect(page.getByRole('button', { name: 'Close Thesis' })).toBeVisible()

    const visibilityInput = page.getByRole('textbox', { name: 'Visibility' })
    const currentVisibility = await visibilityInput.inputValue()
    const targetVisibility = currentVisibility === 'Internal' ? 'Private' : 'Internal'

    // Change visibility to a different valid value so retries remain idempotent
    await visibilityInput.click()
    await page.getByRole('option', { name: new RegExp(`^${targetVisibility}`) }).click()

    const updateButton = page.getByRole('button', { name: 'Update' })
    await expect(updateButton).toBeEnabled({ timeout: 10_000 })

    // Submit the update
    await updateButton.click()

    // Verify success notification with exact text from source code
    await expect(page.getByText('Thesis updated successfully')).toBeVisible({ timeout: 10_000 })

    // Reload page and verify the updated configuration persisted
    await page.reload({ waitUntil: 'domcontentloaded' })
    await expect(page.getByRole('heading', { name: /E2E Gap2/i })).toBeVisible({ timeout: 15_000 })

    // Re-expand Configuration and verify field values persisted
    await expandAccordion(page, 'Configuration', page.getByRole('button', { name: 'Update' }))
    await expect(page.getByRole('textbox', { name: 'Thesis Title' })).toHaveValue(THESIS_TITLE)
    await expect(page.getByRole('textbox', { name: 'Visibility' })).toHaveValue(targetVisibility)
  })
})

test.describe('Thesis Content Editing - Student', () => {
  test('student can edit thesis info', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authStatePath('student4') })
    const page = await context.newPage()

    try {
      test.setTimeout(90_000)

      await navigateTo(page, THESIS_URL)
      await expect(page.getByRole('heading', { name: /E2E Gap2/i })).toBeVisible({
        timeout: 15_000,
      })

      // Verify Info section is visible (first accordion, typically open by default)
      // Click Edit button to enter edit mode
      const editButton = page.getByRole('button', { name: 'Edit' })
      await expect(editButton).toBeVisible({ timeout: 10_000 })
      await editButton.click()

      // Verify edit mode buttons appear
      await expect(page.getByRole('button', { name: 'Save' })).toBeVisible()
      await expect(page.getByRole('button', { name: 'Cancel' })).toBeVisible()

      // Verify Edit button is no longer visible in edit mode
      await expect(editButton).toBeHidden()

      // Fill in the abstract (DocumentEditor with ProseMirror rich text editor)
      await fillRichTextEditor(
        page,
        'Abstract',
        'E2E test abstract: exploring content editing workflows',
      )

      // Save the changes
      await page.getByRole('button', { name: 'Save' }).click()

      // Verify success notification with exact text from source code
      await expect(page.getByText('Thesis info updated successfully')).toBeVisible({
        timeout: 10_000,
      })

      // Verify we're back in read mode (Edit button visible again)
      await expect(page.getByRole('button', { name: 'Edit' })).toBeVisible({ timeout: 10_000 })

      // Verify the abstract text is displayed in read mode
      await expect(page.getByText('exploring content editing workflows')).toBeVisible()
    } finally {
      await context.close()
    }
  })

  test('student cannot see Update button in config', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authStatePath('student4') })
    const page = await context.newPage()

    try {
      test.setTimeout(90_000)

      await navigateTo(page, THESIS_URL)
      await expect(page.getByRole('heading', { name: /E2E Gap2/i })).toBeVisible({
        timeout: 15_000,
      })

      // Expand Configuration accordion
      await expandAccordion(page, 'Configuration', page.getByText('Thesis Title'))

      // Verify "Update" button is NOT visible for students
      await expect(page.getByRole('button', { name: 'Update' })).toBeHidden()

      // Verify "Close Thesis" button is NOT visible for students
      await expect(page.getByRole('button', { name: 'Close Thesis' })).toBeHidden()

      // Verify Thesis Title input is disabled/readonly for students
      await expect(page.getByRole('textbox', { name: 'Thesis Title' })).toBeDisabled()

      // Verify Visibility select is also disabled for students
      await expect(page.getByRole('textbox', { name: 'Visibility' })).toBeDisabled()
    } finally {
      await context.close()
    }
  })
})
