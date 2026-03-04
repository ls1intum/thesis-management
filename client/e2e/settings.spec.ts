import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe('Settings - Student', () => {
  test('settings page shows My Information and Notification Settings tabs', async ({ page }) => {
    await navigateTo(page, '/settings')

    await expect(page).toHaveURL(/\/settings/)
    // Tab navigation should be visible
    await expect(page.getByText('My Information')).toBeVisible({ timeout: 15_000 })
    await expect(page.getByText('Notification Settings')).toBeVisible()

    // My Information tab should show user profile fields from seed data
    await expect(page.getByLabel('First Name')).toBeVisible()
    await expect(page.getByLabel('Last Name')).toBeVisible()
    await expect(page.getByLabel('Email')).toBeVisible()

    // Verify seed data is pre-filled for student user
    await expect(page.getByLabel('First Name')).toHaveValue('Student')
    await expect(page.getByLabel('Last Name')).toHaveValue('User')
    await expect(page.getByLabel('Email')).toHaveValue('student@test.local')

    // Student should see matriculation number field
    await expect(page.getByLabel(/Matriculation Number/i)).toBeVisible()
  })

  test('notifications tab shows notification preferences', async ({ page }) => {
    await navigateTo(page, '/settings/notifications')

    await expect(page).toHaveURL(/\/settings\/notifications/)
    await expect(page.getByText('Notification Settings')).toBeVisible({ timeout: 15_000 })

    // Should show notification preference options (email notification toggles)
    await expect(page.getByText(/email/i).first()).toBeVisible()
  })
})

test.describe('Settings - Supervisor', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('settings page shows supervisor profile data', async ({ page }) => {
    await navigateTo(page, '/settings')

    await expect(page.getByText('My Information')).toBeVisible({ timeout: 15_000 })
    await expect(page.getByText('Notification Settings')).toBeVisible()

    // Verify supervisor-specific profile data from seed
    await expect(page.getByLabel('First Name')).toHaveValue('Supervisor')
    await expect(page.getByLabel('Last Name')).toHaveValue('User')
    await expect(page.getByLabel('Email')).toHaveValue('supervisor@test.local')
  })
})

test.describe('Settings - Examiner', () => {
  test.use({ storageState: authStatePath('examiner') })

  test('settings page shows examiner profile data', async ({ page }) => {
    await navigateTo(page, '/settings')

    await expect(page.getByText('My Information')).toBeVisible({ timeout: 15_000 })

    // Verify examiner-specific profile data from seed
    await expect(page.getByLabel('First Name')).toHaveValue('Examiner')
    await expect(page.getByLabel('Last Name')).toHaveValue('User')
    await expect(page.getByLabel('Email')).toHaveValue('examiner@test.local')
  })

  test('examiner notification settings show expected preferences', async ({ page }) => {
    await navigateTo(page, '/settings/notifications')

    await expect(page.getByText('Notification Settings')).toBeVisible({ timeout: 15_000 })

    // Examiner has notification settings configured in seed data
    await expect(page.getByText(/email/i).first()).toBeVisible()
  })
})
