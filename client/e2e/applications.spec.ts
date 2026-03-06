import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo, navigateToDetail } from './helpers'

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

    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 30_000 })
    // Student should see My Applications section
    await expect(page.getByRole('heading', { name: /my applications/i })).toBeVisible({
      timeout: 15_000,
    })
  })
})

test.describe('Applications - Supervisor review', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('review page loads with application sidebar', async ({ page }) => {
    await navigateTo(page, '/applications')

    await expect(page).toHaveURL(/\/applications/)
    // The page should show the application filter sidebar
    await expect(page.getByPlaceholder(/search applications/i)).toBeVisible({ timeout: 15_000 })
  })
})

test.describe('Applications - Examiner review', () => {
  test.use({ storageState: authStatePath('examiner') })

  test('review page is accessible', async ({ page }) => {
    await navigateTo(page, '/applications')

    await expect(page).toHaveURL(/\/applications/)
    await expect(page.getByPlaceholder(/search applications/i)).toBeVisible({ timeout: 15_000 })
  })

  test('application detail shows student data and topic', async ({ page }) => {
    test.setTimeout(120_000)
    // Navigate to ACCEPTED application: student on topic 1 (stable across re-runs)
    const heading = page.getByRole('heading', { name: 'Student User' })
    const loaded = await navigateToDetail(
      page,
      '/applications/00000000-0000-4000-c000-000000000001',
      heading,
      30_000,
    )
    expect(loaded, 'Application detail page should load').toBeTruthy()

    // Topic accordion button with topic title
    await expect(
      page.getByRole('button', { name: 'Automated Code Review Using Large Language Models' }),
    ).toBeVisible()

    // Motivation section with actual content from seed data
    await expect(page.getByText('Motivation')).toBeVisible()
    await expect(page.getByText(/passionate about LLMs/)).toBeVisible()

    // Student detail values from seed data
    await expect(page.getByText('student@test.local')).toBeVisible()
    await expect(page.getByText('Computer Science')).toBeVisible()
    await expect(page.getByText('Accepted').first()).toBeVisible()
  })
})
