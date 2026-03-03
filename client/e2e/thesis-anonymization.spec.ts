import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo, navigateToDetail } from './helpers'

const EXPIRED_THESIS_ID = '00000000-0000-4000-d000-000000000008'
const ALREADY_ANONYMIZED_THESIS_ID = '00000000-0000-4000-d000-000000000009'
const RECENT_FINISHED_THESIS_ID = '00000000-0000-4000-d000-000000000004'
// Thesis 1 — always available, not anonymized, examiner has EXAMINER role on it
const NON_ANONYMIZED_THESIS_ID = '00000000-0000-4000-d000-000000000001'

test.describe('Thesis Anonymization - Admin Batch Operations', () => {
  test.use({ storageState: authStatePath('admin') })

  test.describe.configure({ mode: 'serial' })

  test('admin can trigger thesis anonymization from admin page', async ({ page }) => {
    await navigateTo(page, '/admin')

    await expect(page.getByRole('heading', { name: 'Administration' })).toBeVisible({
      timeout: 30_000,
    })

    // Verify the Thesis Anonymization section is present with description
    await expect(page.getByRole('heading', { name: 'Thesis Anonymization' })).toBeVisible()
    await expect(page.getByText(/Anonymize theses that have exceeded/i)).toBeVisible()
    await expect(page.getByText(/5-year legal retention period/i)).toBeVisible()

    const anonymizeButton = page.getByRole('button', { name: 'Run Anonymization' })
    await expect(anonymizeButton).toBeVisible()
    await anonymizeButton.click()

    // Thesis 8 (expired retention) should be anonymized
    await expect(page.getByText(/Anonymized \d+ expired thes(is|es)/)).toBeVisible({
      timeout: 15_000,
    })
  })

  test('second anonymization run finds no new theses (idempotent)', async ({ page }) => {
    await navigateTo(page, '/admin')

    await expect(page.getByRole('heading', { name: 'Administration' })).toBeVisible({
      timeout: 30_000,
    })

    const anonymizeButton = page.getByRole('button', { name: 'Run Anonymization' })
    await anonymizeButton.click()

    // After the first run already anonymized everything, second run should find nothing
    await expect(page.getByText('No expired theses found')).toBeVisible({ timeout: 15_000 })
  })

  test('batch-anonymized thesis shows banner on detail page', async ({ page }) => {
    // Thesis 8 was just anonymized by the batch run above
    await navigateTo(page, `/theses/${EXPIRED_THESIS_ID}`)

    const banner = page.getByText(/This thesis was anonymized on/i)
    await expect(banner).toBeVisible({ timeout: 30_000 })
    await expect(page.getByText(/per data retention policy/i)).toBeVisible()
    await expect(page.getByText(/permanently removed/i)).toBeVisible()
  })

  test('batch-anonymized thesis preserves structural data', async ({ page }) => {
    // Thesis 8 was batch-anonymized — structural fields should remain
    const heading = page.getByRole('heading', { name: /Legacy Data Processing Pipeline/i })
    const loaded = await navigateToDetail(page, `/theses/${EXPIRED_THESIS_ID}`, heading, 30_000)
    expect(loaded).toBeTruthy()

    // Title should still be visible
    await expect(heading).toBeVisible()

    // Anonymization banner should be present (confirming it was anonymized)
    await expect(page.getByText(/This thesis was anonymized on/i)).toBeVisible()
  })
})

test.describe('Thesis Anonymization - Pre-Anonymized Thesis Banner', () => {
  test.use({ storageState: authStatePath('admin') })

  test('pre-anonymized thesis shows banner with correct content', async ({ page }) => {
    await navigateTo(page, `/theses/${ALREADY_ANONYMIZED_THESIS_ID}`)

    // Verify the anonymization banner is displayed with correct content
    const banner = page.getByText(/This thesis was anonymized on/i)
    await expect(banner).toBeVisible({ timeout: 30_000 })
    await expect(page.getByText(/per data retention policy/i)).toBeVisible()
    await expect(page.getByText(/permanently removed/i)).toBeVisible()
  })

  test('anonymize button is hidden on already-anonymized thesis', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /Archived Research on Software/i })
    const loaded = await navigateToDetail(
      page,
      `/theses/${ALREADY_ANONYMIZED_THESIS_ID}`,
      heading,
      30_000,
    )
    expect(loaded).toBeTruthy()

    // Open Configuration accordion
    await page.getByText('Configuration').click()

    // Admin should see Update button but NOT Anonymize Thesis (already anonymized)
    await expect(page.getByRole('button', { name: 'Update' })).toBeVisible({ timeout: 5_000 })
    await expect(page.getByRole('button', { name: 'Anonymize Thesis' })).not.toBeVisible({
      timeout: 3_000,
    })
  })
})

test.describe('Thesis Anonymization - Non-Anonymized Thesis', () => {
  test.use({ storageState: authStatePath('admin') })

  test('recently finished thesis is not affected by anonymization', async ({ page }) => {
    // Thesis 4 finished 60 days ago — well within the 5-year retention period
    const heading = page.getByRole('heading', { name: /Systematic Monolith/i })
    const loaded = await navigateToDetail(
      page,
      `/theses/${RECENT_FINISHED_THESIS_ID}`,
      heading,
      30_000,
    )
    expect(loaded).toBeTruthy()

    // Should NOT show anonymization banner
    await expect(page.getByText(/This thesis was anonymized on/i)).not.toBeVisible({
      timeout: 3_000,
    })

    // Should still have its data intact (thesis title is visible)
    await expect(heading).toBeVisible()
  })

  test('non-anonymized thesis shows anonymize button for admin', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /Automated Code Review/i })
    const loaded = await navigateToDetail(
      page,
      `/theses/${NON_ANONYMIZED_THESIS_ID}`,
      heading,
      30_000,
    )
    expect(loaded).toBeTruthy()

    // Open Configuration accordion
    await page.getByText('Configuration').click()

    // Admin should see the Anonymize Thesis button on non-anonymized thesis
    await expect(page.getByRole('button', { name: 'Anonymize Thesis' })).toBeVisible({
      timeout: 5_000,
    })
  })
})

test.describe('Thesis Anonymization - Non-Admin Restrictions', () => {
  test.use({ storageState: authStatePath('student') })

  test('student cannot access admin page for batch anonymization', async ({ page }) => {
    await navigateTo(page, '/admin')

    await expect(page.getByRole('heading', { name: 'Administration' })).not.toBeVisible({
      timeout: 10_000,
    })
  })
})

test.describe('Thesis Anonymization - Supervisor Restrictions', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('supervisor cannot access admin page for batch anonymization', async ({ page }) => {
    await navigateTo(page, '/admin')

    await expect(page.getByRole('heading', { name: 'Administration' })).not.toBeVisible({
      timeout: 10_000,
    })
  })

  test('supervisor does not see anonymize button on thesis detail', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /Automated Code Review/i })
    const loaded = await navigateToDetail(
      page,
      `/theses/${NON_ANONYMIZED_THESIS_ID}`,
      heading,
      30_000,
    )
    expect(loaded).toBeTruthy()

    // Open Configuration accordion
    await page.getByText('Configuration').click()

    // Supervisor should see Update button but NOT Anonymize Thesis
    await expect(page.getByRole('button', { name: 'Update' })).toBeVisible({ timeout: 5_000 })
    await expect(page.getByRole('button', { name: 'Anonymize Thesis' })).not.toBeVisible({
      timeout: 3_000,
    })
  })
})
