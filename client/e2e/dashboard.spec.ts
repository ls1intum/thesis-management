import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe('Dashboard - Student', () => {
  test('shows dashboard with My Theses and My Applications', async ({ page }) => {
    await navigateTo(page, '/dashboard')

    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 15_000 })
    // Student should see My Theses section
    await expect(page.getByRole('heading', { name: /my theses/i })).toBeVisible()
    // Student should see My Applications section
    await expect(page.getByRole('heading', { name: /my applications/i })).toBeVisible()
  })
})

test.describe('Dashboard - Advisor', () => {
  test.use({ storageState: authStatePath('advisor') })

  test('shows dashboard with My Theses section', async ({ page }) => {
    await navigateTo(page, '/dashboard')

    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 15_000 })
    await expect(page.getByRole('heading', { name: /my theses/i })).toBeVisible()
  })
})

test.describe('Dashboard - Supervisor', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('shows dashboard with management view', async ({ page }) => {
    await navigateTo(page, '/dashboard')

    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 15_000 })
    await expect(page.getByRole('heading', { name: /my theses/i })).toBeVisible()
  })
})

test.describe('Dashboard - Admin', () => {
  test.use({ storageState: authStatePath('admin') })

  test('admin can access dashboard', async ({ page }) => {
    await navigateTo(page, '/dashboard')

    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 15_000 })
  })
})
