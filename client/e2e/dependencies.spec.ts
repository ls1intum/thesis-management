import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

// The Spring Boot server makes the actual OSV.dev / GitHub calls server-side, so they
// can't be intercepted from the browser. The page is designed so the SBOM table and
// the action buttons render as soon as the (instant) SBOM fetch resolves — vulnerability
// and version data fill in independently. We therefore assert structure and counts
// without waiting on the OSV round-trip.
//
// Note on selectors: plain "Server" / "Client" text matches the per-row source badges in
// the components table (hundreds of matches). We anchor on text that's unique to the page
// chrome — "Total components", "Showing N of M components", and the segmented-control
// labels that include their numeric counts ("Server (123)").

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

    // Action buttons — each is unique by accessible name.
    await expect(page.getByRole('button', { name: /refresh vulnerabilities/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /send email/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /server sbom/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /client sbom/i })).toBeVisible()

    // Summary section anchors. "Total components" is unique to the summary card.
    await expect(page.getByText('Total components')).toBeVisible()

    // Filter row controls. The text input is unique.
    await expect(page.getByPlaceholder(/search name, group, version/i)).toBeVisible()

    // Segmented control labels include their counts ("Server (123)"), so anchoring on the
    // count parenthesis disambiguates them from the per-row "Server"/"Client" badges in the
    // components table.
    await expect(page.getByText(/^All \(\d+\)$/)).toBeVisible()
    await expect(page.getByText(/^Server \(\d+\)$/)).toBeVisible()
    await expect(page.getByText(/^Client \(\d+\)$/)).toBeVisible()

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

    // Click the Server label of the SegmentedControl. Mantine's SegmentedControl renders
    // the actual radio inputs as visually hidden behind label elements, so we click the
    // visible label by its full "Server (n)" text rather than the role=radio input.
    await page.getByText(/^Server \(\d+\)$/).click()

    await expect
      .poll(async () => (await countsLine.textContent()) ?? '', { timeout: 10_000 })
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
