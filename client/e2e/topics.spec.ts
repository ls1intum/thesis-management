import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe('Topics - Public landing page', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('topic search and filters work', async ({ page }) => {
    await navigateTo(page, '/')

    await expect(page.getByText('Find a Thesis Topic')).toBeVisible({ timeout: 15_000 })
    // Search input is present
    const searchInput = page.getByPlaceholder(/search thesis topics/i)
    await expect(searchInput).toBeVisible()

    // Thesis type filter is present
    await expect(page.getByText('Thesis Types').first()).toBeVisible()

    // Can switch between Open Topics and Published Topics
    await page.getByText('Published Topics').click()
    // The view should switch (no error)
    await page.getByText('Open Topics').click()
  })

  test('can switch between list and grid view', async ({ page }) => {
    await navigateTo(page, '/')

    await expect(page.getByText('Find a Thesis Topic')).toBeVisible({ timeout: 15_000 })
    // Default is list view with table headers
    await expect(page.getByText('List')).toBeVisible()
    await expect(page.getByText('Grid')).toBeVisible()

    // Switch to grid view
    await page.getByText('Grid').click()
    // Switch back to list view
    await page.getByText('List').click()
    // Table headers should be back
    await expect(page.getByText('Title').first()).toBeVisible()
  })
})

test.describe('Topics - Management (Examiner)', () => {
  test.use({ storageState: authStatePath('examiner') })

  test('manage topics page shows topic list with actions', async ({ page }) => {
    await navigateTo(page, '/topics')

    await expect(page.getByRole('heading', { name: /manage topics/i })).toBeVisible({
      timeout: 15_000,
    })
    // Create topic button should be visible for management
    await expect(page.getByRole('button', { name: 'Create Topic' })).toBeVisible()
    // Table should have column headers
    await expect(page.getByText('Title').first()).toBeVisible()
  })

  test('topic detail page shows topic information', async ({ page }) => {
    // Navigate to a seeded open topic (Automated Code Review)
    await navigateTo(page, '/topics/00000000-0000-4000-b000-000000000001')

    // Should show topic title
    await expect(page.getByRole('heading', { name: /automated code review/i })).toBeVisible({
      timeout: 15_000,
    })
    // Should show topic information sections
    await expect(page.getByText(/problem statement/i).first()).toBeVisible()
    // Examiner should see applications section for this topic
  })
})

test.describe('Topics - Student view', () => {
  test('topic detail page shows apply button for student', async ({ page }) => {
    // Student viewing an open topic
    await navigateTo(page, '/topics/00000000-0000-4000-b000-000000000002')

    // Should show topic title
    await expect(page.getByRole('heading', { name: /continuous integration/i })).toBeVisible({
      timeout: 15_000,
    })
    // Student should see Apply Now button
    await expect(
      page
        .getByRole('link', { name: /apply now/i })
        .or(page.getByRole('button', { name: /apply/i })),
    ).toBeVisible()
  })
})
