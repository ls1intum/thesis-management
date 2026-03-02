import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe('Data Export - Student', () => {
  test.use({ storageState: authStatePath('student') })

  // Tests must run serially: requesting an export rate-limits the user for 7 days,
  // so subsequent tests in the same session see a disabled button.
  test.describe.configure({ mode: 'serial' })

  test('data export page shows informational text and request button', async ({ page }) => {
    await navigateTo(page, '/data-export')

    await expect(page.getByRole('heading', { name: 'Data Export' })).toBeVisible({
      timeout: 30_000,
    })

    // Check for informational text explaining what data is exported
    await expect(page.getByText(/request an export of all your personal data/i)).toBeVisible()
    await expect(page.getByText(/profile information/i)).toBeVisible()
    await expect(page.getByText(/ZIP file/i)).toBeVisible()

    // Check for request button
    const requestButton = page.getByRole('button', { name: /Request Data Export/i })
    await expect(requestButton).toBeVisible()

    // Check for rate-limit info text
    await expect(page.getByText(/7 days/i)).toBeVisible()
  })

  test('can request a data export and see processing status', async ({ page }) => {
    await navigateTo(page, '/data-export')

    await expect(page.getByRole('heading', { name: 'Data Export' })).toBeVisible({
      timeout: 30_000,
    })

    const requestButton = page.getByRole('button', { name: /Request Data Export/i })

    // The button may be disabled if the student already requested an export (from prior E2E runs)
    const isEnabled = await requestButton.isEnabled({ timeout: 5_000 }).catch(() => false)
    if (!isEnabled) {
      // Already has an export — verify the status section shows meaningful info
      await expect(page.getByText('Latest Export')).toBeVisible({ timeout: 10_000 })
      await expect(page.getByText(/Status:/i)).toBeVisible()
      // Verify a status badge is shown (Processing, Ready for Download, Downloaded, or Expired)
      await expect(
        page.getByText(/Processing|Ready for Download|Downloaded|Expired|Failed/i),
      ).toBeVisible()
      return
    }

    await requestButton.click()

    // Should show success notification
    await expect(page.getByText(/Data export requested/i)).toBeVisible({ timeout: 15_000 })

    // Reload and verify status persists
    await navigateTo(page, '/data-export')

    await expect(page.getByRole('heading', { name: 'Data Export' })).toBeVisible({
      timeout: 30_000,
    })

    // Should show the Latest Export section with status info
    await expect(page.getByText('Latest Export')).toBeVisible({ timeout: 10_000 })
    await expect(page.getByText(/Status:/i)).toBeVisible()
    // After requesting, status should be Processing (REQUESTED or IN_CREATION)
    await expect(page.getByText(/Processing/i)).toBeVisible({ timeout: 10_000 })

    // Request button should now be disabled (rate-limited)
    await expect(requestButton).toBeDisabled()
  })
})

test.describe('Data Export - Privacy Page Link (authenticated)', () => {
  test.use({ storageState: authStatePath('student') })

  test('can navigate to data export page from privacy page', async ({ page }) => {
    await navigateTo(page, '/privacy')

    await expect(page.getByRole('heading', { name: 'Privacy' }).first()).toBeVisible({
      timeout: 30_000,
    })

    // The link is at the bottom of the privacy page — scroll to it
    const exportLink = page.getByRole('link', { name: 'Go to Data Export' })
    await exportLink.scrollIntoViewIfNeeded()
    await expect(exportLink).toBeVisible({ timeout: 5_000 })
    await exportLink.click()

    await expect(page).toHaveURL(/\/data-export/, { timeout: 15_000 })
    await expect(page.getByRole('heading', { name: 'Data Export' })).toBeVisible({
      timeout: 15_000,
    })
  })
})

test.describe('Data Export - Privacy Page Link (unauthenticated)', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('unauthenticated users do not see data export link on privacy page', async ({ page }) => {
    await navigateTo(page, '/privacy')

    await expect(page.getByRole('heading', { name: 'Privacy' }).first()).toBeVisible({
      timeout: 30_000,
    })

    // Data export link should not be visible for unauthenticated users
    const exportLink = page.getByRole('link', { name: 'Go to Data Export' })
    await expect(exportLink).not.toBeVisible({ timeout: 3_000 })
  })
})

test.describe('Data Export - Route Protection (authenticated)', () => {
  test.use({ storageState: authStatePath('student') })

  test('data export page is accessible for authenticated users', async ({ page }) => {
    await navigateTo(page, '/data-export')

    await expect(page.getByRole('heading', { name: 'Data Export' })).toBeVisible({
      timeout: 30_000,
    })

    // Verify the page is fully rendered (not just a heading)
    await expect(page.getByText(/request an export/i)).toBeVisible()
    await expect(page.getByRole('button', { name: /Request Data Export/i })).toBeVisible()
  })
})

test.describe('Data Export - Route Protection (unauthenticated)', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('unauthenticated users are redirected from data export page', async ({ page }) => {
    await page.goto('/data-export', { waitUntil: 'domcontentloaded', timeout: 30_000 })

    // Should redirect to login — the Data Export heading should not be visible
    await expect(page.getByRole('heading', { name: 'Data Export' })).not.toBeVisible({
      timeout: 15_000,
    })
  })
})
