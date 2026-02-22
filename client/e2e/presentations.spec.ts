import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe('Presentations - Student', () => {
  test('presentations page shows heading and calendar', async ({ page }) => {
    await navigateTo(page, '/presentations')

    await expect(page.getByRole('heading', { name: 'Presentations', exact: true })).toBeVisible({ timeout: 15_000 })
  })
})

test.describe('Presentations - Supervisor', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('presentations page is accessible for supervisor', async ({ page }) => {
    await navigateTo(page, '/presentations')

    await expect(page.getByRole('heading', { name: 'Presentations', exact: true })).toBeVisible({ timeout: 15_000 })
  })
})

test.describe('Presentations - Public', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('presentation detail page is accessible without login', async ({ page }) => {
    // Seeded public presentation for thesis 3 (final, public visibility)
    await navigateTo(page, '/presentations/00000000-0000-4000-e300-000000000002')

    // Should show presentation info (thesis title)
    await expect(page.getByRole('heading', { name: /anomaly detection/i })).toBeVisible({ timeout: 15_000 })
  })
})
