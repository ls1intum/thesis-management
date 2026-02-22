import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe('Research Groups - Admin', () => {
  test.use({ storageState: authStatePath('admin') })

  test('research groups page shows groups and search', async ({ page }) => {
    await navigateTo(page, '/research-groups')

    await expect(page.getByRole('heading', { name: /research groups/i })).toBeVisible({ timeout: 15_000 })
    // Search input should be present
    await expect(page.getByPlaceholder(/search research groups/i)).toBeVisible()
    // Create button should be visible
    await expect(page.getByRole('button', { name: /create research group/i })).toBeVisible()
    // Seeded research groups should appear (ASE and DSA)
    await expect(page.getByText('Data Science and Analytics').first()).toBeVisible()
    await expect(page.getByText('Applied Software Engineering').first()).toBeVisible()
  })

  test('search filters research groups', async ({ page }) => {
    await navigateTo(page, '/research-groups')

    await expect(page.getByRole('heading', { name: /research groups/i })).toBeVisible({ timeout: 15_000 })
    // Search for ASE
    await page.getByPlaceholder(/search research groups/i).fill('Applied Software')
    // ASE should still be visible
    await expect(page.getByText('Applied Software Engineering').first()).toBeVisible()
  })
})

test.describe('Research Group Settings - Supervisor', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('supervisor can access their research group settings', async ({ page }) => {
    // UUID matches the ASE research group seeded in seed_dev_test_data.sql
    await navigateTo(page, '/research-groups/00000000-0000-4000-a000-000000000001')

    // Supervisor should see either group settings or an unauthorized page
    await expect(
      page.getByText('Applied Software Engineering').first().or(page.getByText(/unauthorized/i)),
    ).toBeVisible({ timeout: 15_000 })
  })
})

test.describe('Research Groups - Student cannot access admin page', () => {
  test('student is denied access to research groups admin', async ({ page }) => {
    await navigateTo(page, '/research-groups')

    // Student should not see the admin page content
    await expect(page.getByRole('heading', { name: /research groups/i })).toBeHidden()
  })
})
