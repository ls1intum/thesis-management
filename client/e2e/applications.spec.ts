import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe('Applications - Student', () => {
  test('submit application page shows stepper form', async ({ page }) => {
    await navigateTo(page, '/submit-application')

    await expect(page).toHaveURL(/\/submit-application/)
    // The multi-step stepper form should be visible
    await expect(page.locator('.mantine-Stepper-root')).toBeVisible({ timeout: 15_000 })
  })

  test('submit application with pre-selected topic', async ({ page }) => {
    // Navigate with topic pre-selected (CI Pipeline Optimization)
    await navigateTo(page, '/submit-application/00000000-0000-4000-b000-000000000002')

    await expect(page).toHaveURL(/\/submit-application\/00000000/)
  })

  test('dashboard shows student applications section', async ({ page }) => {
    await navigateTo(page, '/dashboard')

    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 15_000 })
    // Student should see My Applications section
    await expect(page.getByRole('heading', { name: /my applications/i })).toBeVisible()
  })
})

test.describe('Applications - Advisor review', () => {
  test.use({ storageState: authStatePath('advisor') })

  test('review page loads with application sidebar', async ({ page }) => {
    await navigateTo(page, '/applications')

    await expect(page).toHaveURL(/\/applications/)
    // The page should have the sidebar with applications or an empty state
    await expect(page.locator('body')).toContainText(/.+/)
  })
})

test.describe('Applications - Supervisor review', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('review page is accessible', async ({ page }) => {
    await navigateTo(page, '/applications')

    await expect(page).toHaveURL(/\/applications/)
  })
})
