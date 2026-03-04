import { test, expect } from '@playwright/test'
import {
  authStatePath,
  clickMultiSelect,
  fillRichTextEditor,
  navigateTo,
  searchAndSelectMultiSelect,
} from './helpers'

test.describe('Topic Workflow - Examiner creates a topic', () => {
  test.use({ storageState: authStatePath('examiner') })

  test('examiner can create a new topic via the manage topics page', async ({ page }) => {
    test.setTimeout(120_000) // Extended timeout — form with server-side search fields

    await navigateTo(page, '/topics')
    await expect(page.getByRole('heading', { name: 'Manage Topics', exact: true })).toBeVisible({
      timeout: 30_000,
    })

    // Verify Create Topic button is present
    const createTopicButton = page.getByRole('button', { name: 'Create Topic' })
    await expect(createTopicButton).toBeVisible()

    // Click "Create Topic" button
    await createTopicButton.click()

    // Modal should open with form fields
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // Verify essential form fields are present
    await expect(dialog.getByLabel('Title')).toBeVisible()

    // Fill in the topic form
    await page.getByLabel('Title').fill('E2E Test Topic: Automated Testing Strategies')

    // Thesis Types (multi-select) - use force click to bypass wrapper interception
    await clickMultiSelect(page, 'Thesis Types')
    await page.getByRole('option', { name: /master/i }).click()
    // Close the Thesis Types dropdown by pressing Tab (blurs input without closing modal)
    await page.keyboard.press('Tab')
    await page.waitForTimeout(1_000)

    // Examiner - click to open, then select from results
    await searchAndSelectMultiSelect(page, 'Examiner', /examiner/i)

    // Supervisor(s) - click to open, then select from results
    await searchAndSelectMultiSelect(page, 'Supervisor(s)', /supervisor/i)

    // Research Group should be pre-filled for single-group examiners
    const researchGroupInput = page.getByRole('textbox', { name: 'Research Group' })
    await expect(researchGroupInput).not.toHaveValue('', { timeout: 5_000 })

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

test.describe('Topic Workflow - Examiner2 creates a topic for DSA group', () => {
  test.use({ storageState: authStatePath('examiner2') })

  test('examiner2 can access the create topic form', async ({
    page,
  }) => {
    test.setTimeout(120_000)

    await navigateTo(page, '/topics')
    await expect(page.getByRole('heading', { name: 'Manage Topics', exact: true })).toBeVisible({
      timeout: 30_000,
    })

    // Should show seeded DSA topics (Anomaly Detection is Topic 3, DSA group)
    await expect(page.getByText(/Anomaly Detection/i).first()).toBeVisible({ timeout: 10_000 })

    // Open the Create Topic modal
    await page.getByRole('button', { name: 'Create Topic' }).click()
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // Verify essential fields exist in the form
    await expect(dialog.getByLabel('Title')).toBeVisible()

    // Close without creating
    const closeButton = dialog
      .getByRole('button', { name: 'Cancel' })
      .or(dialog.locator('button.mantine-Modal-close'))
    await closeButton.first().click()
    await expect(dialog).not.toBeVisible({ timeout: 3_000 })
  })
})
