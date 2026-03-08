import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe('Dashboard - Student', () => {
  test('shows dashboard with My Theses and My Applications', async ({ page }) => {
    await navigateTo(page, '/dashboard')

    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 15_000 })
    // Student should see both dashboard sections
    await expect(page.getByRole('heading', { name: /my theses/i })).toBeVisible()
    await expect(page.getByRole('heading', { name: /my applications/i })).toBeVisible()

    // Student is assigned to thesis 1 (WRITING) and thesis 4 (FINISHED)
    await expect(page.getByText(/Automated Code Review/i).first()).toBeVisible({ timeout: 10_000 })

    // Student has an ACCEPTED application for topic 1 — should show application data
    await expect(page.getByText(/accepted/i).first()).toBeVisible({ timeout: 10_000 })
  })
})

test.describe('Dashboard - Supervisor', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('shows dashboard with My Theses and thesis data', async ({ page }) => {
    await navigateTo(page, '/dashboard')

    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 15_000 })
    await expect(page.getByRole('heading', { name: /my theses/i })).toBeVisible()

    // Supervisor is assigned to thesis 1 (WRITING) and thesis 2 (PROPOSAL)
    await expect(page.getByText(/Automated Code Review/i).first()).toBeVisible({ timeout: 10_000 })

    // Supervisor should also see thesis 2
    await expect(page.getByText(/CI Pipeline Optimization/i).first()).toBeVisible({
      timeout: 5_000,
    })
  })
})

test.describe('Dashboard - Examiner', () => {
  test.use({ storageState: authStatePath('examiner') })

  test('shows dashboard with theses management view', async ({ page }) => {
    await navigateTo(page, '/dashboard')

    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 15_000 })
    await expect(page.getByRole('heading', { name: /my theses/i })).toBeVisible()

    // Examiner is assigned to thesis 1 (WRITING) and thesis 2 (PROPOSAL) as examiner
    await expect(page.getByText(/Automated Code Review/i).first()).toBeVisible({ timeout: 10_000 })
    await expect(page.getByText(/CI Pipeline Optimization/i).first()).toBeVisible({
      timeout: 5_000,
    })
  })
})

test.describe('Dashboard - Admin', () => {
  test.use({ storageState: authStatePath('admin') })

  test('admin can access dashboard', async ({ page }) => {
    await navigateTo(page, '/dashboard')

    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 15_000 })
    // Admin should see the dashboard without errors
    await expect(page.getByText(/error|something went wrong/i)).not.toBeVisible({ timeout: 2_000 })
  })
})
