import { test, expect } from '@playwright/test'
import { authStatePath, navigateToDetail } from './helpers'

const OLD_THESIS_ID = '00000000-0000-4000-d000-000000000010'
const RECENT_THESIS_ID = '00000000-0000-4000-d000-000000000011'
const ACTIVE_THESIS_ID = '00000000-0000-4000-d000-000000000012'
// Thesis 1 — always available, examiner has EXAMINER role on it
const EXAMINER_THESIS_ID = '00000000-0000-4000-d000-000000000001'

test.describe('Thesis Delete (Anonymize) - Admin', () => {
  test.use({ storageState: authStatePath('admin') })

  test.describe.configure({ mode: 'serial' })

  test('admin can anonymize old non-terminal thesis with state warning only', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /Historical Analysis of Compiler/i })
    const loaded = await navigateToDetail(page, `/theses/${OLD_THESIS_ID}`, heading, 30_000)
    expect(loaded).toBeTruthy()

    // Open Configuration accordion
    await page.getByText('Configuration').click()

    // Anonymize Thesis button should be visible for admin on non-anonymized thesis
    const deleteButton = page.getByRole('button', { name: 'Anonymize Thesis' })
    await expect(deleteButton).toBeVisible({ timeout: 5_000 })
    await deleteButton.click()

    // Modal should open with correct title
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 5_000 })
    await expect(dialog.getByRole('heading', { name: 'Anonymize Thesis' })).toBeVisible()

    // Old GRADED thesis: state warning (not terminal) but NO retention warning (expired)
    const alert = dialog.locator('.mantine-Alert-root')
    await expect(alert).toBeVisible({ timeout: 5_000 })
    await expect(alert.getByText(/GRADED/i)).toBeVisible()
    await expect(alert.getByText(/retention period/i)).not.toBeVisible({ timeout: 2_000 })

    // Confirmation text should mention anonymization, not deletion
    await expect(dialog.getByText(/anonymize this thesis/i)).toBeVisible()
    await expect(dialog.getByText(/This action cannot be undone/i)).toBeVisible()
    await expect(dialog.getByText(/structural thesis metadata is retained/i)).toBeVisible()

    // Cancel and Anonymize buttons should be present
    await expect(dialog.getByRole('button', { name: 'Cancel' })).toBeVisible()

    // Confirm anonymize
    await dialog.getByRole('button', { name: 'Anonymize Thesis' }).click()

    // Should redirect to /theses list after successful anonymization
    await expect(page).toHaveURL(/\/theses(?:\?|$)/, { timeout: 15_000 })
  })

  test('admin can anonymize recent thesis with retention warning', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /Machine Learning Approaches/i })
    const loaded = await navigateToDetail(page, `/theses/${RECENT_THESIS_ID}`, heading, 30_000)
    expect(loaded).toBeTruthy()

    await page.getByText('Configuration').click()

    const deleteButton = page.getByRole('button', { name: 'Anonymize Thesis' })
    await expect(deleteButton).toBeVisible({ timeout: 5_000 })
    await deleteButton.click()

    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // Recent finished thesis should show retention warning but NOT state warning
    const alert = dialog.locator('.mantine-Alert-root')
    await expect(alert).toBeVisible({ timeout: 5_000 })
    await expect(alert.getByText(/retention period/i)).toBeVisible()
    await expect(alert.getByText(/expires on/i)).toBeVisible()
    await expect(alert.getByText(/WRITING|GRADED|SUBMITTED/i)).not.toBeVisible({ timeout: 2_000 })

    // Confirm anonymize despite warning
    await dialog.getByRole('button', { name: 'Anonymize Thesis' }).click()

    await expect(page).toHaveURL(/\/theses(?:\?|$)/, { timeout: 15_000 })
  })

  test('admin can anonymize active thesis with state and retention warnings', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /Real-Time Anomaly Detection/i })
    const loaded = await navigateToDetail(page, `/theses/${ACTIVE_THESIS_ID}`, heading, 30_000)
    expect(loaded).toBeTruthy()

    await page.getByText('Configuration').click()

    const deleteButton = page.getByRole('button', { name: 'Anonymize Thesis' })
    await expect(deleteButton).toBeVisible({ timeout: 5_000 })
    await deleteButton.click()

    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // Active (WRITING) thesis should show state warning
    const alert = dialog.locator('.mantine-Alert-root')
    await expect(alert).toBeVisible({ timeout: 5_000 })
    await expect(alert.getByText(/WRITING/i)).toBeVisible()
    // Should also show retention warning since thesis is only 30 days old
    await expect(alert.getByText(/retention period/i)).toBeVisible()

    // Confirm anonymize despite warnings
    await dialog.getByRole('button', { name: 'Anonymize Thesis' }).click()

    await expect(page).toHaveURL(/\/theses(?:\?|$)/, { timeout: 15_000 })
  })
})

test.describe('Thesis Delete (Anonymize) - Modal Interactions', () => {
  test.use({ storageState: authStatePath('admin') })

  test('admin can cancel anonymization modal without effect', async ({ page }) => {
    // Use thesis 1 which won't be affected by serial delete tests
    const heading = page.getByRole('heading', { name: /Automated Code Review/i })
    const loaded = await navigateToDetail(page, `/theses/${EXAMINER_THESIS_ID}`, heading, 30_000)
    expect(loaded).toBeTruthy()

    await page.getByText('Configuration').click()

    const anonymizeButton = page.getByRole('button', { name: 'Anonymize Thesis' })
    await expect(anonymizeButton).toBeVisible({ timeout: 5_000 })
    await anonymizeButton.click()

    // Modal should open
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // Click Cancel
    await dialog.getByRole('button', { name: 'Cancel' }).click()

    // Modal should close
    await expect(dialog).not.toBeVisible({ timeout: 3_000 })

    // Should still be on the thesis page (not redirected)
    await expect(heading).toBeVisible()

    // Anonymize button should still be visible (thesis wasn't anonymized)
    await expect(anonymizeButton).toBeVisible()
  })

  test('anonymization modal closes via X button', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /Automated Code Review/i })
    const loaded = await navigateToDetail(page, `/theses/${EXAMINER_THESIS_ID}`, heading, 30_000)
    expect(loaded).toBeTruthy()

    await page.getByText('Configuration').click()

    const anonymizeButton = page.getByRole('button', { name: 'Anonymize Thesis' })
    await anonymizeButton.click()

    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // Close via the modal close button (X)
    await dialog.locator('button.mantine-Modal-close').click()

    // Modal should close and thesis should remain unchanged
    await expect(dialog).not.toBeVisible({ timeout: 3_000 })
    await expect(heading).toBeVisible()
  })
})

test.describe('Thesis Delete (Anonymize) - Non-Admin Restrictions', () => {
  test.use({ storageState: authStatePath('examiner') })

  test('examiner does not see Anonymize Thesis button', async ({ page }) => {
    // Use thesis 1 where examiner has EXAMINER role — not affected by admin delete tests
    const heading = page.getByRole('heading', { name: /Automated Code Review/i })
    const loaded = await navigateToDetail(page, `/theses/${EXAMINER_THESIS_ID}`, heading, 30_000)
    expect(loaded).toBeTruthy()

    // Open Configuration accordion
    await page.getByText('Configuration').click()

    // Examiner should see Update button (they have supervisor access) but NOT Anonymize Thesis
    await expect(page.getByRole('button', { name: 'Update' })).toBeVisible({ timeout: 5_000 })
    await expect(page.getByRole('button', { name: 'Anonymize Thesis' })).not.toBeVisible({
      timeout: 3_000,
    })
  })
})

test.describe('Thesis Delete (Anonymize) - Student Restrictions', () => {
  test.use({ storageState: authStatePath('student') })

  test('student does not see Anonymize Thesis button', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /Automated Code Review/i })
    const loaded = await navigateToDetail(page, `/theses/${EXAMINER_THESIS_ID}`, heading, 30_000)
    expect(loaded).toBeTruthy()

    // Students should not see the Configuration section's admin controls
    // Check if Configuration accordion is present at all
    const configSection = page.getByText('Configuration')
    const configVisible = await configSection.isVisible().catch(() => false)

    if (configVisible) {
      await configSection.click()
      // Student should NOT see Anonymize Thesis button
      await expect(page.getByRole('button', { name: 'Anonymize Thesis' })).not.toBeVisible({
        timeout: 3_000,
      })
    }
    // If Configuration section is not visible to students, that's also correct behavior
  })
})
