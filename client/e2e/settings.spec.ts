import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe('Settings - Student', () => {
  test('settings page shows My Information and Notification Settings tabs', async ({ page }) => {
    await navigateTo(page, '/settings')

    await expect(page).toHaveURL(/\/settings/)
    // Tab navigation should be visible
    await expect(page.getByText('My Information')).toBeVisible({ timeout: 15_000 })
    await expect(page.getByText('Notification Settings')).toBeVisible()
  })

  test('notifications tab is accessible', async ({ page }) => {
    await navigateTo(page, '/settings/notifications')

    await expect(page).toHaveURL(/\/settings\/notifications/)
    await expect(page.getByText('Notification Settings')).toBeVisible({ timeout: 15_000 })
  })
})

test.describe('Settings - Supervisor', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('settings page is accessible for supervisor', async ({ page }) => {
    await navigateTo(page, '/settings')

    await expect(page.getByText('My Information')).toBeVisible({ timeout: 15_000 })
    await expect(page.getByText('Notification Settings')).toBeVisible()
  })
})
