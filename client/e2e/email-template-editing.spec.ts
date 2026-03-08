import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe('Email Template Editing', () => {
  test.use({ storageState: authStatePath('admin') })

  const researchGroupUrl = '/research-groups/00000000-0000-4000-a000-000000000001'

  test('admin can navigate to template editor', async ({ page }) => {
    test.setTimeout(60_000)

    await navigateTo(page, researchGroupUrl)
    await expect(page.getByRole('heading', { name: /research group settings/i })).toBeVisible({
      timeout: 15_000,
    })

    // Click the Email Settings tab
    await page.getByRole('tab', { name: 'Email Settings' }).click()

    // Verify Email Templates section loads
    await expect(page.getByRole('heading', { name: 'Email Templates', level: 3 })).toBeVisible()

    // Verify Application Email Content section is also visible
    await expect(
      page.getByRole('heading', { name: 'Application Email Content', level: 3 }),
    ).toBeVisible()

    // Find an enabled Edit button on a non-disabled template card
    const enabledCard = page
      .locator('.mantine-Card-root')
      .filter({ hasText: 'Thesis Application Acceptance' })
      .first()
    await expect(enabledCard).toBeVisible({ timeout: 10_000 })

    const editButton = enabledCard.getByRole('button', { name: 'Edit' })
    await expect(editButton).toBeEnabled()

    // Also verify Preview button exists
    await expect(enabledCard.getByRole('button', { name: 'Preview' })).toBeVisible()

    // Click Edit to navigate to the template editor
    await editButton.click()

    // Verify template edit page loads with expected elements
    await expect(page.getByRole('heading', { name: 'Edit Email Template' })).toBeVisible({
      timeout: 15_000,
    })

    // Verify Subject input is present and has a value
    const subjectInput = page.getByRole('textbox', { name: 'Subject' })
    await expect(subjectInput).toBeVisible()
    await expect(subjectInput).not.toHaveValue('')

    // Verify all action buttons are present
    await expect(page.getByRole('button', { name: 'Save changes' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Reset to default' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Discard changes' })).toBeVisible()
  })

  test('admin can edit and save template', async ({ page }) => {
    test.setTimeout(60_000)

    await navigateTo(page, researchGroupUrl)
    await expect(page.getByRole('heading', { name: /research group settings/i })).toBeVisible({
      timeout: 15_000,
    })

    // Navigate to Email Settings tab
    await page.getByRole('tab', { name: 'Email Settings' }).click()
    await expect(page.getByRole('heading', { name: 'Email Templates', level: 3 })).toBeVisible()

    // Find and click Edit on an enabled template
    const enabledCard = page
      .locator('.mantine-Card-root')
      .filter({ hasText: 'Thesis Application Acceptance' })
      .first()
    await expect(enabledCard).toBeVisible({ timeout: 10_000 })
    await enabledCard.getByRole('button', { name: 'Edit' }).click()

    // Wait for the template edit page to load
    await expect(page.getByRole('heading', { name: 'Edit Email Template' })).toBeVisible({
      timeout: 15_000,
    })

    // Save the original subject for comparison
    const subjectInput = page.getByRole('textbox', { name: 'Subject' })
    const originalSubject = await subjectInput.inputValue()

    // Change the Subject text
    await subjectInput.clear()
    await subjectInput.fill('E2E Test - Modified Subject')

    // Click "Save changes"
    await page.getByRole('button', { name: 'Save changes' }).click()

    // Verify success notification with exact text
    await expect(page.getByText('Email template updated successfully')).toBeVisible({
      timeout: 10_000,
    })

    // Verify the subject field still shows the new value
    await expect(subjectInput).toHaveValue('E2E Test - Modified Subject')

    // Reset to default to clean up test data
    const resetButton = page.getByRole('button', { name: 'Reset to default' })
    await expect(resetButton).toBeEnabled({ timeout: 5_000 })
    await resetButton.click()

    // Verify reset success notification
    await expect(
      page.getByText('Custom template deleted. Reverted to default template.'),
    ).toBeVisible({ timeout: 10_000 })

    // Verify the subject reverted to the original value
    await expect(subjectInput).toHaveValue(originalSubject)
  })
})
