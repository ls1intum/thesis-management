import { test, expect } from '@playwright/test'
import {
  authStatePath,
  clickMultiSelect,
  fillRichTextEditor,
  navigateTo,
  searchAndSelectMultiSelect,
} from './helpers'

test.describe('Topic Workflow - Supervisor creates a topic', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('supervisor can create a new topic via the manage topics page', async ({ page }) => {
    test.setTimeout(120_000) // Extended timeout — form with server-side search fields

    await navigateTo(page, '/topics')
    await expect(page.getByRole('heading', { name: 'Manage Topics', exact: true })).toBeVisible({
      timeout: 30_000,
    })

    // Click "Create Topic" button
    await page.getByRole('button', { name: 'Create Topic' }).click()

    // Modal should open
    await expect(page.getByRole('dialog')).toBeVisible({ timeout: 5_000 })

    // Fill in the topic form
    await page.getByLabel('Title').fill('E2E Test Topic: Automated Testing Strategies')

    // Thesis Types (multi-select) - use force click to bypass wrapper interception
    await clickMultiSelect(page, 'Thesis Types')
    await page.getByRole('option', { name: /master/i }).click()
    // Close the Thesis Types dropdown by pressing Tab (blurs input without closing modal)
    await page.keyboard.press('Tab')
    await page.waitForTimeout(1_000)

    // Examiner - click to open, then select from results
    await searchAndSelectMultiSelect(page, 'Examiner', /supervisor/i)

    // Supervisor(s) - click to open, then select from results
    await searchAndSelectMultiSelect(page, 'Supervisor(s)', /advisor/i)

    // Research Group should be pre-filled for single-group supervisors
    // Problem Statement (required rich text editor)
    await fillRichTextEditor(
      page,
      'Problem Statement',
      'This topic explores automated testing strategies for modern web applications, focusing on E2E testing frameworks and CI integration.',
    )

    // Click "Create Topic" button in the modal
    const createButton = page.getByRole('dialog').getByRole('button', { name: 'Create Topic' })
    await expect(createButton).toBeEnabled({ timeout: 15_000 })
    await createButton.click()

    // Modal should close and success notification should appear
    await expect(page.getByRole('dialog')).not.toBeVisible({ timeout: 15_000 })
    await expect(page.getByText('Topic created successfully')).toBeVisible({ timeout: 10_000 })
  })
})
