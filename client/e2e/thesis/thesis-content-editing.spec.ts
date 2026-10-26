import { test, expect } from '@playwright/test'
import { authStatePath, fillRichTextEditor, navigateTo, navigateToThesisConfig } from '../helpers'

const THESIS_ID = '00000000-0000-4000-d000-000000000016'
const THESIS_URL = `/theses/${THESIS_ID}`
const THESIS_TITLE = 'E2E Gap2: Content Editing Test Thesis'

test.describe('Thesis Content Editing - Supervisor', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('supervisor can edit thesis configuration', async ({ page }) => {
    test.setTimeout(90_000)

    // Navigate directly to the standalone Configuration page
    await navigateToThesisConfig(page, THESIS_ID)

    // Verify form fields are visible and have expected initial values
    const titleInput = page.getByRole('textbox', { name: 'Thesis Title' })
    await expect(titleInput).toBeVisible()
    await expect(titleInput).toHaveValue(THESIS_TITLE)
    await expect(titleInput).toBeEnabled()

    // Verify Update button is visible for supervisor
    await expect(page.getByRole('button', { name: 'Update' })).toBeVisible()

    // Verify Close Thesis button is visible for supervisor
    await expect(page.getByRole('button', { name: 'Close Thesis' })).toBeVisible()

    const visibilityInput = page.getByRole('combobox', { name: 'Visibility' })
    const currentVisibility = await visibilityInput.inputValue()
    const targetVisibility = currentVisibility === 'Internal' ? 'Private' : 'Internal'

    // Change visibility to a different valid value so retries remain idempotent
    await visibilityInput.click()
    await page.getByRole('option', { name: new RegExp(`^${targetVisibility}`) }).click()

    // Wait for the select value to update before submitting
    await expect(visibilityInput).toHaveValue(targetVisibility, { timeout: 5_000 })

    const updateButton = page.getByRole('button', { name: 'Update' })
    await expect(updateButton).toBeEnabled({ timeout: 10_000 })

    // Submit the update
    await updateButton.click()

    // Verify success notification with exact text from source code
    await expect(page.getByText('Thesis updated successfully')).toBeVisible({ timeout: 10_000 })

    // After saving, the config page should navigate back to the thesis detail page
    await expect(page).toHaveURL(new RegExp(`${THESIS_ID}$`), { timeout: 10_000 })

    // Revisit the configuration page and verify the updated fields persisted
    await navigateToThesisConfig(page, THESIS_ID)
    await expect(page.getByRole('button', { name: 'Update' })).toBeVisible({ timeout: 15_000 })
    await expect(page.getByRole('textbox', { name: 'Thesis Title' })).toHaveValue(THESIS_TITLE)
    await expect(page.getByRole('combobox', { name: 'Visibility' })).toHaveValue(targetVisibility)
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

  test('student is redirected away from the configuration page', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authStatePath('student4') })
    const page = await context.newPage()

    try {
      test.setTimeout(90_000)

      // Students accessing /theses/:id/configuration should be redirected to /theses/:id
      await navigateTo(page, `${THESIS_URL}/configuration`)
      await expect(page).toHaveURL(new RegExp(`${THESIS_ID}$`), { timeout: 15_000 })
      await expect(page.getByRole('heading', { name: /E2E Gap2/i })).toBeVisible({
        timeout: 15_000,
      })

      // The Configuration link in the header is hidden for students
      await expect(page.getByRole('link', { name: 'Configuration' })).toBeHidden()
    } finally {
      await context.close()
    }
  })
})
