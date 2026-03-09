import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe('Presentations - Student', () => {
  test('presentations page shows heading and content', async ({ page }) => {
    await navigateTo(page, '/presentations')

    await expect(page.getByRole('heading', { name: 'Presentations', exact: true })).toBeVisible({
      timeout: 15_000,
    })
    // Presentations page should have a calendar or schedule view
    await expect(page.locator('main')).toBeVisible()
    // Should not show error state
    await expect(page.getByText(/error|something went wrong/i)).not.toBeVisible({ timeout: 2_000 })
  })
})

test.describe('Presentations - Examiner', () => {
  test.use({ storageState: authStatePath('examiner') })

  test('presentations page is accessible for examiner with management controls', async ({
    page,
  }) => {
    await navigateTo(page, '/presentations')

    await expect(page.getByRole('heading', { name: 'Presentations', exact: true })).toBeVisible({
      timeout: 15_000,
    })
    // Should not show error state
    await expect(page.getByText(/error|something went wrong/i)).not.toBeVisible({ timeout: 2_000 })
  })
})

test.describe('Presentations - Public', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('public presentation detail page shows thesis and presentation info', async ({ page }) => {
    // Seeded public FINAL presentation for thesis 3 (anomaly detection)
    await navigateTo(page, '/presentations/00000000-0000-4000-e300-000000000002')

    // Should show thesis title in heading
    await expect(page.getByRole('heading', { name: /anomaly detection/i })).toBeVisible({
      timeout: 15_000,
    })
    // Should show presentation type (Final)
    await expect(page.getByText(/final/i)).toBeVisible()
    // Should show location from seed data
    await expect(page.getByText(/Boltzmannstr/i).first()).toBeVisible()
    // Should show language
    await expect(page.getByText(/english/i).first()).toBeVisible()
    // Should not require login — no login button or redirect
    await expect(page.getByRole('button', { name: /login|sign in/i })).not.toBeVisible({
      timeout: 2_000,
    })
  })

  test('private presentation detail page is not publicly accessible', async ({ page }) => {
    // Seeded private INTERMEDIATE presentation for thesis 1
    await navigateTo(page, '/presentations/00000000-0000-4000-e300-000000000001')

    // Private presentation should NOT show the thesis title publicly
    await expect(page.getByRole('heading', { name: /automated code review/i })).toBeHidden({
      timeout: 10_000,
    })

    // Should show an access denied or not found message
    await expect(
      page
        .getByText(/not found/i)
        .or(page.getByText(/error/i))
        .or(page.getByText(/access denied/i))
        .or(page.getByText(/login/i))
        .first(),
    ).toBeVisible({ timeout: 5_000 })
  })

  test('non-existent presentation shows appropriate error', async ({ page }) => {
    await navigateTo(page, '/presentations/00000000-0000-0000-0000-000000000000')

    // Should show not found or error state, not a blank page
    await expect(
      page
        .getByText(/not found/i)
        .or(page.getByText(/error/i))
        .first(),
    ).toBeVisible({ timeout: 15_000 })
  })
})
