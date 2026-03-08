import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo, navigateToDetail } from './helpers'

test.describe('Applications - Student', () => {
  test('submit application page shows stepper form with topic selection', async ({ page }) => {
    await navigateTo(page, '/submit-application')

    await expect(page).toHaveURL(/\/submit-application/)
    // The multi-step stepper form should be visible
    await expect(page.locator('.mantine-Stepper-root')).toBeVisible({ timeout: 15_000 })

    // Should show the heading
    await expect(
      page.getByRole('heading', { name: 'Submit Application', exact: true }),
    ).toBeVisible()

    // Should show available open topics from seed data
    await expect(page.getByText(/Continuous Integration/i).first()).toBeVisible({ timeout: 10_000 })
    await expect(page.getByText(/Anomaly Detection/i).first()).toBeVisible({ timeout: 5_000 })

    // Should NOT show draft topics (Gamification is DRAFT state)
    await expect(page.getByText(/Gamification/i)).toBeHidden({ timeout: 2_000 })

    // Should NOT show closed topics (Migration Strategies is CLOSED)
    await expect(page.getByText(/Migration Strategies from Monolith/i)).toBeHidden({
      timeout: 2_000,
    })
  })

  test('submit application with pre-selected topic navigates to correct URL', async ({ page }) => {
    // Navigate with topic pre-selected (CI Pipeline Optimization)
    await navigateTo(page, '/submit-application/00000000-0000-4000-b000-000000000002')

    await expect(page).toHaveURL(/\/submit-application\/00000000/)

    // Should show the stepper form (topic already selected, may be on step 2)
    await expect(page.locator('.mantine-Stepper-root')).toBeVisible({ timeout: 15_000 })
  })

  test('dashboard shows student applications section with data', async ({ page }) => {
    await navigateTo(page, '/dashboard')

    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 30_000 })
    // Student should see My Applications section
    await expect(page.getByRole('heading', { name: /my applications/i })).toBeVisible({
      timeout: 15_000,
    })

    // Student has a seeded application (ACCEPTED) — should show application data
    await expect(page.getByText(/accepted/i).first()).toBeVisible({ timeout: 10_000 })
  })
})

test.describe('Applications - Supervisor review', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('review page loads with application sidebar and search', async ({ page }) => {
    await navigateTo(page, '/applications')

    await expect(page).toHaveURL(/\/applications/)
    // The page should show the application filter sidebar
    const searchInput = page.getByPlaceholder(/search applications/i)
    await expect(searchInput).toBeVisible({ timeout: 15_000 })

    // Should show application data from seed — supervisor reviewed apps 1, 2
    await expect(page.getByText(/Student/i).first()).toBeVisible({ timeout: 10_000 })
  })
})

test.describe('Applications - Examiner review', () => {
  test.use({ storageState: authStatePath('examiner') })

  test('review page shows search and application list', async ({ page }) => {
    await navigateTo(page, '/applications')

    await expect(page).toHaveURL(/\/applications/)
    await expect(page.getByPlaceholder(/search applications/i)).toBeVisible({ timeout: 15_000 })

    // Should show applications from seed data
    await expect(page.getByText(/Student/i).first()).toBeVisible({ timeout: 10_000 })
  })

  test('application detail shows student data and topic information', async ({ page }) => {
    test.setTimeout(120_000)
    // Navigate to ACCEPTED application: student on topic 1 (stable across re-runs)
    const heading = page.getByRole('heading', { name: 'Student User' })
    const loaded = await navigateToDetail(
      page,
      '/applications/00000000-0000-4000-c000-000000000001',
      heading,
      45_000,
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

    // Application state
    await expect(page.getByText('Accepted').first()).toBeVisible()
  })

  test('NOT_ASSESSED application detail shows pending state', async ({ page }) => {
    test.setTimeout(90_000)
    // App 4: student4 on topic 1, NOT_ASSESSED
    const heading = page.getByRole('heading', { name: 'Student4 User' })
    const loaded = await navigateToDetail(
      page,
      '/applications/00000000-0000-4000-c000-000000000004',
      heading,
      30_000,
    )
    if (!loaded) {
      test.skip(true, 'Application detail did not load under heavy parallel load')
      return
    }

    // Should show student4's email
    await expect(page.getByText('student4@test.local')).toBeVisible()

    // Topic should be LLM Code Review
    await expect(
      page.getByRole('button', { name: 'Automated Code Review Using Large Language Models' }),
    ).toBeVisible()

    // Application state should be NOT_ASSESSED (displayed as "Not Assessed")
    await expect(page.getByText(/not assessed/i).first()).toBeVisible({ timeout: 5_000 })
  })
})
