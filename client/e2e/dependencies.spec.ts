import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

// The Spring Boot server makes the actual OSV.dev / GitHub calls server-side, so they
// can't be intercepted from the browser. The page is designed so the SBOM table and
// the action buttons render as soon as the (instant) SBOM fetch resolves — vulnerability
// and version data fill in independently. We therefore assert structure and counts
// without waiting on the OSV round-trip.

test.describe('Admin Dependencies Page', () => {
  test.use({ storageState: authStatePath('admin') })

  test('admin sees the Dependencies link in the navigation', async ({ page }) => {
    await navigateTo(page, '/admin')
    await expect(page.getByRole('heading', { name: 'Administration' })).toBeVisible({
      timeout: 30_000,
    })

    const navLink = page.getByRole('link', { name: 'Dependencies' })
    await expect(navLink).toBeVisible({ timeout: 10_000 })
  })

  test('admin can open /admin/dependencies and see the header, summary cards and filters', async ({
    page,
  }) => {
    test.setTimeout(60_000)
    await navigateTo(page, '/admin/dependencies')

    await expect(page.getByRole('heading', { name: /^Dependencies$/ })).toBeVisible({
      timeout: 30_000,
    })

    // Action buttons
    await expect(page.getByRole('button', { name: /refresh vulnerabilities/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /send email/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /server sbom/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /client sbom/i })).toBeVisible()

    // Summary cards (labels are stable regardless of bundled SBOM content)
    await expect(page.getByText('Total components')).toBeVisible()
    await expect(page.getByText('Server', { exact: true })).toBeVisible()
    await expect(page.getByText('Client', { exact: true })).toBeVisible()
    await expect(page.getByText('Vulnerabilities', { exact: true })).toBeVisible()

    // Filter row controls
    await expect(page.getByPlaceholder(/search name, group, version/i)).toBeVisible()
    await expect(page.getByRole('radio', { name: /^All/ })).toBeVisible()
    await expect(page.getByRole('radio', { name: /^Server/ })).toBeVisible()
    await expect(page.getByRole('radio', { name: /^Client/ })).toBeVisible()

    // The "Showing N of M components" line appears once SBOM data is loaded.
    await expect(page.getByText(/Showing \d+ of \d+ components/)).toBeVisible({ timeout: 30_000 })
  })

  test('server SBOM download produces a JSON file', async ({ page }) => {
    test.setTimeout(60_000)
    await navigateTo(page, '/admin/dependencies')
    await expect(page.getByRole('heading', { name: /^Dependencies$/ })).toBeVisible({
      timeout: 30_000,
    })

    const downloadPromise = page.waitForEvent('download')
    await page.getByRole('button', { name: /server sbom/i }).click()
    const download = await downloadPromise

    expect(download.suggestedFilename()).toBe('server-sbom.json')
  })

  test('source filter switches the visible counts', async ({ page }) => {
    test.setTimeout(60_000)
    await navigateTo(page, '/admin/dependencies')
    await expect(page.getByRole('heading', { name: /^Dependencies$/ })).toBeVisible({
      timeout: 30_000,
    })

    // Wait for SBOM to load and the counts line to appear.
    const countsLine = page.getByText(/Showing \d+ of \d+ components/)
    await expect(countsLine).toBeVisible({ timeout: 30_000 })
    const beforeText = await countsLine.textContent()

    // Switch to Server-only view and confirm the counts line updates.
    await page.getByRole('radio', { name: /^Server/ }).click()
    await expect
      .poll(async () => (await countsLine.textContent()) ?? '', { timeout: 5_000 })
      .not.toBe(beforeText)
  })
})

test.describe('Admin Dependencies Page - non-admin', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('supervisor sees the 403 page on /admin/dependencies', async ({ page }) => {
    await navigateTo(page, '/admin/dependencies')

    // AuthenticatedArea renders a "403 - Unauthorized" heading for non-admin users.
    await expect(page.getByRole('heading', { name: /403 - Unauthorized/i })).toBeVisible({
      timeout: 15_000,
    })
    // And the dependency page itself must not render.
    await expect(page.getByRole('heading', { name: /^Dependencies$/ })).not.toBeVisible()
  })
})
