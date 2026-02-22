import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo, searchAndSelectMultiSelect, selectOption } from './helpers'

test.describe('Thesis Workflow - Supervisor creates a thesis', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('supervisor can create a new thesis via the browse theses page', async ({ page }) => {
    await navigateTo(page, '/theses')
    await expect(
      page.getByRole('heading', { name: 'Browse Theses', exact: true }),
    ).toBeVisible({ timeout: 15_000 })

    // Click "Create Thesis" button
    await page.getByRole('button', { name: 'Create Thesis' }).click()

    // Modal should open
    await expect(page.getByRole('dialog')).toBeVisible({ timeout: 5_000 })

    // Fill in the thesis creation form
    await page.getByLabel('Thesis Title').fill('E2E Test Thesis: Performance Analysis')

    // Thesis Type
    await selectOption(page, 'Thesis Type', /master/i)

    // Language
    await selectOption(page, 'Thesis Language', /english/i)

    // Student(s) - search and select
    await searchAndSelectMultiSelect(page, 'Student(s)', /student4/i)
    await page.keyboard.press('Escape')

    // Supervisor(s) - search and select advisor
    await searchAndSelectMultiSelect(page, 'Supervisor(s)', /advisor/i)
    await page.keyboard.press('Escape')

    // Examiner - search and select self (supervisor)
    await searchAndSelectMultiSelect(page, 'Examiner', /supervisor/i)

    // Research Group should be pre-filled

    // Click "Create Thesis"
    const createButton = page
      .getByRole('dialog')
      .getByRole('button', { name: 'Create Thesis' })
    await expect(createButton).toBeEnabled({ timeout: 10_000 })
    await createButton.click()

    // Should navigate to the new thesis detail page
    await expect(page).toHaveURL(/\/theses\//, { timeout: 15_000 })
    await expect(
      page.getByRole('heading', { name: 'E2E Test Thesis: Performance Analysis' }),
    ).toBeVisible({ timeout: 15_000 })
  })
})
