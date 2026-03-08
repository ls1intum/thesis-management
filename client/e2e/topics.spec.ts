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

    // Should show all 3 open seeded topics
    await expect(page.getByText(/Automated Code Review/i).first()).toBeVisible({ timeout: 10_000 })
    await expect(page.getByText(/Continuous Integration/i).first()).toBeVisible({ timeout: 5_000 })
    await expect(page.getByText(/Anomaly Detection/i).first()).toBeVisible({ timeout: 5_000 })

    // Can switch between Open Topics and Published Topics (Mantine SegmentedControl uses radio inputs)
    await page.getByRole('radio', { name: 'Published Topics' }).click({ force: true })
    // Published Topics radio should be checked
    await expect(page.getByRole('radio', { name: 'Published Topics' })).toBeChecked()
    // Switch back
    await page.getByRole('radio', { name: 'Open Topics' }).click({ force: true })
    await expect(page.getByText(/Automated Code Review/i).first()).toBeVisible({ timeout: 10_000 })
  })

  test('search filters topics by keyword', async ({ page }) => {
    await navigateTo(page, '/')

    await expect(page.getByText('Find a Thesis Topic')).toBeVisible({ timeout: 15_000 })

    // Search for "Anomaly" — should filter to only the matching topic
    await page.getByPlaceholder(/search thesis topics/i).fill('Anomaly')
    await expect(page.getByText(/Anomaly Detection/i).first()).toBeVisible({ timeout: 10_000 })

    // Other topics should be filtered out
    await expect(page.getByText(/Automated Code Review/i)).toBeHidden({ timeout: 3_000 })
  })

  test('can switch between list and grid view', async ({ page }) => {
    await navigateTo(page, '/')

    await expect(page.getByText('Find a Thesis Topic')).toBeVisible({ timeout: 15_000 })
    // Default view controls
    await expect(page.getByText('List')).toBeVisible()
    await expect(page.getByText('Grid')).toBeVisible()

    // Switch to grid view
    await page.getByText('Grid').click()
    // Grid should show topic cards (not table rows)
    await expect(page.getByText(/Automated Code Review/i).first()).toBeVisible({ timeout: 10_000 })

    // Switch back to list view
    await page.getByText('List').click()
    // Table headers should be back
    await expect(page.getByText('Title').first()).toBeVisible()
    // Topics should still be visible
    await expect(page.getByText(/Automated Code Review/i).first()).toBeVisible({ timeout: 5_000 })
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

    // Should show seeded topics belonging to examiner's group (ASE)
    await expect(page.getByText(/Automated Code Review/i).first()).toBeVisible({ timeout: 10_000 })
    await expect(page.getByText(/Continuous Integration/i).first()).toBeVisible({ timeout: 5_000 })
  })

  test('topic detail page shows topic information and sections', async ({ page }) => {
    // Navigate to seeded open topic 1 (Automated Code Review)
    await navigateTo(page, '/topics/00000000-0000-4000-b000-000000000001')

    // Should show topic title
    await expect(page.getByRole('heading', { name: /automated code review/i })).toBeVisible({
      timeout: 15_000,
    })
    // Should show problem statement section
    await expect(page.getByText(/problem statement/i).first()).toBeVisible()
    // Should show actual problem statement text from seed data
    await expect(page.getByText(/Manual code reviews are time-consuming/i).first()).toBeVisible()

    // Should show topic metadata (type)
    await expect(page.getByText(/master/i).first()).toBeVisible()

    // Should show assigned examiner and supervisor from seed data
    await expect(page.getByText(/Examiner User/i).first()).toBeVisible()
    await expect(page.getByText(/Supervisor User/i).first()).toBeVisible()

    // Examiner should see applications section for this topic
    await expect(page.getByText(/application/i).first()).toBeVisible()
  })

  test('topic detail for CI Pipeline topic shows multiple thesis types', async ({ page }) => {
    // Topic 2: CI Pipeline Optimization, has BACHELOR + MASTER types
    await navigateTo(page, '/topics/00000000-0000-4000-b000-000000000002')

    await expect(page.getByRole('heading', { name: /continuous integration/i })).toBeVisible({
      timeout: 15_000,
    })
    // Should show problem statement from seed
    await expect(page.getByText(/CI pipelines in large projects/i).first()).toBeVisible()
    // Should show both thesis types
    await expect(page.getByText(/bachelor/i).first()).toBeVisible()
    await expect(page.getByText(/master/i).first()).toBeVisible()
  })
})

test.describe('Topics - Student view', () => {
  test('topic detail page shows topic info and apply button for student', async ({ page }) => {
    // Student viewing an open topic (CI Pipeline Optimization)
    await navigateTo(page, '/topics/00000000-0000-4000-b000-000000000002')

    // Should show topic title
    await expect(page.getByRole('heading', { name: /continuous integration/i })).toBeVisible({
      timeout: 15_000,
    })

    // Should show topic details
    await expect(page.getByText(/problem statement/i).first()).toBeVisible()

    // Student should see Apply Now button
    await expect(
      page
        .getByRole('link', { name: /apply now/i })
        .or(page.getByRole('button', { name: /apply/i })),
    ).toBeVisible()

    // Should show topic types
    await expect(page.getByText(/master/i).first()).toBeVisible()
  })
})
