import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe('Navigation - Public pages', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('landing page shows topic browsing UI', async ({ page }) => {
    await navigateTo(page, '/')

    await expect(page.getByText('Find a Thesis Topic')).toBeVisible({ timeout: 15_000 })
    // Topic search and filter controls
    await expect(page.getByPlaceholder(/search thesis topics/i)).toBeVisible()
    await expect(page.getByText('Open Topics')).toBeVisible()
    await expect(page.getByText('Published Topics')).toBeVisible()
    // View toggle (List / Grid)
    await expect(page.getByText('List')).toBeVisible()
    await expect(page.getByText('Grid')).toBeVisible()
    // Table headers for list view
    await expect(page.getByText('Title').first()).toBeVisible()
    await expect(page.getByText('Thesis Types').first()).toBeVisible()

    // Should show open topics from seed data
    await expect(page.getByText(/Automated Code Review/i).first()).toBeVisible({ timeout: 10_000 })
    await expect(page.getByText(/Continuous Integration/i).first()).toBeVisible({ timeout: 5_000 })
  })

  test('about page renders full content', async ({ page }) => {
    await navigateTo(page, '/about')

    // Main heading
    await expect(
      page.getByRole('heading', { name: 'Thesis Management', exact: true }),
    ).toBeVisible()
    // Section headings
    await expect(page.getByRole('heading', { name: 'Project Managers' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Contributors' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Features' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Git Information' })).toBeVisible()
    // Key people
    await expect(page.getByText('Stephan Krusche')).toBeVisible()
    await expect(page.getByText('Fabian Emilius')).toBeVisible()
    // Support contact
    await expect(page.getByText('thesis-management-support.aet@xcit.tum.de')).toBeVisible()
  })

  test('footer links are visible on public pages', async ({ page }) => {
    await navigateTo(page, '/')

    await expect(page.getByText('About')).toBeVisible()
    await expect(page.getByText('Imprint')).toBeVisible()
    await expect(page.getByText('Privacy')).toBeVisible()
  })

  test('unknown single-segment route shows landing page', async ({ page }) => {
    // /:researchGroupAbbreviation catches this before the * wildcard
    await navigateTo(page, '/nonexistent-group')

    await expect(page.getByText('Find a Thesis Topic')).toBeVisible({ timeout: 15_000 })
  })

  test('privacy page is accessible without login', async ({ page }) => {
    await navigateTo(page, '/privacy')

    // Privacy page should render with privacy-related content
    await expect(page.getByText(/privacy/i).first()).toBeVisible({ timeout: 15_000 })
  })

  test('imprint page is accessible without login', async ({ page }) => {
    await navigateTo(page, '/imprint')

    // Imprint page should render
    await expect(page.getByText(/imprint/i).first()).toBeVisible({ timeout: 15_000 })
  })
})

test.describe('Navigation - Student routes', () => {
  test('can navigate between pages via sidebar', async ({ page }) => {
    test.setTimeout(60_000)

    await navigateTo(page, '/dashboard')
    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 15_000 })

    // Navigate to Browse Theses
    await page.getByRole('link', { name: 'Browse Theses' }).click()
    await expect(page).toHaveURL(/\/theses/, { timeout: 15_000 })
    await expect(page.getByRole('heading', { name: /browse theses/i })).toBeVisible({
      timeout: 15_000,
    })

    // Navigate to Submit Application
    await page.getByRole('link', { name: 'Submit Application' }).click()
    await expect(page).toHaveURL(/\/submit-application/, { timeout: 15_000 })

    // Navigate back to Dashboard
    await page.getByRole('link', { name: 'Dashboard' }).click()
    await expect(page).toHaveURL(/\/dashboard/, { timeout: 30_000 })
    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 15_000 })
  })

  test('header logo navigates to dashboard when authenticated', async ({ page }) => {
    await navigateTo(page, '/theses')
    await page.getByText('Thesis Management').first().click()
    await expect(page).toHaveURL(/\/dashboard/)
  })

  test('student can navigate to settings page', async ({ page }) => {
    await navigateTo(page, '/settings')

    await expect(page.getByText('My Information')).toBeVisible({ timeout: 15_000 })
    await expect(page.getByText('Notification Settings')).toBeVisible()
  })

  test('student can navigate to presentations page', async ({ page }) => {
    await navigateTo(page, '/presentations')

    await expect(page.getByRole('heading', { name: 'Presentations', exact: true })).toBeVisible({
      timeout: 15_000,
    })
  })
})

test.describe('Navigation - Examiner routes', () => {
  test.use({ storageState: authStatePath('examiner') })

  test('management pages are accessible', async ({ page }) => {
    // Theses Overview
    await navigateTo(page, '/overview')
    await expect(page.getByRole('heading', { name: /theses overview/i })).toBeVisible({
      timeout: 15_000,
    })

    // Application Review
    await navigateTo(page, '/applications')
    await expect(page).toHaveURL(/\/applications/)
    await expect(page.getByPlaceholder(/search applications/i)).toBeVisible({ timeout: 15_000 })

    // Manage Topics
    await navigateTo(page, '/topics')
    await expect(page.getByRole('heading', { name: /manage topics/i })).toBeVisible()

    // Interviews
    await navigateTo(page, '/interviews')
    await expect(page.getByRole('heading', { name: 'Interviews', exact: true })).toBeVisible()
  })
})

test.describe('Navigation - Admin routes', () => {
  test.use({ storageState: authStatePath('admin') })

  test('admin-only pages are accessible', async ({ page }) => {
    await navigateTo(page, '/research-groups')
    await expect(page.getByRole('heading', { name: /research groups/i })).toBeVisible({
      timeout: 15_000,
    })

    // Admin should see both seeded research groups
    await expect(page.getByText('Applied Software Engineering').first()).toBeVisible({
      timeout: 10_000,
    })
    await expect(page.getByText('Data Science and Analytics').first()).toBeVisible()
  })
})
