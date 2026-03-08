import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe.serial('Topic 7 - Edit then Close', () => {
  test.use({ storageState: authStatePath('examiner') })

  test('examiner can edit an open topic', async ({ page }) => {
    test.setTimeout(60_000)

    await navigateTo(page, '/topics')
    await expect(page.getByRole('heading', { name: 'Manage Topics', exact: true })).toBeVisible({
      timeout: 30_000,
    })

    // Verify the topic row exists with expected data before editing
    const row = page.locator('tr').filter({ hasText: 'E2E Gap4: Editable Open Topic' })
    await expect(row).toBeVisible({ timeout: 10_000 })

    // Click the edit button — first ActionIcon in the row actions
    const editButton = row.getByRole('button').first()
    await editButton.click()

    // Verify modal opens with correct title
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 10_000 })
    await expect(dialog.getByText('Edit Topic')).toBeVisible()

    // Verify the title field is pre-filled with the current topic title
    const titleInput = dialog.getByLabel('Title')
    await expect(titleInput).toBeVisible()
    await expect(titleInput).toHaveValue('E2E Gap4: Editable Open Topic')

    // Verify other required fields are present in the modal
    await expect(dialog.getByText('Thesis Types')).toBeVisible()
    await expect(dialog.getByText('Problem Statement')).toBeVisible()

    // Change the title
    await titleInput.clear()
    await titleInput.fill('E2E Gap4: Edited Open Topic')

    // Click "Save Changes" button
    const saveButton = dialog.getByRole('button', { name: 'Save Changes' })
    await expect(saveButton).toBeEnabled({ timeout: 10_000 })
    await saveButton.click()

    // Verify modal closes
    await expect(dialog).not.toBeVisible({ timeout: 15_000 })

    // Verify success notification with exact text from source code
    await expect(page.getByText('Topic updated successfully')).toBeVisible({ timeout: 10_000 })

    // Verify the updated title now appears in the table
    await expect(page.locator('tr').filter({ hasText: 'E2E Gap4: Edited Open Topic' })).toBeVisible(
      {
        timeout: 10_000,
      },
    )

    // Verify the old title is no longer visible
    await expect(page.getByText('E2E Gap4: Editable Open Topic')).toBeHidden()
  })

  test('examiner can close an open topic', async ({ page }) => {
    test.setTimeout(60_000)

    await navigateTo(page, '/topics')
    await expect(page.getByRole('heading', { name: 'Manage Topics', exact: true })).toBeVisible({
      timeout: 30_000,
    })

    // Find the row with the edited title
    const row = page.locator('tr').filter({ hasText: 'E2E Gap4: Edited Open Topic' })
    await expect(row).toBeVisible({ timeout: 10_000 })

    // Click the close button — last ActionIcon in the row
    const closeButton = row.getByRole('button').last()
    await closeButton.click()

    // Verify modal opens with correct title for non-draft topics
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 10_000 })
    await expect(dialog.getByText('Close Topic')).toBeVisible()

    // Verify confirmation text for non-draft topic (mentions rejecting applications)
    await expect(dialog.getByText(/reject all applications/i)).toBeVisible()

    // Verify Reason select is present (only for non-draft topics)
    const reasonInput = dialog.getByRole('textbox', { name: 'Reason' })
    await expect(reasonInput).toBeVisible()

    // Select a reason
    await reasonInput.click()
    await page.getByRole('option', { name: /Topic was filled/i }).click()

    // Verify "Notify Students" checkbox is present and check it
    const notifyCheckbox = dialog.getByRole('checkbox', { name: /Notify Students/i })
    await expect(notifyCheckbox).toBeVisible()
    if (!(await notifyCheckbox.isChecked())) {
      await notifyCheckbox.check()
    }
    await expect(notifyCheckbox).toBeChecked()

    // Click "Close Topic" button in the modal
    await dialog.getByRole('button', { name: 'Close Topic' }).click()

    // Verify success notification with exact text
    await expect(page.getByText('Topic closed successfully')).toBeVisible({ timeout: 10_000 })

    // Verify the topic row is no longer visible in the active topics list
    // (closed topics are typically filtered out or shown differently)
    await expect(row).toBeHidden({ timeout: 10_000 })
  })
})

test.describe('Topic 8 - Close Draft', () => {
  test.use({ storageState: authStatePath('examiner') })

  test('examiner can close a draft topic', async ({ page }) => {
    test.setTimeout(60_000)

    await navigateTo(page, '/topics')
    await expect(page.getByRole('heading', { name: 'Manage Topics', exact: true })).toBeVisible({
      timeout: 30_000,
    })

    // Switch to the Draft tab to see draft topics
    await page.getByRole('tab', { name: 'Draft' }).click()

    // Find the draft topic row
    const row = page.locator('tr').filter({ hasText: 'E2E Gap4: Closable Draft Topic' })
    await expect(row).toBeVisible({ timeout: 10_000 })

    // Click the close button
    const closeButton = row.getByRole('button').last()
    await closeButton.click()

    // Verify modal opens with "Close Draft" title (not "Close Topic")
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 10_000 })
    await expect(dialog.getByText('Close Draft')).toBeVisible()

    // Verify draft-specific confirmation text
    await expect(dialog.getByText(/close this draft/i)).toBeVisible()

    // Verify that Reason select is NOT present for drafts
    await expect(dialog.getByRole('textbox', { name: 'Reason' })).toBeHidden()

    // Verify that "Notify Students" checkbox is NOT present for drafts
    await expect(dialog.getByRole('checkbox', { name: /Notify Students/i })).toBeHidden()

    // Click "Close Draft" button in the modal
    await dialog.getByRole('button', { name: 'Close Draft' }).click()

    // Verify success notification
    await expect(page.getByText('Topic closed successfully')).toBeVisible({ timeout: 10_000 })

    // Verify the draft topic row is no longer visible
    await expect(row).toBeHidden({ timeout: 10_000 })
  })
})
