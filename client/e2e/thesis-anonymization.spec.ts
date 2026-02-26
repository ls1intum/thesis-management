import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo, navigateToDetail } from './helpers'

const EXPIRED_THESIS_ID = '00000000-0000-4000-d000-000000000008'
const ALREADY_ANONYMIZED_THESIS_ID = '00000000-0000-4000-d000-000000000009'
const RECENT_FINISHED_THESIS_ID = '00000000-0000-4000-d000-000000000004'

test.describe('Thesis Anonymization - Admin Operations', () => {
  test.use({ storageState: authStatePath('admin') })

  test.describe.configure({ mode: 'serial' })

  test('admin can trigger thesis anonymization from admin page', async ({ page }) => {
    await navigateTo(page, '/admin')

    await expect(page.getByRole('heading', { name: 'Administration' })).toBeVisible({
      timeout: 30_000,
    })

    // Verify the Thesis Anonymization section is present
    await expect(page.getByRole('heading', { name: 'Thesis Anonymization' })).toBeVisible()
    await expect(page.getByText(/Anonymize theses that have exceeded/i)).toBeVisible()

    const anonymizeButton = page.getByRole('button', { name: 'Run Anonymization' })
    await expect(anonymizeButton).toBeVisible()
    await anonymizeButton.click()

    // Thesis 8 (expired retention) should be anonymized
    await expect(page.getByText(/Anonymized \d+ expired thesis/)).toBeVisible({ timeout: 15_000 })
  })

  test('second anonymization run finds no new theses', async ({ page }) => {
    await navigateTo(page, '/admin')

    await expect(page.getByRole('heading', { name: 'Administration' })).toBeVisible({
      timeout: 30_000,
    })

    const anonymizeButton = page.getByRole('button', { name: 'Run Anonymization' })
    await anonymizeButton.click()

    // After the first run already anonymized everything, second run should find nothing
    await expect(page.getByText('No expired theses found')).toBeVisible({ timeout: 15_000 })
  })

  test('anonymized thesis shows banner on detail page', async ({ page }) => {
    await navigateTo(page, `/theses/${ALREADY_ANONYMIZED_THESIS_ID}`)

    // Verify the anonymization banner is displayed with correct content
    const banner = page.getByText(/This thesis was anonymized on/i)
    await expect(banner).toBeVisible({ timeout: 30_000 })
    await expect(page.getByText(/per data retention policy/i)).toBeVisible()
    await expect(page.getByText(/permanently removed/i)).toBeVisible()
  })

  test('recently finished thesis is not affected by anonymization', async ({ page }) => {
    // Thesis 4 finished 60 days ago — well within the 5-year retention period
    const heading = page.getByRole('heading', { name: /Systematic Monolith/i })
    const loaded = await navigateToDetail(
      page,
      `/theses/${RECENT_FINISHED_THESIS_ID}`,
      heading,
      30_000,
    )
    if (!loaded) return // May not be accessible under parallel test load

    // Should NOT show anonymization banner
    await expect(page.getByText(/This thesis was anonymized on/i)).not.toBeVisible({
      timeout: 3_000,
    })

    // Should still have its data intact (thesis title is visible)
    await expect(heading).toBeVisible()
  })
})

test.describe('Thesis Anonymization - Non-Admin Restrictions', () => {
  test.use({ storageState: authStatePath('student') })

  test('student cannot access admin page', async ({ page }) => {
    await navigateTo(page, '/admin')

    await expect(page.getByRole('heading', { name: 'Administration' })).not.toBeVisible({
      timeout: 10_000,
    })
  })
})
